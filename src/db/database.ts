import Dexie, { type Table } from 'dexie';
import type {
  OrderRecord,
  OrderItemRecord,
  CustomerRecord,
  AuditLogRecord,
  CourierApiConfigRecord,
  PaymentLedgerRecord
} from '../types/models';

export class FashionAuraDB extends Dexie {
  orders!: Table<OrderRecord, string>;
  orderItems!: Table<OrderItemRecord, string>;
  customers!: Table<CustomerRecord, string>;
  payments!: Table<PaymentLedgerRecord, string>;
  auditLogs!: Table<AuditLogRecord, string>;
  courierConfigs!: Table<CourierApiConfigRecord, string>;

  constructor() {
    super('FashionAuraCourierDB');
    this.version(2).stores({
      orders:
        'id, &orderNumber, customerId, primaryPhone, orderType, priority, paymentStatus, fulfillmentStatus, orderDate, requestedDeliveryDate, earliestDispatchDate, district, courier, courierSyncStatus, isDraft, isDeleted, syncState, updatedAt',
      orderItems: 'id, orderId, productName, sku, size',
      customers: 'id, &primaryPhone, name, district, isRisky',
      payments: 'id, orderId, type, method, recordedAt',
      auditLogs: 'id, entityType, entityId, performedAt',
      courierConfigs: 'courier, enabled'
    });
  }
}

export const db = new FashionAuraDB();

export async function ensureDefaultCourierConfigs(): Promise<void> {
  const count = await db.courierConfigs.count();
  if (count > 0) return;
  const now = new Date().toISOString();
  await db.courierConfigs.bulkPut([
    {
      courier: 'Steadfast',
      enabled: false,
      baseUrl: 'https://portal.packzy.com/api/v1',
      apiKey: '',
      secretKeyOrToken: '',
      updatedAt: now
    },
    {
      courier: 'Pathao',
      enabled: false,
      baseUrl: 'https://api-hermes.pathao.com',
      apiKey: '',
      secretKeyOrToken: '',
      storeId: '',
      updatedAt: now
    }
  ]);
}
