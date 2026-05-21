(function () {
  const state = {
    activities: [],
    clubs: [],
    banners: [],
    siteActivityLimit: 6
  };

  const entry = document.getElementById("entry");
  const home = document.getElementById("home");
  const entryImage = document.getElementById("entryImage");
  const enterButton = document.getElementById("enterButton");
  const heroTitle = document.getElementById("heroTitle");
  const heroSubtitle = document.getElementById("heroSubtitle");
  const heroMedia = document.getElementById("heroMedia");
  const clubList = document.getElementById("clubList");
  const activityList = document.getElementById("activityList");
  const emptyState = document.getElementById("emptyState");
  const searchInput = document.getElementById("searchInput");
  const licenseModal = document.getElementById("licenseModal");
  const licenseTitle = document.getElementById("licenseTitle");
  const licenseBody = document.getElementById("licenseBody");

  function normalizeUrl(url) {
    if (!url) {
      return "";
    }
    const value = String(url).trim();
    if (!value) {
      return "";
    }
    if (value.indexOf("http://example.com") === 0) {
      return value.replace("http://", "https://");
    }
    if (value.charAt(0) === "/") {
      return value;
    }
    return value;
  }

  function isVideo(url) {
    const clean = normalizeUrl(url).split("?")[0].split("#")[0].toLowerCase();
    return [".mp4", ".mov", ".m4v", ".webm"].some((suffix) => clean.endsWith(suffix));
  }

  function imageStyle(url) {
    const normalized = normalizeUrl(url);
    if (!normalized || isVideo(normalized)) {
      return "";
    }
    return `background-image: url("${normalized}")`;
  }

  function imageTag(url, alt) {
    const normalized = normalizeUrl(url);
    if (!normalized || isVideo(normalized)) {
      return '<div class="club-card__placeholder">暂无图片</div>';
    }
    return `<img src="${escapeHtml(normalized)}" alt="${escapeHtml(alt)}" loading="lazy">`;
  }

  function text(value, fallback) {
    const result = value === null || value === undefined ? "" : String(value).trim();
    return result || fallback || "";
  }

  function escapeHtml(value) {
    return text(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function money(value) {
    const num = Number(value);
    if (!Number.isFinite(num)) {
      return "";
    }
    return num % 1 === 0 ? String(num) : num.toFixed(2);
  }

  function request(path) {
    return fetch(path, { credentials: "same-origin" })
      .then((res) => {
        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`);
        }
        return res.json();
      })
      .then((json) => (json && Array.isArray(json.data) ? json.data : []));
  }

  function requestOne(path) {
    return fetch(path, { credentials: "same-origin" })
      .then((res) => {
        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`);
        }
        return res.json();
      })
      .then((json) => (json && json.data && typeof json.data === "object" ? json.data : {}));
  }

  function normalizeLimit(value) {
    const num = Number(value);
    if (!Number.isFinite(num) || num < 1) {
      return 6;
    }
    return Math.min(Math.floor(num), 50);
  }

  function pickImage() {
    const banner = state.banners.find((item) => item && item.imageUrl && !isVideo(item.imageUrl));
    if (banner) {
      return banner.imageUrl;
    }
    const activity = state.activities.find((item) => item && item.cover);
    if (activity) {
      return activity.cover;
    }
    const club = state.clubs.find((item) => item && item.cover);
    return club ? club.cover : "";
  }

  function renderHero() {
    const banner = state.banners.find((item) => item && !isVideo(item.imageUrl)) || state.banners[0] || {};
    const image = pickImage();
    heroTitle.textContent = text(banner.title, "海上俱乐部 · 夏季畅玩");
    heroSubtitle.textContent = text(banner.subtitle, "冲浪、浮游、电动冲浪板等项目展示。");
    const style = imageStyle(image);
    if (style) {
      entryImage.setAttribute("style", style);
      heroMedia.setAttribute("style", style);
    }
  }

  function renderClubs() {
    const list = state.clubs.slice(0, 6);
    clubList.innerHTML = list.map((club) => {
      const tags = Array.isArray(club.tags) ? club.tags.slice(0, 3) : [];
      const name = text(club.name, "海上俱乐部");
      const licenseImage = normalizeUrl(club.licenseImage);
      return `
        <article class="club-card" role="button" tabindex="0" data-license-image="${escapeHtml(licenseImage)}" data-club-name="${escapeHtml(name)}">
          <div class="club-card__image">${imageTag(club.cover, name)}</div>
          <div class="club-card__body">
            <h3>${escapeHtml(name)}</h3>
            <p class="meta">${escapeHtml(text(club.location || club.address, "三亚"))}</p>
            <div class="tag-row">${tags.map((tag) => `<span class="tag">${escapeHtml(tag)}</span>`).join("")}</div>
            <p class="club-card__hint">${licenseImage ? "点击查看营业执照" : "营业执照待上传"}</p>
          </div>
        </article>
      `;
    }).join("");
  }

  function openLicenseModal(name, licenseImage) {
    licenseTitle.textContent = `${text(name, "俱乐部")}营业执照`;
    const image = normalizeUrl(licenseImage);
    if (image) {
      licenseBody.innerHTML = `<img src="${escapeHtml(image)}" alt="${escapeHtml(licenseTitle.textContent)}">`;
    } else {
      licenseBody.innerHTML = '<div class="license-modal__empty">该俱乐部营业执照图片待上传。</div>';
    }
    licenseModal.classList.remove("is-hidden");
    licenseModal.setAttribute("aria-hidden", "false");
  }

  function closeLicenseModal() {
    licenseModal.classList.add("is-hidden");
    licenseModal.setAttribute("aria-hidden", "true");
    licenseBody.innerHTML = "";
  }

  function renderActivities(keyword) {
    const key = text(keyword).toLowerCase();
    const list = state.activities
      .filter((item) => item && item.status === "active")
      .filter((item) => {
        if (!key) {
          return true;
        }
        return [item.title, item.clubName, item.category, item.description].some((field) =>
          text(field).toLowerCase().includes(key)
        );
      })
      .slice(0, state.siteActivityLimit);

    activityList.innerHTML = list.map((item) => {
      const price = money(item.price);
      const gallery = Array.isArray(item.gallery) ? item.gallery : [];
      const cover = text(item.cover || item.imageUrl || gallery[0]);
      const title = text(item.title, "精选活动");
      return `
        <article class="activity-card">
          <div class="activity-card__image">${imageTag(cover, title)}</div>
          <div class="activity-card__body">
            <h3>${escapeHtml(title)}</h3>
            <p class="meta">${escapeHtml(text(item.clubName, "御乾推荐"))} · ${escapeHtml(text(item.clubLocation, "三亚"))}</p>
            <div class="price">${price ? `¥${price}` : "到店咨询"}</div>
            <div class="tag-row"><span class="tag">展示中</span><span class="tag">小程序内查看</span></div>
          </div>
        </article>
      `;
    }).join("");

    emptyState.classList.toggle("is-hidden", list.length > 0);
  }

  function render() {
    renderHero();
    renderClubs();
    renderActivities(searchInput.value);
  }

  function enterHome() {
    entry.classList.add("is-hidden");
    home.classList.remove("is-hidden");
    window.scrollTo({ top: 0, behavior: "auto" });
  }

  function bindEvents() {
    entry.addEventListener("click", enterHome);
    entry.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        enterHome();
      }
    });
    enterButton.addEventListener("click", (event) => {
      event.stopPropagation();
      enterHome();
    });
    searchInput.addEventListener("input", () => renderActivities(searchInput.value));
    clubList.addEventListener("click", (event) => {
      const card = event.target.closest(".club-card");
      if (!card) {
        return;
      }
      openLicenseModal(card.dataset.clubName, card.dataset.licenseImage);
    });
    clubList.addEventListener("keydown", (event) => {
      if (event.key !== "Enter" && event.key !== " ") {
        return;
      }
      const card = event.target.closest(".club-card");
      if (!card) {
        return;
      }
      event.preventDefault();
      openLicenseModal(card.dataset.clubName, card.dataset.licenseImage);
    });
    licenseModal.addEventListener("click", (event) => {
      if (event.target.closest("[data-close-license]")) {
        closeLicenseModal();
      }
    });
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && !licenseModal.classList.contains("is-hidden")) {
        closeLicenseModal();
      }
    });
  }

  function load() {
    Promise.all([
      request("/api/banners").catch(() => []),
      request("/api/clubs").catch(() => []),
      request("/api/activities").catch(() => []),
      requestOne("/api/profile-content").catch(() => ({}))
    ]).then(([banners, clubs, activities, profileContent]) => {
      state.banners = banners;
      state.clubs = clubs;
      state.activities = activities;
      state.siteActivityLimit = normalizeLimit(profileContent.siteActivityLimit);
      render();
    }).catch(() => {
      render();
    });
  }

  bindEvents();
  load();
}());
