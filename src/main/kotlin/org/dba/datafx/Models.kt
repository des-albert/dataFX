package org.dba.datafx

data class UcidDetails(val ucid: String, val exportDate: String, val fileName: String)

data class ProductCount(val product: String, val count: Int)

data class ProductOpportunity(val ope: String, val customer: String, val ucid: String, val exportDate: String)

data class AggregationResult(val product: String?, val count: Int)