Component({
  properties: {
    value: {
      type: String,
      value: ""
    },
    placeholder: {
      type: String,
      value: "搜索"
    },
    showAction: {
      type: Boolean,
      value: true
    }
  },
  methods: {
    onInput(e) {
      this.triggerEvent("input", { value: e.detail.value });
    },
    onConfirm() {
      this.triggerEvent("search");
    },
    onSearch() {
      this.triggerEvent("search");
    },
    onClear() {
      this.triggerEvent("input", { value: "" });
      this.triggerEvent("clear");
    }
  }
});
