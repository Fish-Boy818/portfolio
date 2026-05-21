const { request } = require("../../utils/api");
const { ensureAuth } = require("../../utils/auth");
const { buildCategoryMap, applyCategoryLabelList } = require("../../utils/category");
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

function safeDecodeURIComponent(value) {
  try {
    return decodeURIComponent(value);
  } catch (err) {
    return value;
  }
}

function trimTrailingUrlChars(value) {
  return String(value || "").replace(/[)\]}>.,;!?]+$/g, "");
}

function normalizeHttpUrl(value) {
  const text = sanitizeText(value);
  if (!text) {
    return "";
  }
  const decoded = sanitizeText(safeDecodeURIComponent(text));
  const matched = decoded.match(/https?:\/\/[^\s"'<>]+/i);
  let candidate = sanitizeText(matched ? matched[0] : decoded);
  if (!candidate) {
    return "";
  }
  candidate = trimTrailingUrlChars(candidate);
  if (!/^https?:\/\//i.test(candidate)) {
    candidate = `https://${candidate}`;
  }

  const shortDouyinMatch = candidate.match(/^https?:\/\/v\.douyin\.com\/[A-Za-z0-9_-]+\/?/i);
  if (shortDouyinMatch) {
    return shortDouyinMatch[0];
  }

  return candidate.replace(/\s+/g, "");
}

Page({
  _itemTapAt: 0,
  data: {
    clubId: 1,
    status: "loading",
    errorMessage: "俱乐部信息加载失败",
    club: null,
    activities: [],
    userId: null
  },
  onLoad(options) {
    const id = toValidId(options && options.id) || 1;
    this.setData({ clubId: id }, () => {
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
        return Promise.all([
          request({ url: "/api/categories" }),
          request({ url: `/api/clubs/${this.data.clubId}` }),
          request({ url: "/api/activities", data: { clubId: this.data.clubId, userId } })
        ]).then(([categories, club, activities]) => {
          if (!club) {
            this.setData({ status: "empty" });
            return;
          }
          const categoryMap = buildCategoryMap(categories);
          const app = getApp ? getApp() : null;
          if (app && app.globalData) {
            app.globalData.categoryMap = categoryMap;
          }
          const rawTags = Array.isArray(club.tags) ? club.tags : [];
          const tags = rawTags.map(sanitizeText).filter(Boolean);
          const tagsText = tags.length ? tags.join(" / ") : "";
          const openTimeText =
            sanitizeText(club.openTime) ||
            sanitizeText(club.open_time) ||
            sanitizeText(club.openingHours) ||
            "";
          const safeClub = {
            ...club,
            name: sanitizeText(club.name),
            location: sanitizeText(club.location),
            address: sanitizeText(club.address),
            phone: sanitizeText(club.phone),
            douyinUrl: normalizeHttpUrl(club.douyinUrl || club.douyin_url || ""),
            tags
          };
          const mapped = applyCategoryLabelList(
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
          this.setData({
            club: {
              ...safeClub,
              tagsText,
              openTimeText
            },
            activities: mapped,
            status: "ready",
            userId
          });
        });
      })
      .catch((err) => {
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
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
  onGoList() {
    const clubId = this.data.clubId;
    wx.navigateTo({ url: `/pages/list/index?clubId=${clubId}` });
  },
  onRetry() {
    this.loadData();
  },
  onGoHome() {
    wx.switchTab({ url: "/pages/home/index" });
  },
  onGoDouyinHome() {
    const club = this.data.club || {};
    const douyinUrl = normalizeHttpUrl(club.douyinUrl);
    if (!douyinUrl) {
      wx.showToast({ title: "未配置抖音主页链接", icon: "none" });
      return;
    }
    wx.navigateTo({
      url: `/pages/webview/index?title=${encodeURIComponent("俱乐部抖音主页")}&url=${encodeURIComponent(douyinUrl)}`
    });
  }
});
