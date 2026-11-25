package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.ProductOpportunity
import com.example.data.model.WebCitation
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "product_opportunities")
data class ProductOpportunityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val discoveryDate: String,
    val discoverySource: String,
    val productLinks: String,
    val supplierLinks: String,
    val supplierPriceInr: Double,
    val sellingPriceInr: Double,
    val grossMarginPct: Double,
    val netProfitEstimateInr: Double,
    val demandIndicators: String,
    val competitorSummary: String,
    val bestBusinessModel: String,
    val score: Int,
    val status: String,
    val trendDevelopment: String,
    val finalRecommendation: String,
    val rawReport: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val providerLabel: String,
    val citationsJson: String,
    val extractedProductsJson: String,
    val timestampMillis: Long
)

@Dao
interface ProductOpportunityDao {
    @Query("SELECT * FROM product_opportunities ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<ProductOpportunityEntity>>

    @Query("SELECT id FROM product_opportunities")
    fun observeSavedIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProductOpportunityEntity)

    @Query("UPDATE product_opportunities SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM product_opportunities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM product_opportunities")
    suspend fun deleteAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
    fun observeAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(entity: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}

@Database(
    entities = [ProductOpportunityEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NovaCartDatabase : RoomDatabase() {
    abstract fun productDao(): ProductOpportunityDao
    abstract fun chatDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: NovaCartDatabase? = null

        fun getInstance(context: Context): NovaCartDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovaCartDatabase::class.java,
                    "novacart_intelligence.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}

fun ProductOpportunityEntity.toDomain(): ProductOpportunity = ProductOpportunity(
    id = id,
    name = name,
    category = category,
    discoveryDate = discoveryDate,
    discoverySource = discoverySource,
    productLinks = productLinks,
    supplierLinks = supplierLinks,
    supplierPriceInr = supplierPriceInr,
    sellingPriceInr = sellingPriceInr,
    grossMarginPct = grossMarginPct,
    netProfitEstimateInr = netProfitEstimateInr,
    demandIndicators = demandIndicators,
    competitorSummary = competitorSummary,
    bestBusinessModel = bestBusinessModel,
    score = score,
    status = status,
    trendDevelopment = trendDevelopment,
    finalRecommendation = finalRecommendation,
    rawReport = rawReport
)

fun ProductOpportunity.toEntity(): ProductOpportunityEntity = ProductOpportunityEntity(
    id = id,
    name = name,
    category = category,
    discoveryDate = discoveryDate,
    discoverySource = discoverySource,
    productLinks = productLinks,
    supplierLinks = supplierLinks,
    supplierPriceInr = supplierPriceInr,
    sellingPriceInr = sellingPriceInr,
    grossMarginPct = grossMarginPct,
    netProfitEstimateInr = netProfitEstimateInr,
    demandIndicators = demandIndicators,
    competitorSummary = competitorSummary,
    bestBusinessModel = bestBusinessModel,
    score = score,
    status = status,
    trendDevelopment = trendDevelopment,
    finalRecommendation = finalRecommendation,
    rawReport = rawReport
)

fun ChatMessageEntity.toDomain(): ChatMessage {
    val citations = mutableListOf<WebCitation>()
    try {
        val arr = JSONArray(citationsJson)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            citations += WebCitation(
                title = obj.optString("title", "Source"),
                uri = obj.optString("uri", "")
            )
        }
    } catch (_: Exception) {
    }

    val products = mutableListOf<ProductOpportunity>()
    try {
        val arr = JSONArray(extractedProductsJson)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            products += ProductOpportunity(
                id = obj.optString("id"),
                name = obj.optString("name"),
                category = obj.optString("category"),
                discoveryDate = obj.optString("discoveryDate"),
                discoverySource = obj.optString("discoverySource"),
                productLinks = obj.optString("productLinks"),
                supplierLinks = obj.optString("supplierLinks"),
                supplierPriceInr = obj.optDouble("supplierPriceInr", 0.0),
                sellingPriceInr = obj.optDouble("sellingPriceInr", 0.0),
                grossMarginPct = obj.optDouble("grossMarginPct", 0.0),
                netProfitEstimateInr = obj.optDouble("netProfitEstimateInr", 0.0),
                demandIndicators = obj.optString("demandIndicators"),
                competitorSummary = obj.optString("competitorSummary"),
                bestBusinessModel = obj.optString("bestBusinessModel"),
                score = obj.optInt("score", 70),
                status = obj.optString("status", "Newly discovered"),
                trendDevelopment = obj.optString("trendDevelopment", "Growing"),
                finalRecommendation = obj.optString("finalRecommendation"),
                rawReport = obj.optString("rawReport")
            )
        }
    } catch (_: Exception) {
    }

    return ChatMessage(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        providerLabel = providerLabel,
        citations = citations,
        extractedProducts = products,
        timestampMillis = timestampMillis
    )
}

fun ChatMessage.toEntity(): ChatMessageEntity {
    val citationsArr = JSONArray().apply {
        citations.forEach { c ->
            put(JSONObject().apply {
                put("title", c.title)
                put("uri", c.uri)
            })
        }
    }
    val productsArr = JSONArray().apply {
        extractedProducts.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("category", p.category)
                put("discoveryDate", p.discoveryDate)
                put("discoverySource", p.discoverySource)
                put("productLinks", p.productLinks)
                put("supplierLinks", p.supplierLinks)
                put("supplierPriceInr", p.supplierPriceInr)
                put("sellingPriceInr", p.sellingPriceInr)
                put("grossMarginPct", p.grossMarginPct)
                put("netProfitEstimateInr", p.netProfitEstimateInr)
                put("demandIndicators", p.demandIndicators)
                put("competitorSummary", p.competitorSummary)
                put("bestBusinessModel", p.bestBusinessModel)
                put("score", p.score)
                put("status", p.status)
                put("trendDevelopment", p.trendDevelopment)
                put("finalRecommendation", p.finalRecommendation)
                put("rawReport", p.rawReport)
            })
        }
    }
    return ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        providerLabel = providerLabel,
        citationsJson = citationsArr.toString(),
        extractedProductsJson = productsArr.toString(),
        timestampMillis = timestampMillis
    )
}
