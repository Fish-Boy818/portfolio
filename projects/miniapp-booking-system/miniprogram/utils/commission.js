const { request } = require("./api");

const WITHDRAW_NOT_ENOUGH_MESSAGE =
  "宗门库存尚未得到补充，速速联系宗主补充！恭喜你得到了宗主的专属通讯令牌:19529742213";

function normalizeWithdrawFailMessage(message) {
  const raw = String(message || "").trim();
  if (!raw) {
    return "";
  }
  if (
    raw.indexOf("NOT_ENOUGH") > -1 ||
    raw.indexOf("商户运营账户资金不足") > -1 ||
    raw.indexOf("充值后可以原单号发起重试") > -1 ||
    raw.indexOf("更换商户单号") > -1
  ) {
    return WITHDRAW_NOT_ENOUGH_MESSAGE;
  }
  return raw;
}

function getCommissionAccount() {
  return request({ url: "/api/commission/account" });
}

function listCommissionRecords(status) {
  return request({
    url: "/api/commission/records",
    data: { status: status || undefined }
  });
}

function listCommissionWithdrawals(status) {
  return request({
    url: "/api/commission/withdrawals",
    data: { status: status || undefined }
  });
}

function createCommissionWithdrawal(amount, idemKey) {
  return request({
    url: "/api/commission/withdrawals",
    method: "POST",
    data: {
      amount,
      idemKey: idemKey || undefined
    }
  });
}

function syncCommissionWithdrawal(id) {
  return request({
    url: `/api/commission/withdrawals/${id}/sync`,
    method: "POST"
  });
}

function cancelCommissionWithdrawal(id) {
  return request({
    url: `/api/commission/withdrawals/${id}/cancel`,
    method: "POST"
  });
}

module.exports = {
  getCommissionAccount,
  listCommissionRecords,
  listCommissionWithdrawals,
  createCommissionWithdrawal,
  syncCommissionWithdrawal,
  cancelCommissionWithdrawal,
  normalizeWithdrawFailMessage
};
