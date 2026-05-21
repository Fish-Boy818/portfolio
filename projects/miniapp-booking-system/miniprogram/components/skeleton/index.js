Component({
  properties: {
    count: {
      type: Number,
      value: 3
    },
    mode: {
      type: String,
      value: "card"
    }
  },
  data: {
    countList: []
  },
  observers: {
    count(count) {
      const safeCount = Math.max(1, Number(count) || 1);
      const list = Array.from({ length: safeCount }, (_, idx) => idx);
      this.setData({ countList: list });
    }
  },
  lifetimes: {
    attached() {
      const safeCount = Math.max(1, Number(this.data.count) || 1);
      const list = Array.from({ length: safeCount }, (_, idx) => idx);
      this.setData({ countList: list });
    }
  }
});