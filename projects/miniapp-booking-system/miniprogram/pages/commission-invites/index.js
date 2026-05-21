const { request } = require("../../utils/api");
const { ensureAuth } = require("../../utils/auth");

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
    inviteCount: 0,
    records: []
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
      .then(() =>
        Promise.all([
          request({ url: "/api/commission/invite/me" }).catch(() => null),
          request({ url: "/api/commission/invite/records" }).catch(() => [])
        ])
      )
      .then(([overview, records]) => {
        const list = Array.isArray(records) ? records : [];
        this.setData({
          status: "ready",
          inviteCount: overview && overview.inviteCount ? Number(overview.inviteCount) : list.length,
          records: list.map((item) => ({
            ...item,
            nickname: item && item.nickname ? item.nickname : "微信用户",
            phone: item && item.phone ? item.phone : "未绑定手机号",
            invitedAt: formatDateTime(item && item.invitedAt ? item.invitedAt : "")
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
