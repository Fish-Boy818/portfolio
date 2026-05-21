const { request, setToken } = require("./api");

const USER_KEY = "auth_user";
const LOGOUT_KEY = "auth_logout";
const SCAN_PENDING_KEY = "scan_pending_entry";
const INVITE_PENDING_KEY = "invite_pending_entry";
const INVITE_PENDING_TTL_MS = 10 * 60 * 1000;
const SCAN_SCENES = {
  "1011": true,
  "1012": true,
  "1013": true,
  "1047": true,
  "1048": true,
  "1049": true,
  "1124": true
};
let checked = false;

function isDevtools() {
  try {
    if (typeof wx.getSystemInfoSync === "function") {
      const info = wx.getSystemInfoSync();
      return info && info.platform === "devtools";
    }
  } catch (e) {
    return false;
  }
  return false;
}

function getCachedUser() {
  try {
    const user = wx.getStorageSync(USER_KEY);
    if (user && user.id) {
      return user;
    }
  } catch (e) {
    return null;
  }
  return null;
}

function isLoggedOut() {
  try {
    return wx.getStorageSync(LOGOUT_KEY) === "1";
  } catch (e) {
    return false;
  }
}

function setLoggedOut(value) {
  try {
    if (value) {
      wx.setStorageSync(LOGOUT_KEY, "1");
    } else {
      wx.removeStorageSync(LOGOUT_KEY);
    }
  } catch (e) {
    // ignore storage failure
  }
}

function setCachedUser(user) {
  try {
    wx.setStorageSync(USER_KEY, user);
  } catch (e) {
    // ignore
  }
}

function clearAuth() {
  try {
    wx.removeStorageSync(USER_KEY);
  } catch (e) {
    // ignore
  }
  setToken("");
  checked = false;
  setLoggedOut(true);
}

function markScanEntry(options) {
  const opts = options || {};
  const scene = opts.scene !== undefined && opts.scene !== null ? String(opts.scene) : "";
  const query = opts.query || {};
  const qRaw = query.q ? String(query.q) : "";
  let q = qRaw;
  if (qRaw) {
    try {
      q = decodeURIComponent(qRaw);
    } catch (e) {
      q = qRaw;
    }
  }
  const source = query.source || query.s || "";
  const isScan =
    !!SCAN_SCENES[scene] ||
    !!q ||
    String(query.scan || "").toLowerCase() === "1" ||
    String(source || "").toLowerCase() === "scan";
  if (!isScan) {
    return markInviteEntry(opts);
  }
  const inviterId = extractInviterId(opts);
  try {
    wx.setStorageSync(SCAN_PENDING_KEY, {
      scene,
      q,
      source,
      inviterId,
      at: Date.now()
    });
    if (inviterId) {
      wx.setStorageSync(INVITE_PENDING_KEY, {
        inviterId,
        source: source || "scan",
        mustLogin: true,
        at: Date.now()
      });
    }
    return true;
  } catch (e) {
    return false;
  }
}

function markInviteEntry(options) {
  const opts = options || {};
  const inviterId = extractInviterId(opts);
  if (!inviterId) {
    return false;
  }
  const query = opts.query || {};
  const source = String(query.source || query.s || "").trim() || "launch";
  try {
    wx.setStorageSync(INVITE_PENDING_KEY, {
      inviterId,
      source,
      mustLogin: true,
      at: Date.now()
    });
    return true;
  } catch (e) {
    return false;
  }
}

function getPendingScanEntry() {
  try {
    return wx.getStorageSync(SCAN_PENDING_KEY) || null;
  } catch (e) {
    return null;
  }
}

function clearPendingScanEntry() {
  try {
    wx.removeStorageSync(SCAN_PENDING_KEY);
  } catch (e) {
    // ignore
  }
}

function getPendingInviteEntry() {
  try {
    const raw = wx.getStorageSync(INVITE_PENDING_KEY) || null;
    return normalizePendingInviteEntry(raw);
  } catch (e) {
    return null;
  }
}

function clearPendingInviteEntry() {
  try {
    wx.removeStorageSync(INVITE_PENDING_KEY);
  } catch (e) {
    // ignore
  }
}

function hasPendingInviteForLogin() {
  const pending = getPendingInviteEntry();
  return !!(pending && pending.inviterId);
}

function invalidatePendingInvite(reason) {
  clearPendingInviteEntry();
  clearPendingScanEntry();
  return !!reason;
}

function tryActivateScanQualification() {
  const pending = getPendingScanEntry();
  if (!pending) {
    return Promise.resolve(false);
  }
  const cached = getCachedUser();
  if (!(cached && cached.id)) {
    return Promise.resolve(false);
  }
  return request({
    url: "/api/commission/scan/activate",
    method: "POST",
    data: {
      scene: pending.scene || "",
      source: pending.source || pending.q || ""
    }
  })
    .then(() => {
      clearPendingScanEntry();
      return refreshProfile(cached.id).catch(() => cached);
    })
    .then(() => true)
    .catch(() => false);
}

function tryBindInviteRelation() {
  const pending = getPendingInviteEntry();
  if (!pending || !pending.inviterId) {
    return Promise.resolve(false);
  }
  const cached = getCachedUser();
  if (!(cached && cached.id)) {
    return Promise.resolve(false);
  }
  return request({
    url: "/api/commission/invite/bind",
    method: "POST",
    data: {
      inviterId: pending.inviterId,
      source: pending.source || ""
    }
  })
    .then((resp) => {
      clearPendingInviteEntry();
      return !!(resp && resp.bound);
    })
    .catch(() => false);
}

function doPhoneLogin(code, phoneCode, profile) {
  const payload = Object.assign({ code, phoneCode }, profile || {});
  return request({
    url: "/api/auth/wechat/phone",
    method: "POST",
    data: payload
  }).then((resp) => {
    if (resp && resp.token) {
      setToken(resp.token);
    }
    const user = resp ? resp.user : null;
    if (user) {
      setCachedUser(user);
    }
    setLoggedOut(false);
    checked = true;
    return tryActivateScanQualification()
      .catch(() => false)
      .then(() => tryBindInviteRelation().catch(() => false))
      .then(() => user);
  });
}

function loginForDevtools() {
  if (!isDevtools()) {
    return Promise.reject("请在微信内授权手机号登录");
  }
  const devCode = `dev-${Date.now()}`;
  return request({
    url: "/api/auth/wechat/phone",
    method: "POST",
    data: {
      code: devCode,
      phoneCode: devCode,
      nickname: "开发调试用户"
    }
  })
    .then((resp) => {
      if (!(resp && resp.token && resp.user && resp.user.id)) {
        throw new Error("本地调试登录失败");
      }
      setToken(resp.token);
      return refreshProfile(resp.user.id).catch(() => resp.user);
    })
    .then((profile) => {
      if (profile && profile.id) {
        setCachedUser(profile);
      }
      setLoggedOut(false);
      checked = true;
      return profile;
    });
}


function normalizeLoginProfile(profile) {
  const source = profile || {};
  const nickname = source.nickname ? String(source.nickname).trim() : "";
  const avatarUrl = source.avatarUrl ? String(source.avatarUrl).trim() : "";
  const payload = {};
  if (nickname) {
    payload.nickname = nickname;
  }
  if (avatarUrl) {
    payload.avatarUrl = avatarUrl;
  }
  return payload;
}

function loginWithPhoneCode(phoneCode, profile) {
  return new Promise((resolve, reject) => {
    const codeValue = String(phoneCode || "");
    const profilePayload = normalizeLoginProfile(profile);
    if (!codeValue) {
      reject("未获取到手机号授权码");
      return;
    }
    wx.login({
      success(res) {
        const loginCode = res && res.code ? String(res.code) : "";
        if (!loginCode) {
          reject("微信登录失败：未获取到 code");
          return;
        }
        doPhoneLogin(loginCode, codeValue, profilePayload).then(resolve).catch(reject);
      },
      fail() {
        reject("微信登录失败");
      }
    });
  });
}

function refreshProfile(userId) {
  return request({ url: `/api/users/${userId}/profile` }).then((profile) => {
    if (profile) {
      setCachedUser(profile);
    }
    checked = true;
    return profile;
  });
}

function ensureAuth(options) {
  const strict = !!(options && options.strict);
  if (isLoggedOut()) {
    checked = true;
    if (strict) {
      return Promise.reject("请先登录");
    }
    return Promise.resolve(null);
  }
  const cached = getCachedUser();
  if (cached) {
    if (checked) {
      return Promise.resolve(cached);
    }
    return request({ url: `/api/users/${cached.id}` })
      .then((user) => {
        if (user) {
          setCachedUser(user);
        }
        checked = true;
        return user || cached;
      })
      .catch((err) => {
        const message = String(err || "");
        if (message.includes("用户不存在")) {
          clearAuth();
          if (strict) {
            throw new Error("请先登录");
          }
          return null;
        }
        if (strict) {
          throw err;
        }
        checked = true;
        return cached;
      });
  }
  checked = true;
  if (strict) {
    if (hasPendingInviteForLogin()) {
      return Promise.reject("登录安心游，解锁更优惠的价格");
    }
    return Promise.reject("请先登录");
  }
  return Promise.resolve(null);
}

module.exports = {
  ensureAuth,
  loginWithPhoneCode,
  loginForDevtools,
  refreshProfile,
  getCachedUser,
  setCachedUser,
  clearAuth,
  markScanEntry,
  tryActivateScanQualification,
  tryBindInviteRelation,
  hasPendingInviteForLogin,
  invalidatePendingInvite
};

function normalizePendingInviteEntry(entry) {
  if (!entry) {
    return null;
  }
  const inviterId = Number(entry.inviterId);
  if (!Number.isFinite(inviterId) || inviterId <= 0) {
    return null;
  }
  const at = Number(entry.at || 0);
  if (!Number.isFinite(at) || at <= 0) {
    return null;
  }
  if (Date.now() - at > INVITE_PENDING_TTL_MS) {
    clearPendingInviteEntry();
    clearPendingScanEntry();
    return null;
  }
  return {
    inviterId,
    source: entry.source || "scan",
    mustLogin: !!entry.mustLogin,
    at
  };
}

function parseInviterId(value) {
  const text = String(value || "").trim();
  if (!text) {
    return null;
  }
  if (/^\d+$/.test(text)) {
    const num = Number(text);
    return Number.isFinite(num) && num > 0 ? num : null;
  }
  const match = text.match(/(?:^|[?&#])inviterId=(\d+)/i);
  if (match && match[1]) {
    const num = Number(match[1]);
    return Number.isFinite(num) && num > 0 ? num : null;
  }
  const sceneMatch = text.match(/^i[_-]?(\d+)$/i);
  if (sceneMatch && sceneMatch[1]) {
    const num = Number(sceneMatch[1]);
    return Number.isFinite(num) && num > 0 ? num : null;
  }
  return null;
}

function extractInviterId(options) {
  const opts = options || {};
  const query = opts.query || {};
  const direct = parseInviterId(query.inviterId);
  if (direct) {
    return direct;
  }
  if (query.scene) {
    const sceneVal = parseInviterId(decodeSceneValue(query.scene));
    if (sceneVal) {
      return sceneVal;
    }
  }
  if (query.q) {
    const qVal = parseInviterId(decodeSceneValue(query.q));
    if (qVal) {
      return qVal;
    }
  }
  return null;
}

function decodeSceneValue(value) {
  const raw = String(value || "");
  if (!raw) {
    return "";
  }
  try {
    return decodeURIComponent(raw);
  } catch (e) {
    return raw;
  }
}
