package com.jacekgajek.ads

import org.hibernate.query.criteria.internal.predicate.ComparisonPredicate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import javax.persistence.EntityManager

@SpringBootTest
internal class QueryBuilderTest {

    @Autowired
    lateinit var em : EntityManager

    @Test
    fun noSelection() {
        val p = AdsService.QueryParams(
                select = listOf(),
                rsqlFilter = "",
                groupBy = listOf(),
                sum = listOf(),
                avg = listOf(),
                min = listOf(),
                count = listOf(),
                max = listOf(),
                custom = listOf(),
                exclude = listOf()
        )
        val builder = QueryBuilder(p, em.criteriaBuilder)

        assertThrows<IllegalArgumentException>("You have to specify at least one selection.") { builder.build() }
    }

    @Test
    fun selectOne() {
        val p = AdsService.QueryParams(
                select = listOf(AdsService.Fields.campaign),
                rsqlFilter = "",
                groupBy = listOf(),
                sum = listOf(),
                avg = listOf(),
                min = listOf(),
                count = listOf(),
                max = listOf(),
                custom = listOf(),
                exclude = listOf()
        )
        val builder = QueryBuilder(p, em.criteriaBuilder)
        val (query, aliases) = builder.build()
        assertEquals(1, aliases.size)
        assertEquals("campaign", aliases[0])
        assertEquals(1, query.selection.compoundSelectionItems.size)
        assertEquals("campaign", query.selection.compoundSelectionItems[0].alias)
    }

    @Test
    fun filter() {
        val p = AdsService.QueryParams(
                select = listOf(AdsService.Fields.campaign),
                rsqlFilter = "daily>'2020-01-01'",
                groupBy = listOf(),
                sum = listOf(),
                avg = listOf(),
                min = listOf(),
                count = listOf(),
                max = listOf(),
                custom = listOf(),
                exclude = listOf()
        )
        val builder = QueryBuilder(p, em.criteriaBuilder)
        val (query, _) = builder.build()
        assertEquals(1, query.restriction.expressions.size)
        assert(query.restriction.expressions[0] is ComparisonPredicate)
        assertEquals(ComparisonPredicate.ComparisonOperator.GREATER_THAN, (query.restriction.expressions[0] as ComparisonPredicate).comparisonOperator)
    }

    @Test
    fun group() {
        val p = AdsService.QueryParams(
                select = listOf(AdsService.Fields.dataSource),
                rsqlFilter = "",
                groupBy = listOf(AdsService.Fields.dataSource),
                sum = listOf(AdsService.Fields.clicks),
                avg = listOf(),
                min = listOf(),
                count = listOf(),
                max = listOf(),
                custom = listOf(),
                exclude = listOf()
        )
        val builder = QueryBuilder(p, em.criteriaBuilder)
        val (query, aliases) = builder.build()
        assertEquals(2, aliases.size)
        assertEquals("dataSource", aliases[0])
        assertEquals("sum_clicks", aliases[1])
        assertEquals(1, query.groupList.size)
        assertEquals("dataSource", query.groupList[0].alias)
    }

}
