const { request } = require("../../utils/api");

function formatDateLabel(dateStr) {
  if (!dateStr) {
    return "";
  }
  const parts = dateStr.split("-");
  return `${parts[1]}/${parts[2]}`;
}

function getWeekday(dateStr) {
  const map = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
  const date = new Date(dateStr);
  return map[date.getDay()];
}

function parseDescMap(desc) {
  const map = {};
  if (!desc) {
    return map;
  }
  String(desc)
    .split("|")
    .map((part) => part.trim())
    .filter(Boolean)
    .forEach((part) => {
      const pieces = part.split(/[:：]/);
      if (pieces.length > 1) {
        map[pieces[0].trim()] = pieces.slice(1).join(":").trim();
      }
    });
  return map;
}

function normalizeDate(value) {
  if (!value) {
    return "";
  }
  const match = String(value).match(/\d{4}[./-]\d{1,2}[./-]\d{1,2}/);
  if (!match) {
    return "";
  }
  const parts = match[0].split(/[./-]/);
  return `${parts[0]}-${parts[1].padStart(2, "0")}-${parts[2].padStart(2, "0")}`;
}

function extractDateList(text) {
  if (!text) {
    return [];
  }
  const matches = String(text).match(/\d{4}[./-]\d{1,2}[./-]\d{1,2}/g) || [];
  return matches.map(normalizeDate).filter(Boolean);
}

function hasRangeSeparator(text) {
  if (!text) return false;
  return /至|~|到/.test(text);
}

function parseDateRange(text) {
  const list = extractDateList(text);
  if (list.length < 2) {
    return null;
  }
  const first = list[0];
  const last = list[list.length - 1];
  if (hasRangeSeparator(text)) {
    return { start: first, end: last };
  }
  return null;
}

function parseDaysSpan(text) {
  if (!text) return null;
  const match = String(text).match(/延\s*(\d+)\s*天/);
  if (match) {
    return Number(match[1]);
  }
  const loose = String(text).match(/(\d+)\s*天/);
  return loose ? Number(loose[1]) : null;
}

function isNoLimit(text) {
  if (!text) return true;
  return /(所有日期均可用|不限|不限制|长期有效|随时)/.test(text);
}

function isDateAllowedWithRule(rule, date) {
  if (!rule || !date) {
    return true;
  }
  if (date < rule.startDate || date > rule.endDate) {
    return false;
  }
  if (rule.excludedDates && rule.excludedDates.includes(date)) {
    return false;
  }
  if (rule.blockedRanges && rule.blockedRanges.length) {
    return !rule.blockedRanges.some((range) => date >= range.start && date <= range.end);
  }
  return true;
}

Page({
  data: {
    productId: 1,
    status: "loading",
    errorMessage: "加载失败，请稍后重试",
    dates: [],
    slots: [],
    allSlots: [],
    activeDateIndex: 0,
    activeSlotIndex: -1,
    activeSlotId: null,
    selectedTip: "未选择出发时段",
    ctaDisabled: true,
    simulateError: false,
    manualMode: false,
    manualDate: "",
    manualTime: "",
    manualStartDate: "",
    manualEndDate: "",
    manualRule: null
  },
  onLoad(options) {
    const id = Number(options.id) || 1;
    this.setData({ productId: id }, () => {
      this.loadData();
    });
  },
  getToday() {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, "0");
    const d = String(now.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
  },
  getFutureDate(days) {
    const now = new Date();
    now.setDate(now.getDate() + days);
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, "0");
    const d = String(now.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
  },
  buildManualRule(desc) {
    const map = parseDescMap(desc);
    const usable = map["可用日期"] || "";
    const unusable = map["不可用日期"] || "";
    const today = this.getToday();
    const tomorrow = this.getFutureDate(1);
    const rule = {
      startDate: "",
      endDate: "",
      excludedDates: [],
      blockedRanges: []
    };
    if (usable && !isNoLimit(usable)) {
      const range = parseDateRange(usable);
      if (range) {
        rule.startDate = range.start;
        rule.endDate = range.end;
      } else {
        const days = parseDaysSpan(usable);
        if (days) {
          const includeToday = /购买当日|当日默认可用|当天可用/.test(usable);
          rule.startDate = includeToday ? today : tomorrow;
          rule.endDate = this.getFutureDate(days);
        }
      }
    }
    if (unusable && !isNoLimit(unusable)) {
      const list = extractDateList(unusable);
      if (list.length) {
        rule.excludedDates = list;
      }
      const range = parseDateRange(unusable);
      if (range && !rule.blockedRanges.length) {
        rule.blockedRanges = [range];
      }
    }
    if (!rule.startDate) {
      rule.startDate = today;
    }
    if (!rule.endDate) {
      rule.endDate = this.getFutureDate(30);
    }
    if (rule.endDate < rule.startDate) {
      rule.endDate = rule.startDate;
    }
    return rule;
  },
  isDateAllowed(date) {
    return isDateAllowedWithRule(this.data.manualRule, date);
  },
  loadData() {
    this.setData({ status: "loading" });
    Promise.all([
      request({ url: `/api/activities/${this.data.productId}` }),
      request({ url: `/api/activities/${this.data.productId}/slots` })
    ])
      .then(([activity, slots]) => {
        const list = slots || [];
        const rule = this.buildManualRule(activity ? activity.description : "");
        const descMap = parseDescMap(activity ? activity.description : "");
        const hasRule = !!(descMap["可用日期"] || descMap["不可用日期"]);
        const isAutoSlots =
          list.length > 0 &&
          list.every((slot) => (slot.capacity || 0) >= 9999 && (slot.booked || 0) === 0);
        if (hasRule || !list.length || isAutoSlots) {
          this.setData({
            status: "ready",
            dates: [],
            slots: [],
            allSlots: [],
            manualMode: true,
            manualDate: rule.startDate,
            manualTime: "09:00",
            manualStartDate: rule.startDate,
            manualEndDate: rule.endDate,
            manualRule: rule,
            selectedTip: "请选择时间",
            ctaDisabled: false
          });
          return;
        }
        const filteredList = hasRule
          ? list.filter((slot) => isDateAllowedWithRule(rule, slot.slotDate))
          : list;
        if (hasRule && !filteredList.length) {
          this.setData({
            status: "ready",
            dates: [],
            slots: [],
            allSlots: [],
            manualMode: true,
            manualDate: rule.startDate,
            manualTime: "09:00",
            manualStartDate: rule.startDate,
            manualEndDate: rule.endDate,
            manualRule: rule,
            selectedTip: "请选择时间",
            ctaDisabled: false
          });
          return;
        }
        const dateMap = {};
        filteredList.forEach((slot) => {
          if (!dateMap[slot.slotDate]) {
            dateMap[slot.slotDate] = [];
          }
          dateMap[slot.slotDate].push(slot);
        });
        const dates = Object.keys(dateMap)
          .sort()
          .map((date) => ({
            label: formatDateLabel(date),
            value: date,
            week: getWeekday(date)
          }));
        const activeDate = dates[0] ? dates[0].value : "";
        const slotsForDate = (dateMap[activeDate] || []).map((slot) => ({
          id: slot.id,
          time: slot.slotTime,
          left: slot.remaining,
          available: slot.status === "active" && slot.remaining > 0
        }));
        const hasAvailable = slotsForDate.some((slot) => slot.available);
        this.setData({
          status: hasAvailable ? "ready" : "empty",
          dates,
          slots: slotsForDate,
          allSlots: filteredList,
          activeDateIndex: 0,
          activeSlotIndex: -1,
          activeSlotId: null,
          selectedTip: "未选择出发时段",
          ctaDisabled: true,
          manualMode: false,
          manualRule: rule
        });
      })
      .catch((err) => {
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
      });
  },
  onSelectDate(e) {
    const index = e.currentTarget.dataset.index;
    const date = this.data.dates[index] ? this.data.dates[index].value : "";
    const slotsForDate = this.data.allSlots
      .filter((slot) => slot.slotDate === date)
      .map((slot) => ({
        id: slot.id,
        time: slot.slotTime,
        left: slot.remaining,
        available: slot.status === "active" && slot.remaining > 0
      }));
    const hasAvailable = slotsForDate.some((slot) => slot.available);
    this.setData({
      activeDateIndex: index,
      activeSlotIndex: -1,
      activeSlotId: null,
      slots: slotsForDate,
      selectedTip: "未选择出发时段",
      ctaDisabled: !hasAvailable
    });
  },
  onSelectSlot(e) {
    const index = e.currentTarget.dataset.index;
    const available = e.currentTarget.dataset.available;
    if (!available) {
      return;
    }
    const slot = this.data.slots[index];
    this.setData({
      activeSlotIndex: index,
      activeSlotId: slot ? slot.id : null,
      selectedTip: slot ? slot.time : "未选择出发时段",
      ctaDisabled: false
    });
  },
  onPickDate(e) {
    const date = e.detail.value;
    if (!this.isDateAllowed(date)) {
      wx.showToast({ title: "该日期不可用", icon: "none" });
      return;
    }
    this.setData({ manualDate: date, selectedTip: `${date} ${this.data.manualTime || ""}`.trim() });
  },
  onPickTime(e) {
    const time = e.detail.value;
    this.setData({ manualTime: time, selectedTip: `${this.data.manualDate || ""} ${time}`.trim() });
  },
  onConfirm() {
    const slotIndex = this.data.activeSlotIndex;
    if (this.data.manualMode) {
      const date = this.data.manualDate;
      const time = this.data.manualTime;
      if (!date || !time) {
        wx.showToast({ title: "请选择出发时间", icon: "none" });
        return;
      }
      if (!this.isDateAllowed(date)) {
        wx.showToast({ title: "该日期不可用", icon: "none" });
        return;
      }
      const id = this.data.productId || 1;
      request({ url: `/api/activities/${id}/slots`, data: { date } })
        .then((slots) => {
          const exist = (slots || []).find((item) => item.slotTime === time);
          if (exist) {
            return exist;
          }
          return request({
            url: `/api/activities/${id}/slots`,
            method: "POST",
            data: { slotDate: date, slotTime: time, capacity: 9999 }
          });
        })
        .then((slot) => {
          const slotId = slot && slot.id ? slot.id : null;
          if (!slotId) {
            wx.showToast({ title: "创建时段失败", icon: "none" });
            return;
          }
          const url = `/pages/order-confirm/index?id=${id}&slotId=${slotId}&date=${encodeURIComponent(date)}&slot=${encodeURIComponent(time)}`;
          wx.navigateTo({ url });
        })
        .catch((err) => {
          wx.showToast({ title: String(err || "创建时段失败"), icon: "none" });
        });
      return;
    }
    if (slotIndex < 0) {
      wx.showToast({ title: "请选择出发时段", icon: "none" });
      return;
    }
    const date = this.data.dates[this.data.activeDateIndex].value;
    const slot = this.data.slots[slotIndex].time;
    const slotId = this.data.activeSlotId;
    const id = this.data.productId || 1;
    const url = `/pages/order-confirm/index?id=${id}&slotId=${slotId}&date=${encodeURIComponent(date)}&slot=${encodeURIComponent(slot)}`;
    wx.navigateTo({ url });
  },
  onRetry() {
    this.loadData();
  },
  onGoBack() {
    wx.navigateBack();
  }
});
