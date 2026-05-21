const { request } = require("../../utils/api");
const { ensureAuth } = require("../../utils/auth");
const {
  buildCategoryMap,
  applyCategoryLabelList,
  extractItemTags,
  matchCategoryByTags,
  resolveCategoryName,
  buildTagCategories
} = require("../../utils/category");
const { cacheProductPrefetch } = require("../../utils/prefetch");

const CATEGORY_ICONS = {
  jetski: "/assets/images/icons/jetski.png",
  surf: "/assets/images/icons/surf.png",
  banana: "/assets/images/icons/banana.png"
};
const DEFAULT_ICON = "/assets/images/icons/search.png";

function syncTabBar(page, selected) {
  const app = getApp ? getApp() : null;
  if (app && typeof app.syncTabBarSelection === "function") {
    app.syncTabBarSelection(page, selected);
  }
}

function mapCategory(category) {
  if (!category) {
    return null;
  }
  const key = category.key || category.id || "";
  const keywords = category.keywords || category.labels || category.tags || "";
  return {
    key,
    name: resolveCategoryName(category),
    sub: category.sub || "",
    desc: category.desc || "",
    highlights: Array.isArray(category.highlights) ? category.highlights : [],
    icon: CATEGORY_ICONS[key] || DEFAULT_ICON,
    keywords
  };
}

function sanitizeText(value) {
  if (value === null || value === undefined) {
    return "";
  }
  return String(value).trim();
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
  _clubTapAt: 0,
  data: {
    statusBarHeight: 20,
    navBarHeight: 44,
    navTotalHeight: 64,
    keyword: "",
    categories: [],
    activeCategoryKey: "",
    activeCategory: {
      key: "",
      name: "",
      sub: "",
      desc: "",
      highlights: [],
      icon: DEFAULT_ICON
    },
    showClubs: true,
    showItems: true,
    status: "loading",
    errorMessage: "网络异常，请稍后重试",
    allItems: [],
    allClubs: [],
    itemPreview: [],
    clubPreview: [],
    simulateError: false,
    userId: null
  },
  onLoad() {
    this.initNavBar();
    this.loadData();
  },
  onShow() {
    syncTabBar(this, 1);
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
        return Promise.all([
          request({ url: "/api/categories" }),
          request({ url: "/api/clubs" }),
          request({ url: "/api/activities", data: { userId } })
        ]).then(([categoryList, clubs, activities]) => {
          const categoryMap = buildCategoryMap(categoryList);
          const app = getApp ? getApp() : null;
          if (app && app.globalData) {
            app.globalData.categoryMap = categoryMap;
          }
          const mapped = applyCategoryLabelList(
            (activities || []).map((activity) => ({
              ...activity,
              status: activity.status === "active" ? "可预约" : "已下架",
              badgeClass: activity.status === "active" ? "badge--warning" : "badge--danger",
              subtitle: activity.subtitle || `${activity.clubName || ""} ${activity.clubLocation || ""}`.trim()
            })),
            categoryMap
          );
          const categoriesFromApi = (categoryList || []).map(mapCategory).filter(Boolean);
          const categoriesFromTags = buildTagCategories(mapped).map(mapCategory).filter(Boolean);
          const categories = categoriesFromTags.length ? categoriesFromTags : categoriesFromApi;
          const fallbackCategory = categories[0] || {
            key: "",
            name: "",
            sub: "",
            desc: "",
            highlights: [],
            icon: DEFAULT_ICON
          };
          const activeKey = this.data.activeCategoryKey || fallbackCategory.key;
          const activeCategory = categories.find((item) => item.key === activeKey) || fallbackCategory;
          this.setData({
            userId,
            categories,
            activeCategoryKey: activeCategory.key,
            activeCategory,
            allItems: mapped,
            allClubs: clubs || []
          }, () => {
            this.applyFilter();
          });
        });
      })
      .catch((err) => {
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
      });
  },
  applyFilter() {
    const keyword = this.data.keyword.trim();
    const activeCategoryKey = this.data.activeCategoryKey;
    const activeCategory =
      this.data.categories.find((item) => item.key === activeCategoryKey) || this.data.activeCategory || null;
    const activeCategoryName = activeCategory ? String(activeCategory.name || "").trim() : "";
    const hasKeyword = !!keyword;
    let itemList = hasKeyword
      ? this.data.allItems.slice()
      : activeCategoryKey
        ? this.data.allItems.filter((item) => {
            const tags = extractItemTags(item);
            const categoryLabel = sanitizeText(item.categoryLabel || item.categoryName);
            if (activeCategory && tags.length && matchCategoryByTags(tags, activeCategory)) {
              return true;
            }
            if (activeCategoryName) {
              if (
                categoryLabel &&
                (categoryLabel === activeCategoryName ||
                  categoryLabel.includes(activeCategoryName) ||
                  activeCategoryName.includes(categoryLabel))
              ) {
                return true;
              }
              if (tags.length && tags.some(
                (tag) => tag === activeCategoryName || tag.includes(activeCategoryName) || activeCategoryName.includes(tag)
              )) {
                return true;
              }
            }
            if (item.category === activeCategoryKey) {
              return true;
            }
            if (
              categoryLabel &&
              (categoryLabel === activeCategoryKey ||
                categoryLabel.includes(activeCategoryKey) ||
                activeCategoryKey.includes(categoryLabel))
            ) {
              return true;
            }
            return false;
          })
        : this.data.allItems.slice();
    let clubList = this.data.allClubs.slice();

    if (keyword) {
      itemList = itemList.filter((item) => {
        const tags = extractItemTags(item).join(" ");
        const text = `${item.title || ""} ${item.subtitle || ""} ${item.clubName || ""} ${item.clubLocation || ""} ${tags}`;
        return text.includes(keyword);
      });
      clubList = clubList.filter((club) => {
        const tags = Array.isArray(club.tags) ? club.tags.join(" ") : (club.tags || "");
        const text = `${club.name || ""} ${club.location || ""} ${club.address || ""} ${tags}`;
        return text.includes(keyword);
      });
    }

    const itemPreview = itemList.slice(0, 6);
    const clubPreview = clubList.slice(0, 6);
    this.setData({
      itemPreview,
      clubPreview,
      status: itemPreview.length ? "ready" : "empty"
    });
  },
  onSelectCategory(e) {
    const id = e.currentTarget.dataset.id || "";
    const meta =
      this.data.categories.find((item) => item.key === id) ||
      this.data.categories[0] ||
      {
        key: "",
        name: "",
        sub: "",
        desc: "",
        highlights: [],
        icon: DEFAULT_ICON
      };
    this.setData({
      activeCategoryKey: meta.key,
      activeCategory: meta
    }, () => {
      this.applyFilter();
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
  onTapClub(e) {
    const now = Date.now();
    if (this._clubTapAt && now - this._clubTapAt < 450) {
      return;
    }
    this._clubTapAt = now;
    const id = toValidId(e && e.detail ? e.detail.id : 0);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/club-detail/index?id=${id}` });
  },
  onGoList() {
    const category = this.data.activeCategoryKey;
    wx.navigateTo({ url: `/pages/list/index?category=${category}` });
  },
  onGoClubList() {
    const keyword = this.data.keyword.trim();
    const url = keyword
      ? `/pages/club-list/index?keyword=${encodeURIComponent(keyword)}`
      : "/pages/club-list/index";
    wx.navigateTo({ url });
  },
  onRetry() {
    this.loadData();
  },
  onToggleClubs() {
    this.setData({ showClubs: !this.data.showClubs });
  },
  onToggleItems() {
    this.setData({ showItems: !this.data.showItems });
  },
  initNavBar() {
    const sys = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {};
    const statusBarHeight = Number(sys.statusBarHeight || 20);
    let navBarHeight = 44;
    try {
      const menu = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null;
      if (menu && menu.top) {
        navBarHeight = Math.round(menu.height + (menu.top - statusBarHeight) * 2);
      }
    } catch (e) {
      // fallback to defaults
    }
    this.setData({
      statusBarHeight,
      navBarHeight,
      navTotalHeight: statusBarHeight + navBarHeight
    });
  }
});
