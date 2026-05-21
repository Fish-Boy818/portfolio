const { request } = require("./api");

function listWelfareSubmissions() {
  return request({ url: "/api/welfare/submissions" });
}

function createWelfareSubmission(data) {
  return request({
    url: "/api/welfare/submissions",
    method: "POST",
    data
  });
}

module.exports = {
  listWelfareSubmissions,
  createWelfareSubmission
};
