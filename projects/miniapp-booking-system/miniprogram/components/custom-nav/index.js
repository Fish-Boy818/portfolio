Component({
  properties: {
    title: {
      type: String,
      value: ""
    },
    showBack: {
      type: Boolean,
      value: true
    },
    backType: {
      type: String,
      value: "auto"
    },
    backUrl: {
      type: String,
      value: "/pages/home/index"
    }
  },
  data: {
    statusBarHeight: 20,
    navBarHeight: 44,
    sideWidth: 88,
    totalHeight: 64
  },
  lifetimes: {
    attached() {
      this.initLayout();
    }
  },
  methods: {
    initLayout() {
      const sys = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {};
      const statusBarHeight = Number(sys.statusBarHeight || 20);
      let navBarHeight = 44;
      let sideWidth = 88;
      try {
        const menu = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
        if (menu && menu.top) {
          navBarHeight = Math.round(menu.height + (menu.top - statusBarHeight) * 2);
        }
        if (menu && menu.left && sys.windowWidth) {
          sideWidth = Math.max(88, Math.round(sys.windowWidth - menu.left + 8));
        }
      } catch (e) {
        // keep defaults
      }
      this.setData({
        statusBarHeight,
        navBarHeight,
        sideWidth,
        totalHeight: statusBarHeight + navBarHeight
      });
    },
    onBack() {
      const pages = getCurrentPages ? getCurrentPages() : [];
      const type = String(this.data.backType || "auto");
      const url = this.data.backUrl || "/pages/home/index";
      if (type === "switchTab") {
        wx.switchTab({ url });
        return;
      }
      if (type === "navigateTo") {
        wx.navigateTo({ url });
        return;
      }
      if (type === "reLaunch") {
        wx.reLaunch({ url });
        return;
      }
      if (pages.length > 1) {
        wx.navigateBack();
        return;
      }
      wx.switchTab({ url: "/pages/home/index" });
    }
  }
});
