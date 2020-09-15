package com.jacekgajek.ads

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1")
class AdsController(private val service: AdsService) {

    @GetMapping("/ads")
    fun find(@RequestParam(required = false) select: List<String>?,
             @RequestParam(required = false, defaultValue = "") filter: String,
             @RequestParam(required = false) groupBy: List<String>?,
             @RequestParam(required = false) sum: List<String>?,
             @RequestParam(required = false) max: List<String>?,
             @RequestParam(required = false) min: List<String>?,
             @RequestParam(required = false) count: List<String>?,
             @RequestParam(required = false) avg: List<String>?,
            @RequestParam(required = false) custom: List<String>?
    ): ResponseEntity<List<Any>> {
        try {
            val convert = {
                list: List<String>? -> list?.map { AdsService.Fields.valueOfCaseInsensitive(it) }.orEmpty()
            }
            val result =service.query(
                    AdsService.QueryParams(
                            select = convert(select),
                            rsqlFilter = filter,
                            groupBy = convert(groupBy),
                            sum = convert(sum),
                            avg = convert(avg),
                            min = convert(min),
                            count = convert(count),
                            max = convert(max),
                            custom = custom.orEmpty()
                    )
            )
            return ResponseEntity.ok(result)
        }
        catch (ex: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(listOf(ex.message ?: "other error"))
        }
    }


}
