Component({
  properties: {
    club: {
      type: Object,
      value: {}
    },
    variant: {
      type: String,
      value: "default"
    }
  },
  data: {
    safeClub: {}
  },
  observers: {
    club(value) {
      const sanitize = (input) => {
        if (input === null || input === undefined) return "";
        const text = String(input).trim();
        if (!text || text === "null" || text === "undefined") return "";
        return text;
      };
      const rawTags = Array.isArray(value && value.tags) ? value.tags : [];
      const tags = rawTags.map(sanitize).filter(Boolean).slice(0, 2);
      const openTime = sanitize(value && (value.openTime || value.open_time || value.openingHours));
      this.setData({
        safeClub: {
          ...value,
          name: sanitize(value && value.name),
          location: sanitize(value && value.location),
          openTime,
          tags
        }
      });
    }
  },
  methods: {
    onTap(e) {
      const id = Number(e && e.currentTarget && e.currentTarget.dataset ? e.currentTarget.dataset.id : 0);
      if (!Number.isFinite(id) || id <= 0) {
        return;
      }
      this.triggerEvent("tap", { id });
    }
  }
});
