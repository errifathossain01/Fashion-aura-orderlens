import { normalizeBDPhone, toEnglishDigits } from '../utils/coreUtils';
import type { OrderRecord, OrderItemRecord, ProductSize } from '../types/models';

export interface ParsedField<T> {
  value: T;
  status: 'Detected' | 'Ambiguous' | 'Not found';
}

export interface SmartPasteDraft {
  customerName: ParsedField<string>;
  primaryPhone: ParsedField<string>;
  fullAddress: ParsedField<string>;
  district: ParsedField<string>;
  productName: ParsedField<string>;
  size: ParsedField<ProductSize>;
  quantity: ParsedField<number>;
  unitPrice: ParsedField<number>;
  deliveryCharge: ParsedField<number>;
  advancePaid: ParsedField<number>;
  deliveryInstructions: ParsedField<string>;
  rawText: string;
}

/**
 * Deterministic, 100% Offline Order Message Parser (Bangla & English).
 * Never auto-creates an order; returns field statuses (Detected / Ambiguous / Not found).
 */
export function parseOrderMessageOffline(rawText: string): SmartPasteDraft {
  const normalizedDigits = toEnglishDigits(rawText);
  const lines = rawText
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean);

  // 1. Extract BD Mobile Number
  const phoneRegex = /(?:\+?880|0)1[3-9]\d[\d\s\-]{7,10}/g;
  const phoneMatches = normalizedDigits.match(phoneRegex) || [];
  const cleanedPhone = phoneMatches[0] ? normalizeBDPhone(phoneMatches[0]) : '';

  // 2. Extract Size ONLY next to explicit size label (36-48)
  const sizeMatch = normalizedDigits.match(
    /(?:size|সাইজ|বডি)\s*[:\-=]?\s*(36|38|40|42|44|46|48)/i
  );
  const detectedSize = (sizeMatch ? sizeMatch[1] : '40') as ProductSize;

  // 3. Extract Price & Delivery Charge
  const priceMatch = normalizedDigits.match(
    /(?:price|দাম|মূল্য|বিল|tk|taka|৳)\s*[:\-=]?\s*(\d{3,5})/i
  );
  const deliveryMatch = normalizedDigits.match(
    /(?:delivery|ডেলিভারি)\s*[:\-=]?\s*(\d{2,3})/i
  );
  const advanceMatch = normalizedDigits.match(
    /(?:advance|অ্যাডভান্স|অগ্রিম|বিকাশ|bkash)\s*[:\-=]?\s*(\d{2,5})/i
  );

  // 4. Extract Name, Address, Product by labels or line heuristics
  let nameVal = '';
  let addressVal = '';
  let productVal = '';
  let noteVal = '';

  for (const line of lines) {
    const lower = line.toLowerCase();
    if (/^(name|নাম)\s*[:\-]/i.test(line)) {
      nameVal = line.replace(/^(name|নাম)\s*[:\-]\s*/i, '').trim();
    } else if (/^(address|ঠিকানা)\s*[:\-]/i.test(line)) {
      addressVal = line.replace(/^(address|ঠিকানা)\s*[:\-]\s*/i, '').trim();
    } else if (/^(product|dress|ড্রেস|পণ্য)\s*[:\-]/i.test(line)) {
      productVal = line.replace(/^(product|dress|ড্রেস|পণ্য)\s*[:\-]\s*/i, '').trim();
    } else if (
      lower.includes('dispatch') ||
      lower.includes('ডেলিভারি') ||
      lower.includes('তারিখ')
    ) {
      noteVal = line;
    }
  }

  if (!nameVal && lines.length > 0 && !/\d{5}/.test(lines[0])) {
    nameVal = lines[0];
  }

  if (!addressVal && lines.length > 1) {
    addressVal = lines.slice(1, 3).join(', ');
  }

  const isDhaka = /dhaka|ঢাকা|mirpur|dhanmondi|uttara|gulshan/i.test(rawText);

  return {
    customerName: { value: nameVal, status: nameVal ? 'Detected' : 'Not found' },
    primaryPhone: {
      value: cleanedPhone,
      status:
        cleanedPhone.length === 11
          ? 'Detected'
          : phoneMatches.length > 0
          ? 'Ambiguous'
          : 'Not found'
    },
    fullAddress: { value: addressVal, status: addressVal ? 'Detected' : 'Not found' },
    district: {
      value: isDhaka ? 'Dhaka' : 'Chattogram',
      status: isDhaka ? 'Detected' : 'Ambiguous'
    },
    productName: { value: productVal, status: productVal ? 'Detected' : 'Ambiguous' },
    size: { value: detectedSize, status: sizeMatch ? 'Detected' : 'Ambiguous' },
    quantity: { value: 1, status: 'Detected' },
    unitPrice: {
      value: priceMatch ? parseInt(priceMatch[1], 10) : 0,
      status: priceMatch ? 'Detected' : 'Not found'
    },
    deliveryCharge: {
      value: deliveryMatch ? parseInt(deliveryMatch[1], 10) : isDhaka ? 80 : 150,
      status: deliveryMatch ? 'Detected' : 'Ambiguous'
    },
    advancePaid: {
      value: advanceMatch ? parseInt(advanceMatch[1], 10) : 0,
      status: advanceMatch ? 'Detected' : 'Not found'
    },
    deliveryInstructions: { value: noteVal, status: noteVal ? 'Detected' : 'Not found' },
    rawText
  };
}

/**
 * Single Unified Courier & Share Template Engine.
 * Strictly excludes internal staff notes, risk flags, and audit history.
 */
export function generateCourierText(
  order: OrderRecord,
  items: OrderItemRecord[],
  lang: 'bn' | 'en' = 'bn'
): string {
  const itemsText = items
    .map(
      (i) =>
        `${i.productName} (${lang === 'bn' ? 'সাইজ' : 'Size'}: ${
          i.size === 'Other' ? i.customSizeText : i.size
        } × ${i.quantity})`
    )
    .join(', ');

  const cod = Math.max(0, order.outstanding);

  if (lang === 'bn') {
    const lines = [
      `অর্ডার আইডি: ${order.orderNumber}`,
      `নাম: ${order.customerName}`,
      `মোবাইল: ${order.primaryPhone}${order.alternatePhone ? `, ${order.alternatePhone}` : ''}`,
      `ঠিকানা: ${order.fullAddress}, ${order.thanaUpazila}, ${order.district}`,
      `পণ্য: ${itemsText}`,
      `মোট বিল: ৳${order.totalBill} (ডেলিভারি চার্জ: ৳${order.deliveryCharge})`,
      `ক্যাশ অন ডেলিভারি (COD): ${cod === 0 ? 'প্রিপেইড (৳০)' : `৳${cod}`}`
    ];
    if (order.requestedDeliveryDate) {
      lines.push(`ডেলিভারি তারিখ: ${order.requestedDeliveryDate}`);
    }
    if (order.customerDeliveryInstructions) {
      lines.push(`নির্দেশনা: ${order.customerDeliveryInstructions}`);
    }
    return lines.join('\n');
  }

  const enLines = [
    `Order ID: ${order.orderNumber}`,
    `Customer: ${order.customerName}`,
    `Phone: ${order.primaryPhone}${order.alternatePhone ? `, ${order.alternatePhone}` : ''}`,
    `Address: ${order.fullAddress}, ${order.thanaUpazila}, ${order.district}`,
    `Items: ${itemsText}`,
    `Total Bill: BDT ${order.totalBill} (Delivery: BDT ${order.deliveryCharge})`,
    `COD to Collect: ${cod === 0 ? 'Prepaid (BDT 0)' : `BDT ${cod}`}`
  ];
  if (order.requestedDeliveryDate) {
    enLines.push(`Requested Delivery: ${order.requestedDeliveryDate}`);
  }
  if (order.customerDeliveryInstructions) {
    enLines.push(`Note: ${order.customerDeliveryInstructions}`);
  }
  return enLines.join('\n');
}
