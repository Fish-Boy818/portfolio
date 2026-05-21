function sanitizeText(value) {
  if (value === null || value === undefined) {
    return "";
  }
  return String(value).trim();
}

Page({
  data: {
    url: "",
    title: "网页"
  },
  onLoad(options) {
    const title = decodeURIComponent(sanitizeText(options && options.title) || "网页");
    const url = decodeURIComponent(sanitizeText(options && options.url) || "");
    if (!/^https?:\/\//i.test(url)) {
      wx.showToast({ title: "链接无效", icon: "none" });
      setTimeout(() => wx.navigateBack(), 500);
      return;
    }
    this.setData({ url, title });
    wx.setNavigationBarTitle({ title });
  }
});

