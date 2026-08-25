package org.dba.datafx

data class UcidDetails(val ucid: String, val exportDate: String, val fileName: String)

data class ProductCount(val product: String, val count: Int)

data class AggregationResult(val product: String?, val count: Int)