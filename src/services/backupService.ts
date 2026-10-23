import { db } from '../db/database';
import { getDeliveryScheduleState } from '../types/models';

export async function exportOrdersToCSV(): Promise<void> {
  const orders = await db.orders.filter((o) => !o.isDeleted).toArray();
  const headers = [
    'Order ID',
    'Order Date',
    'Customer Name',
    'Phone',
    'Address',
    'District',
    'Thana',
    'Order Type',
    'Priority',
    'Payment Status',
    'Fulfillment Status',
    'Schedule State',
    'Earliest Dispatch',
    'Requested Delivery',
    'Total Bill',
    'Net Paid',
    'COD Outstanding',
    'Courier'
  ];

  const escapeCsv = (val: unknown) => `"${String(val ?? '').replace(/"/g, '""')}"`;

  const rows = orders.map((o) =>
    [
      o.orderNumber,
      o.orderDate.slice(0, 10),
      o.customerName,
      o.primaryPhone,
      o.fullAddress,
      o.district,
      o.thanaUpazila,
      o.orderType,
      o.priority,
      o.paymentStatus,
      o.fulfillmentStatus,
      getDeliveryScheduleState(o),
      o.earliestDispatchDate ?? '',
      o.requestedDeliveryDate ?? '',
      o.totalBill,
      o.netPaid,
      Math.max(0, o.outstanding),
      o.courier
    ]
      .map(escapeCsv)
      .join(',')
  );

  // UTF-8 BOM (\uFEFF) ensures Bangla characters open cleanly in Microsoft Excel
  const csvContent = '\uFEFF' + [headers.join(','), ...rows].join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `FashionAura_Orders_${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

export async function exportFullBackupJSON(): Promise<void> {
  const [orders, orderItems, customers, payments, auditLogs, courierConfigs] =
    await Promise.all([
      db.orders.toArray(),
      db.orderItems.toArray(),
      db.customers.toArray(),
      db.payments ? db.payments.toArray() : Promise.resolve([]),
      db.auditLogs.toArray(),
      db.courierConfigs ? db.courierConfigs.toArray() : Promise.resolve([])
    ]);

  const payload = {
    app: 'Fashion Aura OMS',
    exportedAt: new Date().toISOString(),
    data: { orders, orderItems, customers, payments, auditLogs, courierConfigs }
  };

  const blob = new Blob([JSON.stringify(payload, null, 2)], {
    type: 'application/json'
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `FashionAura_Backup_${new Date().toISOString().slice(0, 10)}.json`;
  link.click();
  URL.revokeObjectURL(url);
}
