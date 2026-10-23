package com.example.util

data class ParsedField<T>(
    val value: T,
    val status: String // "Detected" | "Ambiguous" | "Not found"
)

data class SmartPasteDraft(
    val customerName: ParsedField<String>,
    val primaryPhone: ParsedField<String>,
    val fullAddress: ParsedField<String>,
    val district: ParsedField<String>,
    val productName: ParsedField<String>,
    val size: ParsedField<String>,
    val quantity: ParsedField<Int>,
    val unitPrice: ParsedField<Int>,
    val deliveryCharge: ParsedField<Int>,
    val advancePaid: ParsedField<Int>,
    val deliveryInstructions: ParsedField<String>,
    val rawText: String
)

object SmartPasteParser {
    fun parseOrderMessageOffline(rawText: String): SmartPasteDraft {
        val normalizedDigits = CoreUtils.toEnglishDigits(rawText)
        val lines = rawText.split(Regex("\\r?\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // 1. Extract BD Mobile Number
        val phonePattern = Regex("(?:\\+?880|0)1[3-9]\\d[\\d\\s\\-]{7,10}")
        val phoneMatches = phonePattern.findAll(normalizedDigits).map { it.value }.toList()
        val cleanedPhone = if (phoneMatches.isNotEmpty()) CoreUtils.normalizeBDPhone(phoneMatches[0]) else ""

        // 2. Extract Size
        val sizePattern = Regex("(?:size|সাইজ|বডি)\\s*[:\\-=]?\\s*(36|38|40|42|44|46|48)", RegexOption.IGNORE_CASE)
        val sizeMatch = sizePattern.find(normalizedDigits)
        val detectedSize = sizeMatch?.groupValues?.get(1) ?: "40"

        // 3. Extract Price & Delivery Charge
        val pricePattern = Regex("(?:price|দাম|মূল্য|বিল|tk|taka|৳)\\s*[:\\-=]?\\s*(\\d{3,5})", RegexOption.IGNORE_CASE)
        val priceMatch = pricePattern.find(normalizedDigits)
        val priceVal = priceMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val deliveryPattern = Regex("(?:delivery|ডেলিভারি)\\s*[:\\-=]?\\s*(\\d{2,3})", RegexOption.IGNORE_CASE)
        val deliveryMatch = deliveryPattern.find(normalizedDigits)

        val advancePattern = Regex("(?:advance|অ্যাডভান্স|অগ্রিম|বিকাশ|bkash)\\s*[:\\-=]?\\s*(\\d{2,5})", RegexOption.IGNORE_CASE)
        val advanceMatch = advancePattern.find(normalizedDigits)
        val advanceVal = advanceMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        // 4. Extract Name, Address, Product by labels or line heuristics
        var nameVal = ""
        var addressVal = ""
        var productVal = ""
        var noteVal = ""

        val nameRegex = Regex("^(name|নাম)\\s*[:\\-]", RegexOption.IGNORE_CASE)
        val addressRegex = Regex("^(address|ঠিকানা)\\s*[:\\-]", RegexOption.IGNORE_CASE)
        val productRegex = Regex("^(product|dress|ড্রেস|পণ্য)\\s*[:\\-]", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val lower = line.lowercase()
            if (nameRegex.containsMatchIn(line)) {
                nameVal = line.replace(nameRegex, "").trim()
            } else if (addressRegex.containsMatchIn(line)) {
                addressVal = line.replace(addressRegex, "").trim()
            } else if (productRegex.containsMatchIn(line)) {
                productVal = line.replace(productRegex, "").trim()
            } else if (lower.contains("dispatch") || lower.contains("ডেলিভারি") || lower.contains("তারিখ")) {
                noteVal = line
            }
        }

        if (nameVal.isEmpty() && lines.isNotEmpty() && !Regex("\\d{5}").containsMatchIn(lines[0])) {
            nameVal = lines[0]
        }

        if (addressVal.isEmpty() && lines.size > 1) {
            addressVal = lines.drop(1).take(2).joinToString(", ")
        }

        val isDhaka = Regex("dhaka|ঢাকা|mirpur|dhanmondi|uttara|gulshan", RegexOption.IGNORE_CASE).containsMatchIn(rawText)

        val deliveryChargeVal = if (deliveryMatch != null) {
            deliveryMatch.groupValues[1].toIntOrNull() ?: (if (isDhaka) 80 else 150)
        } else {
            if (isDhaka) 80 else 150
        }

        return SmartPasteDraft(
            customerName = ParsedField(nameVal, if (nameVal.isNotEmpty()) "Detected" else "Not found"),
            primaryPhone = ParsedField(
                cleanedPhone,
                if (cleanedPhone.length == 11) "Detected" else if (phoneMatches.isNotEmpty()) "Ambiguous" else "Not found"
            ),
            fullAddress = ParsedField(addressVal, if (addressVal.isNotEmpty()) "Detected" else "Not found"),
            district = ParsedField(if (isDhaka) "Dhaka" else "Chattogram", if (isDhaka) "Detected" else "Ambiguous"),
            productName = ParsedField(productVal, if (productVal.isNotEmpty()) "Detected" else "Ambiguous"),
            size = ParsedField(detectedSize, if (sizeMatch != null) "Detected" else "Ambiguous"),
            quantity = ParsedField(1, "Detected"),
            unitPrice = ParsedField(priceVal, if (priceMatch != null) "Detected" else "Not found"),
            deliveryCharge = ParsedField(deliveryChargeVal, if (deliveryMatch != null) "Detected" else "Ambiguous"),
            advancePaid = ParsedField(advanceVal, if (advanceMatch != null) "Detected" else "Not found"),
            deliveryInstructions = ParsedField(noteVal, if (noteVal.isNotEmpty()) "Detected" else "Not found"),
            rawText = rawText
        )
    }
}
