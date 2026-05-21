const { request } = require("../../utils/api");

const CONTENT_SCENE = {
  notice: {
    navTitle: "公告",
    emptyText: "暂无公告内容"
  },
  about: {
    navTitle: "关于我们",
    emptyText: "暂无关于我们内容"
  }
};

function normalizeScene(value) {
  const text = String(value || "").trim().toLowerCase();
  if (text === "about") {
    return "about";
  }
  return "notice";
}

function sanitizeText(value) {
  if (value === null || value === undefined) {
    return "";
  }
  return String(value).trim();
}

function formatDate(value) {
  if (!value) {
    return "";
  }
  return String(value).replace("T", " ").replace(/\.\d+$/, "");
}

Page({
  data: {
    status: "loading",
    errorMessage: "加载失败，请稍后重试",
    scene: "notice",
    navTitle: "公告",
    heading: "平台公告",
    emptyText: "暂无公告内容",
    contentText: "",
    updatedAtText: ""
  },
  onLoad(options) {
    const scene = normalizeScene(options && options.type);
    const sceneConfig = CONTENT_SCENE[scene];
    const heading = scene === "notice" ? "平台公告" : "关于我们";
    this.setData(
      {
        scene,
        navTitle: sceneConfig.navTitle,
        heading,
        emptyText: sceneConfig.emptyText
      },
      () => this.loadData()
    );
  },
  loadData() {
    const scene = this.data.scene;
    this.setData({ status: "loading" });
    request({ url: "/api/profile-content" })
      .then((content) => {
        const data = content || {};
        const heading = scene === "notice"
          ? sanitizeText(data.noticeTitle) || "平台公告"
          : "关于我们";
        const contentText = scene === "notice"
          ? sanitizeText(data.noticeContent)
          : sanitizeText(data.aboutUsContent);
        this.setData({
          status: "ready",
          heading,
          contentText,
          updatedAtText: formatDate(data.updatedAt)
        });
      })
      .catch((err) => {
        this.setData({
          status: "error",
          errorMessage: String(err || "加载失败，请稍后重试")
        });
      });
  },
  onRetry() {
    this.loadData();
  }
});
