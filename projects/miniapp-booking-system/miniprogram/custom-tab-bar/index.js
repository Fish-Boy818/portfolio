const DEFAULT_ITEMS = [
  { key: "home", pagePath: "/pages/home/index", text: "首页" },
  { key: "category", pagePath: "/pages/category/index", text: "分类" },
  { key: "orders", pagePath: "/pages/orders/index", text: "订单" },
  { key: "welfare", pagePath: "/pages/bounty/index", text: "悬赏" },
  { key: "profile", pagePath: "/pages/profile/index", text: "我的" }
];

Component({
  data: {
    selected: 0,
    selectedPath: DEFAULT_ITEMS[0].pagePath,
    items: DEFAULT_ITEMS
  },
  lifetimes: {
    attached() {
      this.syncFromGlobal();
      this.updateSelected();
    }
  },
  pageLifetimes: {
    show() {
      this.syncFromGlobal();
      this.updateSelected();
    }
  },
  methods: {
    onSwitchTab(e) {
      const index = Number(e.currentTarget.dataset.index || 0);
      const item = this.data.items[index];
      if (!item || !item.pagePath) {
        return;
      }
      if (index === Number(this.data.selected)) {
        return;
      }
      const app = getApp ? getApp() : null;
      if (app && typeof app.setCurrentTab === "function") {
        app.setCurrentTab(index, item.pagePath);
      }
      this.setActive(item.pagePath);
      wx.switchTab({
        url: item.pagePath,
        fail: () => {
          this.updateSelected();
        }
      });
    },
    setActive(pagePath) {
      const targetPath = normalizePagePath(pagePath || DEFAULT_ITEMS[0].pagePath);
      const index = this.data.items.findIndex((item) => item.pagePath === targetPath);
      const nextIndex = index >= 0 ? index : 0;
      const nextPath = index >= 0 ? targetPath : DEFAULT_ITEMS[0].pagePath;
      const app = getApp ? getApp() : null;
      if (app && typeof app.setCurrentTab === "function") {
        app.setCurrentTab(nextIndex, nextPath);
      }
      this.setData({
        selected: nextIndex,
        selectedPath: nextPath
      });
    },
    updateSelected() {
      const app = getApp ? getApp() : null;
      const globalPath = app && app.globalData ? String(app.globalData.currentTabPath || "") : "";
      const pages = getCurrentPages();
      const currentPage = pages && pages.length ? pages[pages.length - 1] : null;
      const route = currentPage && currentPage.route ? `/${currentPage.route}` : "";
      const selectedPath = route || globalPath || DEFAULT_ITEMS[0].pagePath;
      this.setActive(selectedPath);
    },
    syncFromGlobal() {
      const app = getApp ? getApp() : null;
      const texts = app && app.globalData ? app.globalData.tabBarTexts : null;
      if (!texts) {
        return;
      }
      this.setData({
        items: this.buildItems(texts)
      });
    },
    buildItems(texts) {
      const safe = texts || {};
      return DEFAULT_ITEMS.map((item) => ({
        ...item,
        text: sanitizeText(safe[item.key], item.text)
      }));
    }
  }
});

function sanitizeText(value, fallback) {
  const text = value === null || value === undefined ? "" : String(value).trim();
  return text || fallback;
}

function normalizePagePath(value) {
  const text = value === null || value === undefined ? "" : String(value).trim();
  if (!text) {
    return DEFAULT_ITEMS[0].pagePath;
  }
  return text.startsWith("/") ? text : `/${text}`;
}
