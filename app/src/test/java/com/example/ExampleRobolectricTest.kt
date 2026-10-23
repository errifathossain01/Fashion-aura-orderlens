package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.OrderEntity
import com.example.data.OrderItemEntity
import com.example.util.CoreUtils
import com.example.util.CourierTemplateEngine
import com.example.util.SmartPasteParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Fashion Aura", appName)
  }

  @Test
  fun `test digit conversions and phone validation`() {
    val bnDigits = "০১২৩৪৫৬৭৮৯"
    assertEquals("0123456789", CoreUtils.toEnglishDigits(bnDigits))
    assertEquals("০১২৩৪৫৬৭৮৯", CoreUtils.formatDigits("0123456789", true))

    assertTrue(CoreUtils.isValidBDPhone("01712345678"))
    assertTrue(CoreUtils.isValidBDPhone("+8801712345678"))
    assertTrue(CoreUtils.isValidBDPhone("০১৭১২৩৪৫৬৭৮"))
  }

  @Test
  fun `test financials calculation`() {
    val result = CoreUtils.calculateFinancials(
      orderType = "Regular",
      items = listOf(Pair(1250, 1)),
      deliveryCharge = 80,
      additionalCharge = 0,
      discount = 50,
      netPaid = 200
    )
    assertEquals(1250, result.subtotal)
    assertEquals(1280, result.totalBill) // 1250 + 80 - 50 = 1280
    assertEquals(1080, result.outstanding) // 1280 - 200 = 1080
    assertEquals(1080, result.codAmount)
    assertEquals("Partially Paid", result.paymentStatus)
  }

  @Test
  fun `test smart paste offline parser`() {
    val rawText = """
      নাম: তানিয়া আক্তার
      মোবাইল: ০১৭১২৩৪৫৬৭৮
      ঠিকানা: বাড়ি ১২, রোড ৫, ধানমন্ডি, ঢাকা
      পণ্য: Organza Three Piece
      সাইজ: 42
      দাম: 1650
      অগ্রিম: 200
    """.trimIndent()

    val parsed = SmartPasteParser.parseOrderMessageOffline(rawText)
    assertEquals("তানিয়া আক্তার", parsed.customerName.value)
    assertEquals("01712345678", parsed.primaryPhone.value)
    assertEquals("42", parsed.size.value)
    assertEquals(1650, parsed.unitPrice.value)
    assertEquals(200, parsed.advancePaid.value)
  }

  @Test
  fun `test courier template generator`() {
    val order = OrderEntity(
      id = "ord-1",
      orderNumber = "FA-261023-1234",
      customerId = "01712345678",
      customerName = "তানিয়া আক্তার",
      primaryPhone = "01712345678",
      fullAddress = "বাড়ি ১২, রোড ৫",
      district = "Dhaka",
      thanaUpazila = "Dhanmondi",
      totalBill = 1280,
      outstanding = 1080,
      deliveryCharge = 80,
      createdAt = "2026-10-23T00:00:00Z",
      updatedAt = "2026-10-23T00:00:00Z",
      orderDate = "2026-10-23T00:00:00Z"
    )
    val item = OrderItemEntity(
      id = "item-1",
      orderId = "ord-1",
      productName = "Organza Three Piece",
      size = "42",
      quantity = 1,
      unitPrice = 1250
    )
    val text = CourierTemplateEngine.generateCourierText(order, listOf(item), "bn")
    assertTrue(text.contains("FA-261023-1234"))
    assertTrue(text.contains("তানিয়া আক্তার"))
    assertTrue(text.contains("01712345678"))
    assertTrue(text.contains("1080"))
  }
}
