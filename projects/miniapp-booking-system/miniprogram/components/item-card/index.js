Component({
  properties: {
    item: {
      type: Object,
      value: {}
    }
  },
  data: {
    tagList: [],
    buttonText: "抢购",
    isDisabled: false,
    showOriginal: false,
    showCommission: false,
    expectedCommission: "0.00"
  },
  observers: {
    item(item) {
      const { extractItemTags } = require("../../utils/category");
      const tags = [];
      const extracted = extractItemTags(item);
      extracted.forEach((tag) => {
        if (!tags.includes(tag)) {
          tags.push(tag);
        }
      });
      const hasCnTag = tags.some((tag) => /[\u4e00-\u9fa5]/.test(String(tag)));
      const visibleTags = hasCnTag
        ? tags.filter((tag) => !/^[a-z0-9_-]+$/i.test(String(tag || "").trim()))
        : tags;
      if (tags.length === 0) {
        const app = getApp ? getApp() : null;
        const categoryMap = app && app.globalData ? app.globalData.categoryMap : null;
        const categoryKey = String(item && item.category ? item.category : "").trim();
        const mappedCategory = categoryKey && categoryMap ? categoryMap[categoryKey] : "";
        let category = String(item && (item.categoryName || item.categoryLabel || mappedCategory) || "").trim();
        if (!category && categoryKey && !/^[a-z0-9_-]+$/i.test(categoryKey)) {
          category = categoryKey;
        }
        if (category) {
          tags.push(category);
        }
      }
      const uniqueTags = Array.from(new Set(visibleTags.length ? visibleTags : tags)).slice(0, 3);
      const statusText = String(item && item.status ? item.status : "");
      const isActive = statusText === "可预约" || statusText === "active" || statusText === "";
      const price = Number(item && item.price ? item.price : 0);
      const original = Number(item && item.original ? item.original : 0);
      const commission = Number(item && item.expectedCommission ? item.expectedCommission : 0);
      const showCommission = !!(item && item.showCommission && commission > 0);
      this.setData({
        tagList: uniqueTags,
        buttonText: isActive ? "抢购" : "已下架",
        isDisabled: !isActive,
        showOriginal: original > 0 && original > price,
        showCommission,
        expectedCommission: commission.toFixed(2)
      });
    }
  },
  methods: {
    onTap(e) {
      const id = Number(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.id : 0);
      if (!Number.isFinite(id) || id <= 0) {
        return;
      }
      this.triggerEvent("tap", { id, item: this.properties.item || null });
    }
  }
});
