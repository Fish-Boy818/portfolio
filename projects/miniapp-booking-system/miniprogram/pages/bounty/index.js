const { request } = require("../../utils/api");
const { isReviewMode } = require("../../utils/review-mode");

const REVIEW_GUIDES = [
  {
    title: "预订前须知",
    desc: "出行前请先查看项目说明、适用人群和安全提示，确认时间与天气安排。"
  },
  {
    title: "热门玩法",
    desc: "平台持续上新摩托艇、冲浪、香蕉船、桨板等海上体验项目，可在详情页查看。"
  },
  {
    title: "下单流程",
    desc: "浏览活动详情，确认下单信息并完成微信支付，支付后可在订单页查看状态。"
  }
];

function syncTabBar(page, selected) {
  const app = getApp ? getApp() : null;
  if (app && typeof app.syncTabBarSelection === "function") {
    app.syncTabBarSelection(page, selected);
  }
}

function toValidId(value) {
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : 0;
}

function sanitizeText(value) {
  return value === null || value === undefined ? "" : String(value).trim();
}

function isFormalActivity(item) {
  const title = sanitizeText(item && item.title);
  if (!title) {
    return false;
  }
  if (title.includes("测试")) {
    return false;
  }
  const status = sanitizeText(item && item.status);
  return !status || status === "active" || status === "可预约";
}

function formatPrice(value) {
  const num = Number(value || 0);
  if (!Number.isFinite(num) || num <= 0) {
    return "价格以详情页为准";
  }
  return `¥${num.toFixed(2)} 起`;
}

Page({
  data: {
    reviewMode: false,
    pageTitle: "悬赏",
    status: "loading",
    errorMessage: "悬赏任务加载失败，请稍后重试",
    items: [],
    reviewGuides: REVIEW_GUIDES,
    reviewActivities: []
  },
  onLoad() {
    const reviewMode = !!isReviewMode();
    this.setData({
      reviewMode,
      pageTitle: reviewMode ? "精选推荐" : "悬赏"
    });
    if (reviewMode) {
      this.loadReviewContent();
      return;
    }
    this.loadData();
  },
  onShow() {
    syncTabBar(this, 3);
    if (this.data.reviewMode) {
      this.loadReviewContent(true);
      return;
    }
    if (this.data.status === "loading") {
      return;
    }
    this.loadData(true);
  },
  loadReviewContent(silent) {
    if (!silent) {
      this.setData({ status: "loading", errorMessage: "" });
    }
    request({ url: "/api/activities" })
      .then((items) => {
        const list = (Array.isArray(items) ? items : [])
          .filter(isFormalActivity)
          .slice(0, 4)
          .map((item) => this.mapReviewActivity(item));
        this.setData({
          status: "review",
          reviewActivities: list
        });
      })
      .catch(() => {
        this.setData({
          status: "review",
          reviewActivities: []
        });
      })
      .finally(() => {
        wx.stopPullDownRefresh();
      });
  },
  loadData(silent) {
    if (!silent) {
      this.setData({ status: "loading", errorMessage: "" });
    }
    request({ url: "/api/bounties" })
      .then((items) => {
        const list = Array.isArray(items) ? items : [];
        this.setData({
          status: list.length ? "ready" : "empty",
          items: list.map((item) => this.mapItem(item))
        });
      })
      .catch((err) => {
        this.setData({
          status: "error",
          errorMessage: String(err || "悬赏任务加载失败")
        });
      })
      .finally(() => {
        wx.stopPullDownRefresh();
      });
  },
  mapItem(item) {
    return {
      ...item,
      coverImageUrl: sanitizeText(item && item.coverImageUrl),
      location: sanitizeText(item && item.location) || "待补充地点",
      commissionRangeText:
        sanitizeText(item && item.commissionRangeText) || "预计佣金 0-0 元",
      claimStatusText: "",
      actionText: "接取任务"
    };
  },
  mapReviewActivity(item) {
    const gallery = Array.isArray(item && item.gallery) ? item.gallery : [];
    return {
      id: toValidId(item && item.id),
      title: sanitizeText(item && item.title) || "海上体验活动",
      subtitle: sanitizeText(item && item.subtitle) || sanitizeText(item && item.audience) || "查看详情了解活动内容",
      cover: sanitizeText(item && item.cover) || sanitizeText(item && item.imageUrl) || sanitizeText(gallery[0]),
      priceText: formatPrice(item && (item.originalPrice || item.price || item.basePrice)),
      location: sanitizeText(item && (item.clubLocation || item.location || item.clubName)) || "多城市俱乐部可选"
    };
  },
  onOpenDetail(e) {
    const id = Number(e.currentTarget.dataset.id || 0);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/bounty-detail/index?id=${id}` });
  },
  onOpenReviewActivity(e) {
    const id = Number(e.currentTarget.dataset.id || 0);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/detail/index?id=${id}` });
  },
  onGoCategory() {
    wx.switchTab({ url: "/pages/category/index" });
  },
  onRetry() {
    if (this.data.reviewMode) {
      this.loadReviewContent();
      return;
    }
    this.loadData();
  },
  onPullDownRefresh() {
    if (this.data.reviewMode) {
      this.loadReviewContent(true);
      return;
    }
    this.loadData(true);
  }
});
