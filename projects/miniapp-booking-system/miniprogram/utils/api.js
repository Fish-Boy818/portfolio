const BASE_URLS = {
  local: "http://127.0.0.1:8080",
  cloud: "https://example.com"
};
const BASE_URL_KEY = "api_base_url";
const TOKEN_KEY = "auth_token";
const DEFAULT_TIMEOUT = 10000;

function isDevtools() {
  try {
    if (typeof wx.getDeviceInfo === "function") {
      const info = wx.getDeviceInfo();
      return info && info.platform === "devtools";
    }
    if (typeof wx.getAppBaseInfo === "function") {
      const baseInfo = wx.getAppBaseInfo();
      return baseInfo && baseInfo.platform === "devtools";
    }
    if (typeof wx.getSystemInfoSync === "function") {
      const info = wx.getSystemInfoSync();
      return info && info.platform === "devtools";
    }
  } catch (e) {
    return false;
  }
  return false;
}

function normalizeUploads(value, baseUrl) {
  if (Array.isArray(value)) {
    return value.map((item) => normalizeUploads(item, baseUrl));
  }
  if (value && typeof value === "object") {
    const next = {};
    Object.keys(value).forEach((key) => {
      next[key] = normalizeUploads(value[key], baseUrl);
    });
    return next;
  }
  if (typeof value === "string" && value.startsWith("/uploads/")) {
    return `${baseUrl}${value}`;
  }
  return value;
}

function getEnvVersion() {
  try {
    const info = wx.getAccountInfoSync();
    return info && info.miniProgram ? info.miniProgram.envVersion : "release";
  } catch (e) {
    return "release";
  }
}

function resolveBaseUrl() {
  const env = getEnvVersion();
  const allowLocal = env === "develop";
  // In trial/release, always force cloud domain to avoid stale local IP in storage.
  if (!allowLocal) {
    return BASE_URLS.cloud;
  }
  try {
    const saved = wx.getStorageSync(BASE_URL_KEY);
    if (saved && (saved === BASE_URLS.cloud || saved === BASE_URLS.local)) {
      return saved;
    }
  } catch (e) {
    // ignore
  }
  return BASE_URLS.cloud;
}

function setBaseUrl(url) {
  try {
    wx.setStorageSync(BASE_URL_KEY, url);
  } catch (e) {
    // ignore
  }
}

function getBaseUrl() {
  return resolveBaseUrl();
}

function getToken() {
  try {
    return wx.getStorageSync(TOKEN_KEY);
  } catch (e) {
    return "";
  }
}

function setToken(token) {
  try {
    wx.setStorageSync(TOKEN_KEY, token);
  } catch (e) {
    // ignore storage failure
  }
}

function shouldFallbackToCloud(baseUrl, errMsg) {
  if (!baseUrl || !baseUrl.startsWith(BASE_URLS.local)) {
    return false;
  }
  const text = String(errMsg || "").toLowerCase();
  return (
    text.includes("err_connection_refused") ||
    text.includes("connection refused") ||
    text.includes("failed to connect") ||
    text.includes("request:fail")
  );
}

function request(options, retried) {
  const baseUrl = resolveBaseUrl();
  const token = getToken();
  const data = options.data || {};
  const payload = {};
  Object.keys(data).forEach((key) => {
    const value = data[key];
    if (value !== null && typeof value !== "undefined") {
      payload[key] = value;
    }
  });
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${baseUrl}${options.url}`,
      method: options.method || "GET",
      data: payload,
      timeout: options.timeout || DEFAULT_TIMEOUT,
      header: Object.assign(
        {
          "Content-Type": "application/json"
        },
        token ? { Authorization: token } : {},
        options.header || {}
      ),
      success(res) {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(`接口异常(${res.statusCode})`);
          return;
        }
        const body = res.data;
        if (body && typeof body.code !== "undefined") {
          if (body.code === 0) {
            resolve(normalizeUploads(body.data, baseUrl));
          } else {
            reject(body.message || "请求失败");
          }
          return;
        }
        resolve(body);
      },
      fail(err) {
        const errMsg = err && err.errMsg ? err.errMsg : "网络异常";
        if (!retried && shouldFallbackToCloud(baseUrl, errMsg)) {
          setBaseUrl(BASE_URLS.cloud);
          request(options, true).then(resolve).catch(reject);
          return;
        }
        reject(errMsg);
      }
    });
  });
}

module.exports = {
  BASE_URLS,
  getBaseUrl,
  setBaseUrl,
  TOKEN_KEY,
  getToken,
  setToken,
  request
};
