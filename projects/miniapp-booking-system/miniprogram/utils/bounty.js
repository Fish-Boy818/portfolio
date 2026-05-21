const { request } = require("./api");

function listBounties() {
  return request({ url: "/api/bounties" });
}

function getBountyDetail(id) {
  return request({ url: `/api/bounties/${id}` });
}

function listMyBounties() {
  return request({ url: "/api/bounties/mine" });
}

function acceptBounty(id) {
  return request({
    url: `/api/bounties/${id}/accept`,
    method: "POST"
  });
}

module.exports = {
  listBounties,
  listMyBounties,
  getBountyDetail,
  acceptBounty
};
