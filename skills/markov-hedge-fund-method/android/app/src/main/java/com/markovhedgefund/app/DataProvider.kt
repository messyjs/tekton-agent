package com.markovhedgefund.app

/**
 * Abstract data provider. Each source implements fetchClose() to return
 * parallel arrays of ISO dates and closing prices.
 */
interface DataProvider {
    val name: String
    val category: String            // "Stocks & ETFs", "Crypto", "Forex & Macro"
    val description: String         // One-liner for the settings screen
    val requiresApiKey: Boolean
    val apiKeyHint: String          // Help text for where to get a key
    val vpnRequired: Boolean        // True if the API is geo-blocked
    val vpnWarning: String         // Warning message if VPN is needed
    val tickerFormat: String       // e.g. "SPY, AAPL, BTC-USD" or "BTCUSDT"

    suspend fun fetchClose(symbol: String, years: Int): PriceData

    data class PriceData(
        val source: String,
        val dates: List<String>,
        val closes: DoubleArray,
        val dateStart: String,
        val dateEnd: String
    )
}