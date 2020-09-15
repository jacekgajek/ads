package com.jacekgajek.ads

import com.opencsv.CSVReader
import cz.jirutka.rsql.parser.RSQLParserException
import io.github.perplexhub.rsql.RSQLJPASupport
import org.nfunk.jep.JEP
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.FileReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.annotation.PostConstruct
import javax.persistence.EntityManager
import javax.persistence.PersistenceException
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Selection


@Service
class AdsService(private val repo: AdsRepository,
                 @Value("\${com.jacekgajek.ads.initfile}") private val initFile: String,
                 private val en: EntityManager) {
    private val log = LoggerFactory.getLogger(AdsService::class.java)

    data class QueryParams(val select: List<Fields>,
                           val rsqlFilter: String = "",
                           val groupBy: List<Fields>,
                           val sum: List<Fields>,
                           val avg: List<Fields>,
                           val max: List<Fields>,
                           val min: List<Fields>,
                           val count: List<Fields>,
                           val custom: List<String>
    )

    @Suppress("EnumEntryName")
    enum class Fields {
        clicks, impressions, campaign, dataSource, daily;
        companion object {
            fun valueOfCaseInsensitive(str: String) =
                    values().find { it.name.equals(str, true) } ?: throw IllegalArgumentException("No field named $str")
        }
    }


    fun query(p: QueryParams): List<Any> {
        val criteriaBuilder = en.criteriaBuilder

        val builder = QueryBuilder(p, criteriaBuilder)
        val (query, aliases) = builder.build()

        val queryResult: List<Array<Any>>?
        try {
            queryResult = en.createQuery(query).resultList
        } catch (ex: PersistenceException) {
            throw IllegalArgumentException("There are errors in query. Details: ${ex.message}, ${ex.cause?.message}, ${ex.cause?.cause?.message}", ex)
        }
        val resultList = wrapIfNecessary(queryResult)
                .map {
                    row -> mutableMapOf(
                        *row.mapIndexed { index, data -> Pair(aliases[index], data) }
                                .toTypedArray())
                }
        appendCustomExpressions(resultList, p.custom)
        return resultList
    }

    /**
     * Custom expressions are calculated outside database => worse performance
     */
    private fun appendCustomExpressions(data: List<MutableMap<String, Any?>>, custom: List<String>) {
        if (custom.isEmpty()) return
        data.forEach { row ->
            val jep = JEP()
            row.forEach { (key, value) -> jep.addVariable(key, value) }
            val results = custom.map { expr ->
                jep.parseExpression(expr)
                val res: Any = if (jep.hasError()) jep.errorInfo else jep.value
                Pair(expr, res)
            }
            // using mutability in post-processing for performance reasons
            row.putAll(results)
        }
    }

    private fun wrapIfNecessary(resultList: List<Any>): List<Array<*>> {
        @Suppress("UNCHECKED_CAST")
        return when {
            resultList.isEmpty() -> listOf()
            resultList[0] is Array<*> -> resultList as List<Array<*>>
            else -> resultList.map { listOf(it).toTypedArray() }
        }
    }

    @PostConstruct
    private fun loadSampleData() {
        // Datasource,Campaign,Daily,Clicks,Impressions
        if (repo.count() == 0L) {
            log.info("Database empty, loading sample data")
            CSVReader(FileReader(initFile)).use { reader ->
                generateSequence(reader::readNext)
                        .drop(1)
                        .map { row ->
                            AdRecord(
                                    dataSource = row[0],
                                    campaign = row[1],
                                    daily = LocalDate.parse(row[2], DateTimeFormatter.ofPattern("MM/dd/uu")),
                                    clicks = row[3].toInt(),
                                    impressions = row[4].toInt()
                            )
                        }
                        .forEach { repo.save(it) }
            }
            log.info("Sample data loaded successfully")
        }
    }
}

/**
 * Interprets parameters in QueryParams and generates a CriteriaQuery.
 */
private class QueryBuilder(private val p: AdsService.QueryParams, private val criteriaBuilder: CriteriaBuilder) {
    private var query = criteriaBuilder.createQuery(Array<Any>::class.java)
    private var built = false
    private val root = query.from(AdRecord::class.java)

    fun build(): Pair<CriteriaQuery<Array<Any>>, List<String>> {
        buildSelections()
        buildFilters()
        query = query.groupBy(p.groupBy.map { root.get<Any>(it.name) })
        built = true
        return Pair(query, extractAliases())
    }

    private fun buildSelections() {
        val selections = mutableListOf<Selection<*>>()

        selections.addAll(p.select.map { root.get<Any>(it.name).alias(it.name) })

        selections.addAll(p.sum.map { criteriaBuilder.sum(root[it.name]).alias("sum_${it.name}") })
        selections.addAll(p.avg.map { criteriaBuilder.avg(root[it.name]).alias("avg_${it.name}") })
        selections.addAll(p.min.map { criteriaBuilder.min(root[it.name]).alias("min_${it.name}") })
        selections.addAll(p.max.map { criteriaBuilder.max(root[it.name]).alias("max_${it.name}") })
        selections.addAll(p.count.map { criteriaBuilder.count(root.get<Any>(it.name)).alias("count_${it.name}") })

        if (selections.isEmpty()) {
            throw IllegalArgumentException("You have to specify at least one selection")
        }

        query = query.multiselect(selections)
    }

    private fun buildFilters() {
        if (p.rsqlFilter.isNotEmpty()) {
            try {
                val predicate = RSQLJPASupport.toSpecification<AdRecord>(fixCasing(p.rsqlFilter)).toPredicate(root, query, criteriaBuilder)
                query = query.where(predicate)
            } catch (ex: RSQLParserException) {
                throw IllegalArgumentException("Filter expression invalid: ${ex.message}")
            }
        }
    }

    private fun extractAliases(): List<String> {
        return when {
            query.selection.isCompoundSelection -> query.selection.compoundSelectionItems.map { it.alias }
            else -> listOf(query.selection.alias)
        }
    }

    /**
     * User can put either "dataSource", "DataSource", etc. in the filter expression - we don't want to care about it.
     */
    private fun fixCasing(rsqlFilter: String): String {
        return AdsService.Fields.values().fold(rsqlFilter) {
            filter: String, f: AdsService.Fields -> filter.replace(f.name, f.name, true)
        }
    }
}
