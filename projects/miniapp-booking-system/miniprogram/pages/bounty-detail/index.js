const { getBountyDetail, acceptBounty } = require("../../utils/bounty");
const { ensureAuth } = require("../../utils/auth");
const { isReviewMode } = require("../../utils/review-mode");

function normalizeText(value) {
  return value === null || value === undefined ? "" : String(value).trim();
}

function toValidId(value) {
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : 0;
}

Page({
  data: {
    reviewMode: false,
    pageTitle: "悬赏详情",
    status: "loading",
    errorMessage: "悬赏详情加载失败，请稍后重试",
    bountyId: 0,
    bounty: null,
    steps: [],
    accepting: false
  },
  onLoad(options) {
    const reviewMode = !!isReviewMode();
    this.setData({
      reviewMode,
      pageTitle: reviewMode ? "任务详情" : "悬赏详情"
    });
    if (reviewMode) {
      this.setData({
        status: "review",
        errorMessage: ""
      });
      return;
    }
    const bountyId = toValidId(options && options.id);
    if (!bountyId) {
      this.setData({
        status: "error",
        errorMessage: "悬赏参数异常，请返回重试"
      });
      return;
    }
    this.setData({ bountyId });
    this.loadData();
  },
  loadData() {
    this.setData({ status: "loading", errorMessage: "" });
    getBountyDetail(this.data.bountyId)
      .then((item) => {
        if (!item || !item.id) {
          throw new Error("悬赏不存在");
        }
        this.setData({
          status: "ready",
          bounty: this.mapItem(item),
          steps: this.buildSteps(item)
        });
      })
      .catch((err) => {
        this.setData({
          status: "error",
          errorMessage: String(err || "悬赏详情加载失败")
        });
      });
  },
  mapItem(item) {
    return {
      ...item,
      location: normalizeText(item.location) || "待补充地点",
      commissionRangeText:
        normalizeText(item.commissionRangeText) || "预计佣金 0-0 元",
      detailImageUrl: normalizeText(item.detailImageUrl) || normalizeText(item.coverImageUrl),
      claimStatus: normalizeText(item.claimStatus),
      claimStatusText: "",
      actionText: "提交任务"
    };
  },
  buildSteps(item) {
    const normalizedSteps = Array.isArray(item && item.steps)
      ? item.steps
          .map((step, index) => ({
            index: index + 1,
            text: normalizeText(step && step.text),
            imageUrl: normalizeText(step && step.imageUrl)
          }))
          .filter((step) => step.text || step.imageUrl)
      : [];

    if (normalizedSteps.length) {
      return normalizedSteps;
    }

    return [1, 2, 3]
      .map((index) => ({
        index,
        text: normalizeText(item[`step${index}Text`]),
        imageUrl: normalizeText(item[`step${index}ImageUrl`])
      }))
      .filter((step) => step.text || step.imageUrl);
  },
  onTakeTask() {
    if (this.data.reviewMode) {
      wx.showToast({ title: "功能升级中，暂不可用", icon: "none" });
      return;
    }
    const bountyId = toValidId(this.data.bountyId);
    if (!bountyId || this.data.accepting) {
      return;
    }
    const bounty = this.data.bounty || {};
    if (bounty.claimStatus) {
      wx.navigateTo({
        url: `/pages/welfare/index?bountyId=${bountyId}`
      });
      return;
    }
    this.setData({ accepting: true });
    ensureAuth({ strict: true })
      .then(() => acceptBounty(bountyId))
      .then((item) => {
        const mapped = this.mapItem(item || {});
        this.setData({
          bounty: mapped,
          steps: this.buildSteps(item || {})
        });
        wx.showToast({ title: "已接取悬赏", icon: "success" });
        setTimeout(() => {
          wx.navigateTo({
            url: `/pages/welfare/index?bountyId=${bountyId}`
          });
        }, 250);
      })
      .catch((err) => {
        const message = String(err || "接取失败");
        if (message.includes("请先登录") || message.includes("登录后可参与")) {
          wx.showToast({ title: "请先登录后再接取", icon: "none" });
          setTimeout(() => {
            wx.switchTab({ url: "/pages/profile/index" });
          }, 350);
          return;
        }
        wx.showToast({ title: message, icon: "none" });
      })
      .finally(() => {
        this.setData({ accepting: false });
      });
  },
  onPreviewImage(e) {
    const url = normalizeText(e.currentTarget.dataset.url);
    if (!url) {
      return;
    }
    wx.previewImage({
      current: url,
      urls: [url]
    });
  },
  onRetry() {
    this.loadData();
  }
});
