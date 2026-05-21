const { request } = require("../../utils/api");
const { ensureAuth } = require("../../utils/auth");
const { getProductPrefetch, cacheProductPrefetch } = require("../../utils/prefetch");

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

function toNumber(value) {
  const num = Number(value);
  if (!Number.isFinite(num) || num < 0) {
    return 0;
  }
  return num;
}

function isActiveStatus(value) {
  const status = String(value || "").trim();
  return status === "" || status === "active" || status === "可预约";
}

function formatDate(dateStr) {
  if (!dateStr) {
    return "";
  }
  return dateStr.split("T")[0];
}

function parseDescItems(desc) {
  const text = sanitizeText(desc);
  if (!text) {
    return [];
  }
  return text
    .split("|")
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => {
      const pieces = part.split(/[:：]/);
      if (pieces.length > 1) {
        return {
          label: pieces[0].trim(),
          value: pieces.slice(1).join(":").trim()
        };
      }
      return { label: part, value: "" };
    });
}

function cleanLine(value) {
  return value.replace(/^[\s•\-]+|[\s]+$/g, "");
}

function splitTitleNote(text) {
  const match = text.match(/（[^）]+）/);
  if (!match) {
    return { title: text, note: "" };
  }
  const note = match[0];
  const title = text.replace(note, "").trim();
  return { title, note };
}

function normalizePriceText(value) {
  const text = sanitizeText(value);
  if (!text) {
    return "";
  }
  const match = text.replace(/,/g, "").match(/(\d+(?:\.\d+)?)/);
  if (!match) {
    return "";
  }
  return `¥${match[1]}`;
}

function parseBundleSection(bundle) {
  const text = sanitizeText(bundle);
  if (!text) {
    return { title: "", note: "", items: [] };
  }
  const parts = text
    .split(/[|\n]/)
    .map((part) => cleanLine(part.trim()))
    .filter(Boolean);

  let title = "";
  let note = "";
  let itemParts = parts;
  const isQtyLine = (line) => /^\d+\s*份$/.test(line);
  const isPriceLine = (line) => /[¥￥]\s*\d+|\d+(?:\.\d+)?\s*元/.test(line);
  if (parts.length >= 4 && !isQtyLine(parts[0]) && !isPriceLine(parts[0])) {
    const titleParts = splitTitleNote(parts[0]);
    title = titleParts.title;
    note = titleParts.note;
    itemParts = parts.slice(1);
  }

  const items = [];
  if (itemParts.length >= 3 && itemParts.length % 3 === 0) {
    for (let i = 0; i < itemParts.length; i += 3) {
      items.push({
        name: itemParts[i],
        qty: itemParts[i + 1],
        price: normalizePriceText(itemParts[i + 2])
      });
    }
    return { title, note, items };
  }

  itemParts.forEach((line) => {
    let name = line;
    const qtyMatch = line.match(/(\d+\s*份)/);
    const priceMatch = line.match(/[¥￥]?\s*(\d+(?:\.\d+)?)\s*元?/);
    if (qtyMatch) {
      name = name.replace(qtyMatch[0], "");
    }
    if (priceMatch) {
      name = name.replace(priceMatch[0], "");
    }
    name = cleanLine(name.replace(/^[-•\s]+/, "").trim());
    items.push({
      name: name || line,
      qty: qtyMatch ? qtyMatch[0].replace(/\s+/g, "") : "",
      price: priceMatch ? normalizePriceText(priceMatch[0]) : ""
    });
  });

  return { title, note, items };
}

function normalizeTag(value) {
  const text = sanitizeText(value);
  if (!text) {
    return "";
  }
  if (/^[a-z0-9_-]+$/i.test(text)) {
    return "";
  }
  return text;
}

function parseImageList(value) {
  if (Array.isArray(value)) {
    return value
      .map((item) => sanitizeText(item))
      .filter(Boolean);
  }
  const text = sanitizeText(value);
  if (!text) {
    return [];
  }
  if (text.startsWith("[") && text.endsWith("]")) {
    try {
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) {
        return parsed
          .map((item) => sanitizeText(item))
          .filter(Boolean);
      }
    } catch (e) {
      // fallback to split text
    }
  }
  return text
    .split(/[|,]/)
    .map((item) => sanitizeText(item))
    .filter(Boolean);
}

function buildGallery(activity) {
  const primary = parseImageList(activity && activity.gallery);
  const fallback = [
    sanitizeText(activity && activity.cover),
    sanitizeText(activity && activity.imageUrl),
    sanitizeText(activity && activity.thumbUrl),
    sanitizeText(activity && activity.thumbnail)
  ].filter(Boolean);
  return Array.from(new Set(primary.concat(fallback)));
}

function buildDetailImages(activity, gallery) {
  const detail = parseImageList(activity && activity.detailImages);
  if (detail.length) {
    return detail;
  }
  return (gallery || []).slice();
}

function mapProduct(activity) {
  const active = isActiveStatus(activity && activity.status);
  const statusText = active ? "可预约" : "已下架";
  const badgeClass = active ? "badge--warning" : "badge--danger";
  const expire = formatDate(activity.expireDate) || "长期有效";
  const gallery = buildGallery(activity);
  const price = toNumber(activity.price || activity.basePrice);
  const original = toNumber(activity.original || activity.originalPrice);
  const expectedCommission = toNumber(
    activity.expectedCommission || activity.buyerCommissionAmount || activity.commissionAmount
  );
  const showCommission = !!(activity.showCommission || expectedCommission > 0);
  const commissionTipText = sanitizeText(activity.commissionTipText);
  const tags = [];
  const categoryTag = normalizeTag(activity.categoryLabel || activity.categoryName || activity.category);
  const audienceTag = normalizeTag(activity.audience);
  if (categoryTag) {
    tags.push(categoryTag);
  }
  if (audienceTag && !tags.includes(audienceTag)) {
    tags.push(audienceTag);
  }
  return {
    ...activity,
    status: statusText,
    badgeClass,
    store: sanitizeText(activity.clubLocation) || sanitizeText(activity.clubName) || "",
    times: "不限时间",
    tags,
    audience: sanitizeText(activity.audience),
    desc: sanitizeText(activity.description),
    expire,
    bundle: sanitizeText(activity.bundle),
    gallery,
    detailImages: buildDetailImages(activity, gallery),
    price,
    original,
    showCommission,
    expectedCommission: expectedCommission.toFixed(2),
    commissionTipText
  };
}

function buildShareTitle(product) {
  const title = sanitizeText(product && product.title);
  if (title) {
    return title;
  }
  return "活动详情";
}

function buildShareImage(product) {
  const gallery = Array.isArray(product && product.gallery) ? product.gallery : [];
  return sanitizeText((product && product.cover) || gallery[0] || "");
}

Page({
  _loadSeq: 0,
  _retryTimer: null,
  data: {
    productId: 1,
    status: "loading",
    errorMessage: "商品信息加载失败",
    product: null,
    stockText: "",
    ctaDisabled: false,
    ctaText: "",
    descItems: [],
    bundleItems: [],
    bundleTitle: "",
    bundleNote: "",
    userId: null
  },
  onLoad(options) {
    const id = toValidId(options && options.id);
    if (!id) {
      this.setData({
        status: "error",
        errorMessage: "活动参数异常，请返回重试"
      });
      return;
    }
    this.enableShareMenu();
    this._loadSeq = 0;
    if (this._retryTimer) {
      clearTimeout(this._retryTimer);
      this._retryTimer = null;
    }
    this.setData({ productId: id }, () => {
      const prefetched = getProductPrefetch(id);
      if (prefetched) {
        this.applyProductState(prefetched, this.data.userId);
        this.loadData({ silent: true });
        return;
      }
      this.loadData();
    });
  },
  onShow() {
    if (this.data.status === "loading") {
      return;
    }
    this.loadData({ silent: !!this.data.product });
  },
  onUnload() {
    if (this._retryTimer) {
      clearTimeout(this._retryTimer);
      this._retryTimer = null;
    }
  },
  enableShareMenu() {
    if (typeof wx.showShareMenu !== "function") {
      return;
    }
    try {
      wx.showShareMenu({
        withShareTicket: true,
        menus: ["shareAppMessage", "shareTimeline"]
      });
    } catch (err) {
      try {
        wx.showShareMenu({ withShareTicket: true });
      } catch (ignored) {
        // ignore unsupported versions
      }
    }
  },
  resolveShareInviterId() {
    const userId = toValidId(this.data.userId);
    if (!userId) {
      return 0;
    }
    const product = this.data.product || {};
    return product.showCommission ? userId : 0;
  },
  buildShareInfo() {
    const productId = toValidId(this.data.productId);
    const inviterId = this.resolveShareInviterId();
    const query = [`id=${productId}`];
    if (inviterId) {
      query.push(`inviterId=${inviterId}`);
      query.push("source=share");
    }
    const product = this.data.product || {};
    return {
      title: buildShareTitle(product),
      path: `/pages/detail/index?${query.join("&")}`,
      query: query.join("&"),
      imageUrl: buildShareImage(product)
    };
  },
  applyProductState(activity, userId) {
    const source = activity || {};
    const product = mapProduct(source);
    const stockText = "默认可用";
    const ctaDisabled = !isActiveStatus(source.status);
    const ctaText = ctaDisabled ? "已下架" : "立即下单";
    const descItems = parseDescItems(product.desc);
    const bundleSection = parseBundleSection(product.bundle);
    const hasBundle = bundleSection.items.length > 0 || bundleSection.title;
    const bundleTitle = hasBundle ? bundleSection.title || "商品搭配" : "";
    const bundleNote = bundleSection.note || "";
    this.setData({
      product,
      stockText,
      ctaDisabled,
      ctaText,
      descItems,
      bundleItems: bundleSection.items,
      bundleTitle,
      bundleNote,
      status: "ready",
      userId
    });
  },
  loadData(options) {
    const attempt = Number(options && options.attempt ? options.attempt : 1);
    const silent = !!(options && options.silent);
    const seq = (this._loadSeq || 0) + 1;
    this._loadSeq = seq;
    if (this._retryTimer) {
      clearTimeout(this._retryTimer);
      this._retryTimer = null;
    }
    const hasCurrentProduct = !!this.data.product;
    if (!(silent && hasCurrentProduct)) {
      this.setData({ status: "loading" });
    }
    let resolvedUserId = this.data.userId;
    ensureAuth()
      .catch(() => null)
      .then((user) => {
        resolvedUserId = user ? user.id : null;
        return request({ url: `/api/activities/${this.data.productId}`, data: { userId: resolvedUserId } })
          .then((activity) => {
            if (seq !== this._loadSeq) {
              return;
            }
            if (!activity) {
              this.setData({ status: "empty" });
              return;
            }
            cacheProductPrefetch(activity);
            this.applyProductState(activity, resolvedUserId);
          });
      })
      .catch((err) => {
        if (seq !== this._loadSeq) {
          return;
        }
        if (attempt < 2) {
          this._retryTimer = setTimeout(() => {
            this.loadData({ attempt: attempt + 1, silent: silent || !!this.data.product });
          }, 280);
          return;
        }
        if (this.data.product) {
          this.setData({
            status: "ready",
            userId: resolvedUserId
          });
          return;
        }
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
      });
  },
  onOrder() {
    if (this.data.ctaDisabled) {
      wx.showToast({ title: "活动已下架", icon: "none" });
      return;
    }
    const id = toValidId(this.data.productId);
    if (!id) {
      return;
    }
    wx.navigateTo({ url: `/pages/order-confirm/index?id=${id}` });
  },
  onRetry() {
    this.loadData();
  },
  onGoHome() {
    wx.switchTab({ url: "/pages/home/index" });
  },
  onShareAppMessage() {
    return this.buildShareInfo();
  },
  onShareTimeline() {
    const shareInfo = this.buildShareInfo();
    return {
      title: shareInfo.title,
      query: shareInfo.query,
      imageUrl: shareInfo.imageUrl
    };
  }
});
