import { db } from '../db/database';

export async function checkDeliveryReminders(): Promise<void> {
  if (typeof window === 'undefined' || !('Notification' in window)) return;

  try {
    if (Notification.permission === 'default') {
      await Notification.requestPermission();
    }

    if (Notification.permission !== 'granted') return;

    const todayIso = new Date().toISOString().slice(0, 10);
    const orders = await db.orders
      .filter(
        (o) =>
          !o.isDeleted &&
          (o.earliestDispatchDate === todayIso || o.requestedDeliveryDate === todayIso)
      )
      .toArray();

    for (const o of orders) {
      const typeText =
        o.earliestDispatchDate === todayIso
          ? 'Dispatch Ready Today'
          : 'Delivery Scheduled Today';
      new Notification(`Fashion Aura Reminder: ${o.orderNumber}`, {
        body: `${typeText}! Customer ${o.customerName} (${o.primaryPhone}) — Total BDT ${o.totalBill}`,
        icon: '/icon-192.png'
      });
    }
  } catch (err) {
    console.warn('Notification error or permission denied:', err);
  }
}
