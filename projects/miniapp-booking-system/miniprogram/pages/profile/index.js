const { getBaseUrl, request, getToken } = require("../../utils/api");
const { isReviewMode } = require("../../utils/review-mode");
const {
  refreshProfile,
  setCachedUser,
  getCachedUser,
  clearAuth,
  loginWithPhoneCode,
  hasPendingInviteForLogin,
  invalidatePendingInvite
} = require("../../utils/auth");

const USER_AGREEMENT_TEXT = "欢迎使用示例平台小程序。你在使用本服务时，应遵守法律法规及平台规则，不得进行违规或侵权行为。你需对账号下的操作与订单行为负责。";
const PRIVACY_POLICY_TEXT = "为完成登录、下单、支付与售后服务，我们会处理必要信息（微信标识、手机号、订单与支付结果）。你可在“我的”页面申请查询、更正或删除相关信息。";

function syncTabBar(page, selected) {
  const app = getApp ? getApp() : null;
  if (app && typeof app.syncTabBarSelection === "function") {
    app.syncTabBarSelection(page, selected);
  }
}

function maskPhone(phone) {
  const value = String(phone || "");
  if (/^\d{11}$/.test(value)) {
    return `${value.slice(0, 3)} **** ${value.slice(7)}`;
  }
  return value;
}

Page({
  data: {
    status: "loading",
    errorMessage: "加载失败，请稍后重试",
    unauthDesc: "请先完成授权登录后查看个人信息",
    user: {},
    displayName: "",
    displayPhone: "",
    userId: null,
    editing: false,
    editName: "",
    editError: "",
    simulateError: false,
    privacyAgreed: false,
    uploadingAvatar: false,
    reviewMode: false,
    orderSummary: {
      all: 0,
      unpaid: 0,
      paid: 0,
      used: 0,
      refund: 0
    }
  },
  onLoad() {
    this.setReviewMode();
    this.loadData();
  },
  onShow() {
    this.setReviewMode();
    syncTabBar(this, 4);
    this.loadData();
  },
  setReviewMode() {
    this.setData({ reviewMode: !!isReviewMode() });
  },
  loadData() {
    const cached = getCachedUser();
    if (!cached || !cached.id) {
      this.setUnauthState();
      return;
    }
    this.setData({ status: "loading" });
    refreshProfile(cached.id)
      .then((profile) => {
        this.setData({
          status: "ready",
          user: profile,
          displayName: profile.nickname || "微信用户",
          displayPhone: maskPhone(profile.phone),
          userId: profile.id
        });
        return this.loadOrderSummary(profile.id);
      })
      .catch((err) => {
        const message = String(err || "加载失败");
        if (message.includes("用户不存在") || message.includes("未授权") || message.includes("请先登录")) {
          clearAuth();
          this.setUnauthState();
          return;
        }
        this.setData({ status: "error", errorMessage: message });
      });
  },
  loadOrderSummary(userId) {
    if (!userId) {
      return Promise.resolve();
    }
    return request({ url: "/api/orders", data: { userId } })
      .then((orders) => {
        const summary = this.calcOrderSummary(Array.isArray(orders) ? orders : []);
        this.setData({ orderSummary: summary });
      })
      .catch(() => {
        this.setData({
          orderSummary: {
            all: 0,
            unpaid: 0,
            paid: 0,
            used: 0,
            refund: 0
          }
        });
      });
  },
  calcOrderSummary(orders) {
    const summary = {
      all: orders.length || 0,
      unpaid: 0,
      paid: 0,
      used: 0,
      refund: 0
    };
    orders.forEach((item) => {
      const status = item && item.status ? String(item.status) : "";
      if (status === "unpaid") {
        summary.unpaid += 1;
      } else if (status === "paid") {
        summary.paid += 1;
      } else if (status === "used") {
        summary.used += 1;
      } else if (status === "refund") {
        summary.refund += 1;
      }
    });
    return summary;
  },
  getWechatProfile() {
    if (typeof wx.getUserProfile !== "function") {
      return Promise.resolve({});
    }
    return new Promise((resolve) => {
      wx.getUserProfile({
        desc: "用于补充微信头像和昵称",
        lang: "zh_CN",
        success: (res) => {
          const info = (res && res.userInfo) || {};
          resolve({
            nickname: info.nickName || "",
            avatarUrl: info.avatarUrl || ""
          });
        },
        fail: () => resolve({})
      });
    });
  },
  syncWechatProfileIfNeeded(profile, wxProfile) {
    const current = profile || {};
    const source = wxProfile || {};
    const avatarUrl = source.avatarUrl ? String(source.avatarUrl).trim() : "";
    const nickname = source.nickname ? String(source.nickname).trim() : "";
    if (!current.id) {
      return Promise.resolve(current);
    }
    const needAvatarSync = !!avatarUrl && avatarUrl !== String(current.avatarUrl || "");
    const needNicknameSync = !!nickname && nickname !== String(current.nickname || "");
    if (!needAvatarSync && !needNicknameSync) {
      return Promise.resolve(current);
    }
    return request({
      url: `/api/users/${current.id}`,
      method: "PUT",
      data: {
        nickname: nickname || current.nickname || "微信用户",
        avatarUrl: avatarUrl || current.avatarUrl || "",
        phone: current.phone || ""
      }
    })
      .then((user) => refreshProfile((user && user.id) || current.id).catch(() => user || current))
      .catch(() => current);
  },
  onChooseAvatar(e) {
    const detail = (e && e.detail) || {};
    const filePath = detail.avatarUrl ? String(detail.avatarUrl).trim() : "";
    if (!filePath) {
      wx.showToast({ title: "你已取消头像选择", icon: "none" });
      return;
    }
    if (!this.data.userId || this.data.uploadingAvatar) {
      return;
    }
    this.setData({ uploadingAvatar: true });
    this.uploadAvatarFile(filePath)
      .then(() => refreshProfile(this.data.userId))
      .then((profile) => {
        if (!profile) {
          throw new Error("头像更新失败");
        }
        setCachedUser(profile);
        this.setData({
          user: profile,
          displayName: profile.nickname || "微信用户",
          displayPhone: maskPhone(profile.phone)
        });
        wx.showToast({ title: "头像已更新", icon: "success" });
      })
      .catch((err) => {
        wx.showToast({ title: String(err || "头像上传失败"), icon: "none" });
      })
      .finally(() => {
        this.setData({ uploadingAvatar: false });
      });
  },
  uploadAvatarFile(filePath) {
    const userId = this.data.userId;
    const baseUrl = getBaseUrl();
    const token = getToken();
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: `${baseUrl}/api/users/${userId}/avatar`,
        filePath,
        name: "file",
        header: token ? { Authorization: token } : {},
        success: (res) => {
          if (res.statusCode < 200 || res.statusCode >= 300) {
            reject(`上传失败(${res.statusCode})`);
            return;
          }
          let body;
          try {
            body = JSON.parse(res.data || "{}");
          } catch (err) {
            reject("上传失败: 响应异常");
            return;
          }
          if (body && typeof body.code !== "undefined") {
            if (body.code === 0) {
              resolve(body.data || {});
            } else {
              reject(body.message || "上传失败");
            }
            return;
          }
          resolve(body || {});
        },
        fail: (err) => {
          reject((err && err.errMsg) || "上传失败");
        }
      });
    });
  },
  onPhoneAuthLogin(e) {
    if (this.data.status === "loading") {
      return;
    }
    if (!this.ensurePrivacyAgreed()) {
      return;
    }
    const detail = (e && e.detail) || {};
    const fromInviteScan = hasPendingInviteForLogin();
    if (!detail.code) {
      if (fromInviteScan) {
        invalidatePendingInvite("cancel");
        this.setUnauthState("未登录，扫码优惠已作废，请重新扫码");
        wx.showToast({ title: "扫码优惠已作废", icon: "none" });
        return;
      }
      wx.showToast({ title: "你已取消手机号授权", icon: "none" });
      return;
    }
    this.setData({ status: "loading", errorMessage: "" });
    const phoneCode = String(detail.code || "");
    let loginProfile = {};
    loginWithPhoneCode(phoneCode)
      .then((user) => {
        if (!user || !user.id) {
          throw new Error("登录失败");
        }
        return refreshProfile(user.id);
      })
      .then((profile) => {
        loginProfile = profile || {};
        this.setData({
          status: "ready",
          user: loginProfile,
          displayName: loginProfile.nickname || "微信用户",
          displayPhone: maskPhone(loginProfile.phone),
          userId: loginProfile.id
        });
        return this.loadOrderSummary(loginProfile.id).catch(() => null);
      })
      .then(() => {
        return this.getWechatProfile()
          .then((wxProfile) => this.syncWechatProfileIfNeeded(loginProfile, wxProfile))
          .then((syncedProfile) => {
            const profile = syncedProfile && syncedProfile.id ? syncedProfile : loginProfile;
            if (!profile || !profile.id) {
              return;
            }
            this.setData({
              user: profile,
              displayName: profile.nickname || "微信用户",
              displayPhone: maskPhone(profile.phone),
              userId: profile.id
            });
          })
          .catch(() => null);
      })
      .catch((err) => {
        const message = String(err || "登录失败");
        if (message.includes("取消")) {
          if (fromInviteScan) {
            invalidatePendingInvite("cancel");
            this.setUnauthState("未登录，扫码优惠已作废，请重新扫码");
            wx.showToast({ title: "扫码优惠已作废", icon: "none" });
            return;
          }
          this.setUnauthState();
          return;
        }
        this.setData({ status: "error", errorMessage: message });
      });
  },
  onTogglePrivacyAgree() {
    this.setData({ privacyAgreed: !this.data.privacyAgreed });
  },
  onOpenUserAgreement() {
    wx.showModal({
      title: "用户协议",
      content: USER_AGREEMENT_TEXT,
      showCancel: false,
      confirmText: "我已知晓"
    });
  },
  onOpenPrivacyPolicy() {
    wx.showModal({
      title: "隐私政策",
      content: PRIVACY_POLICY_TEXT,
      showCancel: false,
      confirmText: "我已知晓"
    });
  },
  ensurePrivacyAgreed() {
    if (this.data.privacyAgreed) {
      return true;
    }
    wx.showToast({ title: "请先勾选并同意协议", icon: "none" });
    return false;
  },
  onGoOrders() {
    wx.switchTab({ url: "/pages/orders/index" });
  },
  onGoOrdersByStatus(e) {
    const status = (e && e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.status) || "all";
    try {
      wx.setStorageSync("orders_active_status", status);
    } catch (err) {
      // ignore
    }
    wx.switchTab({ url: "/pages/orders/index" });
  },
  onGoBountyRecords() {
    wx.navigateTo({ url: "/pages/bounty-records/index" });
  },
  onGoCommissionCenter() {
    wx.navigateTo({ url: "/pages/commission-center/index" });
  },
  onOpenNotice() {
    wx.navigateTo({ url: "/pages/profile-content/index?type=notice" });
  },
  onOpenAboutUs() {
    wx.navigateTo({ url: "/pages/profile-content/index?type=about" });
  },
  onEditOpen() {
    const current = this.data.user || {};
    this.setData({
      editing: true,
      editName: current.nickname || "",
      editError: ""
    });
  },
  onEditClose() {
    this.setData({ editing: false, editError: "" });
  },
  onEditNameInput(e) {
    this.setData({ editName: e.detail.value });
  },
  onEditSave() {
    const name = (this.data.editName || "").trim();
    if (!name) {
      this.setData({ editError: "请输入姓名" });
      return;
    }
    request({
      url: `/api/users/${this.data.userId}`,
      method: "PUT",
      data: {
        nickname: name,
        avatarUrl: (this.data.user && this.data.user.avatarUrl) || "",
        phone: (this.data.user && this.data.user.phone) || ""
      }
    })
      .then((user) => {
        setCachedUser(user);
        this.setData({ editing: false, editError: "" });
        wx.showToast({ title: "已保存", icon: "success" });
        this.loadData();
      })
      .catch((err) => {
        this.setData({ editError: String(err || "保存失败") });
      });
  },
  onEditPanel() {},
  onRetry() {
    this.loadData();
  },
  onLogout() {
    wx.showModal({
      title: "退出登录",
      content: "确定要退出登录吗？",
      success: (res) => {
        if (res.confirm) {
          clearAuth();
          this.setUnauthState();
        }
      }
    });
  },
  setUnauthState(customDesc) {
    const inviteHint = hasPendingInviteForLogin();
    const desc = customDesc || (inviteHint
      ? "登录安心游，解锁更优惠的价格。温馨提示：这次不登录，等您回去了非扫码登录无法开启优惠哦。"
      : "请先完成授权登录后查看个人信息");
    this.setData({
      status: "unauth",
      unauthDesc: desc,
      user: {},
      displayName: "",
      displayPhone: "",
      userId: null,
      orderSummary: {
        all: 0,
        unpaid: 0,
        paid: 0,
        used: 0,
        refund: 0
      }
    });
  }
});
