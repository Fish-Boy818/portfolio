const { request } = require("../../utils/api");

function toValidId(value) {
  const id = Number(value);
  if (!Number.isFinite(id) || id <= 0) {
    return 0;
  }
  return id;
}

Page({
  data: {
    keyword: "",
    status: "loading",
    errorMessage: "网络异常，请稍后重试",
    clubs: [],
    allClubs: []
  },
  onLoad(options) {
    const keyword = options.keyword ? decodeURIComponent(options.keyword) : "";
    this.setData({ keyword }, () => {
      this.loadData();
    });
  },
  onShow() {
    if (this.data.status === "loading") {
      return;
    }
    this.loadData();
  },
  loadData() {
    this.setData({ status: "loading" });
    request({ url: "/api/clubs" })
      .then((clubs) => {
        this.setData({ allClubs: clubs || [] }, () => {
          this.applyFilter();
        });
      })
      .catch((err) => {
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
      });
  },
  applyFilter() {
    const keyword = this.data.keyword.trim();
    let list = this.data.allClubs.slice();
    if (keyword) {
      list = list.filter((club) => {
        const name = club.name || "";
        const location = club.location || "";
        const tags = Array.isArray(club.tags) ? club.tags.join(",") : "";
        return name.includes(keyword) || location.includes(keyword) || tags.includes(keyword);
      });
    }
    this.setData({
      clubs: list,
      status: list.length ? "ready" : "empty"
    });
  },
  onInput(e) {
    this.setData({ keyword: e.detail.value });
  },
  onSearch() {
    this.applyFilter();
    wx.showToast({ title: "已更新筛选", icon: "none" });
  },
  onClear() {
    this.setData({ keyword: "" }, () => {
      this.applyFilter();
    });
  },
  onTapClub(e) {
    const id = toValidId(e && e.detail ? e.detail.id : 0);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/club-detail/index?id=${id}` });
  },
  onRetry() {
    this.loadData();
  }
});
