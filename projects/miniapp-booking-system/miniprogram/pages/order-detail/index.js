const { request } = require("../../utils/api");

const PLATFORM_SERVICE_PHONE = "";
const PLATFORM_SERVICE_PHONE_STORAGE_KEY = "platform_service_phone";
const DEFAULT_ACTIVITY_COVER = "/assets/images/activities/activity-1.png";

function sanitizeText(value) {
  if (value === null || value === undefined) {
    return "";
  }
  return String(value).trim();
}

function getPlatformServicePhone() {
  try {
    const saved = wx.getStorageSync(PLATFORM_SERVICE_PHONE_STORAGE_KEY);
    if (saved) {
      return sanitizeText(saved);
    }
  } catch (e) {
    // ignore storage error
  }
  return PLATFORM_SERVICE_PHONE;
}

function setPlatformServicePhone(value) {
  const normalized = sanitizeText(value);
  try {
    wx.setStorageSync(PLATFORM_SERVICE_PHONE_STORAGE_KEY, normalized);
  } catch (e) {
    // ignore storage error
  }
  return normalized;
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }
  const raw = String(value).trim().replace("T", " ").replace(/\.\d+$/, "").replace("Z", "").replace(/\//g, "-");
  const matched = raw.match(/(\d{4})[-.](\d{1,2})[-.](\d{1,2})(?:\s+(\d{1,2}):(\d{1,2}))?/);
  if (matched) {
    const year = matched[1];
    const month = String(matched[2]).padStart(2, "0");
    const day = String(matched[3]).padStart(2, "0");
    const hour = String(matched[4] || 0).padStart(2, "0");
    const minute = String(matched[5] || 0).padStart(2, "0");
    return `${year}.${month}.${day} ${hour}:${minute}`;
  }
  const date = new Date(raw.replace(/-/g, "/"));
  if (!Number.isNaN(date.getTime())) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hour = String(date.getHours()).padStart(2, "0");
    const minute = String(date.getMinutes()).padStart(2, "0");
    return `${year}.${month}.${day} ${hour}:${minute}`;
  }
  return raw;
}

function resolveOrderTime(order) {
  return formatDateTime(order.createdAt || order.createTime || order.orderTime || order.created_time || order.create_time);
}

Page({
  data: {
    orderId: 1,
    status: "loading",
    errorMessage: "订单信息加载失败",
    order: null,
    statusTitle: "",
    statusSub: "",
    statusBadgeClass: "",
    statusBadgeText: "",
    showPay: false,
    paying: false,
    payButtonText: "",
    payTip: "",
    showRefund: false,
    refundButtonText: "",
    refundTip: "",
    showVerify: false,
    verifyDisabled: true,
    verifyButtonText: "",
    verifyTip: "",
    showExperienceSupport: false,
    merchantPhone: "",
    platformPhone: ""
  },
  onLoad(options) {
    const id = Number(options.id) || 1;
    this.setData({ orderId: id }, () => {
      this.loadData();
    });
  },
  loadData() {
    this.setData({ status: "loading" });
    Promise.all([
      request({ url: `/api/orders/${this.data.orderId}` }),
      request({ url: "/api/profile-content" }).catch(() => null)
    ])
      .then(([order, profileContent]) => {
        if (!order) {
          this.setData({ status: "empty" });
          return;
        }
        const platformPhone = setPlatformServicePhone(profileContent && profileContent.platformServicePhone);
        return this.applyOrderState(order, "ready", platformPhone);
      })
      .catch((err) => {
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
      });
  },
  applyOrderState(order, pageStatus, platformPhoneOverride) {
    const mapped = this.mapOrder(order);
    const statusMeta = this.getStatusMeta(mapped);
    const payMeta = this.getPayMeta(mapped);
    const refundMeta = this.getRefundMeta(mapped);
    const verifyMeta = this.getVerifyMeta(mapped);
    const platformPhone = sanitizeText(platformPhoneOverride) || getPlatformServicePhone();
    return this.fetchMerchantPhone(mapped.clubId).then((merchantPhone) => {
      this.setData({
        order: mapped,
        status: pageStatus || "ready",
        showExperienceSupport: mapped.status === "used",
        merchantPhone,
        platformPhone,
        ...statusMeta,
        ...payMeta,
        ...refundMeta,
        ...verifyMeta
      });
    });
  },
  fetchMerchantPhone(clubId) {
    if (!clubId) {
      return Promise.resolve("");
    }
    return request({ url: `/api/clubs/${clubId}` })
      .then((club) => sanitizeText(club && club.phone))
      .catch(() => "");
  },
  getStatusMeta(order) {
    const map = {
      unpaid: { title: "待支付", sub: "请在 5 分钟内完成支付" },
      paid: { title: "已支付", sub: "请到服务地点核销" },
      used: { title: "已核销", sub: "核销已完成" },
      refund: { title: "已退款", sub: "退款已原路返回" },
      cancelled: { title: "已取消", sub: "订单已关闭" }
    };
    const info = map[order.status] || { title: order.statusText, sub: "" };
    return {
      statusTitle: info.title,
      statusSub: info.sub,
      statusBadgeClass: order.badgeClass,
      statusBadgeText: order.statusText
    };
  },
  getVerifyMeta(order) {
    const showVerify = order.status === "paid" || order.status === "used";
    const verifyDisabled = order.status !== "paid";
    const verifyButtonText = verifyDisabled ? "已核销" : "去核销";
    const verifyTip = order.status === "used" ? "核销已完成" : "确认后立即完成核销，请谨慎操作";
    return {
      showVerify,
      verifyDisabled,
      verifyButtonText,
      verifyTip
    };
  },
  getPayMeta(order) {
    const showPay = order.status === "unpaid";
    const payButtonText = "立即支付";
    const payTip = "完成支付后将生成核销码";
    return {
      showPay,
      payButtonText,
      payTip
    };
  },
  getRefundMeta(order) {
    const showRefund = order.status === "paid";
    const refundButtonText = "申请退款";
    const refundTip = "退款后订单将关闭";
    return {
      showRefund,
      refundButtonText,
      refundTip
    };
  },
  onVerify() {
    if (this.data.verifyDisabled) {
      return;
    }
    wx.showModal({
      title: "确认核销",
      content: "确认后将立即核销，无法撤销",
      success: (res) => {
        if (res.confirm) {
          request({ url: `/api/orders/${this.data.orderId}/verify`, method: "POST" })
            .then((order) => {
              this.applyOrderState(order).then(() => {
                wx.showToast({ title: "核销成功", icon: "success" });
              });
            })
            .catch((err) => {
              wx.showToast({ title: String(err || "核销失败"), icon: "none" });
            });
        }
      }
    });
  },
  onPay() {
    if (this.data.paying || !this.data.order || this.data.order.status !== "unpaid") {
      return;
    }
    this.setData({ paying: true });
    wx.showModal({
      title: "确认支付",
      content: "确认支付后生成核销码",
      success: (res) => {
        if (!res.confirm) {
          this.setData({ paying: false });
          return;
        }
        request({ url: `/api/pay/orders/${this.data.orderId}/jsapi`, method: "POST" })
          .then((payParams) => {
            wx.requestPayment({
              timeStamp: String(payParams.timeStamp || ""),
              nonceStr: payParams.nonceStr || "",
              package: payParams.packageValue || payParams.package || "",
              signType: payParams.signType || "RSA",
              paySign: payParams.paySign || "",
              success: () => {
                wx.showToast({ title: "支付成功", icon: "success" });
                this.confirmPaidAndReload(10);
              },
              fail: (err) => {
                const message = err && err.errMsg ? String(err.errMsg) : "";
                this.setData({ paying: false });
                if (message.indexOf("cancel") > -1) {
                  wx.showToast({ title: "已取消支付", icon: "none" });
                  return;
                }
                wx.showToast({ title: "支付失败，请重试", icon: "none" });
              }
            });
          })
          .catch((err) => {
            this.setData({ paying: false });
            wx.showToast({ title: String(err || "支付失败"), icon: "none" });
        });
      },
      fail: () => {
        this.setData({ paying: false });
      }
    });
  },
  confirmPaidAndReload(remain) {
    request({ url: `/api/orders/${this.data.orderId}/pay`, method: "POST" })
      .then((order) => {
        if (order && (order.status === "paid" || order.status === "used")) {
          this.applyOrderState(order, "ready").then(() => {
            this.setData({ paying: false });
          });
          return;
        }
        if (remain <= 1) {
          this.setData({ paying: false });
          this.loadData();
          return;
        }
        setTimeout(() => this.confirmPaidAndReload(remain - 1), 1000);
      })
      .catch(() => {
        if (remain <= 1) {
          this.setData({ paying: false });
          this.loadData();
          return;
        }
        setTimeout(() => this.confirmPaidAndReload(remain - 1), 1000);
      });
  },
  onRefund() {
    if (!this.data.order || this.data.order.status !== "paid") {
      return;
    }
    wx.showModal({
      title: "申请退款",
      content: "确认退款后订单将关闭",
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        request({ url: `/api/orders/${this.data.orderId}/refund`, method: "POST" })
          .then((order) => {
            this.applyOrderState(order).then(() => {
              wx.showToast({ title: "退款成功", icon: "success" });
            });
          })
          .catch((err) => {
            wx.showToast({ title: String(err || "退款失败"), icon: "none" });
          });
      }
    });
  },
  onRetry() {
    this.loadData();
  },
  onGoOrders() {
    wx.switchTab({ url: "/pages/orders/index" });
  },
  onOrderImageError() {
    const order = this.data.order || {};
    if (order.coverImage === DEFAULT_ACTIVITY_COVER) {
      return;
    }
    this.setData({ "order.coverImage": DEFAULT_ACTIVITY_COVER });
  },
  onCallMerchant() {
    this.callPhone(this.data.merchantPhone, "商家电话暂未配置");
  },
  onCallPlatform() {
    this.callPhone(this.data.platformPhone, "平台电话暂未配置");
  },
  callPhone(phone, emptyMessage) {
    const phoneNumber = sanitizeText(phone);
    if (!phoneNumber) {
      wx.showToast({ title: emptyMessage || "暂无联系电话", icon: "none" });
      return;
    }
    wx.showModal({
      title: "确认拨号",
      content: `是否拨打 ${phoneNumber}？`,
      confirmText: "拨打",
      cancelText: "取消",
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        wx.makePhoneCall({
          phoneNumber,
          fail: () => {
            wx.showToast({ title: "拨打失败，请稍后重试", icon: "none" });
          }
        });
      }
    });
  },
  mapOrder(order) {
    const store = order.clubLocation || order.clubName || "";
    const orderTime = resolveOrderTime(order);
    const statusMap = {
      unpaid: { text: "待支付", badge: "badge--warning" },
      paid: { text: "已支付", badge: "badge--info" },
      used: { text: "已核销", badge: "badge--success" },
      refund: { text: "已退款", badge: "badge--danger" },
      cancelled: { text: "已取消", badge: "badge--danger" }
    };
    const meta = statusMap[order.status] || {};
    return {
      ...order,
      store,
      time: orderTime,
      orderTime,
      price: order.unitPrice,
      original: order.originalPrice || 0,
      phone: order.phone || order.userPhone || "",
      paidAt: formatDateTime(order.paidAt),
      verifiedAt: formatDateTime(order.verifiedAt),
      refundAt: formatDateTime(order.refundAt),
      cancelledAt: formatDateTime(order.cancelledAt),
      coverImage: this.resolveOrderCover(order),
      statusText: order.statusText || meta.text || "",
      badgeClass: order.badgeClass || meta.badge || ""
    };
  },
  resolveOrderCover(order) {
    if (!order) {
      return DEFAULT_ACTIVITY_COVER;
    }
    const gallery = Array.isArray(order.gallery) ? order.gallery : [];
    const candidates = [order.cover, order.imageUrl, gallery[0]];
    for (let i = 0; i < candidates.length; i += 1) {
      const value = String(candidates[i] || "").trim();
      if (value) {
        return value;
      }
    }
    return DEFAULT_ACTIVITY_COVER;
  }
});
