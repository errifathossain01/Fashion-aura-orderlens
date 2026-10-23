import React, { useState, useMemo, useEffect } from 'react';
import { useLiveQuery } from 'dexie-react-hooks';
import {
  PlusCircle,
  ClipboardPaste,
  Search,
  Copy,
  Trash2,
  RotateCcw,
  Download,
  AlertOctagon,
  Printer,
  CheckCircle2,
  Truck,
  Send,
  RefreshCw,
  Settings,
  Moon,
  Sun,
  Camera,
  Image as ImageIcon,
  Bell,
  X
} from 'lucide-react';
import { db, ensureDefaultCourierConfigs } from './db/database';
import type {
  OrderRecord,
  OrderItemRecord,
  OrderType,
  OrderPriority,
  PaymentStatus,
  FulfillmentStatus,
  DeliveryScheduleState,
  ProductSize,
  CourierName
} from './types/models';
import {
  BD_DISTRICTS,
  calculateFinancials,
  formatBDT,
  formatDigits,
  generateOrderId,
  isValidBDPhone,
  normalizeBDPhone,
  parseIntSafe,
  toEnglishDigits
} from './utils/coreUtils';
import {
  generateCourierText,
  parseOrderMessageOffline
} from './services/smartPasteAndCourier';
import { exportFullBackupJSON, exportOrdersToCSV } from './services/backupService';
import { CourierAPIService } from './services/courierApiService';
import { checkDeliveryReminders } from './services/notificationService';
import { CourierSettingsPanel } from './components/CourierSettingsPanel';

export const App: React.FC = () => {
  const [lang, setLang] = useState<'bn' | 'en'>('bn');
  const [useBnDigits, setUseBnDigits] = useState(true);
  const [darkMode, setDarkMode] = useState(() => {
    return localStorage.getItem('fa_dark_mode') === 'true';
  });
  const [activeTab, setActiveTab] = useState<
    'dashboard' | 'orders' | 'create' | 'smartPaste' | 'trash' | 'backup' | 'settings'
  >('dashboard');

  useEffect(() => {
    ensureDefaultCourierConfigs();
    checkDeliveryReminders();
  }, []);

  const toggleDarkMode = () => {
    setDarkMode((prev) => {
      const next = !prev;
      localStorage.setItem('fa_dark_mode', String(next));
      return next;
    });
  };

  // Universal Search & 5 Independent Dimension Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<OrderType | 'All'>('All');
  const [filterPriority, setFilterPriority] = useState<OrderPriority | 'All'>('All');
  const [filterPayment, setFilterPayment] = useState<PaymentStatus | 'All'>('All');
  const [filterFulfillment, setFilterFulfillment] = useState<FulfillmentStatus | 'All'>('All');
  const [filterSchedule, setFilterSchedule] = useState<DeliveryScheduleState | 'All'>('All');

  // Order Form State
  const [editingOrderId, setEditingOrderId] = useState<string | null>(null);
  const [customerName, setCustomerName] = useState('');
  const [primaryPhone, setPrimaryPhone] = useState('');
  const [fullAddress, setFullAddress] = useState('');
  const [district, setDistrict] = useState('Dhaka');
  const [thanaUpazila, setThanaUpazila] = useState('Dhanmondi');
  const [orderType, setOrderType] = useState<OrderType>('Regular');
  const [priority, setPriority] = useState<OrderPriority>('Normal');
  const [fulfillmentStatus, setFulfillmentStatus] = useState<FulfillmentStatus>('New');
  const [courier, setCourier] = useState<CourierName>('Steadfast');
  const [requestedDeliveryDate, setRequestedDeliveryDate] = useState('');
  const [earliestDispatchDate, setEarliestDispatchDate] = useState('');
  const [deliveryCharge, setDeliveryCharge] = useState(80);
  const [discount, setDiscount] = useState(0);
  const [advancePaid, setAdvancePaid] = useState(0);
  const [customerInstructions, setCustomerInstructions] = useState('');
  const [internalNotes, setInternalNotes] = useState('');
  const [productName, setProductName] = useState('');
  const [sku, setSku] = useState('');
  const [size, setSize] = useState<ProductSize>('40');
  const [quantity, setQuantity] = useState(1);
  const [unitPrice, setUnitPrice] = useState(1250);
  const [productImageBase64, setProductImageBase64] = useState<string>('');

  // Smart Paste State
  const [rawPasteText, setRawPasteText] = useState('');
  const [toastMsg, setToastMsg] = useState<string | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);

  const allOrders = (useLiveQuery(() => db.orders.reverse().sortBy('createdAt'), []) || []) as OrderRecord[];
  const allItems = (useLiveQuery(() => db.orderItems.toArray(), []) || []) as OrderItemRecord[];

  const itemsByOrder = useMemo(() => {
    const map: Record<string, OrderItemRecord[]> = {};
    for (const item of allItems) {
      if (!map[item.orderId]) map[item.orderId] = [];
      map[item.orderId].push(item);
    }
    return map;
  }, [allItems]);

  const activeOrders = useMemo(() => allOrders.filter((o) => !o.isDeleted), [allOrders]);
  const trashedOrders = useMemo(() => allOrders.filter((o) => o.isDeleted), [allOrders]);

  const metrics = useMemo(() => {
    const todayIso = new Date().toISOString().slice(0, 10);
    return {
      total: activeOrders.length,
      newToday: activeOrders.filter((o) => o.orderDate.slice(0, 10) === todayIso).length,
      awaitingShip: activeOrders.filter((o) =>
        ['New', 'Confirmed', 'Preparing', 'Ready to Ship'].includes(o.fulfillmentStatus)
      ).length,
      urgent: activeOrders.filter((o) => o.priority === 'Urgent').length,
      scheduled: activeOrders.filter(
        (o) => Boolean(o.requestedDeliveryDate?.trim() || o.earliestDispatchDate?.trim())
      ).length,
      preorders: activeOrders.filter((o) => o.orderType === 'Pre-order').length,
      unpaid: activeOrders.filter((o) => o.paymentStatus === 'Unpaid').length,
      codToCollect: activeOrders.reduce((sum, o) => sum + Math.max(0, o.outstanding), 0),
      courierPending: activeOrders.filter(
        (o) => o.courierSyncStatus === 'queued_offline' || o.courierSyncStatus === 'failed'
      ).length,
      courierSent: activeOrders.filter((o) => o.courierSyncStatus === 'sent').length
    };
  }, [activeOrders]);

  const filteredOrders = useMemo(() => {
    const q = toEnglishDigits(searchQuery).toLowerCase().trim();
    return activeOrders.filter((o) => {
      if (filterType !== 'All' && o.orderType !== filterType) return false;
      if (filterPriority !== 'All' && o.priority !== filterPriority) return false;
      if (filterPayment !== 'All' && o.paymentStatus !== filterPayment) return false;
      if (filterFulfillment !== 'All' && o.fulfillmentStatus !== filterFulfillment) return false;
      const sched = Boolean(o.requestedDeliveryDate?.trim() || o.earliestDispatchDate?.trim())
        ? 'Scheduled'
        : 'Unscheduled';
      if (filterSchedule !== 'All' && sched !== filterSchedule) return false;

      if (!q) return true;
      const items = itemsByOrder[o.id] || [];
      const itemMatch = items.some(
        (i) =>
          i.productName.toLowerCase().includes(q) ||
          (i.sku && i.sku.toLowerCase().includes(q)) ||
          i.size.toLowerCase().includes(q)
      );

      return (
        o.orderNumber.toLowerCase().includes(q) ||
        o.customerName.toLowerCase().includes(q) ||
        o.primaryPhone.includes(q) ||
        o.fullAddress.toLowerCase().includes(q) ||
        o.district.toLowerCase().includes(q) ||
        (o.trackingNumber && o.trackingNumber.toLowerCase().includes(q)) ||
        itemMatch
      );
    });
  }, [
    activeOrders,
    itemsByOrder,
    searchQuery,
    filterType,
    filterPriority,
    filterPayment,
    filterFulfillment,
    filterSchedule
  ]);

  const liveCalc = useMemo(
    () =>
      calculateFinancials({
        orderType,
        items: [{ unitPrice, quantity }],
        deliveryCharge,
        additionalCharge: 0,
        discount,
        netPaid: advancePaid
      }),
    [orderType, unitPrice, quantity, deliveryCharge, discount, advancePaid]
  );

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => setToastMsg(null), 3500);
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      setProductImageBase64(reader.result as string);
    };
    reader.readAsDataURL(file);
  };

  const resetForm = () => {
    setEditingOrderId(null);
    setCustomerName('');
    setPrimaryPhone('');
    setFullAddress('');
    setProductName('');
    setSku('');
    setSize('40');
    setQuantity(1);
    setUnitPrice(1250);
    setProductImageBase64('');
    setAdvancePaid(0);
    setDiscount(0);
    setDeliveryCharge(80);
    setRequestedDeliveryDate('');
    setEarliestDispatchDate('');
    setCustomerInstructions('');
    setInternalNotes('');
    setOrderType('Regular');
    setPriority('Normal');
    setFulfillmentStatus('New');
    setCourier('Steadfast');
    setDistrict('Dhaka');
    setThanaUpazila('Dhanmondi');
  };

  const populateEditOrder = (order: OrderRecord, items: OrderItemRecord[]) => {
    setEditingOrderId(order.id);
    setCustomerName(order.customerName);
    setPrimaryPhone(order.primaryPhone);
    setFullAddress(order.fullAddress);
    setDistrict(order.district);
    setThanaUpazila(order.thanaUpazila);
    setOrderType(order.orderType);
    setPriority(order.priority);
    setFulfillmentStatus(order.fulfillmentStatus);
    setCourier(order.courier);
    setRequestedDeliveryDate(order.requestedDeliveryDate || '');
    setEarliestDispatchDate(order.earliestDispatchDate || '');
    setDeliveryCharge(order.deliveryCharge);
    setDiscount(order.discount);
    setAdvancePaid(order.netPaid);
    setCustomerInstructions(order.customerDeliveryInstructions || '');
    setInternalNotes(order.internalStaffNotes || '');

    const firstItem = items[0];
    if (firstItem) {
      setProductName(firstItem.productName);
      setSku(firstItem.sku || '');
      setSize(firstItem.size);
      setQuantity(firstItem.quantity);
      setUnitPrice(firstItem.unitPrice);
      setProductImageBase64(firstItem.productImageBase64 || '');
    }
    setActiveTab('create');
  };

  const handleSaveOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (
      !customerName.trim() ||
      !isValidBDPhone(primaryPhone) ||
      !fullAddress.trim() ||
      !productName.trim()
    ) {
      showToast(
        lang === 'bn'
          ? 'ক্রেতার নাম, ১১ সংখ্যার মোবাইল নম্বর, ঠিকানা ও পণ্যের নাম আবশ্যক!'
          : 'Name, valid 11-digit BD phone, address, and product name are required!'
      );
      return;
    }

    const now = new Date().toISOString();
    const id = editingOrderId || crypto.randomUUID();
    const existingOrder = editingOrderId ? await db.orders.get(editingOrderId) : null;
    const orderNumber = existingOrder?.orderNumber || generateOrderId();

    const record: OrderRecord = {
      id,
      orderNumber,
      customerId: normalizeBDPhone(primaryPhone),
      customerName: customerName.trim(),
      primaryPhone: normalizeBDPhone(primaryPhone),
      fullAddress: fullAddress.trim(),
      district,
      thanaUpazila,
      orderType,
      priority,
      paymentStatus: liveCalc.paymentStatus,
      fulfillmentStatus,
      orderDate: existingOrder?.orderDate || now,
      requestedDeliveryDate: requestedDeliveryDate || undefined,
      earliestDispatchDate: earliestDispatchDate || undefined,
      source: 'WhatsApp',
      courier,
      trackingNumber: existingOrder?.trackingNumber,
      courierSyncStatus: existingOrder?.courierSyncStatus || 'not_sent',
      courierConsignmentId: existingOrder?.courierConsignmentId,
      courierLastError: existingOrder?.courierLastError,
      courierSentAt: existingOrder?.courierSentAt,
      subtotal: liveCalc.subtotal,
      deliveryCharge,
      additionalCharge: 0,
      discount,
      totalBill: liveCalc.totalBill,
      netPaid: advancePaid,
      outstanding: liveCalc.outstanding,
      customerDeliveryInstructions: customerInstructions.trim() || undefined,
      internalStaffNotes: internalNotes.trim() || undefined,
      isDraft: false,
      isDeleted: false,
      version: 1,
      createdAt: existingOrder?.createdAt || now,
      createdBy: 'Admin',
      updatedAt: now,
      updatedBy: 'Admin',
      syncState: 'pending'
    };

    await db.transaction('rw', [db.orders, db.orderItems, db.auditLogs], async () => {
      await db.orders.put(record);
      await db.orderItems.where('orderId').equals(id).delete();
      await db.orderItems.add({
        id: crypto.randomUUID(),
        orderId: id,
        productName: productName.trim(),
        sku: sku.trim() || undefined,
        size,
        quantity,
        unitPrice,
        productImageBase64: productImageBase64 || undefined
      });
      await db.auditLogs.add({
        id: crypto.randomUUID(),
        entityType: 'order',
        entityId: id,
        action: editingOrderId ? 'UPDATED' : 'CREATED',
        details: `${orderNumber} - BDT ${liveCalc.totalBill}`,
        performedBy: 'Admin',
        performedAt: now
      });
    });

    resetForm();
    showToast(
      lang === 'bn' ? 'অর্ডার সফলভাবে সংরক্ষিত হয়েছে!' : 'Order saved to local database!'
    );
    setActiveTab('orders');
  };

  const handleSendToCourier = async (orderId: string, override: boolean = false) => {
    setIsSyncing(true);
    try {
      const res = await CourierAPIService.sendOrderToCourier(orderId, {
        allowEarlyDispatchOverride: override
      });
      showToast(res.message);
    } catch (err: any) {
      showToast(err.message || 'Error sending to courier');
    } finally {
      setIsSyncing(false);
    }
  };

  const handleBulkSync = async () => {
    setIsSyncing(true);
    try {
      const res = await CourierAPIService.bulkSyncOrders();
      showToast(
        `Courier Sync Complete: ${res.sentCount} sent, ${res.failedCount} failed, ${res.queuedCount} queued.`
      );
    } catch (err: any) {
      showToast(err.message || 'Bulk sync error');
    } finally {
      setIsSyncing(false);
    }
  };

  const handleSmartFill = () => {
    if (!rawPasteText.trim()) return;
    const parsed = parseOrderMessageOffline(rawPasteText);
    setCustomerName(parsed.customerName.value);
    setPrimaryPhone(parsed.primaryPhone.value);
    setFullAddress(parsed.fullAddress.value);
    setDistrict(parsed.district.value);
    setProductName(parsed.productName.value || 'Three Piece');
    setSize(parsed.size.value);
    setUnitPrice(parsed.unitPrice.value || 1250);
    setDeliveryCharge(parsed.deliveryCharge.value);
    setAdvancePaid(parsed.advancePaid.value);
    setCustomerInstructions(parsed.deliveryInstructions.value);
    showToast(
      lang === 'bn'
        ? 'স্মার্ট ফিল সম্পন্ন! অনুগ্রহ করে যাচাই করে সেভ করুন।'
        : 'Smart Fill extracted! Review fields before saving.'
    );
    setActiveTab('create');
  };

  const handleCopyForCourier = async (order: OrderRecord) => {
    const items = itemsByOrder[order.id] || [];
    const text = generateCourierText(order, items, lang);
    await navigator.clipboard.writeText(text);
    await db.auditLogs.add({
      id: crypto.randomUUID(),
      entityType: 'share',
      entityId: order.id,
      action: 'COPIED_FOR_COURIER',
      details: order.orderNumber,
      performedBy: 'Admin',
      performedAt: new Date().toISOString()
    });
    showToast(lang === 'bn' ? 'কুরিয়ার টেক্সট কপি হয়েছে!' : 'Copied for Courier!');
  };

  const handleSoftDelete = async (orderId: string) => {
    await db.orders.update(orderId, { isDeleted: true, updatedAt: new Date().toISOString() });
    showToast(lang === 'bn' ? 'অর্ডার ট্র্যাশে সরানো হয়েছে।' : 'Moved to Trash.');
  };

  const handleRestore = async (orderId: string) => {
    await db.orders.update(orderId, { isDeleted: false, updatedAt: new Date().toISOString() });
    showToast(lang === 'bn' ? 'অর্ডার পুনরুদ্ধার হয়েছে!' : 'Order restored!');
  };

  return (
    <div
      className={`min-h-screen pb-24 font-sans transition-colors duration-200 ${
        darkMode ? 'dark bg-slate-950 text-slate-100' : 'bg-slate-100 text-slate-900'
      }`}
    >
      {/* HIGH-CONTRAST HEADER */}
      <header className="sticky top-0 z-40 bg-pink-900 text-white px-4 py-3 shadow-lg border-b border-pink-950 flex justify-between items-center">
        <div>
          <h1 className="text-lg font-black tracking-wide text-white drop-shadow-sm">
            {lang === 'bn' ? 'ফ্যাশন আউরা — OMS' : 'Fashion Aura — OMS'}
          </h1>
          <p className="text-xs text-pink-200 font-bold">
            {lang === 'bn'
              ? 'ডাইরেক্ট কুরিয়ার API • হাই-কনট্রাস্ট ভিউ'
              : 'Direct Courier API • Sunlight High-Contrast'}
          </p>
        </div>
        <div className="flex gap-2 items-center">
          <button
            onClick={() => checkDeliveryReminders()}
            title="Check Reminders"
            className="p-2 rounded-xl bg-pink-800 hover:bg-pink-700 text-white font-bold transition-colors"
          >
            <Bell className="w-4 h-4" />
          </button>
          <button
            onClick={toggleDarkMode}
            title={darkMode ? 'Light Mode' : 'Dark Mode'}
            className="p-2 rounded-xl bg-pink-800 hover:bg-pink-700 text-white font-bold transition-colors"
          >
            {darkMode ? <Sun className="w-4 h-4 text-amber-300" /> : <Moon className="w-4 h-4 text-slate-100" />}
          </button>
          <button
            onClick={() => setUseBnDigits(!useBnDigits)}
            className="px-2.5 py-1.5 rounded-xl bg-pink-800 hover:bg-pink-700 text-xs font-black text-white transition-colors"
          >
            {useBnDigits ? '১২৩' : '123'}
          </button>
          <button
            onClick={() => setLang(lang === 'bn' ? 'en' : 'bn')}
            className="px-3 py-1.5 rounded-xl bg-white text-pink-950 text-xs font-black shadow transition-transform active:scale-95"
          >
            {lang === 'bn' ? 'English' : 'বাংলা'}
          </button>
        </div>
      </header>

      {/* TOAST ALERT */}
      {toastMsg && (
        <div className="fixed top-16 left-1/2 -translate-x-1/2 z-50 bg-emerald-900 border-2 border-emerald-400 text-white px-4 py-2.5 rounded-2xl shadow-2xl flex items-center gap-2 text-sm font-black">
          <CheckCircle2 className="w-5 h-5 text-emerald-300 shrink-0" />
          <span>{toastMsg}</span>
        </div>
      )}

      <main className="max-w-3xl mx-auto p-4 space-y-4">
        {/* DASHBOARD TAB */}
        {activeTab === 'dashboard' && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {[
                { label: lang === 'bn' ? 'মোট অর্ডার' : 'Total Orders', val: metrics.total },
                { label: lang === 'bn' ? 'আজকের নতুন' : 'New Today', val: metrics.newToday },
                {
                  label: lang === 'bn' ? 'শিপমেন্টের অপেক্ষায়' : 'Awaiting Ship',
                  val: metrics.awaitingShip
                },
                { label: lang === 'bn' ? 'জরুরি (Urgent)' : 'Urgent', val: metrics.urgent, isAlert: true },
                { label: lang === 'bn' ? 'শিডিউলড' : 'Scheduled', val: metrics.scheduled },
                { label: lang === 'bn' ? 'প্রি-অর্ডার' : 'Pre-orders', val: metrics.preorders },
                {
                  label: lang === 'bn' ? 'কুরিয়ার পেন্ডিং' : 'Courier Pending',
                  val: metrics.courierPending,
                  isWarning: metrics.courierPending > 0
                },
                {
                  label: lang === 'bn' ? 'মোট COD বাকি' : 'COD to Collect',
                  val: formatBDT(metrics.codToCollect, useBnDigits)
                }
              ].map((card, i) => (
                <div
                  key={i}
                  onClick={() => setActiveTab('orders')}
                  className={`cursor-pointer p-4 rounded-2xl border-2 shadow-sm transition-all hover:scale-[1.02] ${
                    darkMode
                      ? 'bg-slate-900 border-slate-700 hover:border-pink-500'
                      : 'bg-white border-slate-300 hover:border-pink-700'
                  }`}
                >
                  <p className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                    {card.label}
                  </p>
                  <p
                    className={`text-2xl font-black mt-1 ${
                      card.isAlert
                        ? 'text-red-600 dark:text-red-400'
                        : card.isWarning
                        ? 'text-amber-600 dark:text-amber-400'
                        : 'text-pink-900 dark:text-pink-300'
                    }`}
                  >
                    {typeof card.val === 'number'
                      ? formatDigits(card.val, useBnDigits)
                      : card.val}
                  </p>
                </div>
              ))}
            </div>

            {/* HIGH-CONTRAST PRIMARY ACTION BUTTONS */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <button
                onClick={() => {
                  resetForm();
                  setActiveTab('create');
                }}
                className="fa-btn-primary py-3.5 px-4 rounded-2xl shadow-md text-sm flex items-center justify-center gap-2 active:scale-95 transition-all"
              >
                <PlusCircle className="w-5 h-5" />
                {lang === 'bn' ? '+ নতুন অর্ডার' : '+ New Order'}
              </button>
              <button
                onClick={() => setActiveTab('smartPaste')}
                className="fa-btn-secondary py-3.5 px-4 rounded-2xl shadow-md text-sm flex items-center justify-center gap-2 active:scale-95 transition-all"
              >
                <ClipboardPaste className="w-5 h-5" />
                {lang === 'bn' ? 'স্মার্ট পেস্ট' : 'Smart Paste'}
              </button>
              <button
                onClick={() => setActiveTab('settings')}
                className="fa-btn-dark py-3.5 px-4 rounded-2xl shadow-md text-sm flex items-center justify-center gap-2 active:scale-95 transition-all"
              >
                <Settings className="w-5 h-5" />
                <span>কুরিয়ার API সেটিংস</span>
              </button>
            </div>
          </div>
        )}

        {/* ORDERS LIST TAB */}
        {activeTab === 'orders' && (
          <div className="space-y-4">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="w-5 h-5 absolute left-3.5 top-3.5 text-slate-500" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder={
                    lang === 'bn'
                      ? 'অর্ডার আইডি, নাম, মোবাইল, ট্র্যাকিং কোড দিয়ে খুঁজুন...'
                      : 'Search by Order ID, Name, Phone, Tracking Code...'
                  }
                  className={`w-full pl-11 pr-4 py-3 rounded-2xl border-2 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-pink-600 ${
                    darkMode
                      ? 'bg-slate-900 border-slate-700 text-white placeholder-slate-500'
                      : 'bg-white border-slate-300 text-slate-900 placeholder-slate-400'
                  }`}
                />
              </div>
              {metrics.courierPending > 0 && (
                <button
                  onClick={handleBulkSync}
                  disabled={isSyncing}
                  className="px-4 py-2.5 bg-amber-600 hover:bg-amber-700 text-white text-xs font-black rounded-2xl flex items-center gap-1.5 shadow active:scale-95 disabled:opacity-50"
                >
                  <RefreshCw className={`w-4 h-4 ${isSyncing ? 'animate-spin' : ''}`} />
                  Sync ({metrics.courierPending})
                </button>
              )}
            </div>

            {/* 5 FILTERS GRID (HIGH CONTRAST) */}
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 text-xs">
              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value as OrderType | 'All')}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-900 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                <option value="All">Type: All</option>
                <option value="Regular">Regular</option>
                <option value="Pre-order">Pre-order</option>
                <option value="Free">Free</option>
                <option value="Exchange">Exchange</option>
              </select>
              <select
                value={filterPriority}
                onChange={(e) => setFilterPriority(e.target.value as OrderPriority | 'All')}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-900 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                <option value="All">Priority: All</option>
                <option value="Normal">Normal</option>
                <option value="Urgent">Urgent</option>
              </select>
              <select
                value={filterPayment}
                onChange={(e) => setFilterPayment(e.target.value as PaymentStatus | 'All')}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-900 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                <option value="All">Payment: All</option>
                <option value="Unpaid">Unpaid</option>
                <option value="Partially Paid">Partially Paid</option>
                <option value="Paid">Paid</option>
              </select>
              <select
                value={filterFulfillment}
                onChange={(e) => setFilterFulfillment(e.target.value as FulfillmentStatus | 'All')}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-900 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                <option value="All">Status: All</option>
                <option value="New">New</option>
                <option value="Confirmed">Confirmed</option>
                <option value="Ready to Ship">Ready to Ship</option>
                <option value="Shipped">Shipped</option>
                <option value="Delivered">Delivered</option>
              </select>
              <select
                value={filterSchedule}
                onChange={(e) => setFilterSchedule(e.target.value as DeliveryScheduleState | 'All')}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-900 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                <option value="All">Schedule: All</option>
                <option value="Scheduled">Scheduled</option>
                <option value="Unscheduled">Unscheduled</option>
              </select>
            </div>

            {filteredOrders.length === 0 && (
              <div
                className={`text-center py-12 rounded-2xl border-2 font-bold text-sm ${
                  darkMode ? 'bg-slate-900 border-slate-800 text-slate-400' : 'bg-white border-slate-300 text-slate-500'
                }`}
              >
                {lang === 'bn' ? 'কোনো অর্ডার পাওয়া যায়নি' : 'No orders found'}
              </div>
            )}

            {filteredOrders.map((order) => {
              const scheduleState = Boolean(order.requestedDeliveryDate?.trim() || order.earliestDispatchDate?.trim())
                ? 'Scheduled'
                : 'Unscheduled';
              const items = itemsByOrder[order.id] || [];

              const courierStatusColor =
                order.courierSyncStatus === 'sent'
                  ? 'bg-emerald-100 text-emerald-950 border-emerald-400 dark:bg-emerald-950 dark:text-emerald-200'
                  : order.courierSyncStatus === 'queued_offline'
                  ? 'bg-amber-100 text-amber-950 border-amber-400 dark:bg-amber-950 dark:text-amber-200'
                  : order.courierSyncStatus === 'failed'
                  ? 'bg-rose-100 text-rose-950 border-rose-400 dark:bg-rose-950 dark:text-rose-200'
                  : order.courierSyncStatus === 'sending'
                  ? 'bg-sky-100 text-sky-950 border-sky-400 dark:bg-sky-950 dark:text-sky-200'
                  : 'bg-slate-200 text-slate-900 border-slate-400 dark:bg-slate-800 dark:text-slate-300';

              const firstPhoto = items.find((i) => i.productImageBase64)?.productImageBase64;

              return (
                <div
                  key={order.id}
                  className={`p-4 rounded-2xl border-2 shadow-sm space-y-3 transition-colors ${
                    darkMode
                      ? 'bg-slate-900 border-slate-800 hover:border-slate-700'
                      : 'bg-white border-slate-300 hover:border-slate-400'
                  }`}
                >
                  {/* DO NOT DISPATCH BANNER */}
                  {order.earliestDispatchDate && (
                    <div className="p-2.5 rounded-xl bg-red-600 text-white text-xs font-black flex items-center justify-between shadow">
                      <div className="flex items-center gap-1.5">
                        <AlertOctagon className="w-4 h-4 shrink-0" />
                        <span>Do Not Dispatch Before: {order.earliestDispatchDate}</span>
                      </div>
                      {order.courierSyncStatus !== 'sent' && (
                        <button
                          onClick={() => handleSendToCourier(order.id, true)}
                          className="px-2 py-1 bg-white text-red-700 rounded-lg text-[11px] font-black hover:bg-red-50"
                        >
                          Override & Send
                        </button>
                      )}
                    </div>
                  )}

                  <div className="flex justify-between items-start gap-3">
                    <div className="flex gap-3">
                      {/* PRODUCT VARIANT PHOTO THUMBNAIL */}
                      {firstPhoto ? (
                        <img
                          src={firstPhoto}
                          alt="Variant"
                          className="w-14 h-14 rounded-xl object-cover border-2 border-slate-300 dark:border-slate-700 shrink-0"
                        />
                      ) : (
                        <div
                          className={`w-14 h-14 rounded-xl flex items-center justify-center shrink-0 border-2 ${
                            darkMode ? 'bg-slate-800 border-slate-700 text-slate-500' : 'bg-slate-100 border-slate-300 text-slate-400'
                          }`}
                        >
                          <ImageIcon className="w-6 h-6" />
                        </div>
                      )}

                      <div>
                        <div className="flex items-center gap-2">
                          <span
                            onClick={() => populateEditOrder(order, items)}
                            className="font-mono font-black text-pink-900 dark:text-pink-400 text-base cursor-pointer hover:underline"
                          >
                            {order.orderNumber}
                          </span>
                        </div>
                        <p className="font-bold text-sm text-slate-900 dark:text-slate-100 mt-0.5">
                          {order.customerName} — <span className="font-mono">{order.primaryPhone}</span>
                        </p>
                        <p className="text-xs text-slate-600 dark:text-slate-400 font-medium">
                          {order.fullAddress}, {order.thanaUpazila}, {order.district}
                        </p>
                      </div>
                    </div>

                    <div className="text-right shrink-0">
                      <div className="font-black text-base text-slate-900 dark:text-slate-100">
                        {formatBDT(order.totalBill, useBnDigits)}
                      </div>
                      <div className="text-xs font-black text-emerald-800 dark:text-emerald-400">
                        COD: {formatBDT(Math.max(0, order.outstanding), useBnDigits)}
                      </div>
                    </div>
                  </div>

                  {/* ITEMS LIST */}
                  <div className="text-xs space-y-0.5 font-semibold text-pink-900 dark:text-pink-300 bg-pink-50/70 dark:bg-pink-950/30 p-2 rounded-xl">
                    {items.map((it) => (
                      <div key={it.id}>
                        • {it.productName} | Size: {it.size} × {it.quantity} (৳{it.unitPrice})
                      </div>
                    ))}
                    {/* SMART RETURN NOTE PREVIEW */}
                    <div className="text-[11px] text-amber-900 dark:text-amber-300 font-bold mt-1">
                      ⚠️ স্মার্ট রিটার্ন নোট: রিটার্ন করলে ডেলিভারি চার্জ ৳{order.deliveryCharge} নিয়ে নিবেন।
                    </div>
                  </div>

                  {/* 5 DIMENSION BADGES & COURIER STATUS */}
                  <div className="flex flex-wrap gap-1.5 text-[11px] font-black items-center">
                    <span className="px-2 py-0.5 rounded-md bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200">
                      {order.orderType}
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded-md ${
                        order.priority === 'Urgent'
                          ? 'bg-red-200 text-red-900 dark:bg-red-950 dark:text-red-200'
                          : 'bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200'
                      }`}
                    >
                      {order.priority}
                    </span>
                    <span className="px-2 py-0.5 rounded-md bg-emerald-200 text-emerald-950 dark:bg-emerald-950 dark:text-emerald-200">
                      {order.paymentStatus}
                    </span>
                    <span className="px-2 py-0.5 rounded-md bg-blue-200 text-blue-950 dark:bg-blue-950 dark:text-blue-200">
                      {order.fulfillmentStatus}
                    </span>
                    <span className="px-2 py-0.5 rounded-md bg-purple-200 text-purple-950 dark:bg-purple-950 dark:text-purple-200">
                      {scheduleState}
                    </span>

                    {/* COURIER BADGE */}
                    <span className={`px-2 py-0.5 rounded-md border font-black flex items-center gap-1 ${courierStatusColor}`}>
                      <Truck className="w-3.5 h-3.5" />
                      {order.courier}: {order.courierSyncStatus}
                      {order.trackingNumber && ` (#${order.trackingNumber})`}
                    </span>
                  </div>

                  {order.courierLastError && (
                    <div className="text-xs text-rose-800 dark:text-rose-300 bg-rose-100 dark:bg-rose-950/60 p-2 rounded-xl font-bold border border-rose-300 dark:border-rose-900">
                      Courier Error: {order.courierLastError}
                    </div>
                  )}

                  {/* ACTIONS */}
                  <div className="flex flex-wrap gap-2 pt-2 border-t border-slate-200 dark:border-slate-800 text-xs font-black items-center">
                    {order.courierSyncStatus !== 'sent' ? (
                      <button
                        onClick={() => handleSendToCourier(order.id, false)}
                        disabled={isSyncing}
                        className="fa-btn-secondary px-3.5 py-2 rounded-xl flex items-center gap-1.5 shadow active:scale-95 disabled:opacity-50"
                      >
                        <Send className="w-3.5 h-3.5" />
                        Send to {order.courier}
                      </button>
                    ) : (
                      <span className="px-3 py-1.5 text-emerald-900 dark:text-emerald-200 bg-emerald-100 dark:bg-emerald-950/70 rounded-xl border border-emerald-400 font-mono font-bold flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
                        Track: {order.trackingNumber}
                      </span>
                    )}

                    <button
                      onClick={() => handleCopyForCourier(order)}
                      className="fa-btn-primary px-3 py-2 rounded-xl flex items-center gap-1.5 shadow active:scale-95"
                    >
                      <Copy className="w-3.5 h-3.5" />
                      Copy Slip
                    </button>

                    <button
                      onClick={() => window.print()}
                      className="px-3 py-2 rounded-xl border-2 border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-200 flex items-center gap-1 hover:bg-slate-50 dark:hover:bg-slate-700"
                    >
                      <Printer className="w-3.5 h-3.5" />
                      Print
                    </button>

                    <button
                      onClick={() => populateEditOrder(order, items)}
                      className="px-3 py-2 rounded-xl border-2 border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-200 hover:bg-slate-50"
                    >
                      Edit
                    </button>

                    <button
                      onClick={() => handleSoftDelete(order.id)}
                      className="p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-xl ml-auto"
                      title="Move to Trash"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* SMART PASTE TAB */}
        {activeTab === 'smartPaste' && (
          <div
            className={`p-5 rounded-2xl border-2 shadow-sm space-y-4 ${
              darkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-300'
            }`}
          >
            <h2 className="font-black text-lg text-pink-900 dark:text-pink-400">
              {lang === 'bn' ? 'স্মার্ট পেস্ট (Offline Order Message Parser)' : 'Smart Paste (Offline Order Message Parser)'}
            </h2>
            <p className="text-xs text-slate-500 font-semibold">
              WhatsApp বা Facebook মেসেজ হুবহু পেস্ট করুন। নাম, মোবাইল, সাইজ, দাম ও ডেলিভারি অটোমেটিক ডিটেক্ট হবে।
            </p>
            <textarea
              rows={6}
              value={rawPasteText}
              onChange={(e) => setRawPasteText(e.target.value)}
              placeholder={`নাম: তানিয়া আক্তার\nমোবাইল: ০১৭১২৩৪৫৬৭৮\nঠিকানা: বাড়ি ১২, রোড ৫, ধানমন্ডি, ঢাকা\nপণ্য: Organza Three Piece\nসাইজ: 42\nদাম: 1650\nঅগ্রিম: 200\n২৮ তারিখে ডেলিভারি, ২৬ তারিখের আগে পাঠাবেন না`}
              className={`w-full p-3.5 rounded-xl border-2 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-pink-600 ${
                darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
              }`}
            />
            <button
              onClick={handleSmartFill}
              className="fa-btn-secondary w-full py-3.5 rounded-2xl font-black text-sm flex items-center justify-center gap-2 shadow-md active:scale-95"
            >
              <ClipboardPaste className="w-4 h-4" />
              {lang === 'bn' ? 'স্মার্ট ফিল করুন (রিভিউ ফর্মে যান)' : 'Smart Fill -> Review Form'}
            </button>
          </div>
        )}

        {/* CREATE / EDIT ORDER TAB (WITH PRODUCT VARIANT PHOTO UPLOAD) */}
        {activeTab === 'create' && (
          <form
            onSubmit={handleSaveOrder}
            className={`p-5 rounded-2xl border-2 shadow-sm space-y-4 ${
              darkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-300'
            }`}
          >
            <h2 className="font-black text-lg text-pink-900 dark:text-pink-400">
              {editingOrderId
                ? lang === 'bn'
                  ? 'অর্ডার এডিট করুন'
                  : 'Edit Order'
                : lang === 'bn'
                ? 'নতুন অর্ডার ফর্ম'
                : 'Create New Order'}
            </h2>

            {/* PRODUCT PHOTO VARIANT IDENTIFICATION */}
            <div className={`p-4 rounded-2xl border-2 space-y-2 ${darkMode ? 'bg-slate-950 border-slate-800' : 'bg-slate-50 border-slate-200'}`}>
              <label className="block text-xs font-black uppercase tracking-wider text-pink-900 dark:text-pink-400">
                Product Variant Photo (ছবি দেখে ভ্যারিয়েন্ট আইডেন্টিফাই করুন)
              </label>
              <div className="flex items-center gap-3">
                {productImageBase64 ? (
                  <div className="relative">
                    <img
                      src={productImageBase64}
                      alt="Variant Preview"
                      className="w-16 h-16 rounded-xl object-cover border-2 border-pink-700 shadow"
                    />
                    <button
                      type="button"
                      onClick={() => setProductImageBase64('')}
                      className="absolute -top-1.5 -right-1.5 bg-red-600 text-white p-1 rounded-full shadow"
                      title="Remove Photo"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                ) : (
                  <div
                    className={`w-16 h-16 rounded-xl flex items-center justify-center border-2 border-dashed ${
                      darkMode ? 'bg-slate-900 border-slate-700 text-slate-500' : 'bg-white border-slate-300 text-slate-400'
                    }`}
                  >
                    <Camera className="w-7 h-7" />
                  </div>
                )}
                <div className="flex-1">
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleImageUpload}
                    className="block w-full text-xs text-slate-500 file:mr-3 file:py-2 file:px-3 file:rounded-xl file:border-0 file:text-xs file:font-black file:bg-pink-900 file:text-white hover:file:bg-pink-800 cursor-pointer"
                  />
                  <p className="text-[11px] text-slate-400 font-medium mt-1">
                    ক্যামেরা দিয়ে ছবি তুলুন বা গ্যালারি থেকে সিলেক্ট করুন
                  </p>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <input
                type="text"
                placeholder="Customer Name *"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                className={`w-full px-3.5 py-2.5 rounded-xl border-2 font-bold text-sm ${
                  darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              />
              <input
                type="tel"
                placeholder="Phone 01XXXXXXXXX *"
                value={primaryPhone}
                onChange={(e) => setPrimaryPhone(e.target.value)}
                className={`w-full px-3.5 py-2.5 rounded-xl border-2 font-bold font-mono text-sm ${
                  darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              />
            </div>

            <textarea
              rows={2}
              placeholder="Full Delivery Address *"
              value={fullAddress}
              onChange={(e) => setFullAddress(e.target.value)}
              className={`w-full p-3.5 rounded-xl border-2 font-bold text-sm ${
                darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
              }`}
            />

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-sm">
              <select
                value={district}
                onChange={(e) => setDistrict(e.target.value)}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                {BD_DISTRICTS.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
              <select
                value={orderType}
                onChange={(e) => setOrderType(e.target.value as OrderType)}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                <option value="Regular">Regular</option>
                <option value="Pre-order">Pre-order</option>
                <option value="Free">Free</option>
                <option value="Exchange">Exchange</option>
              </select>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value as OrderPriority)}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                <option value="Normal">Normal</option>
                <option value="Urgent">Urgent</option>
              </select>
              <select
                value={courier}
                onChange={(e) => setCourier(e.target.value as CourierName)}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                <option value="Steadfast">Steadfast (Direct API)</option>
                <option value="Pathao">Pathao (Direct API)</option>
                <option value="RedX">RedX</option>
              </select>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-sm">
              <input
                type="text"
                placeholder="Product Name *"
                value={productName}
                onChange={(e) => setProductName(e.target.value)}
                className={`col-span-2 px-3.5 py-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              />
              <select
                value={size}
                onChange={(e) => setSize(e.target.value as ProductSize)}
                className={`p-2.5 rounded-xl border-2 font-bold ${
                  darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              >
                {(['36', '38', '40', '42', '44', '46', '48', 'Other'] as ProductSize[]).map((s) => (
                  <option key={s} value={s}>
                    Size {s}
                  </option>
                ))}
              </select>
              <input
                type="text"
                placeholder="Unit Price ৳"
                value={unitPrice}
                onChange={(e) => setUnitPrice(parseIntSafe(e.target.value))}
                className={`px-3.5 py-2.5 rounded-xl border-2 font-bold font-mono ${
                  darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                }`}
              />
            </div>

            <div className="grid grid-cols-2 gap-2 text-sm">
              <div>
                <label className="text-xs font-black text-red-600 dark:text-red-400 block mb-1">
                  Do Not Dispatch Before
                </label>
                <input
                  type="date"
                  value={earliestDispatchDate}
                  onChange={(e) => setEarliestDispatchDate(e.target.value)}
                  className={`w-full px-3 py-2 rounded-xl border-2 font-bold ${
                    darkMode ? 'bg-slate-950 border-red-900 text-white' : 'bg-white border-red-300 text-slate-900'
                  }`}
                />
              </div>
              <div>
                <label className="text-xs font-black text-slate-700 dark:text-slate-300 block mb-1">
                  Requested Delivery Date
                </label>
                <input
                  type="date"
                  value={requestedDeliveryDate}
                  onChange={(e) => setRequestedDeliveryDate(e.target.value)}
                  className={`w-full px-3 py-2 rounded-xl border-2 font-bold ${
                    darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                  }`}
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-2 text-sm">
              <div>
                <label className="text-xs font-bold block mb-1">Delivery ৳</label>
                <input
                  type="text"
                  value={deliveryCharge}
                  onChange={(e) => setDeliveryCharge(parseIntSafe(e.target.value))}
                  className={`w-full px-3 py-2 rounded-xl border-2 font-bold font-mono ${
                    darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                  }`}
                />
              </div>
              <div>
                <label className="text-xs font-bold block mb-1">Discount ৳</label>
                <input
                  type="text"
                  value={discount}
                  onChange={(e) => setDiscount(parseIntSafe(e.target.value))}
                  className={`w-full px-3 py-2 rounded-xl border-2 font-bold font-mono ${
                    darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                  }`}
                />
              </div>
              <div>
                <label className="text-xs font-bold block mb-1">Advance ৳</label>
                <input
                  type="text"
                  value={advancePaid}
                  onChange={(e) => setAdvancePaid(parseIntSafe(e.target.value))}
                  className={`w-full px-3 py-2 rounded-xl border-2 font-bold font-mono ${
                    darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
                  }`}
                />
              </div>
            </div>

            <textarea
              rows={2}
              placeholder="Customer Delivery Instructions (Printed on Courier Slip)"
              value={customerInstructions}
              onChange={(e) => setCustomerInstructions(e.target.value)}
              className={`w-full p-3 rounded-xl border-2 font-bold text-sm ${
                darkMode ? 'bg-slate-950 border-slate-700 text-white' : 'bg-white border-slate-300 text-slate-900'
              }`}
            />

            {/* LIVE FINANCIAL CALCULATION BANNER */}
            <div className="p-4 rounded-2xl bg-pink-950 text-white flex justify-between items-center shadow-md">
              <span className="font-bold text-sm">Total: {formatBDT(liveCalc.totalBill, useBnDigits)}</span>
              <span className="font-black text-base text-pink-300">
                COD: {formatBDT(liveCalc.codAmount, useBnDigits)}
              </span>
            </div>

            <button
              type="submit"
              className="fa-btn-primary w-full py-4 rounded-2xl font-black text-base shadow-lg active:scale-95 transition-all"
            >
              {lang === 'bn' ? 'অর্ডার সেভ করুন' : 'Save Order'}
            </button>
          </form>
        )}

        {/* COURIER SETTINGS TAB */}
        {activeTab === 'settings' && (
          <div className="space-y-4">
            <h2 className="font-black text-lg text-pink-900 dark:text-pink-400 flex items-center gap-2">
              <Truck className="w-6 h-6" />
              Direct Courier API Settings (Steadfast & Pathao)
            </h2>
            <CourierSettingsPanel lang={lang} onSaved={(m) => showToast(m)} />
          </div>
        )}

        {/* BACKUP & RESTORE / TRASH TAB */}
        {activeTab === 'backup' && (
          <div
            className={`p-5 rounded-2xl border-2 shadow-sm space-y-4 ${
              darkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-300'
            }`}
          >
            <h2 className="font-black text-lg text-pink-900 dark:text-pink-400">Backup, Export & Trash</h2>
            <div className="flex flex-col sm:flex-row gap-3">
              <button
                onClick={exportOrdersToCSV}
                className="fa-btn-secondary flex-1 py-3.5 px-4 rounded-2xl font-black text-sm flex items-center justify-center gap-2 shadow active:scale-95"
              >
                <Download className="w-4 h-4" />
                Export Excel CSV (UTF-8 Bangla)
              </button>
              <button
                onClick={exportFullBackupJSON}
                className="fa-btn-dark flex-1 py-3.5 px-4 rounded-2xl font-black text-sm flex items-center justify-center gap-2 shadow active:scale-95"
              >
                <Download className="w-4 h-4" />
                Full JSON Backup
              </button>
            </div>

            <div className="pt-4 border-t border-slate-200 dark:border-slate-800 space-y-2">
              <h3 className="font-black text-sm">Trash ({trashedOrders.length})</h3>
              {trashedOrders.length === 0 && (
                <p className="text-xs text-slate-400 font-semibold">
                  {lang === 'bn' ? 'ট্র্যাশে কোনো অর্ডার নেই' : 'No orders in trash'}
                </p>
              )}
              {trashedOrders.map((o) => (
                <div
                  key={o.id}
                  className={`flex justify-between items-center p-3 border-2 rounded-xl text-xs font-bold ${
                    darkMode ? 'bg-slate-950 border-slate-800' : 'bg-slate-50 border-slate-200'
                  }`}
                >
                  <span>
                    {o.orderNumber} — {o.customerName}
                  </span>
                  <button
                    onClick={() => handleRestore(o.id)}
                    className="px-3 py-1.5 rounded-xl bg-emerald-700 text-white font-bold flex items-center gap-1 shadow"
                  >
                    <RotateCcw className="w-3.5 h-3.5" />
                    Restore
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* BOTTOM NAVIGATION */}
      <nav
        className={`fixed bottom-0 left-0 right-0 border-t-2 flex justify-around py-2 z-40 text-xs font-black shadow-2xl ${
          darkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-300'
        }`}
      >
        <button
          onClick={() => setActiveTab('dashboard')}
          className={`flex flex-col items-center gap-0.5 ${
            activeTab === 'dashboard' ? 'text-pink-700 dark:text-pink-400' : 'text-slate-500'
          }`}
        >
          Dashboard
        </button>
        <button
          onClick={() => setActiveTab('orders')}
          className={`flex flex-col items-center gap-0.5 ${
            activeTab === 'orders' ? 'text-pink-700 dark:text-pink-400' : 'text-slate-500'
          }`}
        >
          Orders ({activeOrders.length})
        </button>
        <button
          onClick={() => {
            resetForm();
            setActiveTab('create');
          }}
          className={`flex flex-col items-center gap-0.5 ${
            activeTab === 'create' ? 'text-pink-700 dark:text-pink-400' : 'text-slate-500'
          }`}
        >
          + Add
        </button>
        <button
          onClick={() => setActiveTab('smartPaste')}
          className={`flex flex-col items-center gap-0.5 ${
            activeTab === 'smartPaste' ? 'text-pink-700 dark:text-pink-400' : 'text-slate-500'
          }`}
        >
          Paste
        </button>
        <button
          onClick={() => setActiveTab('settings')}
          className={`flex flex-col items-center gap-0.5 ${
            activeTab === 'settings' ? 'text-pink-700 dark:text-pink-400' : 'text-slate-500'
          }`}
        >
          Courier API
        </button>
        <button
          onClick={() => setActiveTab('backup')}
          className={`flex flex-col items-center gap-0.5 ${
            activeTab === 'backup' ? 'text-pink-700 dark:text-pink-400' : 'text-slate-500'
          }`}
        >
          Backup
        </button>
      </nav>
    </div>
  );
};
