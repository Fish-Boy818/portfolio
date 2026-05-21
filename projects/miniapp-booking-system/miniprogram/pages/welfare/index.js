const { getBaseUrl, getToken } = require("../../utils/api");
const { ensureAuth } = require("../../utils/auth");
const { createWelfareSubmission } = require("../../utils/welfare");
const { getBountyDetail } = require("../../utils/bounty");
const { isReviewMode } = require("../../utils/review-mode");

function sanitizeText(value) {
  return value === null || value === undefined ? "" : String(value).trim();
}

function toValidId(value) {
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : 0;
}

function bountyStatusHint(status) {
  if (status === "accepted") {
    return "任务已接取，把完成截图传到这里，后台审核通过后就会发佣。";
  }
  if (status === "submitted") {
    return "你之前提交过这条悬赏；如果有新的完成截图或补充说明，也可以继续提交。";
  }
  if (status === "approved") {
    return "这条悬赏之前已经完成过；如果有新的完成结果，也可以继续提交。";
  }
  if (status === "rejected") {
    return "上次提交未通过，按要求补图或补说明后可以重新提交。";
  }
  return "完成悬赏后，把截图传到这里，后台审核通过后就会发佣。";
}

function canSubmitBounty(status) {
  return true;
}

Page({
  data: {
    reviewMode: false,
    pageTitle: "悬赏任务",
    status: "loading",
    statusTitle: "请先登录后再参与",
    errorMessage: "服务异常",
    reviewText: "",
    screenshotUrls: [],
    uploading: false,
    submitting: false,
    selectedBountyId: 0,
    selectedSubmissionId: 0,
    selectedBounty: null
  },
  onLoad(options) {
    const reviewMode = !!isReviewMode();
    this.setData({
      reviewMode,
      pageTitle: reviewMode ? "任务提交" : "悬赏任务",
      selectedBountyId: toValidId(options && options.bountyId),
      selectedSubmissionId: toValidId(options && options.submissionId)
    });
    if (reviewMode) {
      this.setData({
        status: "review",
        statusTitle: "任务功能升级中",
        errorMessage: "任务提交入口正在升级，暂时不可用。"
      });
      return;
    }
    this.loadData();
  },
  onShow() {
    if (this.data.reviewMode) {
      this.setData({
        status: "review",
        statusTitle: "任务功能升级中",
        errorMessage: "任务提交入口正在升级，暂时不可用。"
      });
      return;
    }
    if (this.data.status === "loading") {
      return;
    }
    this.loadData(true);
  },
  loadData(silent) {
    if (!silent) {
      this.setData({ status: "loading", errorMessage: "" });
    }
    ensureAuth({ strict: true })
      .then(() => {
        if (this.data.selectedBountyId) {
          return getBountyDetail(this.data.selectedBountyId);
        }
        return null;
      })
      .then((bounty) => {
        this.setData({
          status: "ready",
          selectedBounty: bounty ? this.mapBounty(bounty) : null
        });
      })
      .catch((err) => {
        const message = String(err || "加载失败");
        if (message.includes("请先登录")) {
          this.setData({
            status: "unauth",
            statusTitle: "请先登录后再参与",
            errorMessage: "登录后才能接取悬赏并提交悬赏任务内容"
          });
          return;
        }
        this.setData({
          status: "error",
          statusTitle: "服务异常",
          errorMessage: message
        });
      });
  },
  mapBounty(item) {
    const claimStatus = sanitizeText(item.claimStatus);
    return {
      id: item.id,
      location: sanitizeText(item.location) || "待补充地点",
      commissionRangeText:
        sanitizeText(item.commissionRangeText) || "预计佣金 0-0 元",
      coverImageUrl: sanitizeText(item.coverImageUrl),
      detailImageUrl: sanitizeText(item.detailImageUrl),
      claimStatus,
      claimStatusText: "",
      actionText: "提交审核",
      statusHint: bountyStatusHint(claimStatus),
      canSubmit: canSubmitBounty(claimStatus),
      submitButtonText: claimStatus === "rejected" ? "重新提交审核" : "提交审核"
    };
  },
  onReviewTextInput(e) {
    this.setData({ reviewText: e.detail.value || "" });
  },
  onChooseImage() {
    if (this.data.reviewMode) {
      wx.showToast({ title: "功能升级中，暂不可用", icon: "none" });
      return;
    }
    if (this.data.uploading) {
      return;
    }
    const currentCount = this.data.screenshotUrls.length;
    const remainCount = 9 - currentCount;
    if (remainCount <= 0) {
      wx.showToast({ title: "最多上传9张图片", icon: "none" });
      return;
    }
    wx.chooseMedia({
      count: remainCount,
      mediaType: ["image"],
      sizeType: ["compressed"],
      success: (res) => {
        const files = res && res.tempFiles ? res.tempFiles : [];
        if (files.length === 0) {
          return;
        }
        this.uploadImages(files);
      }
    });
  },
  uploadImages(files) {
    this.setData({ uploading: true });
    const baseUrl = getBaseUrl();
    const token = getToken();
    const uploadedUrls = [];
    let completed = 0;
    const total = files.length;
    files.forEach((file) => {
      const filePath = file.tempFilePath;
      wx.uploadFile({
        url: `${baseUrl}/api/welfare/upload`,
        filePath,
        name: "file",
        header: token ? { Authorization: token } : {},
        success: (res) => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const body = JSON.parse(res.data || "{}");
              if (body.code === 0 && body.data && body.data.url) {
                const rawUrl = String(body.data.url);
                const url = rawUrl.startsWith("/uploads/") ? `${baseUrl}${rawUrl}` : rawUrl;
                uploadedUrls.push(url);
              }
            } catch (e) {}
          }
        },
        complete: () => {
          completed++;
          if (completed === total) {
            const currentUrls = this.data.screenshotUrls || [];
            const newUrls = [...currentUrls, ...uploadedUrls].slice(0, 9);
            this.setData({
              screenshotUrls: newUrls,
              uploading: false
            });
            if (uploadedUrls.length < total) {
              wx.showToast({ title: "部分图片上传失败", icon: "none" });
            }
          }
        }
      });
    });
  },
  onPreviewImage(e) {
    const url = e.currentTarget.dataset.url || this.data.screenshotUrls[0];
    if (!url) {
      return;
    }
    wx.previewImage({
      current: url,
      urls: this.data.screenshotUrls
    });
  },
  onPreviewBountyImage(e) {
    const url = sanitizeText(e.currentTarget.dataset.url);
    if (!url) {
      return;
    }
    wx.previewImage({
      current: url,
      urls: [url]
    });
  },
  onRemoveImage(e) {
    const index = e.currentTarget.dataset.index;
    if (index === undefined || index === null) {
      this.setData({ screenshotUrls: [] });
      return;
    }
    const urls = [...this.data.screenshotUrls];
    urls.splice(index, 1);
    this.setData({ screenshotUrls: urls });
  },
  onSubmit() {
    if (this.data.reviewMode) {
      wx.showToast({ title: "功能升级中，暂不可用", icon: "none" });
      return;
    }
    if (this.data.submitting || this.data.uploading) {
      return;
    }
    const selectedBounty = this.data.selectedBounty;
    const screenshotUrls = this.data.screenshotUrls || [];
    const screenshotUrl = screenshotUrls.join(",");
    const reviewText = sanitizeText(this.data.reviewText);
    if (!screenshotUrl) {
      wx.showToast({ title: "请先上传好评截图", icon: "none" });
      return;
    }
    this.setData({ submitting: true });
    createWelfareSubmission({
      bountyTaskId: selectedBounty ? selectedBounty.id : undefined,
      submissionId: this.data.selectedSubmissionId || undefined,
      platformName: selectedBounty ? selectedBounty.location : "悬赏任务",
      reviewText,
      screenshotUrl
    })
      .then(() => {
        wx.showToast({ title: "已提交审核", icon: "success" });
        this.setData({
          reviewText: "",
          screenshotUrls: [],
          selectedSubmissionId: 0
        });
        this.loadData(true);
      })
      .catch((err) => {
        wx.showToast({ title: String(err || "提交失败"), icon: "none" });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },
  onRetry() {
    this.loadData();
  },
  onGoProfile() {
    wx.switchTab({ url: "/pages/profile/index" });
  }
});
