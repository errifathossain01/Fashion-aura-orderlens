import { db } from '../db/database';
import type { OrderRecord, OrderItemRecord } from '../types/models';

export interface CourierPushResult {
  orderId: string;
  orderNumber: string;
  status: 'sent' | 'queued_offline' | 'failed';
  trackingCode?: string;
  consignmentId?: string;
  message: string;
}

export class CourierAPIService {
  static async sendOrderToCourier(
    orderId: string,
    options?: { allowEarlyDispatchOverride?: boolean }
  ): Promise<CourierPushResult> {
    const order = await db.orders.get(orderId);
    if (!order) throw new Error('Order not found');

    if (order.courierSyncStatus === 'sent' && order.trackingNumber) {
      return {
        orderId: order.id,
        orderNumber: order.orderNumber,
        status: 'sent',
        trackingCode: order.trackingNumber,
        consignmentId: order.courierConsignmentId,
        message: `Already sent (Tracking: ${order.trackingNumber})`
      };
    }

    const todayIso = new Date().toISOString().slice(0, 10);
    if (
      order.earliestDispatchDate &&
      todayIso < order.earliestDispatchDate &&
      !options?.allowEarlyDispatchOverride
    ) {
      return {
        orderId: order.id,
        orderNumber: order.orderNumber,
        status: 'failed',
        message: `Restricted: Do Not Dispatch Before ${order.earliestDispatchDate}`
      };
    }

    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      await db.orders.update(order.id, {
        courierSyncStatus: 'queued_offline',
        courierLastError: 'Queued offline — waiting for internet',
        updatedAt: new Date().toISOString()
      });
      return {
        orderId: order.id,
        orderNumber: order.orderNumber,
        status: 'queued_offline',
        message: 'Phone is offline. Added to Pending Courier Sync queue.'
      };
    }

    const config = await db.courierConfigs.get(order.courier);
    if (!config || !config.enabled) {
      const msg = `${order.courier} API is not enabled in Settings.`;
      await db.orders.update(order.id, {
        courierSyncStatus: 'failed',
        courierLastError: msg,
        updatedAt: new Date().toISOString()
      });
      return {
        orderId: order.id,
        orderNumber: order.orderNumber,
        status: 'failed',
        message: msg
      };
    }

    await db.orders.update(order.id, {
      courierSyncStatus: 'sending',
      courierLastError: undefined,
      updatedAt: new Date().toISOString()
    });

    const items = await db.orderItems.where('orderId').equals(order.id).toArray();

    // SMART RETURN NOTE: Auto-appends return delivery charge rule for riders
    const returnNote = `রিটার্ন করলে, ডেলিভারি চার্জ ${order.deliveryCharge} টাকা নিয়ে নিবেন।`;
    const itemSummary = items
      .map((i) => `${i.productName} (Size ${i.size} x${i.quantity})`)
      .join(', ');
    const finalNote = `${itemSummary} | ${returnNote} ${order.customerDeliveryInstructions || ''}`.trim();

    try {
      let result: { trackingCode: string; consignmentId: string };
      if (order.courier === 'Steadfast') {
        result = await this.pushToSteadfast(
          order,
          items,
          config.baseUrl,
          config.apiKey,
          config.secretKeyOrToken,
          finalNote
        );
      } else if (order.courier === 'Pathao') {
        result = await this.pushToPathao(
          order,
          items,
          config.baseUrl,
          config.secretKeyOrToken,
          config.storeId || '',
          finalNote,
          itemSummary
        );
      } else {
        throw new Error(`Direct API for ${order.courier} not supported.`);
      }

      const now = new Date().toISOString();
      await db.transaction('rw', [db.orders, db.auditLogs], async () => {
        await db.orders.update(order.id, {
          courierSyncStatus: 'sent',
          trackingNumber: result.trackingCode,
          courierConsignmentId: result.consignmentId,
          courierSentAt: now,
          courierLastError: undefined,
          fulfillmentStatus: 'Ready to Ship',
          updatedAt: now
        });
        await db.auditLogs.add({
          id: crypto.randomUUID(),
          entityType: 'courier_api',
          entityId: order.id,
          action: `SENT_${order.courier.toUpperCase()}`,
          details: `Tracking: ${result.trackingCode}`,
          performedBy: 'Admin',
          performedAt: now
        });
      });

      return {
        orderId: order.id,
        orderNumber: order.orderNumber,
        status: 'sent',
        trackingCode: result.trackingCode,
        consignmentId: result.consignmentId,
        message: `Sent to ${order.courier}! Tracking: ${result.trackingCode}`
      };
    } catch (err: any) {
      const errorMsg = err instanceof Error ? err.message : 'Courier API Error';
      await db.orders.update(order.id, {
        courierSyncStatus: 'failed',
        courierLastError: errorMsg,
        updatedAt: new Date().toISOString()
      });
      return {
        orderId: order.id,
        orderNumber: order.orderNumber,
        status: 'failed',
        message: errorMsg
      };
    }
  }

  static async bulkSyncOrders(
    orderIds?: string[]
  ): Promise<{ sentCount: number; failedCount: number; queuedCount: number }> {
    let targetOrders: OrderRecord[];
    if (orderIds && orderIds.length > 0) {
      const fetched = await db.orders.bulkGet(orderIds);
      targetOrders = fetched.filter((o): o is OrderRecord => Boolean(o && !o.isDeleted));
    } else {
      targetOrders = await db.orders
        .filter(
          (o) =>
            !o.isDeleted &&
            (o.courierSyncStatus === 'queued_offline' || o.courierSyncStatus === 'failed')
        )
        .toArray();
    }

    let sentCount = 0;
    let failedCount = 0;
    let queuedCount = 0;

    for (const order of targetOrders) {
      const res = await this.sendOrderToCourier(order.id);
      if (res.status === 'sent') sentCount++;
      else if (res.status === 'queued_offline') queuedCount++;
      else failedCount++;
    }

    return { sentCount, failedCount, queuedCount };
  }

  private static async pushToSteadfast(
    order: OrderRecord,
    items: OrderItemRecord[],
    baseUrl: string,
    apiKey: string,
    secretKey: string,
    finalNote: string
  ): Promise<{ trackingCode: string; consignmentId: string }> {
    if (!apiKey.trim() || !secretKey.trim()) {
      throw new Error('Steadfast API Key and Secret Key required.');
    }

    const res = await fetch(`${baseUrl.replace(/\/$/, '')}/create_order`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Api-Key': apiKey.trim(),
        'Secret-Key': secretKey.trim()
      },
      body: JSON.stringify({
        invoice: order.orderNumber,
        recipient_name: order.customerName,
        recipient_phone: order.primaryPhone,
        recipient_address: `${order.fullAddress}, ${order.thanaUpazila}, ${order.district}`,
        cod_amount: Math.max(0, Math.round(order.outstanding)),
        note: finalNote
      })
    });

    const data = await res.json().catch(() => null);
    if (!res.ok || (data && data.status && data.status !== 200)) {
      throw new Error(data?.message || `Steadfast API error (HTTP ${res.status})`);
    }

    const trackingCode = String(
      data?.consignment?.tracking_code || data?.consignment?.consignment_id || ''
    ).trim();
    if (!trackingCode) throw new Error('Steadfast response missing tracking_code');

    return {
      trackingCode,
      consignmentId: String(data?.consignment?.consignment_id || trackingCode)
    };
  }

  private static async pushToPathao(
    order: OrderRecord,
    items: OrderItemRecord[],
    baseUrl: string,
    bearerToken: string,
    storeId: string,
    finalNote: string,
    itemSummary: string
  ): Promise<{ trackingCode: string; consignmentId: string }> {
    if (!bearerToken.trim() || !storeId.trim()) {
      throw new Error('Pathao Token and Store ID required.');
    }

    const totalQty = items.reduce((sum, i) => sum + Math.max(1, i.quantity), 0);

    const res = await fetch(`${baseUrl.replace(/\/$/, '')}/aladdin/api/v1/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        Authorization: `Bearer ${bearerToken.trim()}`
      },
      body: JSON.stringify({
        store_id: Number(storeId),
        merchant_order_id: order.orderNumber,
        recipient_name: order.customerName,
        recipient_phone: order.primaryPhone,
        recipient_address: `${order.fullAddress}, ${order.thanaUpazila}, ${order.district}`,
        delivery_type: 48,
        item_type: 2,
        item_quantity: totalQty || 1,
        item_weight: 0.5,
        amount_to_collect: Math.max(0, Math.round(order.outstanding)),
        special_instruction: finalNote,
        item_description: itemSummary
      })
    });

    const data = await res.json().catch(() => null);
    if (!res.ok) throw new Error(data?.message || `Pathao API error (HTTP ${res.status})`);

    const consignmentId = String(data?.data?.consignment_id || '').trim();
    if (!consignmentId) throw new Error('Pathao response missing consignment_id');

    return { trackingCode: consignmentId, consignmentId };
  }
}
