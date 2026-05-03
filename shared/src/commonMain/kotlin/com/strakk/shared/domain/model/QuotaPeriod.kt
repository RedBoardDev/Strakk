package com.strakk.shared.domain.model

enum class QuotaPeriod(val key: String) {
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    ;

    companion object {
        fun fromKey(key: String): QuotaPeriod = entries.find { it.key == key } ?: MONTH
    }
}
