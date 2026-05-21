const API_BASE = "";
const TOKEN_KEY = "admin_token";

function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token || "");
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

function parseJsonText(text) {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch (e) {
    return null;
  }
}

async function parseResponse(response) {
  const text = await response.text();
  return { text: text || "", body: parseJsonText(text) };
}

function buildErrorMessage(response, parsed, fallback) {
  const status = response && response.status ? response.status : 0;
  const body = parsed && parsed.body;
  const text = (parsed && parsed.text ? parsed.text : "").trim();
  if (body && typeof body === "object" && body.message) {
    return body.message;
  }
  if (text.startsWith("<")) {
    if (status === 413) {
      return "上传失败：文件过大，请联系服务器放开上传大小限制";
    }
    return `服务器返回了网页内容(HTTP ${status})，请检查 Nginx/网关上传限制`;
  }
  if (status) {
    return `${fallback}(HTTP ${status})`;
  }
  return fallback;
}

async function apiRequest(path, options = {}) {
  const token = getToken();
  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method || "GET",
    headers: Object.assign(
      { "Content-Type": "application/json", "X-Admin-Token": token },
      options.headers || {}
    ),
    body: options.body ? JSON.stringify(options.body) : undefined
  });
  const parsed = await parseResponse(response);
  const body = parsed.body;
  if (body && typeof body.code !== "undefined") {
    if (body.code === 0) {
      return body.data;
    }
    throw new Error(body.message || "请求失败");
  }
  if (!response.ok) {
    throw new Error(buildErrorMessage(response, parsed, "请求失败"));
  }
  if (!body) {
    throw new Error(buildErrorMessage(response, parsed, "服务器响应格式异常"));
  }
  return body;
}

async function uploadFile(file, options = {}) {
  return new Promise((resolve, reject) => {
    const token = getToken();
    const formData = new FormData();
    formData.append("file", file);
    const query = new URLSearchParams();
    if (options.scene) {
      query.set("scene", options.scene);
    }
    if (options.maxVideoSeconds !== undefined && options.maxVideoSeconds !== null) {
      query.set("maxVideoSeconds", String(options.maxVideoSeconds));
    }
    const suffix = query.toString() ? `?${query.toString()}` : "";
    const xhr = new XMLHttpRequest();
    xhr.open("POST", `${API_BASE}/api/admin/upload${suffix}`, true);
    xhr.setRequestHeader("X-Admin-Token", token);
    if (typeof options.onProgress === "function" && xhr.upload) {
      xhr.upload.onprogress = (event) => {
        if (!event || !event.lengthComputable) {
          return;
        }
        const percent = Math.max(0, Math.min(100, Math.round((event.loaded / event.total) * 100)));
        options.onProgress(percent);
      };
    }
    xhr.onload = () => {
      const parsed = { text: xhr.responseText || "", body: parseJsonText(xhr.responseText || "") };
      const body = parsed.body;
      if (body && body.code === 0) {
        resolve(body.data);
        return;
      }
      reject(new Error(buildErrorMessage({ status: xhr.status }, parsed, "上传失败")));
    };
    xhr.onerror = () => {
      reject(new Error("上传失败：网络异常"));
    };
    xhr.onabort = () => {
      reject(new Error("上传已取消"));
    };
    xhr.send(formData);
  });
}

async function deleteUpload(url) {
  if (!url) {
    return;
  }
  return apiRequest("/api/admin/uploads/delete", {
    method: "POST",
    body: { url }
  });
}

window.AdminApi = {
  getToken,
  setToken,
  clearToken,
  apiRequest,
  uploadFile,
  deleteUpload
};
