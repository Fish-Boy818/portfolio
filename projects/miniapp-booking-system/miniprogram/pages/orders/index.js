const { request } = require("../../utils/api");
const { ensureAuth } = require("../../utils/auth");
const ACTIVE_STATUS_KEY = "orders_active_status";

function syncTabBar(page, selected) {
  const app = getApp ? getApp() : null;
  if (app && typeof app.syncTabBarSelection === "function") {
    app.syncTabBarSelection(page, selected);
  }
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
    return `${year}.${month}.${day}`;
  }
  const date = new Date(raw.replace(/-/g, "/"));
  if (!Number.isNaN(date.getTime())) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}.${month}.${day}`;
  }
  return raw;
}

function resolveOrderTime(order) {
  return formatDateTime(order.createdAt || order.createTime || order.orderTime || order.created_time || order.create_time);
}

Page({
  data: {
    activeStatus: "all",
    status: "loading",
    errorMessage: "网络异常，请稍后重试",
    orders: [],
    allOrders: [],
    simulateError: false,
    userId: null
  },
  onLoad() {
    const nextStatus = this.consumeExternalStatus();
    if (nextStatus) {
      this.setData({ activeStatus: nextStatus }, () => {
        this.loadData();
      });
      return;
    }
    this.loadData();
  },
  onShow() {
    syncTabBar(this, 2);
    const nextStatus = this.consumeExternalStatus();
    if (this.data.status === "loading") {
      return;
    }
    if (nextStatus) {
      this.setData({ activeStatus: nextStatus }, () => {
        this.loadData();
      });
      return;
    }
    this.loadData();
  },
  consumeExternalStatus() {
    let status = "";
    try {
      status = String(wx.getStorageSync(ACTIVE_STATUS_KEY) || "");
      if (status) {
        wx.removeStorageSync(ACTIVE_STATUS_KEY);
      }
    } catch (e) {
      status = "";
    }
    const allowed = {
      all: true,
      unpaid: true,
      paid: true,
      used: true,
      refund: true,
      cancelled: true
    };
    if (!status || !allowed[status] || status === this.data.activeStatus) {
      return "";
    }
    return status;
  },
  loadData() {
    this.setData({ status: "loading" });
    ensureAuth({ strict: true })
      .then((user) => {
        const userId = user ? user.id : null;
        if (!userId) {
          this.setData({ status: "error", errorMessage: "请先到“我的”页面登录微信后再查看订单" });
          return Promise.resolve();
        }
        const params = {
          userId
        };
        if (this.data.activeStatus !== "all") {
          params.status = this.data.activeStatus;
        }
        return request({ url: "/api/orders", data: params }).then((orders) => {
          const mapped = (orders || []).map((order) => this.mapOrder(order));
          this.setData({ allOrders: mapped, userId }, () => {
            this.applyFilter();
          });
        });
      })
      .catch((err) => {
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
      });
  },
  getActionMeta(order) {
    if (order.status === "unpaid") {
      return { primaryText: "立即支付", secondaryText: "取消订单" };
    }
    if (order.status === "paid") {
      return { primaryText: "去核销", secondaryText: "申请退款" };
    }
    return { primaryText: "查看详情", secondaryText: "" };
  },
  getStatusMeta(order) {
    const map = {
      unpaid: { text: "待支付", badge: "badge--warning" },
      paid: { text: "已支付", badge: "badge--info" },
      used: { text: "已核销", badge: "badge--success" },
      refund: { text: "已退款", badge: "badge--danger" },
      cancelled: { text: "已取消", badge: "badge--danger" }
    };
    const meta = map[order.status];
    if (!meta) {
      return { statusText: order.statusText, badgeClass: order.badgeClass };
    }
    return { statusText: meta.text, badgeClass: meta.badge };
  },
  applyFilter() {
    const status = this.data.activeStatus;
    let list = this.data.allOrders.slice();
    if (status !== "all") {
      list = list.filter((order) => order.status === status);
    }
    this.setData({
      orders: list,
      status: list.length ? "ready" : "empty"
    });
  },
  onFilter(e) {
    const status = e.currentTarget.dataset.status || "all";
    this.setData({ activeStatus: status }, () => {
      this.loadData();
    });
  },
  onPrimary(e) {
    const id = e.detail.id || 1;
    const order = this.data.allOrders.find((item) => item.id === id);
    if (!order) {
      return;
    }
    wx.navigateTo({ url: `/pages/order-detail/index?id=${id}` });
  },
  onSecondary(e) {
    const id = e.detail.id || 1;
    const order = this.data.allOrders.find((item) => item.id === id);
    if (!order) {
      return;
    }
    if (order.status === "unpaid") {
      wx.showModal({
        title: "取消订单",
        content: "确认取消该订单？",
        success: (res) => {
          if (!res.confirm) {
            return;
          }
          request({ url: `/api/orders/${order.id}/cancel`, method: "POST" })
            .then(() => {
              wx.showToast({ title: "订单已取消", icon: "none" });
              this.loadData();
            })
            .catch((err) => {
              wx.showToast({ title: String(err || "取消失败"), icon: "none" });
            });
        }
      });
      return;
    }
    if (order.status === "paid") {
      wx.showModal({
        title: "申请退款",
        content: "确认退款后订单将关闭",
        success: (res) => {
          if (!res.confirm) {
            return;
          }
          request({ url: `/api/orders/${order.id}/refund`, method: "POST" })
            .then(() => {
              wx.showToast({ title: "退款成功", icon: "success" });
              this.loadData();
            })
            .catch((err) => {
              wx.showToast({ title: String(err || "退款失败"), icon: "none" });
            });
        }
      });
    }
  },
  onRetry() {
    this.loadData();
  },
  onGoHome() {
    wx.switchTab({ url: "/pages/home/index" });
  },
  mapOrder(order) {
    const actionMeta = this.getActionMeta(order);
    const statusMeta = this.getStatusMeta(order);
    const title = order.title || "";
    return {
      ...order,
      ...actionMeta,
      ...statusMeta,
      no: order.orderNo || "",
      shortTitle: title.slice(0, 2),
      displayAmount: order.payAmount || order.unitPrice || 0,
      store: order.clubLocation || order.clubName || "",
      time: resolveOrderTime(order)
    };
  }
});
