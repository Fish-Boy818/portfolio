const user = {
  name: "海浪会员",
  phone: "138 **** 2233",
  totalSpend: 1520
};

const USER_STORAGE_KEY = "user";
const ORDERS_STORAGE_KEY = "orders";

function safeGetStorage(key) {
  if (typeof wx !== "undefined" && wx.getStorageSync) {
    return wx.getStorageSync(key);
  }
  return null;
}

function safeSetStorage(key, value) {
  if (typeof wx !== "undefined" && wx.setStorageSync) {
    wx.setStorageSync(key, value);
  }
}

function loadUser() {
  const stored = safeGetStorage(USER_STORAGE_KEY);
  if (stored && typeof stored.totalSpend === "number") {
    return stored;
  }
  const seed = { ...user };
  safeSetStorage(USER_STORAGE_KEY, seed);
  return seed;
}

function saveUser(nextUser) {
  const value = { ...nextUser };
  safeSetStorage(USER_STORAGE_KEY, value);
  return value;
}

function loadOrders() {
  const stored = safeGetStorage(ORDERS_STORAGE_KEY);
  if (Array.isArray(stored) && stored.length) {
    return stored;
  }
  const seed = orders.slice();
  safeSetStorage(ORDERS_STORAGE_KEY, seed);
  return seed;
}

function saveOrders(list) {
  const next = Array.isArray(list) ? list.slice() : [];
  safeSetStorage(ORDERS_STORAGE_KEY, next);
  return next;
}

function getTotalSpendFromOrders(list) {
  return list.reduce((sum, order) => {
    if (order.status !== "used") {
      return sum;
    }
    return sum + (Number(order.payAmount) || 0);
  }, 0);
}

function getUserSnapshot() {
  const base = loadUser();
  const list = loadOrders();
  const totalSpend = getTotalSpendFromOrders(list);
  return {
    ...base,
    totalSpend
  };
}

const clubs = [
  {
    id: 1,
    name: "蓝鲸海上俱乐部",
    location: "三亚 · 亚龙湾",
    address: "三亚市亚龙湾海边码头",
    lat: 18.2278,
    lng: 109.7484,
    phone: "13312340001",
    tags: ["摩托艇", "冲浪", "潜水"],
    coverClass: "cover-a",
    cover: "/assets/images/clubs/club-1.png",
    gallery: [
      "/assets/images/clubs/club-1.png",
      "/assets/images/clubs/club-2.png",
      "/assets/images/clubs/club-3.png"
    ]
  },
  {
    id: 2,
    name: "珊瑚湾冲浪基地",
    location: "厦门 · 白城",
    address: "厦门市白城沙滩入口",
    lat: 24.4305,
    lng: 118.0915,
    phone: "13312340002",
    tags: ["冲浪", "桨板", "日落"],
    coverClass: "cover-b",
    cover: "/assets/images/clubs/club-2.png",
    gallery: [
      "/assets/images/clubs/club-2.png",
      "/assets/images/clubs/club-4.png",
      "/assets/images/clubs/club-6.png"
    ]
  },
  {
    id: 3,
    name: "疾风摩托艇中心",
    location: "青岛 · 石老人",
    address: "青岛市石老人海滨",
    lat: 36.0806,
    lng: 120.4969,
    phone: "13312340003",
    tags: ["摩托艇", "拖拽伞", "教练"],
    coverClass: "cover-c",
    cover: "/assets/images/clubs/club-3.png",
    gallery: [
      "/assets/images/clubs/club-3.png",
      "/assets/images/clubs/club-5.png",
      "/assets/images/clubs/club-7.png"
    ]
  },
  {
    id: 4,
    name: "日出码头水上会",
    location: "珠海 · 横琴",
    address: "珠海市横琴口岸码头",
    lat: 22.1171,
    lng: 113.5381,
    phone: "13312340004",
    tags: ["香蕉船", "亲子", "沙滩"],
    coverClass: "cover-d",
    cover: "/assets/images/clubs/club-4.png",
    gallery: [
      "/assets/images/clubs/club-4.png",
      "/assets/images/clubs/club-1.png",
      "/assets/images/clubs/club-8.png"
    ]
  },
  {
    id: 5,
    name: "银贝海洋运动",
    location: "北海 · 银滩",
    address: "北海市银滩东区",
    lat: 21.4436,
    lng: 109.1553,
    phone: "13312340005",
    tags: ["摩托艇", "滑水", "摄影"],
    coverClass: "cover-e",
    cover: "/assets/images/clubs/club-5.png",
    gallery: [
      "/assets/images/clubs/club-5.png",
      "/assets/images/clubs/club-6.png",
      "/assets/images/clubs/club-9.png"
    ]
  },
  {
    id: 6,
    name: "潮汐港运动站",
    location: "深圳 · 大鹏",
    address: "深圳市大鹏新区金沙湾",
    lat: 22.5902,
    lng: 114.4871,
    phone: "13312340006",
    tags: ["冲浪", "潜水", "营地"],
    coverClass: "cover-f",
    cover: "/assets/images/clubs/club-6.png",
    gallery: [
      "/assets/images/clubs/club-6.png",
      "/assets/images/clubs/club-2.png",
      "/assets/images/clubs/club-10.png"
    ]
  },
  {
    id: 7,
    name: "海风沙滩俱乐部",
    location: "湛江 · 南三岛",
    address: "湛江市南三岛海岸",
    lat: 21.0464,
    lng: 110.5147,
    phone: "13312340007",
    tags: ["香蕉船", "摩托艇", "团建"],
    coverClass: "cover-g",
    cover: "/assets/images/clubs/club-7.png",
    gallery: [
      "/assets/images/clubs/club-7.png",
      "/assets/images/clubs/club-3.png",
      "/assets/images/clubs/club-8.png"
    ]
  },
  {
    id: 8,
    name: "浪屿水上营地",
    location: "宁波 · 象山",
    address: "宁波市象山松兰山",
    lat: 29.4834,
    lng: 121.9191,
    phone: "13312340008",
    tags: ["桨板", "冲浪", "露营"],
    coverClass: "cover-h",
    cover: "/assets/images/clubs/club-8.png",
    gallery: [
      "/assets/images/clubs/club-8.png",
      "/assets/images/clubs/club-6.png",
      "/assets/images/clubs/club-9.png"
    ]
  },
  {
    id: 9,
    name: "深蓝潜行中心",
    location: "海南 · 万宁",
    address: "海南省万宁市日月湾",
    lat: 18.7942,
    lng: 110.4223,
    phone: "13312340009",
    tags: ["潜水", "冲浪", "摄影"],
    coverClass: "cover-i",
    cover: "/assets/images/clubs/club-9.png",
    gallery: [
      "/assets/images/clubs/club-9.png",
      "/assets/images/clubs/club-4.png",
      "/assets/images/clubs/club-10.png"
    ]
  },
  {
    id: 10,
    name: "阳光海上运动",
    location: "广州 · 南沙",
    address: "广州市南沙湾码头",
    lat: 22.7826,
    lng: 113.6096,
    phone: "13312340010",
    tags: ["摩托艇", "香蕉船", "日落"],
    coverClass: "cover-j",
    cover: "/assets/images/clubs/club-10.png",
    gallery: [
      "/assets/images/clubs/club-10.png",
      "/assets/images/clubs/club-5.png",
      "/assets/images/clubs/club-1.png"
    ]
  }
];

const activityTemplates = [
  {
    category: "jetski",
    title: "摩托艇体验",
    subtitle: "极速体验",
    basePrice: 260,
    audience: "需穿救生衣",
    desc: "极速冲浪体验，教练全程陪同并提供安全装备。"
  },
  {
    category: "jetski",
    title: "摩托艇巡航",
    subtitle: "海湾巡游",
    basePrice: 240,
    audience: "适合新手",
    desc: "沿海岸线巡航，风景优美，适合第一次体验。"
  },
  {
    category: "surf",
    title: "冲浪课程",
    subtitle: "私教教学",
    basePrice: 420,
    audience: "建议有基础",
    desc: "包含基础动作教学与板具使用指导，教练一对一带练。"
  },
  {
    category: "surf",
    title: "冲浪进阶",
    subtitle: "进阶训练",
    basePrice: 480,
    audience: "需有基础",
    desc: "进阶技巧训练，适合有冲浪经验的玩家。"
  },
  {
    category: "banana",
    title: "香蕉船欢乐",
    subtitle: "团体畅玩",
    basePrice: 180,
    audience: "亲子推荐",
    desc: "欢乐水上项目，适合家庭或朋友结伴体验。"
  },
  {
    category: "banana",
    title: "香蕉船团建",
    subtitle: "团队项目",
    basePrice: 220,
    audience: "团建推荐",
    desc: "团队出游项目，含安全讲解与统一装备。"
  }
];

const durations = ["20分钟", "30分钟", "40分钟", "60分钟", "90分钟"];
const timeSlots = [
  "09:00 - 09:30",
  "10:30 - 11:10",
  "13:00 - 13:40",
  "15:00 - 15:40",
  "17:00 - 17:40"
];

function createRandom(seed) {
  let value = seed;
  return function rand() {
    value = (value * 9301 + 49297) % 233280;
    return value / 233280;
  };
}

const rand = createRandom(202406);

function createActivities() {
  const list = [];
  let id = 1;
  clubs.forEach((club, clubIndex) => {
    for (let i = 0; i < 10; i += 1) {
      const template = activityTemplates[Math.floor(rand() * activityTemplates.length)];
      const duration = durations[Math.floor(rand() * durations.length)];
      const priceBase = template.basePrice + Math.round(rand() * 160);
      const price = Math.round(priceBase / 10) * 10;
      const original = price + 40 + Math.round(rand() * 80);
      const stock = rand() > 0.12 ? Math.ceil(rand() * 9) : 0;
      const status = stock > 0 ? "可预约" : "已售罄";
      const badgeClass = stock > 0 ? "badge--warning" : "badge--danger";
      const tip = "活动价";
      const expireDay = 10 + Math.floor(rand() * 20);
      const coverIndex = ((clubIndex + i) % 10) + 1;
      const cover = `/assets/images/activities/activity-${coverIndex}.png`;
      const gallery = [
        cover,
        `/assets/images/activities/activity-${(coverIndex % 10) + 1}.png`,
        `/assets/images/activities/activity-${((coverIndex + 1) % 10) + 1}.png`
      ];
      list.push({
        id: id,
        clubId: club.id,
        title: `${template.title} · ${duration}`,
        subtitle: `${template.subtitle} · ${club.name}`,
        status,
        badgeClass,
        price,
        original,
        priceLevel: 1,
        tip,
        mediaClass: "",
        category: template.category,
        store: club.location,
        expire: `2026-07-${String(expireDay).padStart(2, "0")}`,
        times: timeSlots[Math.floor(rand() * timeSlots.length)],
        stock,
        audience: template.audience,
        desc: template.desc,
        cover,
        gallery
      });
      id += 1;
    }
  });
  return list;
}

const items = createActivities();

const orders = [
  {
    id: 1,
    no: "202606080001",
    status: "paid",
    statusText: "已支付",
    badgeClass: "badge--info",
    title: "摩托艇体验 · 30分钟",
    time: "2026-06-01 11:20",
    price: 299,
    original: 359,
    priceLevel: 1,
    tip: "活动价",
    mediaClass: "",
    primaryText: "去核销",
    secondaryText: "查看详情",
    store: "蓝湾码头",
    date: "2026-06-08",
    slot: "10:30 - 11:00",
    verifyCode: "8932 2211",
    quantity: 1,
    payAmount: 299,
    canVerify: true
  },
  {
    id: 2,
    no: "202606020002",
    status: "unpaid",
    statusText: "待支付",
    badgeClass: "badge--warning",
    title: "冲浪课程 · 双人",
    time: "2026-06-01 18:10",
    price: 499,
    original: 599,
    priceLevel: 1,
    tip: "活动价",
    mediaClass: "thumb-alt",
    primaryText: "立即支付",
    secondaryText: "取消订单",
    store: "珊瑚湾基地",
    date: "2026-06-09",
    slot: "15:00 - 16:30",
    verifyCode: "",
    quantity: 1,
    payAmount: 499,
    canVerify: false
  },
  {
    id: 3,
    no: "202605280003",
    status: "used",
    statusText: "已核销",
    badgeClass: "badge--success",
    title: "海上香蕉船 · 4人",
    time: "2026-05-28 09:40",
    price: 260,
    original: 320,
    priceLevel: 1,
    tip: "活动价",
    mediaClass: "",
    primaryText: "查看详情",
    secondaryText: "",
    store: "海风沙滩",
    date: "2026-06-05",
    slot: "11:30 - 12:00",
    verifyCode: "8201 7644",
    quantity: 1,
    payAmount: 260,
    canVerify: false,
    verifiedAt: "2026-06-05 11:58"
  }
];

function getItemById(id) {
  return items.find((item) => item.id === id) || null;
}

function getOrderById(id) {
  return orders.find((order) => order.id === id) || null;
}

function getClubById(id) {
  return clubs.find((club) => club.id === id) || null;
}

function getUserLevel() {
  return 1;
}

function getRemain() {
  return 0;
}

function applyPriceForLevel(item) {
  if (!item) {
    return null;
  }
  return {
    ...item,
    priceLevel: 1,
    tip: "活动价"
  };
}

function getUserLevelFromUser() {
  return 1;
}

function mapItemsForUser(list) {
  return list.map((item) => applyPriceForLevel(item));
}

function getItemByIdForUser(id) {
  const item = getItemById(id);
  if (!item) {
    return null;
  }
  return applyPriceForLevel(item);
}

module.exports = {
  user,
  loadUser,
  saveUser,
  loadOrders,
  saveOrders,
  getUserSnapshot,
  getTotalSpendFromOrders,
  clubs,
  items,
  orders,
  getItemById,
  getItemByIdForUser,
  getOrderById,
  getClubById,
  getUserLevel,
  getUserLevelFromUser,
  getRemain,
  applyPriceForLevel,
  mapItemsForUser
};
