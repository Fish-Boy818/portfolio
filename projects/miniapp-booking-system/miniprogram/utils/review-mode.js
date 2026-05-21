const REVIEW_MODE_STORAGE_KEY = "review_mode_enabled";
const REVIEW_MODE_FALLBACK_ENV = "";

function detectEnvVersion() {
  try {
    const info = wx.getAccountInfoSync();
    return info && info.miniProgram ? String(info.miniProgram.envVersion || "").trim() : "release";
  } catch (e) {
    return "release";
  }
}

function parseReviewModeFlag(value) {
  if (value === null || typeof value === "undefined") {
    return null;
  }
  if (typeof value === "boolean") {
    return value;
  }
  if (typeof value === "number") {
    return value === 1;
  }
  const text = String(value).trim().toLowerCase();
  if (!text) {
    return null;
  }
  if (text === "1" || text === "true" || text === "on" || text === "enabled") {
    return true;
  }
  if (text === "0" || text === "false" || text === "off" || text === "disabled") {
    return false;
  }
  return null;
}

function getCachedReviewMode() {
  try {
    const value = wx.getStorageSync(REVIEW_MODE_STORAGE_KEY);
    return parseReviewModeFlag(value);
  } catch (e) {
    return null;
  }
}

function setCachedReviewMode(value) {
  const parsed = parseReviewModeFlag(value);
  if (parsed === null) {
    return;
  }
  try {
    wx.setStorageSync(REVIEW_MODE_STORAGE_KEY, parsed ? 1 : 0);
  } catch (e) {
    // ignore
  }
}

function resolveReviewModeFromContent(content) {
  if (!content || typeof content !== "object") {
    return null;
  }
  return parseReviewModeFlag(content.reviewModeEnabled);
}

function fallbackReviewModeByEnv() {
  if (!REVIEW_MODE_FALLBACK_ENV) {
    return false;
  }
  return detectEnvVersion() === REVIEW_MODE_FALLBACK_ENV;
}

function isReviewMode() {
  try {
    const app = getApp ? getApp() : null;
    if (app && app.globalData && typeof app.globalData.reviewMode !== "undefined") {
      return !!app.globalData.reviewMode;
    }
  } catch (e) {
    // ignore
  }
  const cached = getCachedReviewMode();
  if (cached !== null) {
    return cached;
  }
  return fallbackReviewModeByEnv();
}

module.exports = {
  detectEnvVersion,
  parseReviewModeFlag,
  getCachedReviewMode,
  setCachedReviewMode,
  resolveReviewModeFromContent,
  isReviewMode
};
