package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.NovaCartPrompts
import com.example.data.ai.NovaCartResponseParser
import com.example.data.repository.NovaCartRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches NovaCart`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NovaCart", appName)
    }

    @Test
    fun `response parser extracts product opportunity and strips json markers`() {
        val raw = """
            ## 1. Executive Summary
            Strong Indian demand for Silicone Sink Splash Guard under ₹500.

            ${NovaCartPrompts.START_MARKER}
            [
              {
                "name": "Silicone Sink Splash Guard",
                "category": "Home, Kitchen & Living",
                "discoverySource": "Amazon.in / Instagram Reels / IndiaMART",
                "productLinks": "https://amazon.in/dp/example",
                "supplierLinks": "https://indiamart.com/proddetail/example",
                "supplierPriceInr": 145,
                "sellingPriceInr": 499,
                "grossMarginPct": 70.9,
                "netProfitEstimateInr": 132,
                "demandIndicators": "High Google Trends India & Reels traction",
                "competitorSummary": "Medium saturation, ₹449-₹699 price band",
                "bestBusinessModel": "Private Label for lower RTO",
                "score": 86,
                "status": "Promising",
                "trendDevelopment": "Growing",
                "finalRecommendation": "Order 20 samples from Rajkot manufacturer"
              }
            ]
            ${NovaCartPrompts.END_MARKER}
        """.trimIndent()

        val parsed = NovaCartResponseParser.parse(raw, "Gemini • gemini-3.8-flash")
        assertFalse(parsed.displayMarkdown.contains(NovaCartPrompts.START_MARKER))
        assertTrue(parsed.displayMarkdown.contains("Executive Summary"))
        assertEquals(1, parsed.products.size)
        val item = parsed.products.first()
        assertEquals("Silicone Sink Splash Guard", item.name)
        assertEquals("Home, Kitchen & Living", item.category)
        assertEquals(145.0, item.supplierPriceInr, 0.01)
        assertEquals(86, item.score)
        assertEquals("Promising", item.status)
    }

    @Test
    fun `csv export includes all Part C7 headers and escapes values`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = NovaCartRepository(context)
        val parsed = NovaCartResponseParser.parse(
            """
            Report text
            ${NovaCartPrompts.START_MARKER}
            [{"name":"Gadget, \"Pro\"","category":"Electronics & Gadgets","supplierPriceInr":220,"sellingPriceInr":699,"netProfitEstimateInr":160,"score":82}]
            ${NovaCartPrompts.END_MARKER}
            """.trimIndent(),
            "Perplexity • sonar-pro"
        )
        val csv = repo.buildCsvExport(parsed.products)
        assertTrue(csv.startsWith("id,name,category,discoveryDate,discoverySource"))
        assertTrue(csv.contains("rawReport"))
        assertTrue(csv.contains("\"Gadget, \"\"Pro\"\"\""))
    }

    @Test
    fun `wake phrase matcher detects hello bhai variants and extracts follow-up`() {
        val generic = com.example.data.voice.WakePhraseMatcher.evaluate("Hello bhai batao product kya hai")
        assertTrue(generic.matched)
        assertTrue(generic.followUpQuery.contains("₹500"))

        val followUp = com.example.data.voice.WakePhraseMatcher.evaluate("Hello bhai kitchen gadgets under 300")
        assertTrue(followUp.matched)
        assertEquals("kitchen gadgets under 300", followUp.followUpQuery)

        val nonWake = com.example.data.voice.WakePhraseMatcher.evaluate("check amazon price today")
        assertFalse(nonWake.matched)
    }

    @Test
    fun `stripMarkdownForSpeech cleans formatting and currency symbol`() {
        val cleaned = com.example.data.voice.NovaCartVoiceManager.stripMarkdownForSpeech(
            "## **Voice Summary**: Cost is ₹250 on [IndiaMART](https://indiamart.com)"
        )
        assertFalse(cleaned.contains("#"))
        assertFalse(cleaned.contains("*"))
        assertFalse(cleaned.contains("https://"))
        assertTrue(cleaned.contains("rupees 250"))
    }
}
