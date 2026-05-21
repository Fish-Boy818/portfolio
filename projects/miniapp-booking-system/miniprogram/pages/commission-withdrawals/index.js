const { ensureAuth } = require("../../utils/auth");
const { listCommissionWithdrawals, normalizeWithdrawFailMessage } = require("../../utils/commission");

function money(value) {
  const num = Number(value || 0);
  if (Number.isNaN(num)) {
    return "0.00";
  }
  return num.toFixed(2);
}

function formatDateTime(value) {
  if (!value) {
    return "";
  }
  const raw = String(value)
    .trim()
    .replace("T", " ")
    .replace(/\.\d+$/, "")
    .replace("Z", "")
    .replace(/\//g, "-");
  const matched = raw.match(/(\d{4})[-.](\d{1,2})[-.](\d{1,2})(?:\s+(\d{1,2}):(\d{1,2}))?/);
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

Page({
  data: {
    status: "loading",
    errorMessage: "加载失败，请稍后重试",
    withdrawals: []
  },
  onLoad() {
    this.loadData();
  },
  onPullDownRefresh() {
    this.loadData(true);
  },
  loadData(fromPullDown) {
    if (!fromPullDown) {
      this.setData({ status: "loading" });
    }
    ensureAuth({ strict: true })
      .then(() => listCommissionWithdrawals())
      .then((withdrawals) => {
        const list = Array.isArray(withdrawals) ? withdrawals : [];
        this.setData({
          status: "ready",
          withdrawals: list.map((item) => ({
            ...item,
            amount: money(item && item.amount ? item.amount : 0),
            statusText: item && item.statusText ? item.statusText : "处理中",
            failReason: normalizeWithdrawFailMessage(item && item.failReason ? item.failReason : ""),
            timeText: formatDateTime(
              (item && (item.createdAt || item.createdTime || item.updatedAt || item.updatedTime)) || ""
            )
          }))
        });
      })
      .catch((err) => {
        this.setData({
          status: "error",
          errorMessage: String(err || "加载失败")
        });
      })
      .finally(() => {
        if (fromPullDown) {
          wx.stopPullDownRefresh();
        }
      });
  },
  onRetry() {
    this.loadData();
  }
});
