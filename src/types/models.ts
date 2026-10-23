export type OrderType = 'Regular' | 'Pre-order' | 'Free' | 'Exchange';
export type OrderPriority = 'Normal' | 'Urgent';
export type PaymentStatus =
  | 'Unpaid'
  | 'Partially Paid'
  | 'Paid'
  | 'Refund Pending'
  | 'Partially Refunded'
  | 'Refunded';
export type FulfillmentStatus =
  | 'New'
  | 'Confirmed'
  | 'Preparing'
  | 'Ready to Ship'
  | 'Shipped'
  | 'In Transit'
  | 'Delivered'
  | 'Delivery Failed'
  | 'Return Requested'
  | 'Returned'
  | 'Exchange Processing'
  | 'Cancelled';
export type DeliveryScheduleState = 'Unscheduled' | 'Scheduled';
export type PaymentEntryType = 'advance' | 'payment' | 'refund' | 'adjustment';
export type PaymentMethod = 'Cash/COD' | 'bKash' | 'Nagad' | 'Rocket' | 'Bank' | 'Other';
export type OrderSource =
  | 'Facebook'
  | 'Meta Business Suite'
  | 'WhatsApp'
  | 'Repeat Customer'
  | 'Other';
export type CourierName = 'Steadfast' | 'Pathao' | 'RedX' | 'Paperfly' | 'Sundarban' | 'Other';
export type ProductSize = '36' | '38' | '40' | '42' | '44' | '46' | '48' | 'Other';
export type SyncState = 'pending' | 'syncing' | 'synced' | 'error';
export type CourierSyncStatus = 'not_sent' | 'queued_offline' | 'sending' | 'sent' | 'failed';

export interface CourierApiConfigRecord {
  courier: CourierName;
  enabled: boolean;
  baseUrl: string;
  apiKey: string;
  secretKeyOrToken: string;
  storeId?: string;
  updatedAt: string;
}

export interface OrderItemRecord {
  id: string;
  orderId: string;
  productName: string;
  sku?: string;
  size: ProductSize;
  customSizeText?: string;
  quantity: number;
  unitPrice: number;
  color?: string;
  productNotes?: string;
  productImageBase64?: string; // NEW: Product variant image identification
  imageDataUrls?: string[];
}

export interface OrderRecord {
  id: string;
  orderNumber: string;
  customerId: string;
  customerName: string;
  primaryPhone: string;
  alternatePhone?: string;
  fullAddress: string;
  areaVillageStreet?: string;
  district: string;
  thanaUpazila: string;
  landmark?: string;
  orderType: OrderType;
  priority: OrderPriority;
  paymentStatus: PaymentStatus;
  fulfillmentStatus: FulfillmentStatus;
  orderDate: string;
  requestedDeliveryDate?: string;
  requestedDeliveryTimeWindow?: string;
  earliestDispatchDate?: string;
  preorderAvailabilityDate?: string;
  actualShipDate?: string;
  actualDeliveryDate?: string;
  assignedEmployeeName?: string;
  source: OrderSource;
  courier: CourierName;
  trackingNumber?: string;
  courierSyncStatus: CourierSyncStatus;
  courierConsignmentId?: string;
  courierLastError?: string;
  courierSentAt?: string;
  subtotal: number;
  deliveryCharge: number;
  additionalCharge: number;
  discount: number;
  totalBill: number;
  netPaid: number;
  outstanding: number;
  customerDeliveryInstructions?: string;
  internalStaffNotes?: string;
  exchangeReturnNotes?: string;
  freeOrderReason?: string;
  dispatchOverrideReason?: string;
  isDraft: boolean;
  isDeleted: boolean;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
  syncState: SyncState;
}

export interface CustomerRecord {
  id: string;
  name: string;
  primaryPhone: string;
  defaultAddress: string;
  district: string;
  thanaUpazila: string;
  orderCount: number;
  totalSpent: number;
  returnCount: number;
  cancelCount: number;
  isRisky: boolean;
  riskReason?: string;
  updatedAt: string;
}

export interface PaymentLedgerRecord {
  id: string;
  orderId: string;
  type: PaymentEntryType;
  amount: number;
  method: PaymentMethod;
  trxId?: string;
  recordedBy: string;
  recordedAt: string;
}

export interface AuditLogRecord {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  details: string;
  performedBy: string;
  performedAt: string;
}

export function getDeliveryScheduleState(
  order: Pick<OrderRecord, 'requestedDeliveryDate' | 'earliestDispatchDate'>
): DeliveryScheduleState {
  return Boolean(order.requestedDeliveryDate?.trim() || order.earliestDispatchDate?.trim())
    ? 'Scheduled'
    : 'Unscheduled';
}
