import React, { useEffect, useState } from 'react';
import { ShieldCheck, Save, Truck } from 'lucide-react';
import { db, ensureDefaultCourierConfigs } from '../db/database';
import type { CourierApiConfigRecord } from '../types/models';

export const CourierSettingsPanel: React.FC<{
  lang: 'bn' | 'en';
  onSaved: (m: string) => void;
}> = ({ lang, onSaved }) => {
  const [configs, setConfigs] = useState<CourierApiConfigRecord[]>([]);

  useEffect(() => {
    ensureDefaultCourierConfigs().then(async () => {
      setConfigs(await db.courierConfigs.toArray());
    });
  }, []);

  const handleChange = (courier: string, field: string, val: any) => {
    setConfigs((prev) =>
      prev.map((c) => (c.courier === courier ? { ...c, [field]: val } : c))
    );
  };

  const handleSave = async (cfg: CourierApiConfigRecord) => {
    await db.courierConfigs.put({ ...cfg, updatedAt: new Date().toISOString() });
    onSaved(`${cfg.courier} API settings saved locally!`);
  };

  return (
    <div className="space-y-4">
      <div className="p-3 bg-amber-50 border border-amber-300 text-xs rounded-xl text-amber-900">
        <ShieldCheck className="w-4 h-4 inline mr-1 text-amber-800" />
        {lang === 'bn'
          ? 'এপিআই কি আপনার ফোনের IndexedDB-তে সুরক্ষিত থাকে।'
          : 'API keys stored securely in local IndexedDB.'}
      </div>

      {configs.map((cfg) => (
        <div
          key={cfg.courier}
          className="bg-white p-4 rounded-2xl border space-y-3 shadow-sm"
        >
          <div className="flex justify-between items-center border-b pb-2">
            <span className="font-bold text-pink-900 flex items-center gap-1.5">
              <Truck className="w-4 h-4" />
              {cfg.courier} API
            </span>
            <label className="text-xs font-bold flex items-center gap-1 cursor-pointer">
              <input
                type="checkbox"
                checked={cfg.enabled}
                onChange={(e) =>
                  handleChange(cfg.courier, 'enabled', e.target.checked)
                }
              />
              {cfg.enabled ? 'Enabled' : 'Disabled'}
            </label>
          </div>

          <div className="space-y-2 text-xs">
            <div>
              <label className="block text-[11px] font-semibold text-slate-500 mb-1">
                Base URL
              </label>
              <input
                type="url"
                value={cfg.baseUrl}
                onChange={(e) =>
                  handleChange(cfg.courier, 'baseUrl', e.target.value)
                }
                placeholder="Base URL"
                className="w-full p-2 border rounded font-mono"
              />
            </div>

            {cfg.courier === 'Pathao' && (
              <div>
                <label className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Store ID
                </label>
                <input
                  type="text"
                  value={cfg.storeId || ''}
                  onChange={(e) =>
                    handleChange(cfg.courier, 'storeId', e.target.value)
                  }
                  placeholder="Store ID"
                  className="w-full p-2 border rounded font-mono"
                />
              </div>
            )}

            {cfg.courier === 'Steadfast' ? (
              <>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-500 mb-1">
                    API Key
                  </label>
                  <input
                    type="password"
                    value={cfg.apiKey}
                    onChange={(e) =>
                      handleChange(cfg.courier, 'apiKey', e.target.value)
                    }
                    placeholder="Steadfast API Key"
                    className="w-full p-2 border rounded font-mono"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-slate-500 mb-1">
                    Secret Key
                  </label>
                  <input
                    type="password"
                    value={cfg.secretKeyOrToken}
                    onChange={(e) =>
                      handleChange(cfg.courier, 'secretKeyOrToken', e.target.value)
                    }
                    placeholder="Steadfast Secret Key"
                    className="w-full p-2 border rounded font-mono"
                  />
                </div>
              </>
            ) : (
              <div>
                <label className="block text-[11px] font-semibold text-slate-500 mb-1">
                  Bearer Token
                </label>
                <input
                  type="password"
                  value={cfg.secretKeyOrToken}
                  onChange={(e) =>
                    handleChange(cfg.courier, 'secretKeyOrToken', e.target.value)
                  }
                  placeholder="Pathao Bearer Token"
                  className="w-full p-2 border rounded font-mono"
                />
              </div>
            )}
          </div>

          <button
            onClick={() => handleSave(cfg)}
            className="px-3 py-2 bg-pink-800 text-white font-bold text-xs rounded-xl flex items-center gap-1 hover:bg-pink-900"
          >
            <Save className="w-3.5 h-3.5" />
            Save {cfg.courier}
          </button>
        </div>
      ))}
    </div>
  );
};
