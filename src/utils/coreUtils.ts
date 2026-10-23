import type { PaymentStatus } from '../types/models';

const EN_TO_BN: Record<string, string> = {
  '0': '০',
  '1': '১',
  '2': '২',
  '3': '৩',
  '4': '৪',
  '5': '৫',
  '6': '৬',
  '7': '৭',
  '8': '৮',
  '9': '৯'
};

const BN_TO_EN: Record<string, string> = {
  '০': '0',
  '১': '1',
  '২': '2',
  '৩': '3',
  '৪': '4',
  '৫': '5',
  '৬': '6',
  '৭': '7',
  '৮': '8',
  '৯': '9'
};

export function toEnglishDigits(str: string): string {
  return (str || '').replace(/[০-৯]/g, (c) => BN_TO_EN[c] ?? c);
}

export function formatDigits(val: string | number, useBn: boolean): string {
  const s = String(val ?? '');
  if (!useBn) return toEnglishDigits(s);
  return s.replace(/[0-9]/g, (c) => EN_TO_BN[c] ?? c);
}

export function formatBDT(amount: number, useBn: boolean): string {
  const rounded = Math.round(amount || 0);
  const abs = Math.abs(rounded).toLocaleString('en-IN');
  return `${rounded < 0 ? '-' : ''}৳${formatDigits(abs, useBn)}`;
}

export function normalizeBDPhone(raw: string): string {
  const ascii = toEnglishDigits(raw).replace(/[\s\-().]/g, '');
  if (ascii.startsWith('+880')) return '0' + ascii.slice(4);
  if (ascii.startsWith('880') && ascii.length === 13) return '0' + ascii.slice(3);
  return ascii;
}

export function isValidBDPhone(raw: string): boolean {
  return /^01[3-9]\d{8}$/.test(normalizeBDPhone(raw));
}

export function parseIntSafe(raw: string): number {
  const clean = toEnglishDigits(raw).replace(/[^0-9]/g, '');
  return clean ? parseInt(clean, 10) : 0;
}

export function generateOrderId(): string {
  const now = new Date();
  const yy = String(now.getFullYear()).slice(-2);
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  const rand = Math.floor(1000 + Math.random() * 9000);
  return `FA-${yy}${mm}${dd}-${rand}`;
}

export function calculateFinancials(params: {
  orderType: string;
  items: Array<{ unitPrice: number; quantity: number }>;
  deliveryCharge: number;
  additionalCharge: number;
  discount: number;
  netPaid: number;
}) {
  const subtotal = params.items.reduce(
    (sum, i) =>
      sum + Math.max(0, Math.round(i.unitPrice)) * Math.max(1, Math.round(i.quantity)),
    0
  );
  const rawTotal =
    params.orderType === 'Free'
      ? 0
      : subtotal +
        Math.round(params.deliveryCharge || 0) +
        Math.round(params.additionalCharge || 0) -
        Math.round(params.discount || 0);
  const totalBill = Math.max(0, rawTotal);
  const outstanding = totalBill - Math.round(params.netPaid || 0);
  const codAmount = Math.max(0, outstanding);

  let paymentStatus: PaymentStatus = 'Unpaid';
  if (params.orderType === 'Free' || (totalBill > 0 && params.netPaid >= totalBill)) {
    paymentStatus = 'Paid';
  } else if (params.netPaid > 0 && params.netPaid < totalBill) {
    paymentStatus = 'Partially Paid';
  }

  return { subtotal, totalBill, outstanding, codAmount, paymentStatus };
}

export const BD_DISTRICTS = [
  'Dhaka',
  'Chattogram',
  'Gazipur',
  'Narayanganj',
  'Sylhet',
  'Rajshahi',
  'Khulna',
  'Barishal',
  'Rangpur',
  'Mymensingh',
  'Cumilla',
  'Bogura',
  "Cox's Bazar",
  'Jessore'
];
