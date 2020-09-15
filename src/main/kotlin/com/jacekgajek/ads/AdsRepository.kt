package com.jacekgajek.ads

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.NotNull

@Entity
data class AdRecord(
        @Id val id: UUID = UUID.randomUUID(),
        @NotNull val campaign: String,
        @NotNull val dataSource: String,
        @NotNull val daily: LocalDate,
        val clicks: Int,
        val impressions: Int)

interface AdsRepository : JpaRepository<AdRecord, UUID> {
}

