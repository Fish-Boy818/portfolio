const { request, getBaseUrl } = require("../../utils/api");
const { ensureAuth } = require("../../utils/auth");
const {
  getCommissionAccount,
  listCommissionRecords,
  listCommissionWithdrawals,
  createCommissionWithdrawal,
  syncCommissionWithdrawal,
  cancelCommissionWithdrawal,
  normalizeWithdrawFailMessage
} = require("../../utils/commission");

function money(value) {
  const num = Number(value || 0);
  if (Number.isNaN(num)) {
    return "0.00";
  }
  return num.toFixed(2);
}

Page({
  data: {
    status: "loading",
    errorMessage: "加载失败，请稍后重试",
    userId: null,
    account: {
      scanUser: false,
      withdrawableBalance: "0.00",
      pendingBalance: "0.00",
      withdrawingBalance: "0.00",
      withdrawnTotal: "0.00"
    },
    records: [],
    withdrawals: [],
    inviteEnabled: false,
    inviteCode: "",
    inviteCount: 0,
    inviteQrcodeUrl: "",
    withdrawAmount: "",
    withdrawError: "",
    submitting: false
  },
  onLoad() {
    this.loadData();
  },
  onShow() {
    if (this.data.status === "loading") {
      return;
    }
    this.loadData();
  },
  loadData() {
    this.setData({ status: "loading", withdrawError: "" });
    ensureAuth({ strict: true })
      .then((user) => {
        const userId = user && user.id;
        if (!userId) {
          throw new Error("请先登录");
        }
        return Promise.all([
          getCommissionAccount(),
          listCommissionRecords(),
          listCommissionWithdrawals(),
          request({ url: "/api/commission/invite/me" }).catch(() => null)
        ]).then(([account, records, withdrawals, inviteOverview]) => {
          const normalizedAccount = {
            scanUser: !!(account && account.scanUser),
            withdrawableBalance: money(account && account.withdrawableBalance),
            pendingBalance: money(account && account.pendingBalance),
            withdrawingBalance: money(account && account.withdrawingBalance),
            withdrawnTotal: money(account && account.withdrawnTotal)
          };
          const hasCommissionData =
            Number(account && account.withdrawableBalance) > 0 ||
            Number(account && account.pendingBalance) > 0 ||
            Number(account && account.withdrawingBalance) > 0 ||
            Number(account && account.withdrawnTotal) > 0 ||
            (Array.isArray(records) && records.length > 0) ||
            (Array.isArray(withdrawals) && withdrawals.length > 0);
          if (!(account && account.scanUser) && !hasCommissionData) {
            this.setData({
              status: "empty",
              errorMessage: "当前账号还没有佣金记录，扫码参与或完成福利奖励后会显示在这里",
              userId,
              account: normalizedAccount,
              records: [],
              withdrawals: [],
              inviteEnabled: false,
              inviteCode: "",
              inviteCount: 0,
              inviteQrcodeUrl: ""
            });
            return;
          }
          const qrPath = inviteOverview && inviteOverview.qrCodePath ? String(inviteOverview.qrCodePath) : "";
          const inviteQrcodeUrl = qrPath
            ? (/^https?:\/\//i.test(qrPath) ? qrPath : `${getBaseUrl()}${qrPath}`)
            : "";
          this.setData({
            status: "ready",
            userId,
            account: normalizedAccount,
            records: (records || []).slice(0, 50).map((item) => ({
              ...item,
              commissionAmount: money(item.commissionAmount)
            })),
            withdrawals: (withdrawals || []).slice(0, 50).map((item) => ({
              ...item,
              amount: money(item.amount)
            })),
            inviteEnabled: !!(inviteOverview && inviteOverview.scanUser),
            inviteCode: inviteOverview && inviteOverview.inviteCode ? inviteOverview.inviteCode : "",
            inviteCount: inviteOverview && inviteOverview.inviteCount ? inviteOverview.inviteCount : 0,
            inviteQrcodeUrl
          });
        });
      })
      .catch((err) => {
        this.setData({
          status: "error",
          errorMessage: String(err || "加载失败")
        });
      });
  },
  onAmountInput(e) {
    this.setData({ withdrawAmount: e.detail.value, withdrawError: "" });
  },
  onWithdrawAll() {
    this.setData({
      withdrawAmount: this.data.account.withdrawableBalance || "0.00",
      withdrawError: ""
    });
  },
  onSubmitWithdraw() {
    if (this.data.submitting) {
      return;
    }
    const amountNum = Number(this.data.withdrawAmount);
    const max = Number(this.data.account.withdrawableBalance || 0);
    if (!(amountNum > 0)) {
      this.setData({ withdrawError: "请输入正确提现金额" });
      return;
    }
    if (amountNum > max) {
      this.setData({ withdrawError: "提现金额不能超过可提现余额" });
      return;
    }
    const amount = Number(amountNum.toFixed(2));
    const idemKey = `wd-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
    this.setData({ submitting: true, withdrawError: "" });
    createCommissionWithdrawal(amount, idemKey)
      .then((withdrawal) => {
        const withdrawalId = withdrawal && withdrawal.id;
        const packageInfo = withdrawal && withdrawal.transferPackageInfo;
        const mchId = withdrawal && withdrawal.transferMchId;
        const appId = withdrawal && withdrawal.transferAppId;
        this.setData({ withdrawAmount: "" });
        if (!withdrawalId) {
          wx.showToast({ title: "提现申请已提交", icon: "success" });
          this.loadData();
          return;
        }
        if (!packageInfo || !mchId || !appId) {
          this.syncAndRefresh(withdrawalId);
          return;
        }
        this.requestMerchantTransferAndSync(withdrawalId, {
          packageInfo,
          mchId,
          appId
        });
      })
      .catch((err) => {
        this.setData({ withdrawError: normalizeWithdrawFailMessage(err) || "提交失败" });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },
  requestMerchantTransferAndSync(withdrawalId, payload) {
    if (!wx.requestMerchantTransfer) {
      cancelCommissionWithdrawal(withdrawalId)
        .catch(() => null)
        .finally(() => {
          this.setData({
            withdrawError: "当前微信版本不支持提现确认，请升级微信后重试"
          });
          this.loadData();
        });
      return;
    }
    wx.requestMerchantTransfer({
      mchId: payload.mchId,
      appId: payload.appId,
      package: payload.packageInfo,
      success: () => {
        this.syncAndRefresh(withdrawalId);
      },
      fail: (err) => {
        const msg = err && err.errMsg ? String(err.errMsg) : "";
        if (msg.indexOf("cancel") > -1) {
          cancelCommissionWithdrawal(withdrawalId)
            .catch(() => null)
            .finally(() => {
              this.setData({ withdrawError: "" });
              this.loadData();
            });
          return;
        }
        this.setData({ withdrawError: normalizeWithdrawFailMessage(msg) || "提现确认失败" });
        this.syncAndRefresh(withdrawalId);
      }
    });
  },
  syncAndRefresh(withdrawalId) {
    syncCommissionWithdrawal(withdrawalId)
      .then((latest) => {
        if (latest && latest.status === "success") {
          wx.showToast({ title: "提现成功", icon: "success" });
        } else if (latest && latest.status === "failed") {
          this.setData({
            withdrawError: normalizeWithdrawFailMessage(latest.failReason) || "提现失败"
          });
        } else {
          wx.showToast({ title: "提现申请已提交", icon: "success" });
        }
      })
      .catch((err) => {
        this.setData({ withdrawError: normalizeWithdrawFailMessage(err) || "提现状态同步失败" });
      })
      .finally(() => {
        this.loadData();
      });
  },
  onRetry() {
    this.loadData();
  },
  onPreviewInviteQrcode() {
    const url = this.data.inviteQrcodeUrl;
    if (!url) {
      return;
    }
    wx.previewImage({
      current: url,
      urls: [url]
    });
  },
  onOpenInviteRecords() {
    wx.navigateTo({
      url: "/pages/commission-invites/index"
    });
  },
  onOpenCommissionRecords() {
    wx.navigateTo({
      url: "/pages/commission-records/index"
    });
  },
  onOpenWithdrawals() {
    wx.navigateTo({
      url: "/pages/commission-withdrawals/index"
    });
  }
});
