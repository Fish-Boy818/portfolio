const PRODUCT_CACHE_KEY = "__product_prefetch_cache__";
const DEFAULT_CACHE_TTL = 5 * 60 * 1000;

function getGlobalData() {
  try {
    const app = getApp ? getApp() : null;
    if (app && app.globalData) {
      return app.globalData;
    }
  } catch (e) {
    return null;
  }
  return null;
}

function ensureCacheStore() {
  const globalData = getGlobalData();
  if (!globalData) {
    return null;
  }
  if (!globalData[PRODUCT_CACHE_KEY]) {
    globalData[PRODUCT_CACHE_KEY] = {};
  }
  return globalData[PRODUCT_CACHE_KEY];
}

function toValidId(value) {
  const id = Number(value);
  if (!Number.isFinite(id) || id <= 0) {
    return 0;
  }
  return id;
}

function cacheProductPrefetch(product) {
  const id = toValidId(product && product.id);
  if (!id || !product) {
    return;
  }
  const store = ensureCacheStore();
  if (!store) {
    return;
  }
  store[id] = {
    at: Date.now(),
    data: { ...product }
  };
}

function getProductPrefetch(id, ttlMs) {
  const targetId = toValidId(id);
  if (!targetId) {
    return null;
  }
  const store = ensureCacheStore();
  if (!store || !store[targetId]) {
    return null;
  }
  const ttl = Number(ttlMs) > 0 ? Number(ttlMs) : DEFAULT_CACHE_TTL;
  const item = store[targetId];
  if (!item || !item.data || !item.at || Date.now() - Number(item.at) > ttl) {
    delete store[targetId];
    return null;
  }
  return { ...item.data };
}

module.exports = {
  cacheProductPrefetch,
  getProductPrefetch
};
