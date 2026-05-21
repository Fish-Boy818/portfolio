const { listMyBounties } = require("../../utils/bounty");
const { ensureAuth } = require("../../utils/auth");
const { isReviewMode } = require("../../utils/review-mode");

function normalizeText(value) {
  return value === null || value === undefined ? "" : String(value).trim();
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }
  const raw = String(value).trim().replace("T", " ").replace(/\.\d+$/, "").replace(/Z$/, "");
  const matched = raw.match(/(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})(?:\s+(\d{1,2}):(\d{1,2}))?/);
  if (matched) {
    const year = matched[1];
    const month = String(matched[2]).padStart(2, "0");
    const day = String(matched[3]).padStart(2, "0");
    const hour = String(matched[4] || "00").padStart(2, "0");
    const minute = String(matched[5] || "00").padStart(2, "0");
    return `${year}.${month}.${day} ${hour}:${minute}`;
  }
  return raw;
}

function money(value) {
  const num = Number(value || 0);
  if (Number.isNaN(num)) {
    return "0.00";
  }
  return num.toFixed(2);
}

function resolveStatusMeta(item) {
  const claimStatus = normalizeText(item && item.claimStatus);
  if (claimStatus === "approved") {
    return {
      text: "已完成",
      className: "record-status--success",
      actionText: "查看结果",
      buttonClassName: "btn-secondary"
    };
  }
  if (claimStatus === "rejected") {
    return {
      text: "未通过",
      className: "record-status--danger",
      actionText: "重新提交",
      buttonClassName: "btn-primary"
    };
  }
  if (claimStatus === "submitted") {
    return {
      text: "审核中",
      className: "record-status--pending",
      actionText: "查看进度",
      buttonClassName: "btn-secondary"
    };
  }
  return {
    text: "待提交",
    className: "record-status--pending",
    actionText: "去提交",
    buttonClassName: "btn-primary"
  };
}

Page({
  data: {
    reviewMode: false,
    pageTitle: "悬赏记录",
    status: "loading",
    errorMessage: "悬赏记录加载失败，请稍后重试",
    items: []
  },
  onLoad() {
    const reviewMode = !!isReviewMode();
    this.setData({
      reviewMode,
      pageTitle: reviewMode ? "任务记录" : "悬赏记录"
    });
    if (reviewMode) {
      this.setData({ status: "review", items: [] });
      return;
    }
    this.loadData();
  },
  onShow() {
    if (this.data.reviewMode) {
      this.setData({ status: "review", items: [] });
      return;
    }
    if (this.data.status === "loading") {
      return;
    }
    this.loadData(true);
  },
  onPullDownRefresh() {
    if (this.data.reviewMode) {
      wx.stopPullDownRefresh();
      return;
    }
    this.loadData(true);
  },
  loadData(silent) {
    if (!silent) {
      this.setData({
        status: "loading",
        errorMessage: ""
      });
    }
    ensureAuth({ strict: true })
      .then(() => listMyBounties())
      .then((items) => {
        const list = Array.isArray(items) ? items : [];
        this.setData({
          status: list.length ? "ready" : "empty",
          items: list.map((item, index) => this.mapItem(item, index))
        });
      })
      .catch((err) => {
        const message = String(err || "悬赏记录加载失败");
        if (message.includes("请先登录")) {
          this.setData({
            status: "unauth",
            errorMessage: "登录后才能查看你接取过的悬赏任务"
          });
          return;
        }
        this.setData({
          status: "error",
          errorMessage: message
        });
      })
      .finally(() => {
        wx.stopPullDownRefresh();
      });
  },
  mapItem(item, index) {
    const statusMeta = resolveStatusMeta(item);
    const screenshotUrl = normalizeText(item && item.latestScreenshotUrl);
    const id = item && item.id ? Number(item.id) : 0;
    const submissionId = item && item.latestSubmissionId ? Number(item.latestSubmissionId) : 0;
    const acceptedAtText = formatDateTime(item && item.acceptedAt);
    const submittedAtText = formatDateTime(item && item.latestSubmittedAt);
    const reviewedAtText = formatDateTime(item && item.latestReviewedAt);
    return {
      id,
      submissionId,
      resubmitSubmissionId: normalizeText(item && item.claimStatus) === "rejected" ? submissionId : 0,
      recordKey: submissionId
        ? `submission-${submissionId}`
        : `claim-${id}-${acceptedAtText || "none"}-${index || 0}`,
      location: normalizeText(item && item.location) || "待补充地点",
      commissionRangeText:
        normalizeText(item && item.commissionRangeText) || "预计佣金 0-0 元",
      claimStatus: normalizeText(item && item.claimStatus),
      claimStatusText: statusMeta.text,
      claimStatusClass: statusMeta.className,
      actionText: statusMeta.actionText,
      actionButtonClass: statusMeta.buttonClassName,
      acceptedAtText,
      submittedAtText,
      reviewedAtText,
      rewardAmountText: money(item && item.latestSubmissionRewardAmount),
      reviewNote: normalizeText(item && item.latestSubmissionReviewNote),
      rejectionReasonText:
        normalizeText(item && item.latestSubmissionReviewNote) ||
        "任务未通过，请根据要求补充后重新提交。",
      screenshotUrl,
      showScreenshot: !!screenshotUrl
    };
  },
  onOpenRecord(e) {
    const id = Number(e.currentTarget.dataset.id || 0);
    const submissionId = Number(e.currentTarget.dataset.submissionId || 0);
    if (!id) {
      return;
    }
    const query = [`bountyId=${id}`];
    if (submissionId > 0) {
      query.push(`submissionId=${submissionId}`);
    }
    wx.navigateTo({ url: `/pages/welfare/index?${query.join("&")}` });
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
    if (this.data.reviewMode) {
      this.setData({ status: "review", items: [] });
      return;
    }
    this.loadData();
  },
  onGoProfile() {
    wx.switchTab({ url: "/pages/profile/index" });
  }
});
