package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object CoreUtils {
    val BD_DISTRICTS = listOf(
        "Dhaka", "Chattogram", "Gazipur", "Narayanganj", "Sylhet", "Rajshahi", "Khulna",
        "Barishal", "Rangpur", "Mymensingh", "Cumilla", "Bogura", "Cox's Bazar", "Jessore"
    )

    private val EN_TO_BN = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )

    private val BN_TO_EN = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    fun toEnglishDigits(str: String?): String {
        if (str.isNullOrEmpty()) return ""
        val sb = StringBuilder()
        for (c in str) {
            sb.append(BN_TO_EN[c] ?: c)
        }
        return sb.toString()
    }

    fun formatDigits(strOrNum: Any?, useBn: Boolean): String {
        val s = strOrNum?.toString() ?: ""
        if (!useBn) return toEnglishDigits(s)
        val sb = StringBuilder()
        for (c in s) {
            sb.append(EN_TO_BN[c] ?: c)
        }
        return sb.toString()
    }

    fun formatBDT(amount: Int, useBn: Boolean): String {
        val nf = NumberFormat.getIntegerInstance(Locale("en", "IN"))
        val formatted = nf.format(kotlin.math.abs(amount))
        val sign = if (amount < 0) "-" else ""
        return "${sign}৳${formatDigits(formatted, useBn)}"
    }

    fun normalizeBDPhone(raw: String): String {
        val ascii = toEnglishDigits(raw).replace(Regex("[\\s\\-().]"), "")
        if (ascii.startsWith("+880")) return "0" + ascii.substring(4)
        if (ascii.startsWith("880") && ascii.length == 13) return "0" + ascii.substring(3)
        return ascii
    }

    fun isValidBDPhone(raw: String): Boolean {
        val normalized = normalizeBDPhone(raw)
        return Regex("^01[3-9]\\d{8}$").matches(normalized)
    }

    fun parseIntSafe(raw: String?): Int {
        if (raw.isNullOrEmpty()) return 0
        val clean = toEnglishDigits(raw).replace(Regex("[^0-9]"), "")
        return clean.toIntOrNull() ?: 0
    }

    fun generateOrderId(): String {
        val now = Date()
        val yy = SimpleDateFormat("yy", Locale.US).format(now)
        val mm = SimpleDateFormat("MM", Locale.US).format(now)
        val dd = SimpleDateFormat("dd", Locale.US).format(now)
        val rand = Random.nextInt(1000, 9999)
        return "FA-$yy$mm$dd-$rand"
    }

    data class FinancialResult(
        val subtotal: Int,
        val totalBill: Int,
        val outstanding: Int,
        val codAmount: Int,
        val paymentStatus: String
    )

    fun calculateFinancials(
        orderType: String,
        items: List<Pair<Int, Int>>, // unitPrice to quantity
        deliveryCharge: Int,
        additionalCharge: Int = 0,
        discount: Int = 0,
        netPaid: Int = 0
    ): FinancialResult {
        var subtotal = 0
        for ((unitPrice, quantity) in items) {
            subtotal += maxOf(0, unitPrice) * maxOf(1, quantity)
        }

        val rawTotal = if (orderType == "Free") {
            0
        } else {
            subtotal + maxOf(0, deliveryCharge) + maxOf(0, additionalCharge) - maxOf(0, discount)
        }
        val totalBill = maxOf(0, rawTotal)
        val outstanding = totalBill - maxOf(0, netPaid)
        val codAmount = maxOf(0, outstanding)

        val paymentStatus = when {
            orderType == "Free" || (totalBill > 0 && netPaid >= totalBill) -> "Paid"
            netPaid > 0 && netPaid < totalBill -> "Partially Paid"
            else -> "Unpaid"
        }

        return FinancialResult(subtotal, totalBill, outstanding, codAmount, paymentStatus)
    }

    fun getDeliveryScheduleState(earliestDispatch: String?, requestedDelivery: String?): String {
        return if (!earliestDispatch.isNullOrBlank() || !requestedDelivery.isNullOrBlank()) {
            "Scheduled"
        } else {
            "Unscheduled"
        }
    }
}
