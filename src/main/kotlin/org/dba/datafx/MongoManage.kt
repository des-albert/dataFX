package org.dba.datafx

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.slf4j.LoggerFactory

object MongoManage {
    private var client: MongoClient? = null

    private const val DATABASE_NAME = "HPC-AI"
    private var database: MongoDatabase? = null
    lateinit var collection: MongoCollection<Config>

    private val logger = LoggerFactory.getLogger(MongoManage::class.java)

    fun connect() {
        if (client != null) {
            return
        }
        try {
            logger.info("Connecting to MongoDB at mongodb://localhost:27017")
            client = MongoClient.create("mongodb://localhost:27017")
            database = org.dba.datafx.MongoManage.client?.getDatabase(DATABASE_NAME)

        } catch (e: Exception) {
            logger.error("Failed to connect to MongoDB: ${e.message}")
        }
    }

    fun select(year: String) {
        try {
            collection = database?.getCollection<Config>("configs$year")
                ?: throw IllegalStateException("MongoDB is not connected. Call MongoManage.connect() first.")
        } catch (e: Exception) {
            logger.error("Failed to select collection: ${e.message}")
        }
    }

}