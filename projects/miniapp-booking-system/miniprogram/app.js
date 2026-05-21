const {
  ensureAuth,
  markScanEntry,
  tryActivateScanQualification,
  tryBindInviteRelation,
  hasPendingInviteForLogin,
  getCachedUser,
  invalidatePendingInvite
} = require("./utils/auth");
const { request } = require("./utils/api");
const {
  detectEnvVersion,
  getCachedReviewMode,
  setCachedReviewMode,
  resolveReviewModeFromContent
} = require("./utils/review-mode");

const REVIEW_MODE_FORCE = false;
const REVIEW_MODE_ENV_FALLBACK = "";

const DEFAULT_TAB_BAR_TEXTS = {
  home: "首页",
  category: "分类",
  orders: "订单",
  welfare: "悬赏",
  profile: "我的"
};

const DEFAULT_TAB_PAGE_PATHS = [
  "/pages/home/index",
  "/pages/category/index",
  "/pages/orders/index",
  "/pages/bounty/index",
  "/pages/profile/index"
];

App({
  globalData: {
    userInfo: null,
    tabBarTexts: { ...DEFAULT_TAB_BAR_TEXTS },
    tabBarTextUpdatedAt: 0,
    reviewMode: false,
    currentTabIndex: 0,
    currentTabPath: DEFAULT_TAB_PAGE_PATHS[0]
  },
  _invitePrompting: false,
  _invitePromptAt: 0,
  onLaunch(options) {
    this.globalData.reviewMode = this.resolveReviewMode();
    this.globalData.tabBarTexts = this.getDefaultTabBarTexts(this.globalData.reviewMode);
    markScanEntry(options);
    this.loadTabBarConfig().catch(() => null);
    ensureAuth()
      .then((user) => {
        this.globalData.userInfo = user;
        if (!user) {
          this.redirectToLoginForInvite();
          return null;
        }
        return tryActivateScanQualification()
          .then(() => tryBindInviteRelation())
          .then(() => user);
      })
      .catch(() => {
        this.globalData.userInfo = null;
        this.redirectToLoginForInvite();
      });
  },
  onShow(options) {
    this.globalData.reviewMode = this.resolveReviewMode();
    this.globalData.tabBarTexts = this.getDefaultTabBarTexts(this.globalData.reviewMode);
    markScanEntry(options);
    this.loadTabBarConfig().catch(() => null);
    if (!getCachedUser()) {
      this.redirectToLoginForInvite();
    }
    tryActivateScanQualification()
      .then(() => tryBindInviteRelation())
      .then(() => ensureAuth().catch(() => null))
      .then((user) => {
        if (user) {
          this.globalData.userInfo = user;
        }
      })
      .catch(() => {
        // ignore
      });
  },
  onHide() {
    // If scanned user never logs in, invalidate this scan on leave.
    if (!getCachedUser() && hasPendingInviteForLogin()) {
      invalidatePendingInvite("hide");
    }
  },
  redirectToLoginForInvite() {
    if (!hasPendingInviteForLogin()) {
      return;
    }
    const now = Date.now();
    if (this._invitePrompting || now - this._invitePromptAt < 1500) {
      return;
    }
    this._invitePrompting = true;
    this._invitePromptAt = now;
    wx.showModal({
      title: "登录提醒",
      content: "登录安心游，解锁更优惠的价格\n温馨提示：这次不登录，等您回去了非扫码登录无法开启优惠哦",
      confirmText: "去登录",
      cancelText: "稍后",
      success: (res) => {
        if (res && res.confirm) {
          wx.switchTab({
            url: "/pages/profile/index"
          });
        }
      },
      complete: () => {
        this._invitePrompting = false;
      }
    });
  },
  loadTabBarConfig(force) {
    const lastUpdatedAt = Number(this.globalData.tabBarTextUpdatedAt || 0);
    if (!force && lastUpdatedAt && Date.now() - lastUpdatedAt < 30000) {
      return Promise.resolve(this.globalData.tabBarTexts);
    }
    return request({ url: "/api/profile-content" })
      .then((content) => {
        const backendReviewMode = resolveReviewModeFromContent(content);
        if (!REVIEW_MODE_FORCE && backendReviewMode !== null) {
          this.globalData.reviewMode = backendReviewMode;
          setCachedReviewMode(backendReviewMode);
        }
        const reviewMode = !!this.globalData.reviewMode;
        const welfareFallback = reviewMode ? "推荐" : "悬赏";
        const texts = {
          home: sanitizeTabText(content && content.tabHomeText, DEFAULT_TAB_BAR_TEXTS.home),
          category: sanitizeTabText(content && content.tabCategoryText, DEFAULT_TAB_BAR_TEXTS.category),
          orders: sanitizeTabText(content && content.tabOrdersText, DEFAULT_TAB_BAR_TEXTS.orders),
          welfare: reviewMode ? welfareFallback : sanitizeTabText(content && content.tabWelfareText, welfareFallback),
          profile: sanitizeTabText(content && content.tabProfileText, DEFAULT_TAB_BAR_TEXTS.profile)
        };
        this.globalData.tabBarTexts = texts;
        this.globalData.tabBarTextUpdatedAt = Date.now();
        return texts;
      })
      .catch((err) => {
        if (!this.globalData.tabBarTexts) {
          this.globalData.tabBarTexts = { ...DEFAULT_TAB_BAR_TEXTS };
        }
        return Promise.reject(err);
      });
  },
  resolveReviewMode() {
    if (REVIEW_MODE_FORCE) {
      return true;
    }
    const cached = getCachedReviewMode();
    if (cached !== null) {
      return cached;
    }
    if (REVIEW_MODE_ENV_FALLBACK) {
      return detectEnvVersion() === REVIEW_MODE_ENV_FALLBACK;
    }
    return false;
  },
  getDefaultTabBarTexts(reviewMode) {
    return {
      ...DEFAULT_TAB_BAR_TEXTS,
      welfare: reviewMode ? "推荐" : "悬赏"
    };
  },
  syncTabBarSelection(page, selected) {
    this.setCurrentTab(selected);
    const targetPath = this.globalData.currentTabPath || DEFAULT_TAB_PAGE_PATHS[Number(selected) || 0] || DEFAULT_TAB_PAGE_PATHS[0];
    if (!page || typeof page.getTabBar !== "function") {
      return;
    }
    [0, 60, 180].forEach((delay) => {
      setTimeout(() => {
        const tabBar = page.getTabBar();
        if (!tabBar || typeof tabBar.setData !== "function") {
          return;
        }
        if (typeof tabBar.syncFromGlobal === "function") {
          tabBar.syncFromGlobal();
        }
        if (typeof tabBar.setActive === "function") {
          tabBar.setActive(targetPath);
          return;
        }
        tabBar.setData({
          selected: Number(selected) || 0,
          selectedPath: targetPath
        });
      }, delay);
    });
  },
  setCurrentTab(selected, pagePath) {
    const index = Number(selected) || 0;
    this.globalData.currentTabIndex = index;
    this.globalData.currentTabPath = pagePath || DEFAULT_TAB_PAGE_PATHS[index] || DEFAULT_TAB_PAGE_PATHS[0];
  }
});

function sanitizeTabText(value, fallback) {
  const text = value === null || value === undefined ? "" : String(value).trim();
  return text || fallback;
}
