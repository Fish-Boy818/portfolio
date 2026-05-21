const TITLE_TAG_KEYWORDS = [
  { key: "尾波冲浪", label: "尾波冲浪" },
  { key: "小黄鸭", label: "小黄鸭" },
  { key: "摩托艇", label: "摩托艇" },
  { key: "冲浪", label: "冲浪" },
  { key: "潜水", label: "潜水" },
  { key: "桨板", label: "桨板" },
  { key: "香蕉船", label: "香蕉船" },
  { key: "拖伞", label: "拖伞" },
  { key: "帆船", label: "帆船" },
  { key: "游艇", label: "游艇" },
  { key: "滑水", label: "滑水" },
  { key: "海钓", label: "海钓" },
  { key: "自驾", label: "自驾" },
  { key: "亲子", label: "亲子" },
  { key: "团建", label: "团建" }
];
const TITLE_TAG_LABELS = TITLE_TAG_KEYWORDS.map((item) => item.label);

function extractTitleTags(title) {
  const text = String(title || "").trim();
  if (!text) {
    return [];
  }
  const hits = [];
  TITLE_TAG_KEYWORDS.forEach(({ key, label }) => {
    if (text.includes(key)) {
      hits.push(label);
    }
  });
  return Array.from(new Set(hits));
}

function normalizeTagList(list) {
  if (!Array.isArray(list)) {
    return [];
  }
  const result = [];
  list.forEach((item) => {
    const text = String(item || "").trim();
    if (text && !result.includes(text)) {
      result.push(text);
    }
  });
  return result;
}

function parseKeywords(value) {
  if (!value) {
    return [];
  }
  if (Array.isArray(value)) {
    return normalizeTagList(value);
  }
  const text = String(value || "").trim();
  if (!text) {
    return [];
  }
  const parts = text.split(/[,\s、|/]+/g);
  return normalizeTagList(parts);
}

const FALLBACK_CATEGORY_NAME_BY_KEY = {
  jetski: "摩托艇",
  surf: "冲浪",
  banana: "香蕉船",
  paddle: "桨板",
  dive: "潜水"
};

function resolveCategoryName(category) {
  if (!category) {
    return "";
  }
  const key = String(category.key || category.id || "").trim();
  const name = String(category.name || "").trim();
  if (name && !/^[a-z0-9_-]+$/i.test(name)) {
    return name;
  }
  if (name && key && name !== key) {
    return name;
  }
  if (key && FALLBACK_CATEGORY_NAME_BY_KEY[key]) {
    return FALLBACK_CATEGORY_NAME_BY_KEY[key];
  }
  return name || key || "";
}

function extractCategoryKeywords(category) {
  if (!category) {
    return [];
  }
  const keywords =
    parseKeywords(category.keywords) ||
    parseKeywords(category.labels) ||
    parseKeywords(category.tags);
  const name = resolveCategoryName(category);
  const list = keywords && keywords.length ? keywords.slice() : [];
  if (name && !list.includes(name)) {
    list.push(name);
  }
  return list;
}

function extractItemTags(item) {
  const tags = [];
  const pushTag = (value) => {
    const text = String(value || "").trim();
    if (!text || tags.includes(text)) {
      return;
    }
    tags.push(text);
  };

  extractTitleTags(item && item.title).forEach((tag) => pushTag(tag));
  parseKeywords(item && item.tags).forEach((tag) => pushTag(tag));

  const categoryKey = String((item && item.category) || "").trim();
  if (categoryKey) {
    pushTag(resolveCategoryName({ key: categoryKey }));
  }

  pushTag(item && item.categoryLabel);
  pushTag(item && item.categoryName);

  return tags;
}

function buildTagCategories(items) {
  const tagSet = new Set();
  (items || []).forEach((item) => {
    const tags = extractItemTags(item);
    tags.forEach((tag) => {
      if (tag) {
        tagSet.add(tag);
      }
    });
  });
  if (tagSet.size === 0) {
    return [];
  }
  const ordered = [];
  TITLE_TAG_LABELS.forEach((label) => {
    if (tagSet.has(label)) {
      ordered.push(label);
      tagSet.delete(label);
    }
  });
  const rest = Array.from(tagSet);
  rest.sort((a, b) => String(a).localeCompare(String(b), "zh-Hans-CN"));
  rest.forEach((tag) => ordered.push(tag));
  return ordered.map((tag) => ({ key: tag, name: tag, keywords: tag }));
}

function matchCategoryByTags(tags, category) {
  if (!Array.isArray(tags) || tags.length === 0 || !category) {
    return false;
  }
  const keywords = extractCategoryKeywords(category);
  if (!keywords.length) {
    return false;
  }
  return tags.some((tag) =>
    keywords.some((kw) => kw === tag || kw.includes(tag) || tag.includes(kw))
  );
}

function buildCategoryMap(list) {
  const map = {};
  (list || []).forEach((item) => {
    if (!item) {
      return;
    }
    const key = item.key || item.id;
    const name = item.name;
    if (key && name) {
      map[key] = name;
    }
  });
  return map;
}

function matchCategoryKeyByTags(item, categories) {
  const list = Array.isArray(categories) ? categories : [];
  if (!item) {
    return "";
  }
  const categoryKey = item.category;
  if (categoryKey && list.some((cat) => cat && cat.key === categoryKey)) {
    return categoryKey;
  }
  const combined = extractItemTags(item);
  if (!combined.length) {
    return "";
  }
  const sorted = list
    .slice()
    .sort((a, b) => String(b.name || "").length - String(a.name || "").length);
  for (const cat of sorted) {
    if (matchCategoryByTags(combined, cat)) {
      return cat.key;
    }
  }
  return "";
}

function applyCategoryLabel(item, categoryMap) {
  if (!item) {
    return item;
  }
  const key = item.category;
  const mapped = categoryMap && key ? categoryMap[key] : "";
  const label = item.categoryName || item.categoryLabel || mapped || resolveCategoryName({ key }) || "";
  return {
    ...item,
    categoryLabel: label
  };
}

function applyCategoryLabelList(list, categoryMap) {
  return (list || []).map((item) => applyCategoryLabel(item, categoryMap));
}

module.exports = {
  buildCategoryMap,
  extractTitleTags,
  extractItemTags,
  buildTagCategories,
  extractCategoryKeywords,
  resolveCategoryName,
  matchCategoryByTags,
  matchCategoryKeyByTags,
  applyCategoryLabel,
  applyCategoryLabelList
};
