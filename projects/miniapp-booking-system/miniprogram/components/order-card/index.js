Component({
  properties: {
    order: {
      type: Object,
      value: {}
    }
  },
  methods: {
    onPrimary(e) {
      const id = e.currentTarget.dataset.id;
      this.triggerEvent("primary", { id });
    },
    onSecondary(e) {
      const id = e.currentTarget.dataset.id;
      this.triggerEvent("secondary", { id });
    }
  }
});
