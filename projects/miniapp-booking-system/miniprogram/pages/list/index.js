const { request } = require("../../utils/api");
const { ensureAuth } = require("../../utils/auth");
const {
  buildCategoryMap,
  applyCategoryLabelList,
  extractItemTags,
  matchCategoryByTags,
  resolveCategoryName
} = require("../../utils/category");
const { cacheProductPrefetch } = require("../../utils/prefetch");

function sanitizeText(value) {
  if (value === null || value === undefined) {
    return "";
  }
  const text = String(value).trim();
  if (!text || text === "null" || text === "undefined") {
    return "";
  }
  return text;
}

function toValidId(value) {
  const id = Number(value);
  if (!Number.isFinite(id) || id <= 0) {
    return 0;
  }
  return id;
}

Page({
  _itemTapAt: 0,
  data: {
    keyword: "",
    activeFilter: "all",
    clubId: null,
    club: null,
    filters: [{ id: "all", name: "全部" }],
    priceFilters: [
      { id: "all", name: "价格不限", min: 0, max: 0 },
      { id: "p1", name: "≤200", min: 0, max: 200 },
      { id: "p2", name: "201-400", min: 201, max: 400 },
      { id: "p3", name: "401-600", min: 401, max: 600 },
      { id: "p4", name: "600+", min: 601, max: 9999 }
    ],
    timeFilters: [
      { id: "all", name: "不限" },
      { id: "morning", name: "上午" },
      { id: "afternoon", name: "下午" },
      { id: "evening", name: "傍晚" }
    ],
    activePrice: "all",
    activeTime: "all",
    status: "loading",
    errorMessage: "网络异常，请稍后重试",
    items: [],
    allItems: [],
    simulateError: false,
    userId: null
  },
  onLoad(options) {
    const keyword = options.keyword ? decodeURIComponent(options.keyword) : "";
    const category = options.category || "";
    const clubId = options.clubId ? Number(options.clubId) : null;
    this.setData({
      keyword,
      activeFilter: category || this.data.activeFilter,
      clubId,
      club: null
    }, () => {
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
    ensureAuth()
      .catch(() => null)
      .then((user) => {
        const userId = user ? user.id : null;
        const clubPromise = this.data.clubId ? request({ url: `/api/clubs/${this.data.clubId}` }) : Promise.resolve(null);
        return Promise.all([request({ url: "/api/categories" }), clubPromise]).then(([categories, club]) => {
          let filters = [{ id: "all", name: "全部", keywords: "" }].concat(
            (categories || []).map((item) => ({
              id: item.key,
              name: resolveCategoryName(item),
              keywords: item.keywords || item.labels || item.tags || ""
            }))
          );
          const desiredFilter = this.data.activeFilter;
          if (desiredFilter && desiredFilter !== "all" && !filters.some((item) => item.id === desiredFilter)) {
            filters = filters.concat([{ id: desiredFilter, name: desiredFilter, keywords: desiredFilter }]);
          }
          const activeFilter = filters.some((item) => item.id === desiredFilter) ? desiredFilter : "all";
          const categoryMap = buildCategoryMap(categories);
          const app = getApp ? getApp() : null;
          if (app && app.globalData) {
            app.globalData.categoryMap = categoryMap;
          }
          const params = {
            userId,
            keyword: this.data.keyword || undefined,
            clubId: this.data.clubId || undefined
          };
          return request({ url: "/api/activities", data: params }).then((activities) => {
            const decorated = applyCategoryLabelList(
              (activities || []).map((item) => {
              const rawSubtitle = sanitizeText(item.subtitle);
              const cleanedSubtitle = /^\d+$/.test(rawSubtitle) ? "" : rawSubtitle;
              const clubName = sanitizeText(item.clubName);
              const clubLocation = sanitizeText(item.clubLocation);
              const isClubInfo =
                (cleanedSubtitle && clubName && cleanedSubtitle.includes(clubName)) ||
                (cleanedSubtitle && clubLocation && cleanedSubtitle.includes(clubLocation)) ||
                cleanedSubtitle === clubName ||
                cleanedSubtitle === clubLocation;
              return {
                ...item,
                status: item.status === "active" ? "可预约" : "已下架",
                badgeClass: item.status === "active" ? "badge--warning" : "badge--danger",
                subtitle: isClubInfo ? "" : cleanedSubtitle
              };
              }),
              categoryMap
            );
            this.setData({ allItems: decorated, club, userId, filters, activeFilter }, () => {
              this.applyFilter();
            });
          });
        });
      })
      .catch((err) => {
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
      });
  },
  applyFilter() {
    const keyword = this.data.keyword.trim();
    const activeFilter = this.data.activeFilter;
    const activeFilterMeta = this.data.filters.find((item) => item.id === activeFilter);
    const activeFilterName = activeFilterMeta ? String(activeFilterMeta.name || "").trim() : "";
    const clubId = this.data.clubId;
    const priceFilter = this.data.priceFilters.find((item) => item.id === this.data.activePrice);
    let list = this.data.allItems.slice();

    if (clubId) {
      list = list.filter((item) => item.clubId === clubId);
    }

    if (activeFilter !== "all") {
      list = list.filter((item) => {
        const tags = extractItemTags(item);
        const categoryLabel = sanitizeText(
          item.categoryLabel || item.categoryName || resolveCategoryName({ key: item.category })
        );
        if (activeFilterMeta && tags.length && matchCategoryByTags(tags, activeFilterMeta)) {
          return true;
        }
        if (activeFilterName) {
          if (
            categoryLabel &&
            (categoryLabel === activeFilterName ||
              categoryLabel.includes(activeFilterName) ||
              activeFilterName.includes(categoryLabel))
          ) {
            return true;
          }
          if (tags.length && tags.some(
            (tag) => tag === activeFilterName || tag.includes(activeFilterName) || activeFilterName.includes(tag)
          )) {
            return true;
          }
        }
        if (item.category === activeFilter) {
          return true;
        }
        if (
          categoryLabel &&
          (categoryLabel === activeFilter || categoryLabel.includes(activeFilter) || activeFilter.includes(categoryLabel))
        ) {
          return true;
        }
        return false;
      });
    }

    if (priceFilter && priceFilter.id !== "all") {
      list = list.filter((item) => item.price >= priceFilter.min && item.price <= priceFilter.max);
    }

    const hasTime = list.some((item) => item.timeSlot);
    if (hasTime && this.data.activeTime !== "all") {
      list = list.filter((item) => item.timeSlot === this.data.activeTime);
    }

    if (keyword) {
      list = list.filter((item) => {
        const title = sanitizeText(item.title);
        const subtitle = sanitizeText(item.subtitle);
        return title.includes(keyword) || subtitle.includes(keyword);
      });
    }

    this.setData({
      items: list,
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
  onSelectFilter(e) {
    const id = e.currentTarget.dataset.id || "all";
    this.setData({ activeFilter: id }, () => {
      this.loadData();
    });
  },
  onSelectPrice(e) {
    const id = e.currentTarget.dataset.id || "all";
    this.setData({ activePrice: id }, () => {
      this.applyFilter();
    });
  },
  onSelectTime(e) {
    const id = e.currentTarget.dataset.id || "all";
    this.setData({ activeTime: id }, () => {
      this.applyFilter();
    });
  },
  onTapItem(e) {
    const now = Date.now();
    if (this._itemTapAt && now - this._itemTapAt < 450) {
      return;
    }
    this._itemTapAt = now;
    const id = toValidId(e && e.detail ? e.detail.id : 0);
    if (!id) {
      return;
    }
    const item = e && e.detail ? e.detail.item : null;
    if (item && toValidId(item.id) === id) {
      cacheProductPrefetch(item);
    }
    wx.navigateTo({ url: `/pages/detail/index?id=${id}` });
  },
  onGoClub() {
    if (!this.data.clubId) {
      return;
    }
    wx.navigateTo({ url: `/pages/club-detail/index?id=${this.data.clubId}` });
  },
  onRetry() {
    this.loadData();
  },
  onGoHome() {
    wx.switchTab({ url: "/pages/home/index" });
  }
});
