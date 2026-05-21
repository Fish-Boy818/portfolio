const { createApp } = Vue;

function parseList(value) {
  if (!value) return [];
  return String(value)
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function formatList(value) {
  if (!value) return "";
  if (Array.isArray(value)) return value.join(", ");
  return String(value);
}

function formatDate(value) {
  if (!value) return "";
  return String(value).replace("T", " ").replace(/\.\d+$/, "").replace(/-/g, ".");
}

function parseDateTimeValue(value) {
  if (!value) {
    return null;
  }
  const raw = String(value)
    .trim()
    .replace("T", " ")
    .replace(/\.\d+$/, "")
    .replace(/Z$/, "");
  if (!raw) {
    return null;
  }
  const direct = new Date(raw.replace(/-/g, "/"));
  if (!Number.isNaN(direct.getTime())) {
    return direct;
  }
  const matched = raw.match(/(\d{4})[./-](\d{1,2})[./-](\d{1,2})(?:\s+(\d{1,2}):(\d{1,2}))?/);
  if (!matched) {
    return null;
  }
  const year = Number(matched[1]);
  const month = Number(matched[2]) - 1;
  const day = Number(matched[3]);
  const hour = Number(matched[4] || 0);
  const minute = Number(matched[5] || 0);
  const date = new Date(year, month, day, hour, minute, 0);
  return Number.isNaN(date.getTime()) ? null : date;
}

function isWithinRecentDays(value, days) {
  const date = parseDateTimeValue(value);
  if (!date) {
    return false;
  }
  const now = new Date();
  const min = new Date(now.getTime() - Math.max(1, Number(days) || 1) * 24 * 60 * 60 * 1000);
  return date.getTime() >= min.getTime() && date.getTime() <= now.getTime();
}

function normalizeSearchText(value) {
  if (!value) return "";
  return String(value)
    .toLowerCase()
    .replace(/\s+/g, "")
    .replace(/[，。！？：；、（）()【】[\]「」『』《》“”‘’·—–\-~…、]/g, "");
}

function normalizePhoneDigits(value) {
  if (!value) return "";
  const halfWidth = String(value).replace(/[０-９]/g, (ch) =>
    String.fromCharCode(ch.charCodeAt(0) - 65248)
  );
  return halfWidth.replace(/\D+/g, "");
}

function normalizeHeaderKey(value) {
  return String(value || "")
    .trim()
    .toLowerCase()
    .replace(/[\s（）()]/g, "");
}

const DY_PRODUCT_IMPORT_MAP = (() => {
  const mapping = {
    商品名称: "title",
    商品id: "subtitle",
    商家平台券id: "subtitle",
    售价: "price",
    商品总价商品划线价: "originalPrice",
    商促价: "promoPrice",
    商品状态: "statusText",
    商品归属户: "ownerAccount",
    归属户名称: "ownerName",
    归属户: "ownerName",
    归属账户: "ownerName",
    归属账户名称: "ownerName",
    商品类型: "audience",
    可用日期: "usableDates",
    不可用日期: "unusableDates",
    每日可用时段: "usableTimes",
    投放渠道: "channel",
    售卖结束时间: "expireDate",
    售卖开始时间: "startDate"
  };
  const map = {};
  Object.keys(mapping).forEach((key) => {
    map[normalizeHeaderKey(key)] = mapping[key];
  });
  return map;
})();

function parseNumber(value) {
  if (value === null || value === undefined) return null;
  const cleaned = String(value).replace(/[^0-9.\-]/g, "");
  if (!cleaned) return null;
  const number = Number(cleaned);
  return Number.isFinite(number) ? number : null;
}

function parseDateOnly(value) {
  if (!value) return "";
  const text = String(value).trim();
  if (!text) return "";
  const isoMatch = text.match(/\d{4}-\d{1,2}-\d{1,2}/);
  if (isoMatch) {
    const parts = isoMatch[0].split("-");
    return `${parts[0]}-${parts[1].padStart(2, "0")}-${parts[2].padStart(2, "0")}`;
  }
  const slashMatch = text.match(/\d{4}[/.]\d{1,2}[/.]\d{1,2}/);
  if (slashMatch) {
    const parts = slashMatch[0].split(/[/.]/);
    return `${parts[0]}-${parts[1].padStart(2, "0")}-${parts[2].padStart(2, "0")}`;
  }
  return "";
}

function isVideoFileLike(file) {
  if (!file) return false;
  const type = String(file.type || "").toLowerCase();
  if (type.startsWith("video/")) {
    return true;
  }
  const name = String(file.name || "").toLowerCase();
  return (
    name.endsWith(".mp4") ||
    name.endsWith(".mov") ||
    name.endsWith(".m4v") ||
    name.endsWith(".webm") ||
    name.endsWith(".avi") ||
    name.endsWith(".mkv")
  );
}

const CATEGORY_NAME_FALLBACK = {
  jetski: "摩托艇",
  surf: "冲浪",
  banana: "香蕉船",
  paddle: "桨板",
  dive: "潜水"
};
const CATEGORY_KEY_SUGGESTIONS = Object.keys(CATEGORY_NAME_FALLBACK).reduce((map, key) => {
  map[CATEGORY_NAME_FALLBACK[key]] = key;
  return map;
}, {});

function resolveCategoryName(item) {
  if (!item) return "";
  const key = String(item.key || item.value || item.id || "").trim();
  const name = String(item.name || item.label || "").trim();
  if (name && !/^[a-z0-9_-]+$/i.test(name)) {
    return name;
  }
  if (name && key && name !== key) {
    return name;
  }
  if (key && CATEGORY_NAME_FALLBACK[key]) {
    return CATEGORY_NAME_FALLBACK[key];
  }
  return name || key || "";
}

function suggestCategoryKey(value) {
  const text = String(value || "").trim();
  if (!text) {
    return "";
  }
  if (/^[a-z0-9_-]+$/i.test(text)) {
    return text.toLowerCase();
  }
  if (CATEGORY_KEY_SUGGESTIONS[text]) {
    return CATEGORY_KEY_SUGGESTIONS[text];
  }
  return "";
}

const ADMIN_FRAMEWORK_STATE_KEY = "untitled_admin_framework_state_v1";
const ADMIN_UI_PREFS_KEY = "untitled_admin_ui_prefs_v1";
const ADMIN_TAB_GROUPS = [
  {
    key: "operate",
    label: "运营内容",
    tabs: ["banners", "clubs", "activities", "bounties", "categories", "profileContent"]
  },
  {
    key: "trade",
    label: "交易履约",
    tabs: [
      "orders",
      "withdrawals",
      "commissions",
      "welfareSubmissions"
    ]
  },
  {
    key: "member",
    label: "用户增长",
    tabs: ["users"]
  },
  {
    key: "system",
    label: "系统设置",
    tabs: ["adminAccount"]
  }
];
const ADMIN_QUICK_TAB_KEYS = ["activities", "orders", "clubs", "users", "profileContent", "banners"];
const ADMIN_PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const ADMIN_DEFAULT_PAGE_SIZE = 20;
const ADMIN_MODULE_GUIDES = {
  default: {
    title: "按当前模块整理数据",
    description: "先看当前列表，再用搜索和筛选缩小范围，确认无误后再新增或修改。",
    goal: "先把基础资料整理干净，再处理新增、修改和导出。",
    tableHint: "支持直接搜索名称、手机号、订单号、地址等关键信息。",
    emptyTitle: "当前模块还没有数据",
    emptyDescription: "可以先新增一条数据，后续再批量补充。",
    formTip: "带 * 的字段必须填写，其余内容可以稍后补充。",
    checklist: [
      "先确认当前模块是否正确",
      "再用搜索和筛选定位数据",
      "保存前检查关键字段是否填写完整"
    ],
    actions: [
      { type: "refresh", label: "刷新数据", style: "secondary" }
    ]
  },
  clubs: {
    title: "先录俱乐部资料，再继续建活动",
    description: "俱乐部是后续活动、订单和小程序展示的基础。什么都不懂时，先把名称、位置、地址、营业时间和封面补齐。",
    goal: "先把门店基础资料补完整，后面活动和客服处理会轻松很多。",
    tableHint: "推荐优先检查封面、位置、营业时间是否为空，这些最影响展示效果。",
    emptyTitle: "先创建第一个俱乐部",
    emptyDescription: "俱乐部是活动和订单的归属主体，建议先录入基础资料。",
    formTip: "新建俱乐部时，优先填名称、位置、地址和封面图；抖音门店文案也可以直接粘贴识别。",
    checklist: [
      "先填俱乐部名称和所在位置",
      "再补地址、电话、营业时间",
      "最后上传封面和图集，避免小程序空白图"
    ],
    actions: [
      { type: "create", label: "新增俱乐部", style: "primary" },
      { type: "import", label: "批量导入", style: "secondary" },
      { type: "switchTab", tabKey: "activities", label: "去建活动", style: "secondary" }
    ]
  },
  activities: {
    title: "活动价格和分佣要按顺序填写",
    description: "先确定俱乐部和售价，再填写平台留存、用户1总返佣，以及用户2/3自购返佣。",
    goal: "先保证价格结构正确，再决定是否上架和分佣。",
    tableHint: "建议用俱乐部筛选查看活动，再按分组模式检查每家店的商品是否齐全。",
    emptyTitle: "还没有活动",
    emptyDescription: "先建好俱乐部，再给每家俱乐部录入活动和详情图。",
    formTip: "按“售价 -> 平台留存 -> 用户1总返佣 -> 用户2自购返佣 -> 用户3自购返佣”的顺序填写最稳妥。",
    checklist: [
      "先选择所属俱乐部和活动标题",
      "再填价格、平台留存和分层返佣",
      "最后上传封面、图集和详情图"
    ],
    actions: [
      { type: "create", label: "新增活动", style: "primary" },
      { type: "import", label: "批量导入", style: "secondary" },
      { type: "toggleActivityGroup", label: "切换分组视图", style: "secondary" }
    ]
  },
  bounties: {
      title: "这里维护悬赏任务入口",
      description: "悬赏负责把任务讲清楚，也负责让用户进入提交结果流程。先把地点、佣金区间和步骤说明配完整，用户才知道怎么接、怎么交。",
      goal: "先把悬赏卡片配完整，再检查接取后是否能顺利进入提交结果页。",
      tableHint: "建议优先检查封面、详情头图、佣金区间和三步说明是否完整易懂。",
    emptyTitle: "还没有悬赏任务",
    emptyDescription: "新增悬赏后，小程序底部“悬赏”页才会展示任务卡片。",
    formTip: "先填地点、佣金区间和封面图，再补步骤 1/2/3 的文案或图片。",
    checklist: [
      "先上传列表封面和详情头图",
      "再填地点和预计佣金区间",
      "最后补充步骤1、2、3的说明或图片"
    ],
    actions: [
      { type: "create", label: "新增悬赏", style: "primary" },
      { type: "refresh", label: "刷新列表", style: "secondary" }
    ]
  },
  orders: {
    title: "订单模块主要做巡检和售后",
    description: "重点关注待支付、已支付未核销和退款订单。常用操作是搜索手机号、筛近7天、导出Excel。",
    goal: "每天先看待处理订单，再处理退款和核销问题。",
    tableHint: "支持搜订单号、手机号和活动名。建议先看近 7 天，再切到全部时间。",
    emptyTitle: "当前没有符合条件的订单",
    emptyDescription: "可以先放宽时间筛选或清除状态筛选查看全部订单。",
    formTip: "订单通常不需要手动新增；更多时候是查看状态、核销、退款和导出记录。",
    checklist: [
      "先看近7天订单是否有异常",
      "再处理已支付未核销和退款订单",
      "最后按需导出对账数据"
    ],
    actions: [
      { type: "setOrderDateFilter", value: "week", label: "看近7天订单", style: "primary" },
      { type: "setOrderFilter", value: "paid", label: "看待核销订单", style: "secondary" },
      { type: "exportOrders", label: "导出Excel", style: "secondary" }
    ]
  },
  users: {
    title: "用户模块主要用于查人和看层级",
    description: "这里适合按手机号、昵称或 OpenID 搜索用户，查看邀请层级和注册时间。",
    goal: "先搜手机号定位用户，再看是否为扫码用户和邀请层级。",
    tableHint: "手机号搜索支持模糊匹配，输入后 3 位以上就能快速缩小范围。",
    emptyTitle: "还没有用户数据",
    emptyDescription: "用户一般来自小程序登录注册，后续可再查看邀请关系和订单。",
    formTip: "用户通常不需要频繁手动创建，更多是做查询、修正手机号或辅助排查问题。",
    checklist: [
      "先用手机号或昵称定位用户",
      "再看注册时间和扫码层级",
      "必要时再进入订单模块排查交易记录"
    ],
    actions: [
      { type: "focusSearch", label: "开始搜索用户", style: "primary" },
      { type: "switchTab", tabKey: "orders", label: "查看用户订单", style: "secondary" }
    ]
  },
  banners: {
    title: "轮播图影响首页第一印象",
    description: "建议优先保证封面图正常显示、排序正确、文案简短清晰。缺图会直接影响首页展示。",
    goal: "先补齐轮播图，再处理文案和排序。",
    tableHint: "如果出现缺图提醒，建议先修图再继续运营投放。",
    emptyTitle: "还没有首页轮播",
    emptyDescription: "新增轮播图后，小程序首页才会有活动氛围和入口。",
    formTip: "优先上传一张清晰主图，再填标题、副标题和排序。",
    checklist: [
      "先上传轮播图或视频素材",
      "再填标题和副标题",
      "最后调整排序，确认首页展示顺序"
    ],
    actions: [
      { type: "create", label: "新增轮播", style: "primary" },
      { type: "refresh", label: "刷新检查缺图", style: "secondary" }
    ]
  },
  profileContent: {
    title: "这里维护用户中心的文案内容",
    description: "适合更新公告、关于我们和平台说明。内容尽量简洁，让用户一眼看懂。",
    goal: "先把公告写清楚，再补平台介绍。",
    tableHint: "公告建议简短直接，重要规则写在前面。",
    emptyTitle: "还没有我的页内容",
    emptyDescription: "建议至少先写一条平台公告，方便通知用户。",
    formTip: "先写公告标题，再写公告正文；关于我们可以稍后慢慢补充。",
    checklist: [
      "先确定公告标题",
      "再写公告正文和注意事项",
      "最后补充关于我们介绍"
    ],
    actions: [
      { type: "editFirst", label: "编辑当前内容", style: "primary" }
    ]
  },
  categories: {
    title: "分类决定前台活动归类",
    description: "新增分类时，优先保证名称简单清楚，排序数字越小越靠前。",
    goal: "分类名称要让用户一看就懂，排序要稳定。",
    tableHint: "如非必要，不建议频繁修改分类标识，避免影响已有活动归类。",
    emptyTitle: "还没有活动分类",
    emptyDescription: "建议先创建常用分类，例如摩托艇、冲浪、潜水等。",
    formTip: "分类名称填中文即可，分类标识不会写时可以先留空。",
    checklist: [
      "先填分类名称",
      "再决定是否启用",
      "最后设置排序"
    ],
    actions: [
      { type: "create", label: "新增分类", style: "primary" }
    ]
  },
  adminAccount: {
    title: "这里修改管理端账号和密码",
    description: "先确认当前密码，再修改新的登录账号和密码。保存后，下次登录请使用新的账号密码。",
    goal: "先确认当前密码，再安全更新后台登录凭证。",
    tableHint: "这里只保留一条管理账号记录，用于修改后台登录账号和密码。",
    emptyTitle: "还没有管理账号记录",
    emptyDescription: "系统会自动生成默认管理账号记录，刷新后再试。",
    formTip: "修改时必须填写当前密码，并把新密码输两次，避免输错后登不进去。",
    checklist: [
      "先确认当前密码输入正确",
      "再填写新的管理账号和新密码",
      "最后核对两次新密码是否一致"
    ],
    actions: [
      { type: "editFirst", label: "修改账号密码", style: "primary" }
    ]
  },
  commissions: {
    title: "佣金明细主要用来核对分佣结果",
    description: "重点看订单号、佣金状态和可提现金额，确认有没有异常冻结或逆转。",
    goal: "先按订单号或用户筛查，再核对佣金状态。",
    tableHint: "如果用户反馈佣金异常，建议先搜手机号或订单号定位记录。",
    emptyTitle: "暂无佣金记录",
    emptyDescription: "产生订单并完成分佣后，这里才会出现数据。",
    formTip: "该模块主要用于查看，不建议手动修改。",
    checklist: [
      "先定位用户或订单号",
      "再看佣金状态和可提现金额",
      "必要时去订单或提现模块继续排查"
    ],
    actions: [
      { type: "focusSearch", label: "搜索佣金记录", style: "primary" },
      { type: "switchTab", tabKey: "withdrawals", label: "查看提现单", style: "secondary" }
    ]
  },
  welfareSubmissions: {
      title: "这里处理悬赏审核",
      description: "用户在悬赏里接取任务后，会上传截图和补充说明。这里重点看悬赏地点、手机号、截图和补充说明是否对得上。",
      goal: "先审待审核记录，再确认奖励金额和驳回备注。",
      tableHint: "建议优先检查待审核记录，确认悬赏地点、手机号、截图内容和补充说明里的链接是否一致。",
      emptyTitle: "暂无悬赏提交记录",
    emptyDescription: "用户上传好评截图后，这里会出现待审核数据。",
    formTip: "审核通过时请填写奖励佣金；未通过时请写清原因，方便用户理解。",
    checklist: [
      "先按手机号定位用户",
      "再确认截图真实、清晰、可辨认",
      "最后决定奖励佣金或填写驳回原因"
    ],
    actions: [
      { type: "refresh", label: "刷新待审核", style: "primary" },
      { type: "switchTab", tabKey: "commissions", label: "查看佣金入账", style: "secondary" }
    ]
  },
  withdrawals: {
    title: "提现单要重点处理状态和失败原因",
    description: "建议先看待处理和失败单据，补失败原因和处理备注，便于后续复盘。",
    goal: "先处理异常提现，再整理备注和处理人。",
    tableHint: "失败单请务必写明原因，方便客服和运营追踪。",
    emptyTitle: "暂无提现单",
    emptyDescription: "用户发起提现申请后，这里会出现待处理记录。",
    formTip: "处理提现时，优先更新状态；失败单请补充失败原因和处理备注。",
    checklist: [
      "先看待处理提现",
      "再处理失败单并补原因",
      "最后确认处理人和备注"
    ],
    actions: [
      { type: "focusSearch", label: "搜索提现单", style: "primary" },
      { type: "refresh", label: "刷新列表", style: "secondary" }
    ]
  }
};

function readFrameworkState() {
  try {
    const raw = localStorage.getItem(ADMIN_FRAMEWORK_STATE_KEY);
    if (!raw) {
      return { lastTab: "", tabViews: {} };
    }
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return { lastTab: "", tabViews: {} };
    }
    const lastTab = typeof parsed.lastTab === "string" ? parsed.lastTab : "";
    const tabViews =
      parsed.tabViews && typeof parsed.tabViews === "object" && !Array.isArray(parsed.tabViews)
        ? parsed.tabViews
        : {};
    return { lastTab, tabViews };
  } catch (err) {
    return { lastTab: "", tabViews: {} };
  }
}

function writeFrameworkState(state) {
  try {
    localStorage.setItem(ADMIN_FRAMEWORK_STATE_KEY, JSON.stringify(state || {}));
  } catch (err) {
    // Ignore storage failures (private mode / quota / disabled storage).
  }
}

function readTabFromUrl() {
  try {
    const url = new URL(window.location.href);
    return url.searchParams.get("tab") || "";
  } catch (err) {
    return "";
  }
}

function writeTabToUrl(tabKey) {
  if (!tabKey) {
    return;
  }
  try {
    const url = new URL(window.location.href);
    url.searchParams.set("tab", tabKey);
    window.history.replaceState(null, "", url.toString());
  } catch (err) {
    // Ignore URL rewrite failures.
  }
}

function readAdminUiPrefs() {
  try {
    const raw = localStorage.getItem(ADMIN_UI_PREFS_KEY);
    if (!raw) {
      return { sidebarCollapsed: false, denseTable: false };
    }
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return { sidebarCollapsed: false, denseTable: false };
    }
    return {
      sidebarCollapsed: !!parsed.sidebarCollapsed,
      denseTable: !!parsed.denseTable
    };
  } catch (err) {
    return { sidebarCollapsed: false, denseTable: false };
  }
}

function writeAdminUiPrefs(prefs) {
  try {
    localStorage.setItem(ADMIN_UI_PREFS_KEY, JSON.stringify(prefs || {}));
  } catch (err) {
    // Ignore storage failures.
  }
}

createApp({
  data() {
    return {
      tabs: window.AdminTabs(),
      currentTab: "users",
      tabKeyword: "",
      items: [],
      keyword: "",
      loading: false,
      error: "",
      authed: false,
      loginForm: {
        username: "admin",
        password: ""
      },
      showModal: false,
      editing: null,
      form: {},
      formError: "",
      saving: false,
      options: {
        clubs: [],
        activities: [],
        users: [],
        slots: [],
        categories: []
      },
      showImport: false,
      importMode: "table",
      importText: "",
      importTableText: "",
      importError: "",
      importResult: null,
      importPreview: [],
      importPreviewColumns: [],
      importClubId: "",
      pendingUploads: {},
      pendingPreviewUrls: {},
      clubImportText: "",
      clubImportError: "",
      activityClubFilter: "",
      orderStatusFilter: "total",
      orderDateFilter: "month",
      groupActivitiesByClub: true,
      showOnlyMissingMedia: false,
      toast: {
        show: false,
        message: "",
        type: "success",
        hiding: false
      },
      toastTimer: null,
      backendStats: null,
      missingUploads: {
        skipped: false,
        message: "",
        totalReferences: 0,
        missingCount: 0,
        items: []
      },
      mediaPreview: {
        show: false,
        url: "",
        isVideo: false,
        error: false
      },
      saveProgress: {
        show: false,
        stage: "",
        percent: 0,
        detail: ""
      },
      saveProgressTimer: null,
      frameworkState: readFrameworkState(),
      frameworkStateReady: false,
      refreshRequestId: 0,
      optionLoadingPromises: {},
      uiPrefs: readAdminUiPrefs(),
      sidebarCollapsed: false,
      denseTable: false,
      activeFormSectionKey: "",
      workspaceGuideCollapsed: false,
      currentPage: 1,
      pageSize: ADMIN_DEFAULT_PAGE_SIZE,
      pageSizeOptions: ADMIN_PAGE_SIZE_OPTIONS
    };
  },
  computed: {
    activeTab() {
      return this.tabs.find((tab) => tab.key === this.currentTab) || this.tabs[0];
    },
    currentTabLabel() {
      return this.activeTab ? this.activeTab.label : "";
    },
    currentGroupLabel() {
      const group = ADMIN_TAB_GROUPS.find((item) => Array.isArray(item.tabs) && item.tabs.includes(this.currentTab));
      return group ? group.label : "后台管理";
    },
    moduleGuide() {
      return ADMIN_MODULE_GUIDES[this.currentTab] || ADMIN_MODULE_GUIDES.default;
    },
    filteredTabs() {
      const raw = String(this.tabKeyword || "").trim().toLowerCase();
      if (!raw) {
        return this.tabs;
      }
      return this.tabs.filter((tab) => {
        const label = String(tab && tab.label ? tab.label : "").toLowerCase();
        const key = String(tab && tab.key ? tab.key : "").toLowerCase();
        return label.includes(raw) || key.includes(raw);
      });
    },
    groupedTabs() {
      const filtered = Array.isArray(this.filteredTabs) ? this.filteredTabs : [];
      const groups = ADMIN_TAB_GROUPS.map((group) => ({
        key: group.key,
        label: group.label,
        tabs: []
      }));
      const groupKeyMap = {};
      ADMIN_TAB_GROUPS.forEach((group) => {
        group.tabs.forEach((tabKey) => {
          groupKeyMap[tabKey] = group.key;
        });
      });
      const groupMap = {};
      groups.forEach((group) => {
        groupMap[group.key] = group;
      });
      const others = [];
      filtered.forEach((tab) => {
        const groupKey = groupKeyMap[tab.key];
        if (groupKey && groupMap[groupKey]) {
          groupMap[groupKey].tabs.push(tab);
        } else {
          others.push(tab);
        }
      });
      const result = groups.filter((group) => group.tabs.length > 0);
      if (others.length) {
        result.push({
          key: "others",
          label: "其他",
          tabs: others
        });
      }
      return result;
    },
    quickTabs() {
      const byKey = {};
      this.tabs.forEach((tab) => {
        byKey[tab.key] = tab;
      });
      const result = [];
      ADMIN_QUICK_TAB_KEYS.forEach((tabKey) => {
        if (byKey[tabKey]) {
          result.push(byKey[tabKey]);
        }
      });
      if (!result.length) {
        return this.tabs.slice(0, 6);
      }
      return result;
    },
    displayColumns() {
      if (!(this.activeTab && this.activeTab.columns)) {
        return [];
      }
      if (this.activeTab.key === "activities" && this.groupActivitiesByClub) {
        return this.activeTab.columns.filter((col) => col.key !== "clubId");
      }
      return this.activeTab.columns;
    },
    showActionColumn() {
      return this.currentTab !== "orders";
    },
    tableColumnSpan() {
      return this.displayColumns.length + (this.showActionColumn ? 1 : 0);
    },
    filteredItems() {
      let list = this.items;
      if (this.showOnlyMissingMedia && this.showCleanupUploads) {
        const missingIds = new Set(
          ((this.missingUploads && this.missingUploads.items) || [])
            .filter((item) => {
              const entityType = String(item && item.entityType ? item.entityType : "").toLowerCase();
              if (this.currentTab === "clubs") return entityType === "club";
              if (this.currentTab === "activities") return entityType === "activity";
              if (this.currentTab === "banners") return entityType === "banner";
              return false;
            })
            .map((item) => String(item.entityId || ""))
            .filter(Boolean)
        );
        list = list.filter((item) => missingIds.has(String(item && item.id ? item.id : "")));
      }
      if (this.activeTab && this.activeTab.key === "activities" && this.activityClubFilter) {
        list = list.filter(
          (item) => String(item.clubId) === String(this.activityClubFilter)
        );
      }
      if (this.activeTab && this.activeTab.key === "orders") {
        list = list.filter((item) => this.orderWithinDateFilter(item));
      }
      if (this.activeTab && this.activeTab.key === "orders" && this.orderStatusFilter !== "total") {
        list = list.filter((item) => item.status === this.orderStatusFilter);
      }
      if (!this.keyword) {
        return list;
      }
      const key = this.keyword.toLowerCase().trim();
      const normalizedKey = normalizeSearchText(key);
      const digitKey = normalizePhoneDigits(key);
      const tokens = key.split(/\s+/).filter(Boolean);
      return list.filter((item) => {
        if (digitKey.length >= 3) {
          const phoneLikeValues = [
            item && item.phone,
            item && item.userPhone,
            item && item.mobile,
            item && item.contactPhone,
            item && item.tel
          ];
          const phoneMatched = phoneLikeValues.some((value) => {
            const digits = normalizePhoneDigits(value);
            if (!digits) return false;
            return digits.startsWith(digitKey) || digits.includes(digitKey);
          });
          if (phoneMatched) {
            return true;
          }
        }
        const raw = JSON.stringify(item).toLowerCase();
        if (raw.includes(key)) {
          return true;
        }
        const normalizedRaw = normalizeSearchText(raw);
        if (normalizedKey && normalizedRaw.includes(normalizedKey)) {
          return true;
        }
        if (tokens.length > 1) {
          return tokens.every((token) => {
            const normalizedToken = normalizeSearchText(token);
            return raw.includes(token) || (normalizedToken && normalizedRaw.includes(normalizedToken));
          });
        }
        return false;
      });
    },
    safePageSize() {
      const raw = Number(this.pageSize);
      if (!Number.isFinite(raw) || raw <= 0) {
        return ADMIN_DEFAULT_PAGE_SIZE;
      }
      return Math.floor(raw);
    },
    totalPages() {
      const total = Array.isArray(this.filteredItems) ? this.filteredItems.length : 0;
      if (total <= 0) {
        return 1;
      }
      return Math.max(1, Math.ceil(total / this.safePageSize));
    },
    safeCurrentPage() {
      const raw = Number(this.currentPage);
      const current = Number.isFinite(raw) ? Math.floor(raw) : 1;
      return Math.min(this.totalPages, Math.max(1, current));
    },
    pagedItems() {
      const list = Array.isArray(this.filteredItems) ? this.filteredItems : [];
      if (!list.length) {
        return [];
      }
      const start = (this.safeCurrentPage - 1) * this.safePageSize;
      return list.slice(start, start + this.safePageSize);
    },
    pageStart() {
      if (!Array.isArray(this.filteredItems) || this.filteredItems.length === 0) {
        return 0;
      }
      return (this.safeCurrentPage - 1) * this.safePageSize + 1;
    },
    pageEnd() {
      if (!Array.isArray(this.filteredItems) || this.filteredItems.length === 0) {
        return 0;
      }
      return Math.min(this.safeCurrentPage * this.safePageSize, this.filteredItems.length);
    },
    hasPagination() {
      return Array.isArray(this.filteredItems) && this.filteredItems.length > this.safePageSize;
    },
    visiblePageNumbers() {
      const total = this.totalPages;
      const maxButtons = 5;
      if (total <= maxButtons) {
        const all = [];
        for (let i = 1; i <= total; i += 1) {
          all.push(i);
        }
        return all;
      }
      let start = Math.max(1, this.safeCurrentPage - 2);
      let end = Math.min(total, start + maxButtons - 1);
      start = Math.max(1, end - maxButtons + 1);
      const numbers = [];
      for (let i = start; i <= end; i += 1) {
        numbers.push(i);
      }
      return numbers;
    },
    groupedActivities() {
      if (!(this.activeTab && this.activeTab.key === "activities")) {
        return [];
      }
      const groups = new Map();
      this.pagedItems.forEach((item) => {
        const clubId = item.clubId || 0;
        if (!groups.has(clubId)) {
          groups.set(clubId, []);
        }
        groups.get(clubId).push(item);
      });
      const result = [];
      groups.forEach((items, clubId) => {
        result.push({
          clubId,
          clubName: this.clubLabel(clubId),
          items
        });
      });
      return result.sort((a, b) => String(a.clubName).localeCompare(String(b.clubName)));
    },
    formFields() {
      return this.activeTab.fields || [];
    },
    visibleFormFields() {
      return this.formFields.filter((field) => this.fieldVisible(field));
    },
    activityFormSections() {
      if (this.currentTab !== "activities") {
        return [];
      }
      const sectionMap = {
        basic: {
          key: "basic",
          title: "基础信息",
          description: "先确定活动属于哪家俱乐部，以及活动名称、分类和适用人群。",
          fields: ["clubId", "title", "subtitle", "category", "audience", "expireDate", "status"]
        },
        pricing: {
          key: "pricing",
          title: "价格与分佣",
          description: "只填用户支付价、平台留存和三级佣金，商家金额由系统自动反算。",
          fields: [
            "originalPrice",
            "platformOperationFee",
            "buyerCommissionAmount",
            "inviterCommissionAmount",
            "commissionAmount"
          ]
        },
        content: {
          key: "content",
          title: "文案说明",
          description: "把活动卖点、适用人群和搭配说明写清楚，方便前台直接展示。",
          fields: ["description", "bundle"]
        },
        media: {
          key: "media",
          title: "图片资料",
          description: "封面决定列表展示，图集和详情图决定转化效果。",
          fields: ["cover", "gallery", "detailImages"]
        }
      };

      const fieldMap = {};
      this.visibleFormFields.forEach((field) => {
        fieldMap[field.key] = field;
      });

      const orderedSections = ["basic", "pricing", "content", "media"]
        .map((key) => {
          const section = sectionMap[key];
          const fields = section.fields.map((fieldKey) => fieldMap[fieldKey]).filter(Boolean);
          return fields.length ? { ...section, fields } : null;
        })
        .filter(Boolean);

      const assignedKeys = new Set(
        orderedSections.flatMap((section) => section.fields.map((field) => field.key))
      );
      const extraFields = this.visibleFormFields.filter((field) => !assignedKeys.has(field.key));
      if (extraFields.length) {
        orderedSections.push({
          key: "extra",
          title: "其他设置",
          description: "补充当前活动的其他信息。",
          fields: extraFields
        });
      }
      return orderedSections;
    },
    clubFormSections() {
      if (this.currentTab !== "clubs") {
        return [];
      }
      const sectionMap = {
        basic: {
          key: "basic",
          title: "基础资料",
          description: "先把俱乐部名称、位置、地址和营业时间填完整。",
          fields: ["name", "location", "address", "openTime"]
        },
        contact: {
          key: "contact",
          title: "联系方式与展示",
          description: "补充电话、抖音主页和标签，方便前台展示与联系。",
          fields: ["phone", "douyinUrl", "tags"]
        },
        media: {
          key: "media",
          title: "图片资料",
          description: "封面图用于列表展示，营业执照用于官网点击俱乐部后展示。",
          fields: ["cover", "licenseImage", "gallery"]
        }
      };
      const fieldMap = {};
      this.visibleFormFields.forEach((field) => {
        fieldMap[field.key] = field;
      });
      const sections = ["basic", "contact", "media"]
        .map((key) => {
          const section = sectionMap[key];
          const fields = section.fields.map((fieldKey) => fieldMap[fieldKey]).filter(Boolean);
          return fields.length ? { ...section, fields } : null;
        })
        .filter(Boolean);
      return sections;
    },
    categoryFormSections() {
      if (this.currentTab !== "categories") {
        return [];
      }
      const sectionMap = {
        identity: {
          key: "identity",
          title: "分类信息",
          description: "分类名称给用户看，识别码给系统用，建议保持简短清晰。",
          fields: ["name", "key"]
        },
        display: {
          key: "display",
          title: "展示设置",
          description: "启用状态决定前台是否显示，排序数字越小越靠前。",
          fields: ["status", "sort"]
        }
      };
      const fieldMap = {};
      this.visibleFormFields.forEach((field) => {
        fieldMap[field.key] = field;
      });
      return ["identity", "display"]
        .map((key) => {
          const section = sectionMap[key];
          const fields = section.fields.map((fieldKey) => fieldMap[fieldKey]).filter(Boolean);
          return fields.length ? { ...section, fields } : null;
        })
        .filter(Boolean);
    },
    welfareFormSections() {
      if (this.currentTab !== "welfareSubmissions") {
        return [];
      }
      const sectionMap = {
        basic: {
          key: "basic",
          title: "提交信息",
          description: "先确认用户和截图内容，再决定审核状态。",
          fields: ["submissionNo", "userPhone", "status"]
        },
        review: {
          key: "review",
          title: "审核处理",
          description: "审核通过时填写奖励佣金；未通过时请写清原因。",
          fields: ["rewardAmount", "reviewNote"]
        }
      };
      const fieldMap = {};
      this.visibleFormFields.forEach((field) => {
        fieldMap[field.key] = field;
      });
      return ["basic", "review"]
        .map((key) => {
          const section = sectionMap[key];
          const fields = section.fields.map((fieldKey) => fieldMap[fieldKey]).filter(Boolean);
          return fields.length ? { ...section, fields } : null;
        })
        .filter(Boolean);
    },
    profileContentFormSections() {
      if (this.currentTab !== "profileContent") {
        return [];
      }
      const sectionMap = {
        content: {
          key: "content",
          title: "页面文案",
          description: "维护公告和关于我们内容。",
          fields: ["noticeTitle", "noticeContent", "aboutUsContent"]
        },
        settings: {
          key: "settings",
          title: "平台设置",
          description: "维护平台电话、审核模式和官网展示数量。",
          fields: ["platformServicePhone", "reviewModeEnabled", "siteActivityLimit"]
        },
        tabs: {
          key: "tabs",
          title: "底部导航",
          description: "配置首页、分类、订单、悬赏、我的文案。",
          fields: ["tabHomeText", "tabCategoryText", "tabOrdersText", "tabWelfareText", "tabProfileText"]
        }
      };
      const fieldMap = {};
      this.visibleFormFields.forEach((field) => {
        fieldMap[field.key] = field;
      });
      return ["content", "settings", "tabs"]
        .map((key) => {
          const section = sectionMap[key];
          const fields = section.fields.map((fieldKey) => fieldMap[fieldKey]).filter(Boolean);
          return fields.length ? { ...section, fields } : null;
        })
        .filter(Boolean);
    },
    currentFormSections() {
      if (this.currentTab === "activities") {
        return this.activityFormSections;
      }
      if (this.currentTab === "clubs") {
        return this.clubFormSections;
      }
      if (this.currentTab === "categories") {
        return this.categoryFormSections;
      }
      if (this.currentTab === "welfareSubmissions") {
        return this.welfareFormSections;
      }
      if (this.currentTab === "profileContent") {
        return this.profileContentFormSections;
      }
      return [];
    },
    useSectionedFormLayout() {
      return ["activities", "clubs", "categories", "welfareSubmissions", "profileContent"].includes(this.currentTab);
    },
    supportsBatch() {
      return !!this.activeTab.batchUrl;
    },
    showCleanupUploads() {
      const fields = (this.activeTab && this.activeTab.fields) || [];
      return fields.some((field) => field && (field.type === "image" || field.type === "images"));
    },
    hasMissingUploads() {
      return !!(this.missingUploads && this.missingUploads.missingCount > 0);
    },
    showMissingAlert() {
      return (this.currentTab === "banners" || this.currentTab === "activities" || this.currentTab === "clubs") && this.hasMissingUploads;
    },
    missingUploadItemsPreview() {
      const list = (this.missingUploads && this.missingUploads.items) || [];
      return list.slice(0, 8);
    },
    missingUploadMoreCount() {
      const list = (this.missingUploads && this.missingUploads.items) || [];
      return Math.max(0, list.length - this.missingUploadItemsPreview.length);
    },
    importFields() {
      return this.getImportFields();
    },
    orderSummary() {
      if (!this.activeTab || this.activeTab.key !== "orders") {
        return null;
      }
      const base = [
        { key: "total", label: "全部", count: 0 },
        { key: "unpaid", label: "待支付", count: 0 },
        { key: "paid", label: "已支付", count: 0 },
        { key: "used", label: "已核销", count: 0 },
        { key: "refund", label: "已退款", count: 0 },
        { key: "cancelled", label: "已取消", count: 0 }
      ];
      const map = {};
      base.forEach((item) => {
        map[item.key] = item;
      });
      const list = (Array.isArray(this.items) ? this.items : []).filter((item) =>
        this.orderWithinDateFilter(item)
      );
      map.total.count = list.length;
      list.forEach((order) => {
        const key = order.status || "";
        if (map[key]) {
          map[key].count += 1;
        }
      });
      return base;
    },
    activeFilterTags() {
      const tags = [];
      const keyword = String(this.keyword || "").trim();
      if (keyword) {
        tags.push(`关键词：${keyword}`);
      }
      if (this.currentTab === "activities" && this.activityClubFilter) {
        tags.push(`俱乐部：${this.clubLabel(this.activityClubFilter)}`);
      }
      if (this.showOnlyMissingMedia && this.showCleanupUploads) {
        tags.push("只看缺图");
      }
      if (this.currentTab === "orders") {
        const statusMap = {
          total: "全部状态",
          unpaid: "待支付",
          paid: "已支付",
          used: "已核销",
          refund: "已退款",
          cancelled: "已取消"
        };
        if (this.orderStatusFilter && this.orderStatusFilter !== "total") {
          tags.push(`订单状态：${statusMap[this.orderStatusFilter] || this.orderStatusFilter}`);
        }
        const dateMap = {
          week: "近7天",
          month: "近1个月",
          all: "全部时间"
        };
        if (this.orderDateFilter && this.orderDateFilter !== "month") {
          tags.push(`时间：${dateMap[this.orderDateFilter] || this.orderDateFilter}`);
        }
      }
      return tags;
    },
    dashboardStats() {
      const total = Array.isArray(this.items) ? this.items.length : 0;
      const filtered = Array.isArray(this.filteredItems) ? this.filteredItems.length : 0;
      const cards = [
        {
          label: "当前结果",
          value: `${filtered} 条`,
          hint: this.activeFilterTags.length ? `已筛选 ${this.activeFilterTags.length} 项` : "当前展示结果"
        },
        {
          label: "总数据量",
          value: `${total} 条`,
          hint: this.hasPagination ? `第 ${this.safeCurrentPage} / ${this.totalPages} 页` : "当前已全部展示"
        }
      ];

      if (this.currentTab === "orders") {
        const orders = Array.isArray(this.items) ? this.items : [];
        const pending = orders.filter((item) => item && (item.status === "paid" || item.status === "unpaid")).length;
        const refund = orders.filter((item) => item && item.status === "refund").length;
        const stats = this.backendStats || {};
        cards.push(
          {
            label: "总注册人数",
            value: stats.totalUsers != null ? `${stats.totalUsers} 人` : "--",
            hint: "平台累计注册用户"
          },
          {
            label: "总销售额",
            value: stats.totalSales != null ? `¥${this.formatMoney(stats.totalSales)}` : "--",
            hint: "所有订单总金额"
          },
          {
            label: "已核销金额",
            value: stats.totalVerified != null ? `¥${this.formatMoney(stats.totalVerified)}` : "--",
            hint: "已核销订单金额"
          },
          {
            label: "未核销金额",
            value: stats.totalUnverified != null ? `¥${this.formatMoney(stats.totalUnverified)}` : "--",
            hint: "待核销订单金额"
          },
          {
            label: "已发佣金",
            value: stats.totalWithdrawnCommission != null ? `¥${this.formatMoney(stats.totalWithdrawnCommission)}` : "--",
            hint: "已提现佣金总额"
          },
          {
            label: "未发佣金",
            value: stats.totalPendingCommission != null ? `¥${this.formatMoney(stats.totalPendingCommission)}` : "--",
            hint: "账户中待提现佣金"
          }
        );
        return cards;
      }

      if (this.currentTab === "activities") {
        const activities = Array.isArray(this.items) ? this.items : [];
        const activeCount = activities.filter((item) => item && item.status === "active").length;
        const clubCount = new Set(activities.map((item) => item && item.clubId).filter(Boolean)).size;
        cards.push(
          {
            label: "已上架活动",
            value: `${activeCount} 个`,
            hint: "active 状态的活动"
          },
          {
            label: "关联俱乐部",
            value: `${clubCount} 家`,
            hint: this.groupActivitiesByClub ? "当前按俱乐部分组查看" : "当前为普通列表"
          }
        );
        return cards;
      }

      if (this.currentTab === "welfareSubmissions") {
        const submissions = Array.isArray(this.items) ? this.items : [];
        const pendingCount = submissions.filter((item) => item && item.status === "pending").length;
        const approvedCount = submissions.filter((item) => item && item.status === "approved").length;
        cards.push(
          {
            label: "待审核",
            value: `${pendingCount} 条`,
            hint: "建议优先处理"
          },
          {
            label: "已奖励",
            value: `${approvedCount} 条`,
            hint: "已进入佣金账户"
          }
        );
        return cards;
      }

      if (this.showCleanupUploads) {
        cards.push(
          {
            label: "缺图提醒",
            value: this.hasMissingUploads ? `${this.missingUploads.missingCount} 处` : "正常",
            hint: this.hasMissingUploads ? "建议先补图再继续运营" : "暂未发现缺图"
          }
        );
      }

      cards.push({
        label: "操作提示",
        value: this.activeTab && this.activeTab.allowCreate === false ? "查看为主" : "可新增",
        hint: this.moduleGuide.goal
      });
      if (this.currentTab === "orders") {
        return cards;
      }
      return cards.slice(0, 4);
    },
    tableHintText() {
      return this.moduleGuide && this.moduleGuide.tableHint
        ? this.moduleGuide.tableHint
        : ADMIN_MODULE_GUIDES.default.tableHint;
    },
    emptyState() {
      if (this.activeFilterTags.length) {
        return {
          title: `没有符合条件的${this.currentTabLabel}`,
          description: "可以先清除筛选条件，再查看全部数据。"
        };
      }
      return {
        title: this.moduleGuide.emptyTitle || `暂无${this.currentTabLabel}`,
        description: this.moduleGuide.emptyDescription || "可以先新增一条数据。"
      };
    },
    visibleRequiredFields() {
      return this.formFields.filter((field) => field.required && this.fieldVisible(field));
    },
    visibleRequiredCount() {
      return this.visibleRequiredFields.length;
    },
    completedRequiredCount() {
      return this.visibleRequiredFields.filter((field) => this.isFilledFieldValue(field, this.form[field.key])).length;
    },
    formCompletionText() {
      if (!this.visibleRequiredCount) {
        return "当前模块没有强制必填项";
      }
      return `必填项已完成 ${this.completedRequiredCount} / ${this.visibleRequiredCount}`;
    },
    searchPlaceholder() {
      const map = {
        clubs: "可搜俱乐部名、位置、地址、电话",
        activities: "可搜活动名、俱乐部、分类、价格",
        orders: "可搜订单号、手机号、活动名、俱乐部",
        users: "可搜昵称、手机号、OpenID",
        banners: "可搜标题、副标题、状态",
        bounties: "可搜地点、佣金区间、状态",
        withdrawals: "可搜提现单号、手机号、失败原因",
        commissions: "可搜订单号、手机号、活动名",
        welfareSubmissions: "可搜提交单号、手机号、补充说明",
        categories: "可搜分类名称、状态",
        profileContent: "可搜公告标题或内容",
        adminAccount: "可搜当前管理账号"
      };
      return map[this.currentTab] || "搜索关键字";
    },
    toastIcon() {
      const icons = {
        success: "✓",
        error: "✕",
        warning: "!",
        info: "i"
      };
      return icons[this.toast.type] || icons.info;
    },
    toastTitle() {
      const titles = {
        success: "操作成功",
        error: "操作失败",
        warning: "注意",
        info: "提示"
      };
      return titles[this.toast.type] || titles.info;
    }
  },
  watch: {
    keyword() {
      this.resetPagination();
      this.persistCurrentTabState();
    },
    activityClubFilter() {
      this.resetPagination();
      this.persistCurrentTabState();
    },
    orderStatusFilter() {
      this.resetPagination();
      this.persistCurrentTabState();
    },
    orderDateFilter() {
      this.resetPagination();
      this.persistCurrentTabState();
    },
    groupActivitiesByClub() {
      this.resetPagination();
      this.persistCurrentTabState();
    },
    showOnlyMissingMedia() {
      this.resetPagination();
      this.persistCurrentTabState();
    },
    filteredItems() {
      this.ensurePaginationBounds();
    },
    sidebarCollapsed() {
      this.persistUiPrefs();
      this.syncRootLayoutClass();
    },
    denseTable() {
      this.persistUiPrefs();
      this.syncRootLayoutClass();
    }
  },
  async mounted() {
    const ui = this.uiPrefs || {};
    this.sidebarCollapsed = !!ui.sidebarCollapsed;
    this.denseTable = !!ui.denseTable;
    this.persistUiPrefs();
    this.syncRootLayoutClass();

    const initialTab = this.resolveInitialTab();
    this.currentTab = initialTab;
    this.applyTabViewState(initialTab);
    this.frameworkStateReady = true;
    this.persistCurrentTabState();
    this.syncTabToUrl(initialTab);

    const token = AdminApi.getToken();
    if (!token) {
      this.authed = false;
      this.error = "请登录";
      this.resetMissingUploads();
      return;
    }
    this.loading = true;
    try {
      await AdminApi.apiRequest("/api/admin/users");
      this.authed = true;
      this.error = "";
      this.refresh();
    } catch (err) {
      this.authed = false;
      AdminApi.clearToken();
      this.error = "请登录";
      this.resetMissingUploads();
    } finally {
      this.loading = false;
    }
  },
  beforeUnmount() {
    this.stopSaveProgressTimer();
    this.clearPendingUploads();
  },
  methods: {
    async exportOrders() {
      try {
        const token = AdminApi.getToken();
        const response = await fetch("/api/admin/orders/export", {
          method: "GET",
          headers: { "X-Admin-Token": token }
        });
        if (!response.ok) {
          const text = await response.text();
          throw new Error(text || "导出失败");
        }
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        const stamp = new Date()
          .toISOString()
          .slice(0, 19)
          .replace(/[-:T]/g, "");
        a.download = `orders_${stamp}.csv`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => window.URL.revokeObjectURL(url), 1000);
      } catch (err) {
        this.showToast(err.message || "导出失败", "error");
      }
    },
    persistUiPrefs() {
      const prefs = {
        sidebarCollapsed: !!this.sidebarCollapsed,
        denseTable: !!this.denseTable
      };
      this.uiPrefs = prefs;
      writeAdminUiPrefs(prefs);
    },
    syncRootLayoutClass() {
      const root = document.getElementById("app");
      if (root) {
        root.classList.toggle("app--sidebar-collapsed", !!this.sidebarCollapsed);
        root.classList.toggle("app--dense", !!this.denseTable);
      }
      // Compatibility fallback for stale templates that miss Vue class bindings.
      const sidebar = document.querySelector(".sidebar");
      if (sidebar) {
        sidebar.classList.toggle("sidebar--collapsed", !!this.sidebarCollapsed);
      }
      const main = document.querySelector(".main");
      if (main) {
        main.classList.toggle("main--sidebar-collapsed", !!this.sidebarCollapsed);
        main.classList.toggle("main--dense", !!this.denseTable);
      }
    },
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed;
    },
    toggleDenseTable() {
      this.denseTable = !this.denseTable;
    },
    normalizePageSize(value) {
      const size = Number(value);
      if (!Number.isFinite(size)) {
        return ADMIN_DEFAULT_PAGE_SIZE;
      }
      const normalized = Math.floor(size);
      return ADMIN_PAGE_SIZE_OPTIONS.includes(normalized) ? normalized : ADMIN_DEFAULT_PAGE_SIZE;
    },
    ensurePaginationBounds() {
      const normalizedSize = this.normalizePageSize(this.pageSize);
      if (normalizedSize !== this.pageSize) {
        this.pageSize = normalizedSize;
      }
      const total = this.totalPages;
      const current = Number(this.currentPage);
      const normalizedCurrent = Number.isFinite(current) ? Math.floor(current) : 1;
      const nextPage = Math.min(total, Math.max(1, normalizedCurrent));
      if (nextPage !== this.currentPage) {
        this.currentPage = nextPage;
        this.persistCurrentTabState();
      }
    },
    resetPagination() {
      if (this.currentPage !== 1) {
        this.currentPage = 1;
      }
    },
    goToPage(page) {
      const target = Number(page);
      if (!Number.isFinite(target)) {
        return;
      }
      const nextPage = Math.min(this.totalPages, Math.max(1, Math.floor(target)));
      if (nextPage === this.currentPage) {
        return;
      }
      this.currentPage = nextPage;
      this.persistCurrentTabState();
    },
    prevPage() {
      this.goToPage(this.currentPage - 1);
    },
    nextPage() {
      this.goToPage(this.currentPage + 1);
    },
    setPageSize(value) {
      const size = this.normalizePageSize(value);
      if (size === this.pageSize) {
        return;
      }
      this.pageSize = size;
      this.currentPage = 1;
      this.persistCurrentTabState();
    },
    isValidTabKey(tabKey) {
      if (!tabKey) {
        return false;
      }
      return this.tabs.some((tab) => tab.key === tabKey);
    },
    resolveInitialTab() {
      const fromUrl = readTabFromUrl();
      if (this.isValidTabKey(fromUrl)) {
        return fromUrl;
      }
      const fromStorage = this.frameworkState && this.frameworkState.lastTab;
      if (this.isValidTabKey(fromStorage)) {
        return fromStorage;
      }
      const fallback = this.currentTab || "users";
      if (this.isValidTabKey(fallback)) {
        return fallback;
      }
      return this.tabs[0] ? this.tabs[0].key : "users";
    },
    buildCurrentTabViewState() {
      const tabKey = this.currentTab;
      const state = {
        keyword: this.keyword || "",
        page: this.safeCurrentPage,
        pageSize: this.safePageSize,
        workspaceGuideCollapsed: !!this.workspaceGuideCollapsed
      };
      if (tabKey === "activities") {
        state.activityClubFilter = this.activityClubFilter || "";
        state.groupActivitiesByClub = !!this.groupActivitiesByClub;
      }
      if (["clubs", "activities", "banners"].includes(tabKey)) {
        state.showOnlyMissingMedia = !!this.showOnlyMissingMedia;
      }
      if (tabKey === "orders") {
        state.orderStatusFilter = this.orderStatusFilter || "total";
        state.orderDateFilter = this.orderDateFilter || "month";
      }
      return state;
    },
    applyTabViewState(tabKey) {
      const views =
        this.frameworkState &&
        this.frameworkState.tabViews &&
        typeof this.frameworkState.tabViews === "object"
          ? this.frameworkState.tabViews
          : {};
      const state =
        views && views[tabKey] && typeof views[tabKey] === "object"
          ? views[tabKey]
          : {};

      this.keyword = typeof state.keyword === "string" ? state.keyword : "";
      this.currentPage =
        Number.isFinite(Number(state.page)) && Number(state.page) > 0
          ? Math.floor(Number(state.page))
          : 1;
      this.pageSize = this.normalizePageSize(state.pageSize);
      this.workspaceGuideCollapsed = !!state.workspaceGuideCollapsed;

      if (tabKey === "activities") {
        this.activityClubFilter =
          typeof state.activityClubFilter === "string" ? state.activityClubFilter : "";
        this.groupActivitiesByClub =
          typeof state.groupActivitiesByClub === "boolean" ? state.groupActivitiesByClub : true;
      } else {
        this.activityClubFilter = "";
        this.groupActivitiesByClub = true;
      }

      if (["clubs", "activities", "banners"].includes(tabKey)) {
        this.showOnlyMissingMedia = !!state.showOnlyMissingMedia;
      } else {
        this.showOnlyMissingMedia = false;
      }

      if (tabKey === "orders") {
        this.orderStatusFilter =
          typeof state.orderStatusFilter === "string" && state.orderStatusFilter
            ? state.orderStatusFilter
            : "total";
        this.orderDateFilter =
          typeof state.orderDateFilter === "string" && state.orderDateFilter
            ? state.orderDateFilter
            : "month";
      } else {
        this.orderStatusFilter = "total";
        this.orderDateFilter = "month";
      }
    },
    persistCurrentTabState() {
      if (!this.frameworkStateReady) {
        return;
      }
      const tabKey = this.currentTab;
      if (!this.isValidTabKey(tabKey)) {
        return;
      }
      const state =
        this.frameworkState && typeof this.frameworkState === "object"
          ? { ...this.frameworkState }
          : {};
      const tabViews =
        state.tabViews && typeof state.tabViews === "object" && !Array.isArray(state.tabViews)
          ? { ...state.tabViews }
          : {};
      tabViews[tabKey] = this.buildCurrentTabViewState();
      state.lastTab = tabKey;
      state.tabViews = tabViews;
      this.frameworkState = state;
      writeFrameworkState(state);
    },
    syncTabToUrl(tabKey) {
      if (!this.isValidTabKey(tabKey)) {
        return;
      }
      writeTabToUrl(tabKey);
    },
    guideActionDisabled(action) {
      if (!action) {
        return true;
      }
      if (!this.authed) {
        return true;
      }
      if (action.type === "create") {
        return !!(this.activeTab && this.activeTab.allowCreate === false);
      }
      if (action.type === "import") {
        return !this.supportsBatch;
      }
      if (action.type === "exportOrders") {
        return this.currentTab !== "orders" || this.loading;
      }
      if (action.type === "editFirst") {
        return !(Array.isArray(this.items) && this.items.length);
      }
      return false;
    },
    toggleWorkspaceGuide() {
      this.workspaceGuideCollapsed = !this.workspaceGuideCollapsed;
      this.persistCurrentTabState();
    },
    async runGuideAction(action) {
      if (!action || this.guideActionDisabled(action)) {
        return;
      }
      if (action.type === "create") {
        this.openCreate();
        return;
      }
      if (action.type === "import") {
        this.openImport();
        return;
      }
      if (action.type === "refresh") {
        await this.refresh();
        return;
      }
      if (action.type === "switchTab") {
        this.switchTab(action.tabKey);
        return;
      }
      if (action.type === "toggleActivityGroup") {
        this.groupActivitiesByClub = !this.groupActivitiesByClub;
        return;
      }
      if (action.type === "setOrderFilter") {
        this.setOrderFilter(action.value || "total");
        return;
      }
      if (action.type === "setOrderDateFilter") {
        this.setOrderDateFilter(action.value || "month");
        return;
      }
      if (action.type === "exportOrders") {
        await this.exportOrders();
        return;
      }
      if (action.type === "focusSearch") {
        this.focusSearchInput();
        return;
      }
      if (action.type === "editFirst") {
        const first = Array.isArray(this.items) ? this.items[0] : null;
        if (first) {
          this.openEdit(first);
        }
      }
    },
    focusSearchInput() {
      this.$nextTick(() => {
        const input = document.querySelector(".search");
        if (input && typeof input.focus === "function") {
          input.focus();
          if (typeof input.select === "function") {
            input.select();
          }
        }
      });
    },
    isFilledFieldValue(field, value) {
      if (field && field.type === "multi") {
        return Array.isArray(value) && value.length > 0;
      }
      if (field && field.type === "bountySteps") {
        return this.normalizeBountySteps(value).length > 0;
      }
      if (field && field.type === "number") {
        return value !== "" && value !== null && value !== undefined;
      }
      if (Array.isArray(value)) {
        return value.length > 0;
      }
      return String(value == null ? "" : value).trim() !== "";
    },
    sectionRequiredCount(section) {
      const fields = section && Array.isArray(section.fields) ? section.fields : [];
      return fields.filter((field) => field && field.required).length;
    },
    sectionCompletedCount(section) {
      const fields = section && Array.isArray(section.fields) ? section.fields : [];
      return fields.filter(
        (field) => field && field.required && this.isFilledFieldValue(field, this.form[field.key])
      ).length;
    },
    isFormSectionComplete(section) {
      const requiredCount = this.sectionRequiredCount(section);
      if (!requiredCount) {
        return true;
      }
      return this.sectionCompletedCount(section) >= requiredCount;
    },
    sectionCompletionText(section) {
      const requiredCount = this.sectionRequiredCount(section);
      if (!requiredCount) {
        return "无必填";
      }
      return `${this.sectionCompletedCount(section)}/${requiredCount} 必填`;
    },
    resetModalFormView() {
      this.activeFormSectionKey =
        this.useSectionedFormLayout && this.currentFormSections.length
          ? this.currentFormSections[0].key
          : "";
      this.$nextTick(() => {
        const body = this.$refs.modalBody;
        if (!body) {
          return;
        }
        body.scrollTop = 0;
        this.handleModalBodyScroll();
      });
    },
    scrollToFormSection(sectionKey) {
      const body = this.$refs.modalBody;
      if (!body || !sectionKey) {
        return;
      }
      const sections = Array.from(body.querySelectorAll("[data-form-section]"));
      const target = sections.find((item) => item.getAttribute("data-form-section") === sectionKey);
      if (!target) {
        return;
      }
      this.activeFormSectionKey = sectionKey;
      const top = Math.max(0, target.offsetTop - 10);
      body.scrollTo({ top, behavior: "smooth" });
    },
    handleModalBodyScroll() {
      const body = this.$refs.modalBody;
      if (!body || !this.useSectionedFormLayout) {
        return;
      }
      const sections = Array.from(body.querySelectorAll("[data-form-section]"));
      if (!sections.length) {
        this.activeFormSectionKey = "";
        return;
      }
      let activeKey = sections[0].getAttribute("data-form-section") || "";
      const threshold = body.scrollTop + 20;
      sections.forEach((section) => {
        if (section.offsetTop <= threshold) {
          activeKey = section.getAttribute("data-form-section") || activeKey;
        }
      });
      this.activeFormSectionKey = activeKey;
    },
    emptyBountyStep() {
      return {
        text: "",
        imageUrl: ""
      };
    },
    normalizeBountyStep(step) {
      if (!step || typeof step !== "object") {
        return null;
      }
      const text = String(step.text || "").trim();
      const imageUrl = String(step.imageUrl || "").trim();
      if (!text && !imageUrl) {
        return null;
      }
      return {
        text,
        imageUrl
      };
    },
    normalizeBountySteps(steps) {
      if (!Array.isArray(steps)) {
        return [];
      }
      return steps.map((step) => this.normalizeBountyStep(step)).filter(Boolean);
    },
    buildBountyStepsFromRow(row) {
      if (Array.isArray(row.steps) && row.steps.length) {
        return row.steps.map((step) => ({
          text: step && step.text ? String(step.text) : "",
          imageUrl: step && step.imageUrl ? String(step.imageUrl) : ""
        }));
      }
      if (typeof row.steps === "string" && row.steps.trim()) {
        try {
          const parsed = JSON.parse(row.steps);
          if (Array.isArray(parsed) && parsed.length) {
            return parsed.map((step) => ({
              text: step && step.text ? String(step.text) : "",
              imageUrl: step && step.imageUrl ? String(step.imageUrl) : ""
            }));
          }
        } catch (error) {
          // ignore legacy malformed payloads and fallback to old fixed fields
        }
      }
      const legacy = [
        { text: row.step1Text || "", imageUrl: row.step1ImageUrl || "" },
        { text: row.step2Text || "", imageUrl: row.step2ImageUrl || "" },
        { text: row.step3Text || "", imageUrl: row.step3ImageUrl || "" }
      ].filter((step) => String(step.text || "").trim() || String(step.imageUrl || "").trim());
      return legacy.length ? legacy : [this.emptyBountyStep()];
    },
    addBountyStep(fieldKey) {
      const current = Array.isArray(this.form[fieldKey]) ? this.form[fieldKey].slice() : [];
      current.push(this.emptyBountyStep());
      this.form[fieldKey] = current;
    },
    removeBountyStep(fieldKey, index) {
      const current = Array.isArray(this.form[fieldKey]) ? this.form[fieldKey].slice() : [];
      if (current.length <= 1) {
        this.form[fieldKey] = [this.emptyBountyStep()];
        return;
      }
      current.splice(index, 1);
      this.form[fieldKey] = current.length ? current : [this.emptyBountyStep()];
    },
    removeBountyStepImage(fieldKey, index) {
      if (!Array.isArray(this.form[fieldKey]) || !this.form[fieldKey][index]) {
        return;
      }
      this.form[fieldKey][index].imageUrl = "";
    },
    async onBountyStepUpload(fieldKey, index, event) {
      const input = event && event.target;
      const file = input && input.files ? input.files[0] : null;
      if (!file) {
        return;
      }
      try {
        const data = await AdminApi.uploadFile(file);
        const current = Array.isArray(this.form[fieldKey]) ? this.form[fieldKey].slice() : [];
        while (current.length <= index) {
          current.push(this.emptyBountyStep());
        }
        current[index] = Object.assign({}, current[index], {
          imageUrl: data.url
        });
        this.form[fieldKey] = current;
        this.showToast("上传成功");
      } catch (err) {
        this.showToast(err.message || "上传失败", "error");
      } finally {
        if (input) {
          input.value = "";
        }
      }
    },
    getCategoryOptions() {
      const list = Array.isArray(this.options && this.options.categories)
        ? this.options.categories
        : [];
      return list
        .map((item) => ({
          key: item.key || item.value || item.id,
          name: resolveCategoryName(item),
          raw: item
        }))
        .filter((item) => item.key && item.name);
    },
    matchCategoryFromText(text) {
      const raw = String(text || "").replace(/\s+/g, "").trim();
      if (!raw) return "";
      const normalizedLower = raw.toLowerCase();
      const categories = this.getCategoryOptions();
      const sorted = categories.slice().sort((a, b) => String(b.name).length - String(a.name).length);
      for (const cat of sorted) {
        const name = String(cat.name || "").replace(/\s+/g, "");
        if (name && raw.includes(name)) {
          return cat.key;
        }
      }
      for (const cat of categories) {
        const key = String(cat.key || "");
        if (key && normalizedLower.includes(key.toLowerCase())) {
          return cat.key;
        }
      }
      const fallbackMap = {
        jetski: ["摩托艇", "水上摩托", "喷射艇"],
        surf: ["冲浪", "浪板"],
        banana: ["香蕉船", "拖拽艇", "拖伞"],
        paddle: ["桨板", "立式桨板", "sup", "划水"],
        dive: ["潜水", "浮潜", "深潜"],
        kayak: ["皮划艇", "皮划船"],
        yacht: ["游艇", "帆船"]
      };
      for (const cat of categories) {
        const key = String(cat.key || "");
        const keywords = fallbackMap[key] || [];
        if (keywords.some((kw) => raw.includes(kw))) {
          return cat.key;
        }
      }
      return "";
    },
    showToast(message, type = "success") {
      if (!message) return;
      if (this.toastTimer) {
        clearTimeout(this.toastTimer);
        this.toastTimer = null;
      }
      this.toast = {
        show: true,
        message,
        type,
        hiding: false
      };
    },
    hideToast() {
      this.toast.hiding = true;
      setTimeout(() => {
        this.toast = { show: false, message: "", type: "", hiding: false };
      }, 200);
      if (this.toastTimer) {
        clearTimeout(this.toastTimer);
        this.toastTimer = null;
      }
    },
    async loadDashboardStats() {
      try {
        const stats = await AdminApi.apiRequest("/api/admin/dashboard/stats");
        this.backendStats = stats || null;
      } catch (err) {
        this.backendStats = null;
      }
    },
    resetMissingUploads() {
      this.missingUploads = {
        skipped: false,
        message: "",
        totalReferences: 0,
        missingCount: 0,
        items: []
      };
    },
    normalizeMissingUploads(data) {
      const payload = data || {};
      const list = Array.isArray(payload.items) ? payload.items : [];
      return {
        skipped: !!payload.skipped,
        message: payload.message || "",
        totalReferences: Number(payload.totalReferences || 0),
        missingCount: Number(payload.missingCount || list.length || 0),
        items: list
      };
    },
    async loadMissingUploads(requestId) {
      if (!this.authed) {
        this.resetMissingUploads();
        return;
      }
      try {
        const data = await AdminApi.apiRequest("/api/admin/uploads/missing");
        if (requestId && requestId !== this.refreshRequestId) {
          return;
        }
        this.missingUploads = this.normalizeMissingUploads(data);
      } catch (err) {
        if (requestId && requestId !== this.refreshRequestId) {
          return;
        }
        this.resetMissingUploads();
      }
    },
    missingEntityLabel(item) {
      if (!item) {
        return "";
      }
      const entityType = String(item.entityType || "");
      if (entityType === "activity") {
        const clubName = item.clubName ? `【${item.clubName}】` : "";
        return `${clubName}${item.entityName || "商品"}`;
      }
      if (entityType === "club") {
        return `俱乐部【${item.entityName || "未命名"}】`;
      }
      if (entityType === "banner") {
        return `轮播图【${item.entityName || "未命名"}】`;
      }
      return item.entityName || "资源";
    },
    missingItemText(item) {
      if (!item) {
        return "";
      }
      const entityLabel = this.missingEntityLabel(item);
      const fieldLabel = item.fieldLabel || item.fieldKey || "图片字段";
      const fileName = item.fileName || item.reference || "";
      return `${entityLabel} - ${fieldLabel} 缺失（${fileName}）`;
    },
    async jumpToMissingItem(item) {
      if (!item) {
        return;
      }
      const map = {
        activity: "activities",
        club: "clubs",
        banner: "banners"
      };
      const targetTab = map[String(item.entityType || "").toLowerCase()];
      if (!targetTab) {
        this.showToast("该记录不支持跳转", "warning");
        return;
      }
      if (this.currentTab !== targetTab) {
        this.currentTab = targetTab;
        this.keyword = "";
        if (targetTab !== "activities") {
          this.activityClubFilter = "";
        }
        if (targetTab !== "orders") {
          this.orderStatusFilter = "total";
          this.orderDateFilter = "month";
        }
        this.persistCurrentTabState();
        this.syncTabToUrl(targetTab);
      }
      await this.refresh();
      const targetId = String(item.entityId || "");
      const row = (Array.isArray(this.items) ? this.items : []).find((it) => String(it.id) === targetId);
      if (!row) {
        this.showToast("未找到对应记录，可能已删除", "warning");
        return;
      }
      this.openEdit(row);
    },
    async cleanupUploads() {
      if (!this.authed) {
        return;
      }
      try {
        const preview = await AdminApi.apiRequest("/api/admin/uploads/cleanup?dryRun=true", {
          method: "POST"
        });
        const unused = preview && preview.unusedFiles ? preview.unusedFiles : 0;
        const used = preview && preview.usedFiles ? preview.usedFiles : 0;
        const total = preview && preview.totalFiles ? preview.totalFiles : unused + used;
        const message = `检测到 ${unused} 张闲置图片（共 ${total} 张，已使用 ${used} 张）。确认删除？`;
        if (!window.confirm(message)) {
          return;
        }
        const result = await AdminApi.apiRequest("/api/admin/uploads/cleanup", {
          method: "POST"
        });
        const deleted = result && result.unusedFiles ? result.unusedFiles : 0;
        this.showToast(`已清理 ${deleted} 张图片`);
      } catch (err) {
        this.showToast(err.message || "清理失败", "error");
      }
    },
    hasPending(fieldKey) {
      return !!(this.pendingUploads && this.pendingUploads[fieldKey]);
    },
    clearPendingField(fieldKey) {
      const urls = (this.pendingPreviewUrls && this.pendingPreviewUrls[fieldKey]) || [];
      urls.forEach((item) => {
        const url = item && typeof item === "object" ? item.url : item;
        if (url) {
          URL.revokeObjectURL(url);
        }
      });
      if (this.pendingUploads) {
        delete this.pendingUploads[fieldKey];
      }
      if (this.pendingPreviewUrls) {
        delete this.pendingPreviewUrls[fieldKey];
      }
    },
    clearPendingUploads() {
      if (!this.pendingPreviewUrls) {
        this.pendingUploads = {};
        this.pendingPreviewUrls = {};
        return;
      }
      Object.keys(this.pendingPreviewUrls).forEach((key) => this.clearPendingField(key));
      this.pendingUploads = {};
      this.pendingPreviewUrls = {};
    },
    setPendingUpload(field, files) {
      if (!field || !field.key) {
        return;
      }
      if (!files || files.length === 0) {
        return;
      }
      if (field.type === "images") {
        const list = Array.from(files);
        const existingFiles = Array.isArray(this.pendingUploads[field.key])
          ? this.pendingUploads[field.key]
          : [];
        const existingUrls = Array.isArray(this.pendingPreviewUrls[field.key])
          ? this.pendingPreviewUrls[field.key]
          : [];
        this.pendingUploads[field.key] = existingFiles.concat(list);
        this.pendingPreviewUrls[field.key] = existingUrls.concat(
          list.map((file) => ({
            url: URL.createObjectURL(file),
            isVideo: isVideoFileLike(file)
          }))
        );
        return;
      }
      this.clearPendingField(field.key);
      const file = files[0];
      this.pendingUploads[field.key] = file;
      this.pendingPreviewUrls[field.key] = [
        {
          url: URL.createObjectURL(file),
          isVideo: isVideoFileLike(file)
        }
      ];
    },
    existingImageList(field) {
      if (!field || !field.key) {
        return [];
      }
      const raw = this.form[field.key];
      if (field.type === "image") {
        return raw ? [raw] : [];
      }
      return this.imageList(field);
    },
    pendingPreviewList(field) {
      if (!field || !field.key) {
        return [];
      }
      const pending = (this.pendingPreviewUrls && this.pendingPreviewUrls[field.key]) || [];
      const list = Array.isArray(pending) ? pending : pending ? [pending] : [];
      return list
        .map((item) => {
          if (!item) return null;
          if (typeof item === "object") {
            return {
              url: item.url || "",
              isVideo: !!item.isVideo
            };
          }
          return {
            url: item,
            isVideo: this.isVideoUrl(item)
          };
        })
        .filter((item) => item && item.url);
    },
    uploadAccept(field) {
      if (this.currentTab === "banners" && field && field.key === "imageUrl") {
        return "image/*,video/mp4";
      }
      return "image/*";
    },
    isVideoUrl(url) {
      if (!url) {
        return false;
      }
      const clean = String(url).split("?")[0].split("#")[0].toLowerCase();
      return (
        clean.endsWith(".mp4") ||
        clean.endsWith(".mov") ||
        clean.endsWith(".m4v") ||
        clean.endsWith(".webm") ||
        clean.endsWith(".avi") ||
        clean.endsWith(".mkv")
      );
    },
    isMediaFieldKey(key) {
      const mediaKeys = ["imageUrl", "cover", "licenseImage", "gallery", "detailImages", "avatarUrl", "screenshotUrl"];
      return mediaKeys.includes(String(key || ""));
    },
    extractMediaValues(value) {
      if (!value) {
        return [];
      }
      if (Array.isArray(value)) {
        return value.map((item) => String(item || "").trim()).filter(Boolean);
      }
      const text = String(value || "").trim();
      if (!text) {
        return [];
      }
      return parseList(text.replace(/，/g, ","));
    },
    normalizeMediaUrl(value) {
      if (value === null || value === undefined) {
        return "";
      }
      return String(value).trim();
    },
    normalizeMediaArray(value) {
      const list = this.extractMediaValues(value)
        .map((item) => this.normalizeMediaUrl(item))
        .filter(Boolean);
      return Array.from(new Set(list));
    },
    sanitizeMediaPayload(payload) {
      if (!payload || typeof payload !== "object") {
        return;
      }
      const inClubs = this.currentTab === "clubs";
      const inActivities = this.currentTab === "activities";
      if (!inClubs && !inActivities) {
        return;
      }
      const hasCover = Object.prototype.hasOwnProperty.call(payload, "cover");
      const hasLicenseImage = Object.prototype.hasOwnProperty.call(payload, "licenseImage");
      const hasGallery = Object.prototype.hasOwnProperty.call(payload, "gallery");
      const hasDetail = Object.prototype.hasOwnProperty.call(payload, "detailImages");
      const cover = this.normalizeMediaUrl(payload.cover);
      const licenseImage = this.normalizeMediaUrl(payload.licenseImage);
      let gallery = this.normalizeMediaArray(payload.gallery);
      let detailImages = this.normalizeMediaArray(payload.detailImages);
      const rawGallerySize = gallery.length;
      const rawDetailSize = detailImages.length;

      if (cover) {
        gallery = gallery.filter((url) => url !== cover);
        detailImages = detailImages.filter((url) => url !== cover);
      }
      if (licenseImage) {
        gallery = gallery.filter((url) => url !== licenseImage);
        detailImages = detailImages.filter((url) => url !== licenseImage);
      }
      if (inActivities && gallery.length) {
        const gallerySet = new Set(gallery);
        detailImages = detailImages.filter((url) => !gallerySet.has(url));
      }

      if (hasCover) {
        payload.cover = cover;
      }
      if (hasLicenseImage) {
        payload.licenseImage = licenseImage;
      }
      if (hasGallery) {
        payload.gallery = gallery;
      }
      if (inActivities && hasDetail) {
        payload.detailImages = detailImages;
      }

      if (
        rawGallerySize !== gallery.length ||
        rawDetailSize !== detailImages.length
      ) {
        this.showToast("已自动去重图片，避免在其他图片位置重复显示", "warning");
      }
    },
    cellMediaList(row, col) {
      if (!row || !col || !this.isMediaFieldKey(col.key)) {
        return [];
      }
      return this.extractMediaValues(row[col.key]).map((url) => ({
        url,
        isVideo: this.isVideoUrl(url)
      }));
    },
    tableMediaPreviewList(row, col) {
      return this.cellMediaList(row, col).slice(0, 3);
    },
    tableMediaMoreCount(row, col) {
      return Math.max(0, this.cellMediaList(row, col).length - 3);
    },
    onTableMediaError(event) {
      const target = event && event.target;
      if (!target || !target.parentElement) {
        return;
      }
      target.parentElement.classList.add("table-media-item--error");
    },
    imagePreviewList(field) {
      return this.existingImageList(field).concat(this.pendingPreviewList(field));
    },
    openMediaPreview(media) {
      const url = media && typeof media === "object" ? media.url : media;
      if (!url) {
        return;
      }
      const isVideo = media && typeof media === "object" ? !!media.isVideo : this.isVideoUrl(url);
      this.mediaPreview = {
        show: true,
        url,
        isVideo,
        error: false
      };
    },
    onPreviewMediaError() {
      this.mediaPreview = {
        ...this.mediaPreview,
        error: true
      };
    },
    closeMediaPreview() {
      this.mediaPreview = {
        show: false,
        url: "",
        isVideo: false,
        error: false
      };
    },
    removePendingImage(field, index) {
      if (!field || !field.key) {
        return;
      }
      const key = field.key;
      const urls = (this.pendingPreviewUrls && this.pendingPreviewUrls[key]) || [];
      if (!Array.isArray(urls) || urls.length === 0) {
        return;
      }
      const target = urls[index];
      const url = target && typeof target === "object" ? target.url : target;
      if (url) {
        URL.revokeObjectURL(url);
      }
      const pendingFiles = this.pendingUploads ? this.pendingUploads[key] : null;
      if (!Array.isArray(pendingFiles)) {
        this.clearPendingField(key);
        return;
      }
      pendingFiles.splice(index, 1);
      urls.splice(index, 1);
      if (pendingFiles.length === 0) {
        this.clearPendingField(key);
        return;
      }
      this.pendingUploads[key] = pendingFiles;
      this.pendingPreviewUrls[key] = urls;
    },
    async removeExistingImage(field, index) {
      if (!field || !field.key) {
        return;
      }
      const key = field.key;
      const list = this.existingImageList(field);
      const url = Array.isArray(list) ? list[index] : "";
      if (url) {
        try {
          await AdminApi.deleteUpload(url);
        } catch (err) {
          this.showToast(err.message || "删除失败", "error");
          return;
        }
      }
      if (field.type === "image") {
        this.form[key] = "";
        return;
      }
      if (!Array.isArray(list) || list.length === 0) {
        return;
      }
      list.splice(index, 1);
      if (list.length === 0) {
        this.form[key] = "";
      } else {
        this.form[key] = list;
      }
    },
    categoryLabel(value) {
      if (!value) {
        return "";
      }
      const list = (this.options && this.options.categories) || [];
      const matched = list.find((item) => item.key === value || String(item.id) === String(value));
      return matched ? resolveCategoryName(matched) : resolveCategoryName({ key: value }) || value;
    },
    clubLabel(value) {
      if (value === null || value === undefined || value === "") {
        return "未分配";
      }
      const list = (this.options && this.options.clubs) || [];
      const matched = list.find((item) => String(item.id) === String(value));
      return matched ? matched.name : String(value);
    },
    categoryValue(label) {
      if (!label) {
        return "";
      }
      const list = (this.options && this.options.categories) || [];
      const matched = list.find((item) => {
        const name = resolveCategoryName(item);
        return item.name === label || item.key === label || name === label;
      });
      return matched ? matched.key : label;
    },
    setImportMode(mode) {
      this.importMode = mode;
      this.importError = "";
    },
    getImportFields() {
      const fields = (this.activeTab && this.activeTab.fields) || [];
      return fields.filter((field) => {
        if (field.key === "slotTimes") {
          return false;
        }
        if (this.activeTab && this.activeTab.key === "slots" && field.key === "slotTime") {
          return true;
        }
        return !field.editOnly;
      });
    },
    getImportFieldMap() {
      const fields = this.getImportFields();
      const map = {};
      fields.forEach((field) => {
        const keys = [field.key, field.label].filter(Boolean);
        keys.forEach((key) => {
          const normalized = String(key).trim().toLowerCase().replace(/\s+/g, "");
          if (normalized) {
            map[normalized] = field;
          }
        });
      });
      return map;
    },
    normalizeImportValue(field, raw) {
      const value = String(raw || "").trim();
      if (!value) {
        return null;
      }
      if (field.type === "number") {
        return Number(value);
      }
      if (field.key === "category") {
        return this.categoryValue(value);
      }
      if (field.type === "select") {
        if (field.options && Array.isArray(field.options)) {
          const matched = field.options.find(
            (opt) => String(opt.label) === value || String(opt.value) === value
          );
          if (matched) {
            return field.valueType === "number" ? Number(matched.value) : matched.value;
          }
        }
        return field.valueType === "number" ? Number(value) : value;
      }
      if (field.type === "list" || field.type === "images") {
        return parseList(value.replace(/，/g, ","));
      }
      return value;
    },
    mapDyStatus(value) {
      const text = String(value || "").trim();
      if (!text) return "";
      if (text.includes("上架") || text.includes("在售") || text.includes("可售")) {
        return "active";
      }
      if (text.includes("下架") || text.includes("停售") || text.includes("不可售")) {
        return "inactive";
      }
      return "";
    },
    findClubIdByName(name) {
      const text = String(name || "").trim();
      if (!text) return null;
      const list = (this.options && this.options.clubs) || [];
      let matched = list.find((club) => String(club.name || "").trim() === text);
      if (!matched) {
        matched = list.find((club) => text.includes(String(club.name || "").trim()));
      }
      if (!matched) {
        matched = list.find((club) => String(club.name || "").trim().includes(text));
      }
      return matched ? matched.id : null;
    },
    isDyProductImport(headers) {
      return headers.some((header) => DY_PRODUCT_IMPORT_MAP[normalizeHeaderKey(header)]);
    },
    parseDyProductImport(text) {
      const raw = String(text || "").trim();
      if (!raw) {
        throw new Error("请输入表格内容");
      }
      const lines = raw.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
      if (lines.length < 2) {
        throw new Error("至少需要表头 + 一行数据");
      }
      const headerLine = lines[0];
      const delimiter = headerLine.includes("\t") ? "\t" : ",";
      const normalizedHeaderLine = delimiter === "," ? headerLine.replace(/，/g, ",") : headerLine;
      const headers = this.splitImportLine(normalizedHeaderLine, delimiter);
      if (!this.isDyProductImport(headers)) {
        throw new Error("未识别为抖音商品导出表，请检查表头");
      }
      const columnMap = headers.map((header) => DY_PRODUCT_IMPORT_MAP[normalizeHeaderKey(header)] || null);
      const rows = [];
      const missingClubs = new Set();
      for (let i = 1; i < lines.length; i++) {
        const line = delimiter === "," ? lines[i].replace(/，/g, ",") : lines[i];
        const values = this.splitImportLine(line, delimiter);
        if (values.length === 1 && !values[0]) {
          continue;
        }
        const rawRow = {};
        columnMap.forEach((key, index) => {
          if (!key) return;
          const rawValue = values[index] || "";
          if (rawValue !== "") {
            rawRow[key] = String(rawValue).trim();
          }
        });
        if (!rawRow.title) {
          continue;
        }
        const row = {};
        row.title = rawRow.title;
        const promoPrice = parseNumber(rawRow.promoPrice);
        const price = parseNumber(rawRow.price);
        if (promoPrice !== null) {
          row.basePrice = promoPrice;
        } else if (price !== null) {
          row.basePrice = price;
        }
        if (price !== null) {
          row.originalPrice = price;
        }
        const expireDate = parseDateOnly(rawRow.expireDate);
        if (expireDate) {
          row.expireDate = expireDate;
        }
        const status = this.mapDyStatus(rawRow.statusText);
        if (status) {
          row.status = status;
        }
        if (rawRow.audience) {
          row.audience = rawRow.audience;
        }
        if (!row.category) {
          const categoryText = [
            rawRow.title,
            rawRow.ownerName,
            rawRow.ownerAccount,
            rawRow.audience
          ]
            .filter(Boolean)
            .join(" ");
          const inferred = this.matchCategoryFromText(categoryText);
          if (inferred) {
            row.category = inferred;
          }
        }
        const descParts = [];
        if (rawRow.usableDates) descParts.push(`可用日期: ${rawRow.usableDates}`);
        if (rawRow.unusableDates) descParts.push(`不可用日期: ${rawRow.unusableDates}`);
        if (rawRow.usableTimes) descParts.push(`每日时段: ${rawRow.usableTimes}`);
        if (rawRow.channel) descParts.push(`投放渠道: ${rawRow.channel}`);
        if (descParts.length) {
          row.description = descParts.join(" | ");
        }
        const ownerName = rawRow.ownerName || "";
        const ownerAccount = rawRow.ownerAccount || "";
        const isNumericAccount = /^\d+$/.test(ownerAccount);
        const clubCandidate = ownerName || (!isNumericAccount ? ownerAccount : "");
        const clubId = this.findClubIdByName(clubCandidate);
        if (clubId) {
          row.clubId = clubId;
        } else {
          if (this.importClubId) {
            row.clubId = Number(this.importClubId);
          } else {
            missingClubs.add(clubCandidate || "未填写门店");
          }
        }
        rows.push(row);
      }
      if (missingClubs.size) {
        const names = Array.from(missingClubs).slice(0, 5).join("、");
        throw new Error(`找不到门店: ${names}，请先在“俱乐部”里创建同名门店或手动指定门店`);
      }
      if (!rows.length) {
        throw new Error("没有可导入的数据");
      }
      const fields = this.getImportFields();
      return { rows, columns: fields };
    },
    splitImportLine(line, delimiter) {
      const result = [];
      let current = "";
      let inQuotes = false;
      for (let i = 0; i < line.length; i++) {
        const ch = line[i];
        if (ch === "\"") {
          if (inQuotes && line[i + 1] === "\"") {
            current += "\"";
            i += 1;
          } else {
            inQuotes = !inQuotes;
          }
          continue;
        }
        if (ch === delimiter && !inQuotes) {
          result.push(current.trim());
          current = "";
          continue;
        }
        current += ch;
      }
      result.push(current.trim());
      return result;
    },
    parseImportTable(text) {
      const raw = String(text || "").trim();
      if (!raw) {
        throw new Error("请输入表格内容");
      }
      const lines = raw.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
      if (lines.length < 2) {
        throw new Error("至少需要表头 + 一行数据");
      }
      const headerLine = lines[0];
      const delimiter = headerLine.includes("\t") ? "\t" : ",";
      const normalizedHeaderLine = delimiter === "," ? headerLine.replace(/，/g, ",") : headerLine;
      const headers = this.splitImportLine(normalizedHeaderLine, delimiter);
      if (this.activeTab && this.activeTab.key === "activities" && this.isDyProductImport(headers)) {
        return this.parseDyProductImport(text);
      }
      const fieldMap = this.getImportFieldMap();
      const columns = headers.map((header) => {
        const key = String(header || "").trim().toLowerCase().replace(/\s+/g, "");
        return fieldMap[key] || null;
      });
      const missing = headers.filter((_, idx) => !columns[idx]);
      if (missing.length) {
        throw new Error(`表头未识别: ${missing.join("、")}`);
      }
      const requiredFields = this.getImportFields().filter((field) => {
        if (field.required) {
          return true;
        }
        return this.activeTab && this.activeTab.key === "slots" && field.key === "slotTime";
      });
      const missingRequired = requiredFields.filter((field) => !columns.includes(field));
      if (missingRequired.length) {
        const names = missingRequired.map((field) => field.label || field.key);
        throw new Error(`缺少必填表头: ${names.join("、")}`);
      }
      const rows = [];
      for (let i = 1; i < lines.length; i++) {
        const line = delimiter === "," ? lines[i].replace(/，/g, ",") : lines[i];
        const values = this.splitImportLine(line, delimiter);
        if (values.length === 1 && !values[0]) {
          continue;
        }
        const row = {};
        columns.forEach((field, index) => {
          if (!field) {
            return;
          }
          const rawValue = values[index] || "";
          const normalized = this.normalizeImportValue(field, rawValue);
          if (normalized !== null) {
            row[field.key] = normalized;
          }
        });
        if (this.activeTab && this.activeTab.key === "categories") {
          if (!row.key && row.name) {
            row.key = row.name;
          }
        }
        if (Object.keys(row).length) {
          rows.push(row);
        }
      }
      if (!rows.length) {
        throw new Error("没有可导入的数据");
      }
      return { rows, columns };
    },
    updateTablePreview() {
      if (!this.importTableText) {
        this.importPreview = [];
        this.importPreviewColumns = [];
        this.importError = "";
        return;
      }
      try {
        const parsed = this.parseImportTable(this.importTableText);
        const previewRows = parsed.rows.slice(0, 5).map((row) => {
          if (this.activeTab && this.activeTab.key === "activities" && row && row.category) {
            return { ...row, category: this.categoryLabel(row.category) };
          }
          return row;
        });
        this.importPreview = previewRows;
        this.importPreviewColumns = parsed.columns;
        this.importError = "";
      } catch (err) {
        this.importPreview = [];
        this.importPreviewColumns = [];
        this.importError = err.message || "表格解析失败";
      }
    },
    onImportTextInput() {
      this.updateTablePreview();
    },
    onImportFile(event) {
      const file = event.target.files && event.target.files[0];
      if (!file) {
        return;
      }
      const fileName = file.name.toLowerCase();
      const isXlsx = fileName.endsWith(".xlsx") || fileName.endsWith(".xls");
      const reader = new FileReader();
      reader.onload = () => {
        if (isXlsx && typeof XLSX !== "undefined") {
          try {
            const data = new Uint8Array(reader.result);
            const workbook = XLSX.read(data, { type: "array" });
            const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
            const csv = XLSX.utils.sheet_to_csv(firstSheet, { FS: "\t" });
            this.importTableText = csv;
          } catch (e) {
            this.importError = "Excel文件解析失败: " + (e.message || e);
            event.target.value = "";
            return;
          }
        } else {
          this.importTableText = String(reader.result || "");
        }
        this.updateTablePreview();
      };
      reader.onerror = () => {
        this.importError = "读取文件失败";
      };
      if (isXlsx) {
        reader.readAsArrayBuffer(file);
      } else {
        reader.readAsText(file);
      }
      event.target.value = "";
    },
    importExampleValue(field) {
      if (field.key === "name") return "示例名称";
      if (field.key === "location") return "三亚 · 亚龙湾";
      if (field.key === "address") return "海边码头";
      if (field.key === "phone") return "13312340000";
      if (field.key === "clubId") return "1";
      if (field.key === "activityId") return "101";
      if (field.key === "slotDate") return "2026-06-08";
      if (field.key === "slotTime") return "09:00 - 09:30";
      if (field.key === "category") {
        const list = (this.options && this.options.categories) || [];
        return list.length ? list[0].name : "分类名称";
      }
      if (field.key === "status") {
        if (field.options && field.options.length) {
          return field.options[0].label;
        }
        return "active";
      }
      if (field.type === "number") return "100";
      if (field.type === "date") return "2026-12-31";
      if (field.type === "list") return "标签1,标签2";
      if (field.type === "images") return "https://example.com/a.jpg,https://example.com/b.jpg";
      return "示例内容";
    },
    downloadTemplate() {
      const fields = this.getImportFields();
      if (!fields.length) {
        return;
      }
      const header = fields.map((field) => field.label || field.key).join("\t");
      const example = fields.map((field) => this.importExampleValue(field)).join("\t");
      const content = `${header}\n${example}\n`;
      const blob = new Blob([content], { type: "text/tab-separated-values;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${this.currentTabLabel || "import"}-template.tsv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    },
    formatPreviewValue(value, key) {
      if (Array.isArray(value)) {
        return value.join(", ");
      }
      if (value === null || value === undefined) {
        return "";
      }
      if (this.activeTab && this.activeTab.key === "activities") {
        if (key === "clubId") {
          return this.clubLabel(value);
        }
        if (key === "category") {
          return this.categoryLabel(value);
        }
        if (key === "status") {
          if (value === "active") return "上架";
          if (value === "inactive") return "下架";
        }
      }
      return value;
    },
    async ensureOptions(keys, params) {
      const uniqueKeys = Array.from(new Set(keys.filter(Boolean)));
      if (!uniqueKeys.length) {
        return;
      }
      const tasks = uniqueKeys.map((key) => this.ensureOptionLoaded(key, params));
      await Promise.all(tasks);
    },
    async ensureOptionLoaded(key, params) {
      if (!key) {
        return;
      }
      if (this.options[key] && this.options[key].length && key !== "slots") {
        return;
      }
      const slotSuffix =
        key === "slots" ? String((params && params.activityId) || "all") : "default";
      const cacheKey = `${key}:${slotSuffix}`;
      if (!this.optionLoadingPromises[cacheKey]) {
        this.optionLoadingPromises[cacheKey] = this.loadOptions(key, params).finally(() => {
          delete this.optionLoadingPromises[cacheKey];
        });
      }
      await this.optionLoadingPromises[cacheKey];
    },
    async loadOptions(key, params) {
      if (!key) {
        return;
      }
      let url = "";
      if (key === "clubs") {
        url = "/api/admin/clubs";
      } else if (key === "activities") {
        url = "/api/admin/activities";
      } else if (key === "users") {
        url = "/api/admin/users";
      } else if (key === "categories") {
        url = "/api/admin/categories";
      } else if (key === "slots") {
        const activityId = params && params.activityId ? params.activityId : null;
        url = activityId ? `/api/admin/slots?activityId=${activityId}` : "/api/admin/slots";
      }
      if (!url) {
        return;
      }
      const list = await AdminApi.apiRequest(url);
      this.options[key] = Array.isArray(list) ? list : [];
    },
    optionLabel(field, option) {
      if (!option) {
        return "";
      }
      if (field.optionTemplate === "club") {
        return `${option.name || "未命名"} #${option.id || ""}`.trim();
      }
      if (field.optionTemplate === "activity") {
        return `${option.title || "未命名"} #${option.id || ""}`.trim();
      }
      if (field.optionTemplate === "user") {
        return option.phone || option.nickname || "未绑定手机号";
      }
      if (field.optionTemplate === "slot") {
        return `${option.slotDate || ""} ${option.slotTime || ""} #${option.id || ""}`.trim();
      }
      if (field.optionTemplate === "category") {
        const name = option.name || resolveCategoryName(option) || "";
        const key = option.key || "";
        if (name && key && name !== key) {
          return `${name} (${key})`;
        }
        return key || name || option.id || "";
      }
      return option.name || option.title || option.nickname || option.id || "";
    },
    normalizedOptions(field) {
      if (field.options && Array.isArray(field.options)) {
        return field.options.map((opt) => ({
          value: opt.value,
          label: opt.label
        }));
      }
      const key = field.optionsKey;
      const list = (key && this.options[key]) || [];
      if (key === "categories") {
        return list.map((item) => ({
          value: item.key,
          label: this.optionLabel(field, item)
        }));
      }
      return list.map((item) => ({
        value: item.id,
        label: this.optionLabel(field, item)
      }));
    },
    async onSelectChange(field) {
      if (field.key !== "activityId") {
        return;
      }
      if (this.currentTab === "orders" || this.currentTab === "slots") {
        const activityId = Number(this.form.activityId || 0) || null;
        await this.loadOptions("slots", { activityId });
      }
    },
    switchTab(tabKey) {
      if (!this.isValidTabKey(tabKey)) {
        return;
      }
      if (tabKey === this.currentTab) {
        this.refresh();
        return;
      }
      this.persistCurrentTabState();
      this.currentTab = tabKey;
      this.applyTabViewState(tabKey);
      this.persistCurrentTabState();
      this.syncTabToUrl(tabKey);
      this.refresh();
    },
    async login() {
      this.error = "";
      this.loading = true;
      try {
        const data = await AdminApi.apiRequest("/api/admin/login", {
          method: "POST",
          body: this.loginForm
        });
        AdminApi.setToken(data.token);
        this.authed = true;
        this.refresh();
      } catch (err) {
        this.error = err.message || "登录失败";
      } finally {
        this.loading = false;
      }
    },
    logout() {
      AdminApi.clearToken();
      this.authed = false;
      this.items = [];
      this.refreshRequestId += 1;
      this.error = "请登录";
      this.resetMissingUploads();
    },
    clearFilters() {
      this.keyword = "";
      this.activityClubFilter = "";
      this.orderStatusFilter = "total";
      this.orderDateFilter = "month";
      this.showOnlyMissingMedia = false;
      this.resetPagination();
      this.persistCurrentTabState();
    },
    setOrderFilter(key) {
      this.orderStatusFilter = key || "total";
    },
    setOrderDateFilter(key) {
      this.orderDateFilter = key || "month";
    },
    async copyText(text, successMessage) {
      const value = String(text || "").trim();
      if (!value) {
        this.showToast("没有可复制的内容", "warning");
        return;
      }
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(value);
        } else {
          const input = document.createElement("textarea");
          input.value = value;
          input.setAttribute("readonly", "readonly");
          input.style.position = "fixed";
          input.style.left = "-9999px";
          document.body.appendChild(input);
          input.select();
          document.execCommand("copy");
          document.body.removeChild(input);
        }
        this.showToast(successMessage || "已复制");
      } catch (err) {
        this.showToast("复制失败，请手动复制", "error");
      }
    },
    copyOrderNo(row) {
      this.copyText(row && row.orderNo, "订单号已复制");
    },
    copyClubAddress(row) {
      this.copyText(row && row.address, "地址已复制");
    },
    mediaEmptyText(col) {
      if (!col) {
        return "暂无媒体";
      }
      if (col.key === "cover" || col.key === "imageUrl") {
        return "暂无封面";
      }
      if (col.key === "screenshotUrl") {
        return "截图已清理";
      }
      if (col.key === "detailImages") {
        return "暂无详情图";
      }
      if (col.key === "gallery") {
        return "暂无图集";
      }
      return "暂无媒体";
    },
    orderWithinDateFilter(order) {
      if (!(this.activeTab && this.activeTab.key === "orders")) {
        return true;
      }
      const filter = this.orderDateFilter || "month";
      if (filter === "all") {
        return true;
      }
      const createdAt =
        (order && (order.createdAt || order.createTime || order.orderTime || order.created_time || order.create_time)) || "";
      if (filter === "week") {
        return isWithinRecentDays(createdAt, 7);
      }
      return isWithinRecentDays(createdAt, 30);
    },
    async refresh() {
      if (!this.authed) {
        this.resetMissingUploads();
        return;
      }
      const requestId = ++this.refreshRequestId;
      this.loading = true;
      this.error = "";
      try {
        if (!this.options) {
          this.options = { clubs: [], activities: [], users: [], slots: [], categories: [] };
        }
        await this.ensureOptions(this.formFields.map((field) => field.optionsKey));
        if (requestId !== this.refreshRequestId) {
          return;
        }
        const list = await AdminApi.apiRequest(this.activeTab.listUrl);
        if (requestId !== this.refreshRequestId) {
          return;
        }
        this.items = Array.isArray(list) ? list : [];
        this.ensurePaginationBounds();
        await this.loadMissingUploads(requestId);
        if (this.showOnlyMissingMedia && !this.hasMissingUploads) {
          this.showOnlyMissingMedia = false;
        }
        if (this.currentTab === "orders") {
          this.loadDashboardStats();
        }
      } catch (err) {
        if (requestId !== this.refreshRequestId) {
          return;
        }
        const msg = String((err && err.message) || "");
        if (msg.includes("未授权") || msg.includes("请登录") || msg.includes("401")) {
          this.authed = false;
          AdminApi.clearToken();
          this.error = "请登录";
          this.resetMissingUploads();
          return;
        }
        this.error = err.message || "加载失败";
      } finally {
        if (requestId === this.refreshRequestId) {
          this.loading = false;
        }
      }
    },
    formatCell(row, col) {
      const value = row[col.key];
      if (this.activeTab.key === "activities" && col.key === "title") {
        return value === null || value === undefined ? "" : String(value).trim();
      }
      if (this.activeTab.key === "banners" && col.key === "status") {
        if (value === "active") return "上架";
        if (value === "inactive") return "下架";
      }
      if (this.activeTab.key === "activities" && col.key === "status") {
        if (value === "active") return "上架";
        if (value === "inactive") return "下架";
      }
      if (this.activeTab.key === "categories" && col.key === "status") {
        if (value === "active") return "启用";
        if (value === "inactive") return "停用";
      }
      if (this.activeTab.key === "activities" && col.key === "category") {
        return this.categoryLabel(value);
      }
      if (this.activeTab.key === "activities" && col.key === "clubId") {
        return this.clubLabel(value);
      }
      if (this.activeTab.key === "commissions" && col.key === "statusText") {
        return row.statusText || value || "";
      }
      if (this.activeTab.key === "welfareSubmissions" && col.key === "statusText") {
        return row.statusText || value || "";
      }
      if (this.activeTab.key === "bounties" && col.key === "statusText") {
        return row.statusText || value || "";
      }
      if (this.activeTab.key === "withdrawals" && col.key === "statusText") {
        return row.statusText || value || "";
      }
      if (this.activeTab.key === "users" && col.key === "scanUser") {
        return Number(value) === 1 ? "是" : "否";
      }
      if (this.activeTab.key === "users" && col.key === "depth") {
        return this.userScanLevelText(row);
      }
      if (
        this.activeTab.key === "profileContent" &&
        (col.key === "noticeContent" || col.key === "aboutUsContent")
      ) {
        const text = value === null || value === undefined ? "" : String(value).trim();
        if (text.length <= 40) {
          return text;
        }
        return `${text.slice(0, 40)}...`;
      }
      if (this.activeTab.key === "profileContent" && col.key === "reviewModeEnabled") {
        return Number(value) === 1 ? "开启" : "关闭";
      }
      if (
        this.activeTab.key === "orders" &&
        [
          "payAmount",
          "userCommissionAmount",
          "user1CommissionAmount",
          "user2CommissionAmount",
          "user3CommissionAmount",
          "operatorCommissionAmount",
          "platformOperationFee"
        ].includes(col.key)
      ) {
        return this.formatMoneyCell(value);
      }
      if (col.key === "tags" || col.key === "gallery") {
        return formatList(value);
      }
      if (
        col.key === "slotDate" ||
        col.key === "expireDate" ||
        col.key === "requestedAt" ||
        col.key === "processedAt" ||
        col.key === "createdAt" ||
        col.key === "updatedAt" ||
        col.key === "scanActivatedAt" ||
        col.key === "invitedAt"
      ) {
        return formatDate(value);
      }
      return value === null || value === undefined ? "" : value;
    },
    tableHeaderLabel(col) {
      if (!col) {
        return "";
      }
      const map = {
        originalPrice: "支付价",
        platformOperationFee: "平台留存",
        buyerCommissionAmount: "用户1佣金",
        inviterCommissionAmount: "用户2佣金",
        commissionAmount: "用户3佣金",
        user1CommissionAmount: "用户1",
        user2CommissionAmount: "用户2",
        user3CommissionAmount: "用户3",
        operatorCommissionAmount: "小程序运营",
        userPhone: "手机号",
        imageUrl: "媒体",
        douyinUrl: "抖音",
        noticeContent: "公告",
        aboutUsContent: "关于我们"
      };
      if (this.currentTab === "categories" && col.key === "name") {
        return "名称";
      }
      return map[col.key] || col.label || col.key;
    },
    tableColumnClass(col) {
      if (!col) {
        return "";
      }
      const key = String(col.key || "");
      return {
        "col--media": ["imageUrl", "cover", "gallery", "detailImages", "avatarUrl", "screenshotUrl"].includes(key),
        "col--title": ["title", "name", "activityTitle", "clubName"].includes(key),
        "col--address": ["address", "noticeContent", "aboutUsContent"].includes(key),
        "col--longtext": ["subtitle", "reviewText", "reviewNote", "failReason"].includes(key),
        "col--location": key === "location",
        "col--price": /price|amount|fee/i.test(key),
        "col--status": key === "status" || key === "statusText",
        "col--date": /date|time|at$/i.test(key),
        "col--phone": /phone|mobile|tel/i.test(key),
        "col--id": ["orderNo", "withdrawNo", "openId", "key", "submissionNo"].includes(key),
        "col--url": key === "douyinUrl",
        "col--count": ["sort", "quantity", "depth"].includes(key)
      };
    },
    displayCellText(row, col) {
      const text = this.formatCell(row, col);
      if (text === null || text === undefined || String(text).trim() === "") {
        return this.fallbackCellText(col);
      }
      return text;
    },
    formatMoneyCell(value) {
      const parsed = parseNumber(value);
      if (parsed === null) {
        return value === null || value === undefined || String(value).trim() === "" ? "" : String(value);
      }
      return `¥${parsed.toFixed(2)}`;
    },
    formatMoney(value) {
      const parsed = parseNumber(value);
      if (parsed === null) {
        return "0.00";
      }
      return parsed.toFixed(2);
    },
    fallbackCellText(col) {
      if (!col) {
        return "-";
      }
      if (col.key === "cover" || col.key === "imageUrl" || col.key === "gallery" || col.key === "screenshotUrl") {
        return "暂无图片";
      }
      if (col.key === "openTime") {
        return "未设置";
      }
      if (col.key === "address") {
        return "未填写地址";
      }
      if (col.key === "douyinUrl") {
        return "未填写链接";
      }
      return "-";
    },
    isPlaceholderCell(row, col) {
      const text = this.formatCell(row, col);
      return text === null || text === undefined || String(text).trim() === "";
    },
    isStatusColumn(col) {
      if (!col) {
        return false;
      }
      return col.key === "status" || col.key === "statusText";
    },
    statusTone(row, col) {
      const raw = String((row && row[col.key]) || "").toLowerCase();
      const display = String(this.formatCell(row, col) || "").toLowerCase();
      if (raw === "active" || raw === "paid" || raw === "used" || raw === "success" || raw === "approved" || display.includes("上架") || display.includes("成功") || display.includes("已核销") || display.includes("已支付") || display.includes("已奖励")) {
        return "success";
      }
      if (raw === "pending" || raw === "processing" || raw === "unpaid" || display.includes("待") || display.includes("处理中")) {
        return "warning";
      }
      if (raw === "inactive" || raw === "failed" || raw === "refund" || raw === "cancelled" || raw === "rejected" || display.includes("失败") || display.includes("退款") || display.includes("取消") || display.includes("下架") || display.includes("未通过")) {
        return "danger";
      }
      return "neutral";
    },
    cellTextClass(row, col) {
      const key = String((col && col.key) || "");
      const shouldClampLongText = [
        "title",
        "name",
        "activityTitle",
        "clubName",
        "subtitle",
        "reviewText",
        "reviewNote",
        "failReason",
        "noticeContent",
        "aboutUsContent",
        "address",
        "location"
      ].includes(key);
      return {
        "cell-main": true,
        "cell-main--placeholder": this.isPlaceholderCell(row, col),
        "cell-main--wrap": ["address", "location", "noticeContent", "aboutUsContent"].includes(col.key),
        "cell-main--activity-title": this.activeTab && this.activeTab.key === "activities" && col.key === "title",
        "cell-main--order-title": this.activeTab && this.activeTab.key === "orders" && col.key === "title",
        "cell-main--clamp": shouldClampLongText,
        "cell-main--mono": ["orderNo", "withdrawNo", "openId"].includes(col.key),
        "status-badge": this.isStatusColumn(col),
        [`status-badge--${this.statusTone(row, col)}`]: this.isStatusColumn(col)
      };
    },
    cellTooltipText(row, col) {
      if (!row || !col) {
        return "";
      }
      if (
        [
          "title",
          "name",
          "activityTitle",
          "clubName",
          "subtitle",
          "reviewText",
          "reviewNote",
          "failReason",
          "noticeContent",
          "aboutUsContent",
          "address",
          "location",
          "orderNo",
          "withdrawNo",
          "openId",
          "submissionNo"
        ].includes(col.key)
      ) {
        const raw = row[col.key];
        return raw === null || raw === undefined ? "" : String(raw).trim();
      }
      return "";
    },
    isUrlCell(row, col) {
      if (!col || !row) {
        return false;
      }
      if (!["douyinUrl"].includes(col.key)) {
        return false;
      }
      const value = String(row[col.key] || "").trim();
      return /^https?:\/\//i.test(value);
    },
    normalizeExternalUrl(value) {
      const text = String(value || "").trim();
      if (!text) {
        return "#";
      }
      return /^https?:\/\//i.test(text) ? text : `https://${text}`;
    },
    urlCellLabel(row, col) {
      if (!row || !col) {
        return "查看链接";
      }
      if (col.key === "douyinUrl") {
        return "查看主页";
      }
      return "打开链接";
    },
    userScanLevelText(user) {
      if (!user) {
        return "-";
      }
      if (Number(user.scanUser) !== 1) {
        return "-";
      }
      const level = this.resolveUserScanLevel(user);
      return level > 0 ? String(level) : "-";
    },
    resolveUserScanLevel(user) {
      const users = Array.isArray(this.items) ? this.items : [];
      if (!user || Number(user.scanUser) !== 1) {
        return 0;
      }
      const userMap = {};
      users.forEach((item) => {
        const id = Number(item && item.id);
        if (id > 0) {
          userMap[id] = item;
        }
      });
      const visited = {};
      const resolveDepth = (current) => {
        if (!current || Number(current.scanUser) !== 1) {
          return 0;
        }
        const manualDepth = Number(current.depth);
        if (manualDepth >= 1 && manualDepth <= 3) {
          return manualDepth;
        }
        const currentId = Number(current.id || 0);
        if (currentId > 0) {
          if (visited[currentId]) {
            return 1;
          }
          visited[currentId] = true;
        }
        const inviterId = Number(current.inviterId || 0);
        if (inviterId <= 0) {
          return 1;
        }
        const inviter = userMap[inviterId];
        const inviterDepth = resolveDepth(inviter);
        if (inviterDepth <= 0) {
          return 1;
        }
        return Math.min(inviterDepth + 1, 3);
      };
      return resolveDepth(user);
    },
    fieldVisible(field) {
      if (this.editing && field.createOnly) {
        return false;
      }
      if (!this.editing && field.editOnly) {
        return false;
      }
      return true;
    },
    openCreate() {
      if (this.activeTab && this.activeTab.allowCreate === false) {
        return;
      }
      this.editing = null;
      this.form = {};
      this.formError = "";
      this.resetSaveProgress();
      this.clubImportText = "";
      this.clubImportError = "";
      this.clearPendingUploads();
      this.formFields.forEach((field) => {
        if (field.type === "multi") {
          this.form[field.key] = [];
        } else if (field.type === "bountySteps") {
          this.form[field.key] = [this.emptyBountyStep()];
        }
      });
      if (this.currentTab === "categories") {
        this.form.key = "";
      }
      this.ensureOptions(this.formFields.map((field) => field.optionsKey));
      this.showModal = true;
      this.resetModalFormView();
    },
    openEdit(row) {
      if (this.activeTab && this.activeTab.allowEdit === false) {
        return;
      }
      this.editing = row;
      this.formError = "";
      this.resetSaveProgress();
      this.clubImportText = "";
      this.clubImportError = "";
      this.clearPendingUploads();
      const form = {};
      this.formFields.forEach((field) => {
        if (field.createOnly) return;
        const value = row[field.key];
        if (field.type === "list") {
          form[field.key] = formatList(value);
        } else if (field.type === "bountySteps") {
          form[field.key] = this.buildBountyStepsFromRow(row);
        } else if (field.type === "images") {
          form[field.key] = Array.isArray(value) ? value.join(",") : formatList(value);
        } else if (this.currentTab === "categories" && field.key === "key") {
          form[field.key] = suggestCategoryKey(value) || (value != null ? String(value) : "");
        } else {
          form[field.key] = value != null ? String(value) : "";
        }
      });
      this.form = form;
      const activityId = this.form.activityId ? Number(this.form.activityId) : null;
      this.ensureOptions(this.formFields.map((field) => field.optionsKey), { activityId });
      this.showModal = true;
      this.resetModalFormView();
    },
    openActivityRowEdit(row, event) {
      if (!this.activeTab || this.activeTab.key !== "activities") {
        return;
      }
      const target = event && event.target ? event.target : null;
      if (target && target.closest && target.closest("button,a,input,select,textarea,.table-media-item")) {
        return;
      }
      this.openEdit(row);
    },
    closeModal() {
      this.clearPendingUploads();
      this.resetSaveProgress();
      this.activeFormSectionKey = "";
      this.showModal = false;
    },
    clearClubImport() {
      this.clubImportText = "";
      this.clubImportError = "";
    },
    extractClubTags(text) {
      const keywords = [
        "摩托艇",
        "冲浪",
        "潜水",
        "桨板",
        "皮划艇",
        "香蕉船",
        "拖伞",
        "帆船",
        "游艇",
        "海钓",
        "摄影",
        "露营",
        "赶海",
        "滑水",
        "亲子",
        "团建"
      ];
      const tags = [];
      keywords.forEach((key) => {
        if (text.includes(key)) {
          tags.push(key);
        }
      });
      return Array.from(new Set(tags));
    },
    parseTagTokens(text) {
      if (!text) return [];
      const cleaned = String(text)
        .replace(/^[^:：]*[:：]/, "")
        .replace(/[【】\[\]（）()]/g, " ")
        .trim();
      if (!cleaned) return [];
      const parts = cleaned
        .split(/[·、,/，|;；\s]+/)
        .map((item) => item.trim())
        .filter(Boolean);
      const filtered = parts.filter((item) =>
        item.length <= 12 &&
        !/^\d+$/.test(item) &&
        !/地址|电话|联系人|营业|时间|账号|认证/.test(item)
      );
      return Array.from(new Set(filtered));
    },
    extractLocationFromText(text) {
      const raw = String(text || "").replace(/\s+/g, "").trim();
      if (!raw) return "";
      const cityProvinceMap = {
        北京: "北京市",
        上海: "上海市",
        天津: "天津市",
        重庆: "重庆市",
        三亚: "海南省",
        海口: "海南省",
        厦门: "福建省",
        福州: "福建省",
        泉州: "福建省",
        广州: "广东省",
        深圳: "广东省",
        珠海: "广东省",
        中山: "广东省",
        佛山: "广东省",
        惠州: "广东省",
        湛江: "广东省",
        北海: "广西壮族自治区",
        桂林: "广西壮族自治区",
        青岛: "山东省",
        济南: "山东省",
        威海: "山东省",
        大连: "辽宁省",
        沈阳: "辽宁省",
        成都: "四川省",
        西安: "陕西省",
        南京: "江苏省",
        苏州: "江苏省",
        无锡: "江苏省",
        杭州: "浙江省",
        宁波: "浙江省",
        温州: "浙江省",
        舟山: "浙江省",
        昆明: "云南省",
        丽江: "云南省",
        西双版纳: "云南省",
        哈尔滨: "黑龙江省",
        长春: "吉林省"
      };

      const provinceMatch = raw.match(/[\u4e00-\u9fa5]{2,8}省/);
      const cityMatch = raw.match(/[\u4e00-\u9fa5]{2,8}市/);
      const districtMatch = raw.match(/[\u4e00-\u9fa5]{2,8}(区|县|旗|州)/);

      let province = provinceMatch ? provinceMatch[0] : "";
      let city = cityMatch ? cityMatch[0] : "";
      const district = districtMatch ? districtMatch[0] : "";

      if (!province && city) {
        const cityKey = city.replace(/市$/, "");
        province = cityProvinceMap[cityKey] || "";
      }

      if (!city && province && /市$/.test(province)) {
        city = province;
      }

      if (!city) {
        const cityKeys = Object.keys(cityProvinceMap).sort((a, b) => b.length - a.length);
        const hit = cityKeys.find((key) => raw.includes(key));
        if (hit) {
          city = `${hit}市`;
          if (!province) {
            province = cityProvinceMap[hit] || "";
          }
        }
      }

      let location = "";
      if (province) location += province;
      if (city && !location.includes(city)) location += city;
      if (district && !location.includes(district)) location += district;
      return location;
    },
    parseClubImport(text) {
      const raw = String(text || "").replace(/\r/g, "\n").trim();
      if (!raw) {
        throw new Error("请先粘贴抖音门店信息");
      }
      const lines = raw
        .split("\n")
        .map((line) => line.trim())
        .filter(Boolean);
      const result = { tags: [] };
      let intro = "";

      for (let i = 0; i < lines.length; i += 1) {
        const line = lines[i];
        if (!result.name && line.includes("账号类型") && line.includes("|")) {
          result.name = line.split("|").pop().trim();
        }
        if (!result.address) {
          const isAddressLine =
            /^(门店地址|店铺地址|地址|位置|定位)/.test(line) || /门店地址|店铺地址/.test(line);
          if (isAddressLine) {
            const parts = line.split(/[:：]/);
            const candidate = parts.length > 1 ? parts.slice(1).join(":").trim() : "";
            if (candidate) {
              result.address = candidate;
            } else if (lines[i + 1]) {
              const next = lines[i + 1].trim();
              if (next && !/地址|位置|定位|营业|时间/.test(next)) {
                result.address = next;
              }
            }
          }
        }
        if (!result.phone) {
          const skipLine = /抖音号|账号|认证|分钟|min/i.test(line);
          if (!skipLine) {
            const phoneMatch = line.match(/1[3-9]\d{9}/) || line.match(/\d{3,4}-?\d{7,8}/);
            if (phoneMatch) {
              result.phone = phoneMatch[0];
            }
          }
        }
        if (!result.openTime) {
          const timeKeywords = /营业时间|营业信息|营业状态|正常营业/;
          if (timeKeywords.test(line)) {
            let candidate = "";
            const parts = line.split(/[:：]/);
            if (parts.length > 1) {
              candidate = parts.slice(1).join(":").trim();
            } else {
              candidate = line.replace(timeKeywords, "").trim();
            }
            if (!candidate && lines[i + 1]) {
              const next = lines[i + 1].trim();
              if (next && !/地址|位置|定位|电话|联系方式/.test(next)) {
                candidate = next;
              }
            }
            if (candidate) {
              candidate = candidate.replace(/^[:：]/, "").trim();
              result.openTime = candidate;
            }
          }
        }
        if (!intro && line.startsWith("简介")) {
          const parts = line.split(/[:：]/);
          if (parts.length > 1) {
            intro = parts.slice(1).join(":").trim();
          } else if (lines[i + 1]) {
            intro = lines[i + 1].trim();
          }
        }
        {
          const isCategoryLabel = /门店品类|主营类目|经营类目|品类|类目/.test(line);
          const isCategoryLine =
            isCategoryLabel ||
            (!/地址|电话|联系人|营业|时间|账号|认证|抖音号/.test(line) && /[·、]/.test(line) && line.length <= 40);
          if (isCategoryLine) {
            let candidate = "";
            if (isCategoryLabel) {
              const parts = line.split(/[:：]/);
              candidate = parts.length > 1 ? parts.slice(1).join(":").trim() : "";
              if (!candidate) {
                candidate = line
                  .replace(/^(.*?)(门店品类|主营类目|经营类目|品类|类目)/, "")
                  .replace(/[:：]/g, " ")
                  .trim();
              }
              if (!candidate && lines[i + 1]) {
                candidate = lines[i + 1].trim();
              }
            } else {
              candidate = line;
            }
            const tokens = this.parseTagTokens(candidate);
            if (tokens.length) {
              result.tags = Array.from(new Set(result.tags.concat(tokens)));
            }
          }
        }
      }

      if (!result.name) {
        const nameKeywords = ["俱乐部", "基地", "中心", "营地", "水上", "摩托艇"];
        const candidate = lines.find((line) =>
          nameKeywords.some((key) => line.includes(key)) &&
          !line.includes("账号") &&
          !line.includes("认证") &&
          line.length <= 30
        );
        if (candidate) {
          result.name = candidate.trim();
        }
      }

      if (intro) {
        const introTags = this.extractClubTags(intro);
        result.tags = Array.from(new Set(result.tags.concat(introTags)));
      }

      if (!result.address) {
        const addressLine = lines.find((line) => /地址|位置|定位/.test(line) && line.length < 60);
        if (addressLine) {
          const parts = addressLine.split(/[:：]/);
          const candidate = parts.length > 1 ? parts.slice(1).join(":").trim() : "";
          if (candidate) {
            result.address = candidate;
          } else {
            const idx = lines.indexOf(addressLine);
            const next = idx >= 0 && lines[idx + 1] ? lines[idx + 1].trim() : "";
            if (next && !/地址|位置|定位|营业|时间/.test(next)) {
              result.address = next;
            }
          }
        }
      }
      if (!result.openTime) {
        const timeLine = lines.find((line) => /营业时间|营业信息|营业状态|正常营业/.test(line));
        if (timeLine) {
          const parts = timeLine.split(/[:：]/);
          let candidate = parts.length > 1 ? parts.slice(1).join(":").trim() : "";
          if (!candidate) {
            candidate = timeLine.replace(/营业时间|营业信息|营业状态|正常营业/g, "").trim();
          }
          if (!candidate) {
            const idx = lines.indexOf(timeLine);
            const next = idx >= 0 && lines[idx + 1] ? lines[idx + 1].trim() : "";
            if (next && !/地址|位置|定位|电话|联系方式/.test(next)) {
              candidate = next;
            }
          }
          if (candidate) {
            result.openTime = candidate.replace(/^[:：]/, "").trim();
          }
        }
      }

      if (!result.location) {
        const fromAddress = this.extractLocationFromText(result.address);
        const fromName = this.extractLocationFromText(result.name);
        if (fromAddress || fromName) {
          result.location = fromAddress || fromName;
        }
      }

      if (!result.name && !result.phone && !result.address && result.tags.length === 0) {
        throw new Error("未识别到门店信息，请确认复制内容包含名称/地址/电话");
      }
      return result;
    },
    applyClubImport() {
      this.clubImportError = "";
      try {
        const parsed = this.parseClubImport(this.clubImportText);
        if (parsed.name && !this.form.name) {
          this.form.name = parsed.name;
        }
        if (parsed.address && !this.form.address) {
          this.form.address = parsed.address;
        }
        if (parsed.location && !this.form.location) {
          this.form.location = parsed.location;
        }
        if (parsed.openTime && !this.form.openTime) {
          this.form.openTime = parsed.openTime;
        }
        if (parsed.phone && !this.form.phone) {
          this.form.phone = parsed.phone;
        }
        if (parsed.tags && parsed.tags.length && !this.form.tags) {
          this.form.tags = parsed.tags.join(",");
        }
      } catch (err) {
        this.clubImportError = err.message || "识别失败";
      }
    },
    imageList(field) {
      const raw = this.form[field.key];
      if (!raw) return [];
      if (Array.isArray(raw)) return raw;
      return parseList(raw);
    },
    countPendingUploadFiles() {
      let total = 0;
      const uploadFields = this.formFields.filter(
        (field) => field.type === "image" || field.type === "images"
      );
      uploadFields.forEach((field) => {
        const pending = this.pendingUploads[field.key];
        if (!pending) {
          return;
        }
        const files = Array.isArray(pending) ? pending : [pending];
        total += files.length;
      });
      return total;
    },
    stopSaveProgressTimer() {
      if (this.saveProgressTimer) {
        clearInterval(this.saveProgressTimer);
        this.saveProgressTimer = null;
      }
    },
    resetSaveProgress() {
      this.stopSaveProgressTimer();
      this.saveProgress = {
        show: false,
        stage: "",
        percent: 0,
        detail: ""
      };
    },
    beginSaveProgress(totalUploads) {
      this.stopSaveProgressTimer();
      if (totalUploads > 0) {
        this.saveProgress = {
          show: true,
          stage: "上传中",
          percent: 0,
          detail: `准备上传 0/${totalUploads}`
        };
        return;
      }
      this.saveProgress = {
        show: true,
        stage: "保存中",
        percent: 88,
        detail: "正在保存数据"
      };
      this.enterSaveRequestProgress();
    },
    setUploadProgress(doneBefore, total, fileName, filePercent) {
      const safeTotal = Math.max(1, Number(total) || 1);
      const safeDone = Math.max(0, Number(doneBefore) || 0);
      const safePercent = Math.max(0, Math.min(100, Number(filePercent) || 0));
      const progress = (safeDone + safePercent / 100) / safeTotal;
      const mappedPercent = Math.max(0, Math.min(85, Math.round(progress * 85)));
      const currentIndex = Math.min(safeTotal, safeDone + 1);
      this.saveProgress.show = true;
      this.saveProgress.stage = "上传中";
      this.saveProgress.percent = mappedPercent;
      this.saveProgress.detail = `上传 ${currentIndex}/${safeTotal}：${fileName || "文件"}`;
    },
    enterSaveRequestProgress() {
      this.stopSaveProgressTimer();
      const start = Math.max(88, Number(this.saveProgress.percent) || 0);
      this.saveProgress.show = true;
      this.saveProgress.stage = "保存中";
      this.saveProgress.percent = start;
      this.saveProgress.detail = "正在保存数据";
      this.saveProgressTimer = setInterval(() => {
        if (!this.saveProgress.show) {
          return;
        }
        if (this.saveProgress.percent < 98) {
          this.saveProgress.percent += 1;
        }
      }, 220);
    },
    finishSaveProgress() {
      this.stopSaveProgressTimer();
      this.saveProgress.show = true;
      this.saveProgress.stage = "完成";
      this.saveProgress.percent = 100;
      this.saveProgress.detail = "保存完成";
      setTimeout(() => this.resetSaveProgress(), 420);
    },
    failSaveProgress(message) {
      this.stopSaveProgressTimer();
      this.saveProgress.show = true;
      this.saveProgress.stage = "失败";
      this.saveProgress.percent = Math.max(8, Number(this.saveProgress.percent) || 0);
      this.saveProgress.detail = message || "保存失败";
      setTimeout(() => this.resetSaveProgress(), 1500);
    },
    async onUpload(field, event) {
      const files = event.target.files;
      if (!files || files.length === 0) {
        return;
      }
      this.setPendingUpload(field, files);
      event.target.value = "";
    },
    async applyPendingUploads(payload) {
      const uploadFields = this.formFields.filter(
        (field) => field.type === "image" || field.type === "images"
      );
      const totalFiles = this.countPendingUploadFiles();
      let uploadedFiles = 0;
      for (const field of uploadFields) {
        const pending = this.pendingUploads[field.key];
        if (!pending) {
          continue;
        }
        const files = Array.isArray(pending) ? pending : [pending];
        const urls = [];
        for (const file of files) {
          const uploadOptions =
            this.currentTab === "banners" && field.key === "imageUrl"
              ? { scene: "banner" }
              : {};
          const fileName = (file && file.name) || `文件${uploadedFiles + 1}`;
          if (totalFiles > 0) {
            this.setUploadProgress(uploadedFiles, totalFiles, fileName, 0);
          }
          const data = await AdminApi.uploadFile(file, Object.assign({}, uploadOptions, {
            onProgress: (percent) => {
              if (totalFiles > 0) {
                this.setUploadProgress(uploadedFiles, totalFiles, fileName, percent);
              }
            }
          }));
          if (totalFiles > 0) {
            this.setUploadProgress(uploadedFiles, totalFiles, fileName, 100);
          }
          uploadedFiles += 1;
          urls.push(data.url);
        }
        if (field.type === "image") {
          payload[field.key] = urls[0] || "";
        } else {
          const existing = payload[field.key] || [];
          payload[field.key] = existing.concat(urls);
        }
      }
    },
    async submitForm() {
      this.formError = "";
      this.resetSaveProgress();
      const payload = {};
      if (this.currentTab === "slots" && !this.editing) {
        const times = Array.isArray(this.form.slotTimes) ? this.form.slotTimes : [];
        if (!times.length) {
          this.showToast("请选择至少一个时段", "error");
          return;
        }
        const base = {};
        for (const field of this.formFields) {
          if (!this.fieldVisible(field)) {
            continue;
          }
          if (field.key === "slotTimes" || field.key === "slotTime") {
            continue;
          }
          const raw = this.form[field.key];
          if (field.type === "bountySteps") {
            const steps = this.normalizeBountySteps(raw);
            if (field.required && !steps.length) {
              this.showToast(`${field.label}不能为空`, "error");
              return;
            }
            if (!steps.length) {
              continue;
            }
            base[field.key] = steps;
            continue;
          }
          if (field.required && !raw) {
            this.showToast(`${field.label}不能为空`, "error");
            return;
          }
          if (!raw) {
            continue;
          }
          if (field.type === "number") {
            base[field.key] = Number(raw);
          } else if (field.type === "select") {
            base[field.key] = field.valueType === "number" ? Number(raw) : raw;
          } else if (field.type === "list") {
            base[field.key] = parseList(raw);
          } else if (field.type === "images") {
            base[field.key] = parseList(raw);
          } else if (field.key === "category") {
            base[field.key] = this.categoryValue(raw);
          } else {
            base[field.key] = raw;
          }
        }
        const batch = times.map((slotTime) => Object.assign({}, base, { slotTime }));
        try {
          this.saving = true;
          this.beginSaveProgress(0);
          await AdminApi.apiRequest(this.activeTab.batchUrl, {
            method: "POST",
            body: batch
          });
          this.finishSaveProgress();
          this.showModal = false;
          this.refresh();
          this.showToast("保存成功");
        } catch (err) {
          this.failSaveProgress(err.message || "保存失败");
          this.showToast(err.message || "保存失败", "error");
        } finally {
          this.saving = false;
        }
        return;
      }
      for (const field of this.formFields) {
        if (!this.fieldVisible(field)) {
          continue;
        }
        let raw = this.form[field.key];
        if (field.type === "bountySteps") {
          const steps = this.normalizeBountySteps(raw);
          if (field.required && !steps.length) {
            this.showToast(`${field.label}不能为空`, "error");
            return;
          }
          if (!steps.length) {
            continue;
          }
          payload[field.key] = steps;
          continue;
        }
        if (this.currentTab === "categories" && field.key === "key") {
          const suggestedKey = suggestCategoryKey(raw) || suggestCategoryKey(this.form.name);
          if (suggestedKey) {
            raw = suggestedKey;
          } else if (!raw && this.form.name) {
            this.showToast("分类标识建议填写英文或拼音短码，例如 jetski、surf", "error");
          return;
          }
        }
        if (field.required && (!raw || (Array.isArray(raw) && raw.length === 0))) {
          this.showToast(`${field.label}不能为空`, "error");
          return;
        }
        if (!raw) {
          continue;
        }
        if (field.type === "number") {
          payload[field.key] = Number(raw);
        } else if (field.type === "select") {
          payload[field.key] = field.valueType === "number" ? Number(raw) : raw;
        } else if (field.type === "list") {
          payload[field.key] = parseList(raw);
        } else if (field.type === "images") {
          payload[field.key] = parseList(raw);
        } else if (field.key === "category") {
          payload[field.key] = this.categoryValue(raw);
        } else {
          payload[field.key] = raw;
        }
      }
      if (this.currentTab === "adminAccount") {
        const nextPassword = String(payload.password || "").trim();
        const confirmPassword = String(payload.confirmPassword || "").trim();
        if (nextPassword !== confirmPassword) {
          this.showToast("两次输入的新密码不一致", "error");
          return;
        }
      }
      if (this.currentTab === "welfareSubmissions") {
        if (!payload.status && this.editing && this.editing.status) {
          payload.status = this.editing.status;
        }
        if (payload.status === "rejected" && !String(payload.reviewNote || "").trim()) {
          this.showToast("驳回时请填写驳回原因", "error");
          return;
        }
        if (payload.status === "approved" && !(Number(payload.rewardAmount) > 0)) {
          this.showToast("通过时请填写奖励佣金", "error");
          return;
        }
      }
      try {
        this.saving = true;
        this.beginSaveProgress(this.countPendingUploadFiles());
        await this.applyPendingUploads(payload);
        this.enterSaveRequestProgress();
        this.sanitizeMediaPayload(payload);
        if (this.editing) {
          await AdminApi.apiRequest(this.activeTab.updateUrl(this.editing.id), {
            method: "PUT",
            body: payload
          });
        } else {
          await AdminApi.apiRequest(this.activeTab.createUrl, {
            method: "POST",
            body: payload
          });
        }
        this.finishSaveProgress();
        this.showModal = false;
        this.clearPendingUploads();
        if (this.currentTab === "adminAccount") {
          this.loginForm.username = payload.username || this.loginForm.username;
          this.loginForm.password = "";
        }
        this.refresh();
        this.showToast(this.editing ? "修改成功" : "新增成功");
      } catch (err) {
        this.failSaveProgress(err.message || "保存失败");
        this.showToast(err.message || "保存失败", "error");
      } finally {
        this.saving = false;
      }
    },
    async remove(row) {
      if (this.activeTab && this.activeTab.allowDelete === false) {
        return;
      }
      if (!confirm("确定要删除这条数据吗？")) {
        return;
      }
      try {
        await AdminApi.apiRequest(this.activeTab.deleteUrl(row.id), { method: "DELETE" });
        if (this.currentTab === "clubs") {
          this.options.clubs = [];
        }
        this.refresh();
        this.showToast("删除成功");
      } catch (err) {
        this.error = err.message || "删除失败";
      }
    },
    async refundOrder(row) {
      const orderNo = row && row.orderNo ? row.orderNo : `#${row.id}`;
      const amount = row && row.payAmount != null ? `￥${row.payAmount}` : "";
      const hint = amount ? `（金额 ${amount}）` : "";
      if (!confirm(`确认对订单 ${orderNo} 发起微信退款${hint}？`)) {
        return;
      }
      try {
        await AdminApi.apiRequest(`/api/admin/orders/${row.id}/refund`, {
          method: "POST",
          body: { reclaimCommission: true }
        });
        this.refresh();
        this.showToast("已发起微信退款");
      } catch (err) {
        const message = err.message || "退款失败";
        this.error = message;
        this.showToast(message, "error");
      }
    },
    async changeOrder(row, status) {
      if (status === "refund") {
        await this.refundOrder(row);
        return;
      }
      try {
        await AdminApi.apiRequest(`/api/admin/orders/${row.id}`, {
          method: "PUT",
          body: { status }
        });
        this.refresh();
        this.showToast("订单状态已更新");
      } catch (err) {
        const message = err.message || "操作失败";
        this.error = message;
        this.showToast(message, "error");
      }
    },
    openImport() {
      this.importMode = "table";
      this.importText = "";
      this.importTableText = "";
      this.importError = "";
      this.importResult = null;
      this.importPreview = [];
      this.importPreviewColumns = [];
      this.importClubId = "";
      if (this.activeTab && this.activeTab.key === "activities") {
        this.ensureOptions(["clubs"]);
      }
      this.showImport = true;
    },
    closeImport() {
      this.showImport = false;
    },
    async submitImport() {
      this.importError = "";
      this.importResult = null;
      let data = null;
      if (this.importMode === "json") {
        if (!this.importText) {
          this.importError = "请输入导入内容";
          return;
        }
        try {
          data = JSON.parse(this.importText);
        } catch (err) {
          this.importError = "JSON 格式错误";
          return;
        }
        if (!Array.isArray(data)) {
          this.importError = "需要 JSON 数组";
          return;
        }
      } else {
        if (this.activeTab && this.activeTab.key === "activities") {
          await this.ensureOptions(["clubs"]);
        }
        try {
          const parsed = this.parseImportTable(this.importTableText);
          data = parsed.rows;
        } catch (err) {
          this.importError = err.message || "表格解析失败";
          return;
        }
      }
      if (!Array.isArray(data) || data.length === 0) {
        this.importError = "没有可导入的数据";
        return;
      }
      try {
        this.saving = true;
        const result = await AdminApi.apiRequest(this.activeTab.batchUrl, {
          method: "POST",
          body: data
        });
        this.importResult = result;
        this.refresh();
        const summary = `导入完成：成功 ${result.success || 0} 条，失败 ${result.failed || 0} 条`;
        this.showToast(summary, result.failed ? "warning" : "success");
      } catch (err) {
        this.importError = err.message || "导入失败";
      } finally {
        this.saving = false;
      }
    }
  }
}).mount("#app");
