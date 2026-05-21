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

const DEFAULT_BANNERS = [
  {
    id: "default-1",
    title: "海上俱乐部 · 夏季畅玩",
    subtitle: "摩托艇/冲浪/潜水一站式预订"
  },
  {
    id: "default-2",
    title: "教练陪同 · 安全保障",
    subtitle: "下单即享透明价格与安全保障"
  }
];

function syncTabBar(page, selected) {
  const app = getApp ? getApp() : null;
  if (app && typeof app.syncTabBarSelection === "function") {
    app.syncTabBarSelection(page, selected);
  }
}

function mapActivity(activity) {
  const status = activity.status === "active" ? "可预约" : "已下架";
  const subtitleRaw = sanitizeText(activity.subtitle);
  const clubName = sanitizeText(activity.clubName);
  const clubLocation = sanitizeText(activity.clubLocation);
  const isClubInfo =
    (subtitleRaw && clubName && subtitleRaw.includes(clubName)) ||
    (subtitleRaw && clubLocation && subtitleRaw.includes(clubLocation)) ||
    subtitleRaw === clubName ||
    subtitleRaw === clubLocation;
  return {
    ...activity,
    status,
    badgeClass: activity.status === "active" ? "badge--warning" : "badge--danger",
    subtitle: isClubInfo ? "" : subtitleRaw
  };
}

function bannerStyle(imageUrl, index) {
  const fallbacks = ["#2F6BFF", "#1D4ED8"];
  if (imageUrl) {
    return `background-image: url(${imageUrl}); background-color: ${fallbacks[index % fallbacks.length]};`;
  }
  return `background-color: ${fallbacks[index % fallbacks.length]};`;
}

function isVideoBanner(url) {
  const text = sanitizeText(url).toLowerCase();
  if (!text) {
    return false;
  }
  const clean = text.split("?")[0].split("#")[0];
  return clean.endsWith(".mp4") || clean.endsWith(".mov") || clean.endsWith(".m4v") || clean.endsWith(".webm");
}

function normalizeBannerMediaUrl(url) {
  const text = sanitizeText(url);
  if (!text) {
    return "";
  }
  if (text.startsWith("http://example.com")) {
    return text.replace("http://", "https://");
  }
  return text;
}

function mapBanner(banner, index) {
  const mediaUrl = normalizeBannerMediaUrl(banner.imageUrl || banner.cover || "");
  const video = isVideoBanner(mediaUrl);
  return {
    id: banner.id || `banner-${index}`,
    title: banner.title || "",
    subtitle: banner.subtitle || "",
    mediaUrl,
    isVideo: video,
    videoReady: !video,
    style: bannerStyle(video ? "" : mediaUrl, index)
  };
}

Page({
  _bannerPlayTimers: null,
  _bannerAdvanceTimer: null,
  _bannerReadyTimers: null,
  _itemTapAt: 0,
  _clubTapAt: 0,
  _lastLoadAt: 0,
  data: {
    keyword: "",
    status: "loading",
    errorMessage: "网络异常，请稍后重试",
    items: [],
    clubs: [],
    banners: [],
    bannerInterval: 2500,
    bannerCurrent: 0,
    bannerMuted: true,
    swiperAutoplay: true,
    simulateError: false,
    userId: null
  },
  onLoad() {
    this._bannerPlayTimers = [];
    this._bannerAdvanceTimer = null;
    this._bannerReadyTimers = {};
    this._lastLoadAt = 0;
    this.loadData({ forceLoading: true });
  },
  onShow() {
    syncTabBar(this, 0);
    if (this.data.status === "loading") {
      return;
    }
    this.schedulePlayCurrentBannerVideo();
    if (this.shouldRefreshOnShow()) {
      this.loadData({ silent: true, preserveBanner: true });
    }
  },
  onHide() {
    this.clearBannerPlayTimers();
    this.clearBannerAdvanceTimer();
    this.clearBannerReadyTimers();
    this.pauseAllBannerVideos();
  },
  onUnload() {
    this.clearBannerPlayTimers();
    this.clearBannerAdvanceTimer();
    this.clearBannerReadyTimers();
    this.pauseAllBannerVideos();
  },
  loadData(options = {}) {
    const forceLoading = !!options.forceLoading;
    const silent = !!options.silent;
    const preserveBanner = !!options.preserveBanner;
    if (forceLoading || (!silent && this.data.status !== "ready" && this.data.status !== "empty")) {
      this.setData({ status: "loading" });
    }
    const previousBanners = Array.isArray(this.data.banners) ? this.data.banners : [];
    const previousCurrent = Number(this.data.bannerCurrent || 0);
    ensureAuth()
      .catch(() => null)
      .then((user) => {
        const userId = user ? user.id : null;
        return Promise.all([
          request({ url: "/api/categories" }),
          request({ url: "/api/clubs" }),
          request({ url: "/api/activities", data: { userId } }),
          request({ url: "/api/banners" }).catch(() => [])
        ]).then(([categories, clubs, activities, banners]) => {
          const categoryMap = buildCategoryMap(categories);
          const app = getApp ? getApp() : null;
          if (app && app.globalData) {
            app.globalData.categoryMap = categoryMap;
          }
          const list = applyCategoryLabelList((activities || []).slice(0, 3).map(mapActivity), categoryMap);
          const bannerList = this.mergeBannerState(
            previousBanners,
            (banners && banners.length ? banners : DEFAULT_BANNERS).map(mapBanner)
          );
          const bannerCurrent = preserveBanner
            ? this.resolveNextBannerCurrent(previousBanners, bannerList, previousCurrent)
            : 0;
          this.setData({
            userId,
            items: list,
            clubs: (clubs || []).slice(0, 10),
            banners: bannerList,
            bannerCurrent,
            swiperAutoplay: !this.isVideoAt(bannerCurrent, bannerList),
            status: list.length ? "ready" : "empty"
          });
          this._lastLoadAt = Date.now();
          this.primeBannerReadyFallbacks(bannerList);
          this.schedulePlayCurrentBannerVideo();
        });
      })
      .catch((err) => {
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
      });
  },
  onInput(e) {
    this.setData({ keyword: e.detail.value });
  },
  onSearch() {
    const keyword = this.data.keyword.trim();
    if (!keyword) {
      wx.showToast({ title: "请输入关键词", icon: "none" });
      return;
    }
    wx.navigateTo({ url: `/pages/list/index?keyword=${encodeURIComponent(keyword)}` });
  },
  onClear() {
    this.setData({ keyword: "" });
  },
  onGoList(e) {
    const category = e.currentTarget.dataset.category || "";
    const url = category ? `/pages/list/index?category=${category}` : "/pages/list/index";
    wx.navigateTo({ url });
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
  onBannerChange(e) {
    const current = Number(e && e.detail ? e.detail.current : 0) || 0;
    this.clearBannerAdvanceTimer();
    const nextAutoplay = !this.isVideoAt(current);
    if (current !== Number(this.data.bannerCurrent) || nextAutoplay !== this.data.swiperAutoplay) {
      this.setData({
        bannerCurrent: current,
        swiperAutoplay: nextAutoplay
      });
    }
    this.startBannerReadyFallback(current, 1200);
    this.schedulePlayCurrentBannerVideo();
  },
  onToggleBannerSound() {
    const nextMuted = !this.data.bannerMuted;
    this.setData({ bannerMuted: nextMuted }, () => {
      this.playCurrentBannerVideo();
      wx.showToast({
        title: nextMuted ? "已静音" : "已开启声音",
        icon: "none"
      });
    });
  },
  onBannerVideoLoaded(e) {
    const index = Number(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.index : -1);
    if (index >= 0) {
      this.startBannerReadyFallback(index);
    }
    if (index === Number(this.data.bannerCurrent)) {
      this.schedulePlayCurrentBannerVideo();
    }
  },
  onBannerVideoCanPlay(e) {
    const index = Number(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.index : -1);
    if (index < 0) {
      return;
    }
    const banners = this.data.banners || [];
    if (!banners[index] || !banners[index].isVideo || banners[index].videoReady) {
      return;
    }
    this.clearBannerReadyTimer(index);
    this.setData({
      [`banners[${index}].videoReady`]: true
    });
    if (index === Number(this.data.bannerCurrent)) {
      this.schedulePlayCurrentBannerVideo();
    }
  },
  onBannerVideoError(e) {
    const index = Number(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.index : -1);
    if (index < 0) {
      return;
    }
    // Do not keep loading mask forever when a video source fails.
    this.clearBannerReadyTimer(index);
    this.setData({
      [`banners[${index}].videoReady`]: true
    });
  },
  onBannerVideoEnded(e) {
    const index = Number(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.index : -1);
    if (index !== Number(this.data.bannerCurrent)) {
      return;
    }
    this.clearBannerAdvanceTimer();
    this._bannerAdvanceTimer = setTimeout(() => {
      const banners = this.data.banners || [];
      const size = banners.length;
      if (!size) {
        return;
      }
      const next = (index + 1) % size;
      this.setData({
        bannerCurrent: next,
        swiperAutoplay: !this.isVideoAt(next)
      });
      this.schedulePlayCurrentBannerVideo();
    }, 120);
  },
  shouldRefreshOnShow() {
    const lastLoadAt = Number(this._lastLoadAt || 0);
    if (!lastLoadAt) {
      return true;
    }
    return Date.now() - lastLoadAt > 30000;
  },
  mergeBannerState(previousBanners, nextBanners) {
    const readyMap = {};
    (previousBanners || []).forEach((item) => {
      if (item && item.id) {
        readyMap[item.id] = !!item.videoReady;
      }
    });
    return (nextBanners || []).map((item) => ({
      ...item,
      videoReady: item && item.isVideo ? !!readyMap[item.id] : true
    }));
  },
  resolveNextBannerCurrent(previousBanners, nextBanners, previousCurrent) {
    if (!Array.isArray(nextBanners) || !nextBanners.length) {
      return 0;
    }
    const safePreviousCurrent = Number(previousCurrent || 0);
    const previous = Array.isArray(previousBanners) ? previousBanners[safePreviousCurrent] : null;
    const previousId = previous && previous.id ? previous.id : null;
    if (!previousId) {
      return 0;
    }
    const nextIndex = nextBanners.findIndex((item) => item && item.id === previousId);
    return nextIndex >= 0 ? nextIndex : 0;
  },
  isVideoAt(index, list) {
    const banners = Array.isArray(list) ? list : this.data.banners || [];
    const item = banners[index];
    return !!(item && item.isVideo);
  },
  clearBannerPlayTimers() {
    const timers = this._bannerPlayTimers || [];
    timers.forEach((timer) => clearTimeout(timer));
    this._bannerPlayTimers = [];
  },
  clearBannerAdvanceTimer() {
    if (this._bannerAdvanceTimer) {
      clearTimeout(this._bannerAdvanceTimer);
      this._bannerAdvanceTimer = null;
    }
  },
  clearBannerReadyTimer(index) {
    const timers = this._bannerReadyTimers || {};
    if (timers[index]) {
      clearTimeout(timers[index]);
      delete timers[index];
    }
  },
  clearBannerReadyTimers() {
    const timers = this._bannerReadyTimers || {};
    Object.keys(timers).forEach((key) => {
      clearTimeout(timers[key]);
      delete timers[key];
    });
    this._bannerReadyTimers = {};
  },
  startBannerReadyFallback(index, delayMs) {
    const delay = Number(delayMs) > 0 ? Number(delayMs) : 900;
    this.clearBannerReadyTimer(index);
    this._bannerReadyTimers[index] = setTimeout(() => {
      const banners = this.data.banners || [];
      if (!banners[index] || !banners[index].isVideo || banners[index].videoReady) {
        return;
      }
      this.setData({
        [`banners[${index}].videoReady`]: true
      });
      if (index === Number(this.data.bannerCurrent)) {
        this.schedulePlayCurrentBannerVideo();
      }
    }, delay);
  },
  primeBannerReadyFallbacks(list) {
    const banners = Array.isArray(list) ? list : this.data.banners || [];
    banners.forEach((item, index) => {
      if (item && item.isVideo) {
        this.startBannerReadyFallback(index, 1200);
      }
    });
  },
  schedulePlayCurrentBannerVideo() {
    this.clearBannerPlayTimers();
    [180].forEach((delay) => {
      const timer = setTimeout(() => this.playCurrentBannerVideo(), delay);
      this._bannerPlayTimers.push(timer);
    });
  },
  playCurrentBannerVideo() {
    const banners = this.data.banners || [];
    const current = Number(this.data.bannerCurrent || 0);
    banners.forEach((banner, index) => {
      if (!banner || !banner.isVideo) {
        return;
      }
      const ctx = wx.createVideoContext(`banner-video-${index}`, this);
      if (!ctx) {
        return;
      }
      if (index === current) {
        if (!banner.videoReady) {
          this.startBannerReadyFallback(index, 1200);
        }
        ctx.play();
      } else {
        ctx.pause();
      }
    });
  },
  pauseAllBannerVideos() {
    const banners = this.data.banners || [];
    banners.forEach((banner, index) => {
      if (!banner || !banner.isVideo) {
        return;
      }
      const ctx = wx.createVideoContext(`banner-video-${index}`, this);
      if (ctx) {
        ctx.pause();
      }
    });
  },
  onRetry() {
    this.loadData();
  }
});
