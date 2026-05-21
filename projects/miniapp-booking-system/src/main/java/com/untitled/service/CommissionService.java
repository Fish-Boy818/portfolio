package com.untitled.service;

import com.untitled.config.CommissionProperties;
import com.untitled.dto.AdminWithdrawalUpdateRequest;
import com.untitled.dto.CommissionAccountResponse;
import com.untitled.dto.CommissionRecordResponse;
import com.untitled.dto.CommissionWithdrawalResponse;
import com.untitled.mapper.CommissionAccountMapper;
import com.untitled.mapper.CommissionRecordMapper;
import com.untitled.mapper.CommissionWithdrawalItemMapper;
import com.untitled.mapper.CommissionWithdrawalMapper;
import com.untitled.mapper.UserMapper;
import com.untitled.model.Activity;
import com.untitled.model.CommissionAccount;
import com.untitled.model.CommissionRecord;
import com.untitled.model.CommissionWithdrawal;
import com.untitled.model.CommissionWithdrawalItem;
import com.untitled.model.Order;
import com.untitled.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommissionService {
    public static final String RECORD_PENDING = "pending";
    public static final String RECORD_WITHDRAWABLE = "withdrawable";
    public static final String RECORD_WITHDRAWING = "withdrawing";
    public static final String RECORD_WITHDRAWN = "withdrawn";
    public static final String RECORD_REVERSED = "reversed";

    public static final String WITHDRAW_PENDING = "pending";
    public static final String WITHDRAW_PROCESSING = "processing";
    public static final String WITHDRAW_SUCCESS = "success";
    public static final String WITHDRAW_FAILED = "failed";

    private static final String OPERATOR_SYSTEM = "system";
    private static final long PLATFORM_USER_ID = 0L;
    private static final int INVITE_DEPTH_MAX = 3;

    private final UserMapper userMapper;
    private final CommissionAccountMapper commissionAccountMapper;
    private final CommissionRecordMapper commissionRecordMapper;
    private final CommissionWithdrawalMapper commissionWithdrawalMapper;
    private final CommissionWithdrawalItemMapper commissionWithdrawalItemMapper;
    private final CommissionProperties commissionProperties;
    private final WechatTransferService wechatTransferService;
    private final Random random = new Random();

    public CommissionService(UserMapper userMapper,
                             CommissionAccountMapper commissionAccountMapper,
                             CommissionRecordMapper commissionRecordMapper,
                             CommissionWithdrawalMapper commissionWithdrawalMapper,
                             CommissionWithdrawalItemMapper commissionWithdrawalItemMapper,
                             CommissionProperties commissionProperties,
                             WechatTransferService wechatTransferService) {
        this.userMapper = userMapper;
        this.commissionAccountMapper = commissionAccountMapper;
        this.commissionRecordMapper = commissionRecordMapper;
        this.commissionWithdrawalMapper = commissionWithdrawalMapper;
        this.commissionWithdrawalItemMapper = commissionWithdrawalItemMapper;
        this.commissionProperties = commissionProperties;
        this.wechatTransferService = wechatTransferService;
    }

    public boolean isScanUser(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.findById(userId);
        return user != null && user.getScanUser() != null && user.getScanUser() == 1;
    }

    public boolean hasCommissionRecord(long orderId) {
        return commissionRecordMapper.countByOrderId(orderId) > 0;
    }

    @Transactional
    public boolean activateScanQualification(long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getScanUser() != null && user.getScanUser() == 1) {
            return false;
        }
        return userMapper.activateScanUser(userId) > 0;
    }

    @Transactional
    /**
     * 作用：
     * 在订单创建后生成返利明细记录。
     * 方法：
     * 先校验订单、活动和用户是否满足返利条件，再计算各层级佣金分配结果，
     * 最后逐条写入佣金明细并刷新相关账户余额。
     */
    public void onOrderCreated(Order order, Activity activity, User user) { // 作用：生成订单返利明细；方法：按邀请层级拆分佣金并写入佣金表
        if (order == null || activity == null || user == null) {
            return;
        }
        if (user.getScanUser() == null || user.getScanUser() != 1) {
            return;
        }
        BigDecimal commission = money(order.getUserCommissionAmount()); // 作用：读取返利总额；方法：从订单中获取用户佣金并标准化金额
        if (commission.signum() <= 0) {
            return;
        }
        if (commissionRecordMapper.countByOrderId(order.getId()) > 0) {
            return;
        }
        Map<Long, BigDecimal> allocations = resolveOrderCommissionAllocations(order, activity, user, commission); // 作用：计算返利分配；方法：根据邀请层级和活动配置拆分佣金
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, BigDecimal> entry : allocations.entrySet()) {
            BigDecimal amount = money(entry.getValue());
            if (amount.signum() <= 0) {
                continue;
            }
            CommissionRecord record = new CommissionRecord(); // 作用：创建返利明细对象；方法：实例化佣金记录承载每条返利数据
            record.setUserId(entry.getKey());
            record.setOrderId(order.getId());
            record.setOrderNo(order.getOrderNo());
            record.setActivityId(order.getActivityId());
            record.setActivityTitle(order.getTitle());
            record.setScanUserAtOrderTime(1);
            record.setPriceAmount(money(order.getPayAmount()));
            record.setMerchantSettlementAmount(money(order.getMerchantSettlementAmount()));
            record.setPlatformOperationFee(money(order.getPlatformOperationFee()));
            record.setCommissionAmount(amount);
            record.setAvailableAmount(BigDecimal.ZERO);
            record.setFrozenAmount(BigDecimal.ZERO);
            record.setWithdrawnAmount(BigDecimal.ZERO);
            record.setStatus(RECORD_PENDING);
            record.setOccurTime(now);
            commissionRecordMapper.insert(record); // 作用：保存返利记录；方法：调用 Mapper 将佣金明细写入数据库
        }
        for (Long userId : allocations.keySet()) {
            refreshAccount(userId); // 作用：刷新账户汇总；方法：根据最新明细重新计算用户返利账户余额
        }
    }

    @Transactional
    public void onOrderVerified(Order order) {
        if (order == null) {
            return;
        }
        List<CommissionRecord> records = commissionRecordMapper.findByOrderIdForUpdate(order.getId());
        if (records.isEmpty()) {
            return;
        }
        Set<Long> affectedUsers = new HashSet<Long>();
        LocalDateTime now = LocalDateTime.now();
        for (CommissionRecord record : records) {
            if (!RECORD_PENDING.equals(record.getStatus())) {
                affectedUsers.add(record.getUserId());
                continue;
            }
            record.setStatus(RECORD_WITHDRAWABLE);
            BigDecimal remaining = money(record.getCommissionAmount())
                    .subtract(money(record.getWithdrawnAmount()))
                    .subtract(money(record.getFrozenAmount()));
            if (remaining.signum() < 0) {
                remaining = BigDecimal.ZERO;
            }
            record.setAvailableAmount(remaining);
            record.setAvailableAt(now);
            commissionRecordMapper.update(record);
            affectedUsers.add(record.getUserId());
        }
        for (Long userId : affectedUsers) {
            refreshAccount(userId);
        }
    }

    @Transactional
    public void onOrderReversed(Order order, String reason) {
        if (order == null) {
            return;
        }
        List<CommissionRecord> records = commissionRecordMapper.findByOrderIdForUpdate(order.getId());
        if (records.isEmpty()) {
            return;
        }
        for (CommissionRecord record : records) {
            if (money(record.getFrozenAmount()).signum() > 0 || money(record.getWithdrawnAmount()).signum() > 0) {
                throw new IllegalArgumentException("该订单佣金已进入提现流程，暂不支持退款/取消");
            }
        }
        Set<Long> affectedUsers = new HashSet<Long>();
        LocalDateTime now = LocalDateTime.now();
        for (CommissionRecord record : records) {
            if (!RECORD_REVERSED.equals(record.getStatus())) {
                record.setStatus(RECORD_REVERSED);
                record.setAvailableAmount(BigDecimal.ZERO);
                record.setFrozenAmount(BigDecimal.ZERO);
                record.setReversedAt(now);
                record.setReversalReason(StringUtils.hasText(reason) ? reason : "订单状态变更冲销");
                commissionRecordMapper.update(record);
            }
            affectedUsers.add(record.getUserId());
        }
        for (Long userId : affectedUsers) {
            refreshAccount(userId);
        }
    }

    public void assertOrderRefundable(long orderId) {
        List<CommissionRecord> records = commissionRecordMapper.findByOrderId(orderId);
        if (records.isEmpty()) {
            return;
        }
        for (CommissionRecord record : records) {
            if (money(record.getFrozenAmount()).signum() > 0 || money(record.getWithdrawnAmount()).signum() > 0) {
                throw new IllegalArgumentException("该订单佣金已进入提现流程，暂不支持退款");
            }
        }
    }

    @Transactional
    /**
     * 作用：
     * 发起用户返利提现申请，并调用微信提现接口。
     * 方法：
     * 先校验提现金额和 openId，再锁定可提现佣金并创建提现单，
     * 随后生成提现明细，最后调用微信提现接口并回写提现状态。
     */
    public CommissionWithdrawalResponse createWithdrawal(long userId, BigDecimal amount, String idemKey) { // 作用：发起返利提现；方法：冻结可提现佣金后调用微信提现接口
        BigDecimal target = money(amount); // 作用：标准化提现金额；方法：将输入金额转换为统一的金额格式
        if (target.signum() <= 0) {
            throw new IllegalArgumentException("提现金额必须大于0");
        }
        enforceWithdrawRules(userId, target);

        String normalizedIdemKey = trimToNull(idemKey);
        if (normalizedIdemKey != null) {
            CommissionWithdrawal existing = commissionWithdrawalMapper.findByUserIdemKey(userId, normalizedIdemKey);
            if (existing != null) {
                return toWithdrawalResponse(existing);
            }
        }

        User user = userMapper.findById(userId); // 作用：查询提现用户；方法：根据 userId 读取当前申请提现的用户
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!StringUtils.hasText(user.getOpenId())) {
            throw new IllegalArgumentException("用户缺少 openId，请重新登录");
        }

        List<CommissionRecord> candidates = commissionRecordMapper.findWithdrawableForUpdate(userId); // 作用：锁定可提现佣金；方法：查询并加锁当前用户可提现的佣金明细
        BigDecimal totalAvailable = BigDecimal.ZERO;
        for (CommissionRecord record : candidates) {
            totalAvailable = totalAvailable.add(money(record.getAvailableAmount()));
        }
        if (target.compareTo(totalAvailable) > 0) {
            throw new IllegalArgumentException("提现金额超过可提现余额");
        }

        CommissionWithdrawal withdrawal = new CommissionWithdrawal();
        withdrawal.setWithdrawNo(generateWithdrawNo()); // 作用：生成提现单号；方法：调用提现单号生成规则创建唯一编号
        withdrawal.setUserId(userId);
        withdrawal.setAmount(target);
        withdrawal.setStatus(WITHDRAW_PENDING);
        withdrawal.setIdemKey(normalizedIdemKey);
        withdrawal.setRequestedAt(LocalDateTime.now());
        commissionWithdrawalMapper.insert(withdrawal); // 作用：保存提现主单；方法：调用 Mapper 将提现单写入数据库

        BigDecimal remaining = target;
        for (CommissionRecord record : candidates) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal available = money(record.getAvailableAmount());
            if (available.signum() <= 0) {
                continue;
            }
            BigDecimal allocate = available.min(remaining);
            CommissionWithdrawalItem item = new CommissionWithdrawalItem(); // 作用：创建提现明细对象；方法：实例化提现单与佣金记录的关联对象
            item.setWithdrawalId(withdrawal.getId());
            item.setCommissionRecordId(record.getId());
            item.setAmount(allocate);
            commissionWithdrawalItemMapper.insert(item); // 作用：保存提现明细；方法：把提现单和佣金记录的关系写入数据库

            record.setAvailableAmount(available.subtract(allocate));
            record.setFrozenAmount(money(record.getFrozenAmount()).add(allocate));
            if (money(record.getFrozenAmount()).signum() > 0) {
                record.setStatus(RECORD_WITHDRAWING);
            } else {
                record.setStatus(RECORD_WITHDRAWABLE);
            }
            commissionRecordMapper.update(record); // 作用：冻结返利金额；方法：更新佣金明细的可提现和冻结金额
            remaining = remaining.subtract(allocate);
        }
        if (remaining.signum() > 0) {
            throw new IllegalArgumentException("可提现余额不足");
        }

        WechatTransferService.TransferCreateResult transferResult = wechatTransferService.createTransferBill(
                withdrawal.getWithdrawNo(),
                user.getOpenId(),
                toFen(target)
        ); // 作用：调用微信提现接口；方法：把提现单号、openId 和金额发送到微信零钱转账接口

        withdrawal.setTransferBillNo(trimToNull(transferResult.getTransferBillNo()));
        withdrawal.setTransferState(trimToNull(transferResult.getState()));
        withdrawal.setTransferPackageInfo(trimToNull(transferResult.getPackageInfo()));
        withdrawal.setFailReason(trimToNull(transferResult.getFailReason()));
        commissionWithdrawalMapper.update(withdrawal); // 作用：回写微信提现受理信息；方法：把微信返回的单号和状态更新到提现单

        applyTransferState(withdrawal.getId(),
                transferResult.getState(),
                transferResult.getFailReason(),
                transferResult.getTransferBillNo(),
                "create");

        refreshAccount(userId); // 作用：刷新账户汇总；方法：根据提现后的明细重新计算返利账户余额
        return toWithdrawalResponse(commissionWithdrawalMapper.findById(withdrawal.getId())); // 作用：返回提现结果；方法：查询最新提现单并转换为响应对象
    }

    @Transactional
    public CommissionWithdrawalResponse syncWithdrawal(long userId, long withdrawalId) {
        CommissionWithdrawal current = commissionWithdrawalMapper.findById(withdrawalId);
        if (current == null) {
            throw new IllegalArgumentException("提现单不存在");
        }
        if (current.getUserId() == null || current.getUserId() != userId) {
            throw new IllegalArgumentException("无权访问该提现单");
        }
        if (WITHDRAW_SUCCESS.equals(current.getStatus()) || WITHDRAW_FAILED.equals(current.getStatus())) {
            return toWithdrawalResponse(current);
        }
        WechatTransferService.TransferQueryResult queryResult = wechatTransferService.queryTransferBill(current.getWithdrawNo());
        applyTransferState(withdrawalId,
                queryResult.getState(),
                queryResult.getFailReason(),
                queryResult.getTransferBillNo(),
                "sync");
        CommissionWithdrawal latest = commissionWithdrawalMapper.findById(withdrawalId);
        return toWithdrawalResponse(latest);
    }

    @Transactional
    public void cancelWithdrawalByUser(long userId, long withdrawalId) {
        CommissionWithdrawal withdrawal = commissionWithdrawalMapper.findByIdForUpdate(withdrawalId);
        if (withdrawal == null) {
            return;
        }
        if (withdrawal.getUserId() == null || withdrawal.getUserId() != userId) {
            throw new IllegalArgumentException("无权操作该提现单");
        }
        if (WITHDRAW_SUCCESS.equals(withdrawal.getStatus()) || WITHDRAW_FAILED.equals(withdrawal.getStatus())) {
            return;
        }

        List<CommissionWithdrawalItem> items = commissionWithdrawalItemMapper.findByWithdrawalId(withdrawalId);
        Map<Long, CommissionRecord> recordMap = loadRecordMapForUpdate(items);
        for (CommissionWithdrawalItem item : items) {
            CommissionRecord record = recordMap.get(item.getCommissionRecordId());
            if (record == null) {
                continue;
            }
            BigDecimal amount = money(item.getAmount());
            BigDecimal frozen = money(record.getFrozenAmount());
            if (frozen.compareTo(amount) < 0) {
                throw new IllegalArgumentException("提现明细与佣金冻结金额不一致");
            }
            record.setFrozenAmount(frozen.subtract(amount));
            record.setAvailableAmount(money(record.getAvailableAmount()).add(amount));
            if (!RECORD_REVERSED.equals(record.getStatus())) {
                if (money(record.getFrozenAmount()).signum() > 0) {
                    record.setStatus(RECORD_WITHDRAWING);
                } else {
                    record.setStatus(RECORD_WITHDRAWABLE);
                }
            }
            commissionRecordMapper.update(record);
        }

        commissionWithdrawalItemMapper.deleteByWithdrawalId(withdrawalId);
        commissionWithdrawalMapper.deleteById(withdrawalId);
        refreshAccount(userId);
    }

    @Transactional
    public Map<String, String> handleWithdrawNotify(String payload,
                                                    String timestamp,
                                                    String nonce,
                                                    String signature,
                                                    String serial) {
        try {
            WechatTransferService.TransferNotifyResult result =
                    wechatTransferService.parseTransferNotify(payload, timestamp, nonce, signature, serial);
            String outBillNo = trimToNull(result.getOutBillNo());
            if (outBillNo == null) {
                return wechatTransferService.failResponse("out_bill_no missing");
            }
            CommissionWithdrawal withdrawal = commissionWithdrawalMapper.findByWithdrawNo(outBillNo);
            if (withdrawal == null) {
                return wechatTransferService.successResponse();
            }
            applyTransferState(withdrawal.getId(),
                    result.getState(),
                    result.getFailReason(),
                    result.getTransferBillNo(),
                    "notify");
            return wechatTransferService.successResponse();
        } catch (Exception ex) {
            return wechatTransferService.failResponse("notify handle error");
        }
    }

    public CommissionAccountResponse getAccount(long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        refreshAccount(userId);
        CommissionAccount account = commissionAccountMapper.findByUserId(userId);
        CommissionAccountResponse response = new CommissionAccountResponse();
        response.setScanUser(user.getScanUser() != null && user.getScanUser() == 1);
        response.setWithdrawableBalance(account != null ? money(account.getWithdrawableBalance()) : BigDecimal.ZERO);
        response.setPendingBalance(account != null ? money(account.getPendingBalance()) : BigDecimal.ZERO);
        response.setWithdrawingBalance(account != null ? money(account.getWithdrawingBalance()) : BigDecimal.ZERO);
        response.setWithdrawnTotal(account != null ? money(account.getWithdrawnTotal()) : BigDecimal.ZERO);
        response.setReversedTotal(account != null ? money(account.getReversedTotal()) : BigDecimal.ZERO);
        return response;
    }

    public List<CommissionRecordResponse> listUserRecords(long userId, String status) {
        List<CommissionRecord> records = commissionRecordMapper.findByUser(userId, trimToNull(status));
        Map<Long, User> userMap = buildUserMapFromRecordUsers(records.stream()
                .map(CommissionRecord::getUserId)
                .collect(Collectors.toList()));
        return records.stream()
                .map(record -> toRecordResponse(record, userMap))
                .collect(Collectors.toList());
    }

    public List<CommissionRecordResponse> listAdminRecords(Long userId, String orderNo, String status) {
        List<CommissionRecord> records = commissionRecordMapper.findAllForAdmin(userId, trimToNull(orderNo), trimToNull(status));
        Map<Long, User> userMap = buildUserMapFromRecordUsers(records.stream()
                .map(CommissionRecord::getUserId)
                .collect(Collectors.toList()));
        return records.stream()
                .map(record -> toRecordResponse(record, userMap))
                .collect(Collectors.toList());
    }

    public List<CommissionWithdrawalResponse> listUserWithdrawals(long userId, String status) {
        List<CommissionWithdrawal> withdrawals = commissionWithdrawalMapper.findByUser(userId, trimToNull(status));
        withdrawals = withdrawals.stream()
                .filter(withdrawal -> isUserVisibleWithdrawalStatus(withdrawal != null ? withdrawal.getStatus() : null))
                .collect(Collectors.toList());
        Map<Long, User> userMap = buildUserMapFromRecordUsers(withdrawals.stream()
                .map(CommissionWithdrawal::getUserId)
                .collect(Collectors.toList()));
        return withdrawals.stream()
                .map(withdrawal -> toWithdrawalResponse(withdrawal, userMap))
                .collect(Collectors.toList());
    }

    public List<CommissionWithdrawalResponse> listAdminWithdrawals(Long userId, String status) {
        List<CommissionWithdrawal> withdrawals = commissionWithdrawalMapper.findAllForAdmin(userId, trimToNull(status));
        Map<Long, User> userMap = buildUserMapFromRecordUsers(withdrawals.stream()
                .map(CommissionWithdrawal::getUserId)
                .collect(Collectors.toList()));
        return withdrawals.stream()
                .map(withdrawal -> toWithdrawalResponse(withdrawal, userMap))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommissionRecord grantWelfareCommission(long userId,
                                                   long submissionId,
                                                   String submissionNo,
                                                   BigDecimal amount,
                                                   String platformName) {
        BigDecimal reward = money(amount);
        if (reward.signum() <= 0) {
            throw new IllegalArgumentException("奖励佣金必须大于0");
        }
        long virtualOrderId = -Math.abs(submissionId);
        List<CommissionRecord> existing = commissionRecordMapper.findByOrderId(virtualOrderId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        CommissionRecord record = new CommissionRecord();
        record.setUserId(userId);
        record.setOrderId(virtualOrderId);
        record.setOrderNo(trimToNull(submissionNo) != null ? submissionNo : ("FL-" + submissionId));
        record.setActivityId(null);
        record.setActivityTitle(buildWelfareTitle(platformName));
        record.setScanUserAtOrderTime(user.getScanUser() != null && user.getScanUser() == 1 ? 1 : 0);
        record.setPriceAmount(BigDecimal.ZERO);
        record.setMerchantSettlementAmount(BigDecimal.ZERO);
        record.setPlatformOperationFee(BigDecimal.ZERO);
        record.setCommissionAmount(reward);
        record.setAvailableAmount(reward);
        record.setFrozenAmount(BigDecimal.ZERO);
        record.setWithdrawnAmount(BigDecimal.ZERO);
        record.setStatus(RECORD_WITHDRAWABLE);
        record.setOccurTime(now);
        record.setAvailableAt(now);
        commissionRecordMapper.insert(record);
        refreshAccount(userId);
        return record;
    }

    @Transactional
    /**
     * 作用：
     * 处理后台对提现单的人工审核和状态流转。
     * 方法：
     * 先锁定提现单并校验目标状态是否合法，再按处理中、成功或失败分别更新提现状态，
     * 最后刷新账户汇总并返回最新提现结果。
     */
    public CommissionWithdrawalResponse adminUpdateWithdrawal(long id, AdminWithdrawalUpdateRequest request) { // 作用：处理后台提现审核；方法：按目标状态更新提现单并回写账户数据
        CommissionWithdrawal withdrawal = commissionWithdrawalMapper.findByIdForUpdate(id); // 作用：锁定提现单；方法：按 id 查询并加锁待处理提现记录
        if (withdrawal == null) {
            throw new IllegalArgumentException("提现单不存在");
        }
        String targetStatus = trimToNull(request.getStatus()); // 作用：读取目标状态；方法：从后台请求中提取并标准化状态值
        if (targetStatus == null) {
            throw new IllegalArgumentException("提现状态不能为空");
        }
        if (WITHDRAW_PROCESSING.equals(targetStatus)) {
            if (!WITHDRAW_PENDING.equals(withdrawal.getStatus())) {
                throw new IllegalArgumentException("仅待处理提现单可设为处理中");
            }
            withdrawal.setStatus(WITHDRAW_PROCESSING); // 作用：更新提现状态；方法：把提现单标记为处理中
            withdrawal.setFailReason(null);
            withdrawal.setOperatorName(trimToNull(request.getOperatorName()));
            withdrawal.setOperatorNote(trimToNull(request.getOperatorNote()));
            commissionWithdrawalMapper.update(withdrawal); // 作用：保存处理中状态；方法：调用 Mapper 更新提现单记录
            refreshAccount(withdrawal.getUserId());
            return toWithdrawalResponse(commissionWithdrawalMapper.findById(id));
        }

        if (WITHDRAW_SUCCESS.equals(targetStatus)) {
            if (!WITHDRAW_PENDING.equals(withdrawal.getStatus()) && !WITHDRAW_PROCESSING.equals(withdrawal.getStatus())) {
                throw new IllegalArgumentException("仅待处理或处理中提现单可设为成功");
            }
            updateWithdrawalToSuccess(withdrawal,
                    trimToNull(request.getOperatorName()),
                    trimToNull(request.getOperatorNote())); // 作用：处理提现成功；方法：调用成功流转逻辑更新提现单和佣金明细
            refreshAccount(withdrawal.getUserId());
            return toWithdrawalResponse(commissionWithdrawalMapper.findById(id));
        }

        if (WITHDRAW_FAILED.equals(targetStatus)) {
            if (!WITHDRAW_PENDING.equals(withdrawal.getStatus()) && !WITHDRAW_PROCESSING.equals(withdrawal.getStatus())) {
                throw new IllegalArgumentException("仅待处理或处理中提现单可驳回");
            }
            String failReason = trimToNull(request.getFailReason());
            if (failReason == null) {
                throw new IllegalArgumentException("驳回时请填写失败原因");
            }
            updateWithdrawalToFailed(withdrawal,
                    failReason,
                    trimToNull(request.getOperatorName()),
                    trimToNull(request.getOperatorNote())); // 作用：处理提现失败；方法：调用失败流转逻辑回滚冻结金额并记录原因
            refreshAccount(withdrawal.getUserId());
            return toWithdrawalResponse(commissionWithdrawalMapper.findById(id));
        }

        throw new IllegalArgumentException("不支持的提现状态");
    }

    private void applyTransferState(long withdrawalId,
                                    String transferState,
                                    String failReason,
                                    String transferBillNo,
                                    String note) {
        CommissionWithdrawal withdrawal = commissionWithdrawalMapper.findByIdForUpdate(withdrawalId);
        if (withdrawal == null) {
            return;
        }
        if (StringUtils.hasText(transferBillNo)) {
            withdrawal.setTransferBillNo(transferBillNo.trim());
        }
        if (StringUtils.hasText(transferState)) {
            withdrawal.setTransferState(transferState.trim());
        }

        if (isTransferSuccess(transferState)) {
            updateWithdrawalToSuccess(withdrawal, OPERATOR_SYSTEM, "wechat-" + note);
            return;
        }
        if (isTransferFailed(transferState)) {
            String reason = trimToNull(failReason);
            if (reason == null) {
                reason = "微信提现失败";
            }
            updateWithdrawalToFailed(withdrawal, reason, OPERATOR_SYSTEM, "wechat-" + note);
            return;
        }

        if (!WITHDRAW_SUCCESS.equals(withdrawal.getStatus()) && !WITHDRAW_FAILED.equals(withdrawal.getStatus())) {
            if (isTransferProcessing(transferState)) {
                withdrawal.setStatus(WITHDRAW_PROCESSING);
            } else {
                withdrawal.setStatus(WITHDRAW_PENDING);
            }
            withdrawal.setFailReason(trimToNull(failReason));
            withdrawal.setOperatorName(OPERATOR_SYSTEM);
            withdrawal.setOperatorNote("wechat-" + note);
            commissionWithdrawalMapper.update(withdrawal);
        }
    }

    private void updateWithdrawalToSuccess(CommissionWithdrawal withdrawal, String operatorName, String operatorNote) {
        if (WITHDRAW_SUCCESS.equals(withdrawal.getStatus())) {
            return;
        }
        if (WITHDRAW_FAILED.equals(withdrawal.getStatus())) {
            throw new IllegalArgumentException("提现单已失败，不能再设为成功");
        }
        List<CommissionWithdrawalItem> items = commissionWithdrawalItemMapper.findByWithdrawalId(withdrawal.getId());
        Map<Long, CommissionRecord> recordMap = loadRecordMapForUpdate(items);
        for (CommissionWithdrawalItem item : items) {
            CommissionRecord record = recordMap.get(item.getCommissionRecordId());
            if (record == null) {
                continue;
            }
            BigDecimal amount = money(item.getAmount());
            BigDecimal frozen = money(record.getFrozenAmount());
            if (frozen.compareTo(amount) < 0) {
                throw new IllegalArgumentException("提现明细与佣金冻结金额不一致");
            }
            record.setFrozenAmount(frozen.subtract(amount));
            record.setWithdrawnAmount(money(record.getWithdrawnAmount()).add(amount));
            if (!RECORD_REVERSED.equals(record.getStatus())) {
                if (money(record.getAvailableAmount()).signum() > 0) {
                    record.setStatus(RECORD_WITHDRAWABLE);
                } else if (money(record.getFrozenAmount()).signum() > 0) {
                    record.setStatus(RECORD_WITHDRAWING);
                } else {
                    record.setStatus(RECORD_WITHDRAWN);
                }
            }
            commissionRecordMapper.update(record);
        }
        withdrawal.setStatus(WITHDRAW_SUCCESS);
        withdrawal.setFailReason(null);
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal.setOperatorName(trimToNull(operatorName));
        withdrawal.setOperatorNote(trimToNull(operatorNote));
        commissionWithdrawalMapper.update(withdrawal);
    }

    private void updateWithdrawalToFailed(CommissionWithdrawal withdrawal,
                                          String failReason,
                                          String operatorName,
                                          String operatorNote) {
        if (WITHDRAW_FAILED.equals(withdrawal.getStatus())) {
            return;
        }
        if (WITHDRAW_SUCCESS.equals(withdrawal.getStatus())) {
            throw new IllegalArgumentException("提现单已成功，不能再设为失败");
        }
        List<CommissionWithdrawalItem> items = commissionWithdrawalItemMapper.findByWithdrawalId(withdrawal.getId());
        Map<Long, CommissionRecord> recordMap = loadRecordMapForUpdate(items);
        for (CommissionWithdrawalItem item : items) {
            CommissionRecord record = recordMap.get(item.getCommissionRecordId());
            if (record == null) {
                continue;
            }
            BigDecimal amount = money(item.getAmount());
            BigDecimal frozen = money(record.getFrozenAmount());
            if (frozen.compareTo(amount) < 0) {
                throw new IllegalArgumentException("提现明细与佣金冻结金额不一致");
            }
            record.setFrozenAmount(frozen.subtract(amount));
            record.setAvailableAmount(money(record.getAvailableAmount()).add(amount));
            if (!RECORD_REVERSED.equals(record.getStatus())) {
                if (money(record.getFrozenAmount()).signum() > 0) {
                    record.setStatus(RECORD_WITHDRAWING);
                } else {
                    record.setStatus(RECORD_WITHDRAWABLE);
                }
            }
            commissionRecordMapper.update(record);
        }
        withdrawal.setStatus(WITHDRAW_FAILED);
        withdrawal.setFailReason(trimToNull(failReason));
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal.setOperatorName(trimToNull(operatorName));
        withdrawal.setOperatorNote(trimToNull(operatorNote));
        commissionWithdrawalMapper.update(withdrawal);
    }

    private Map<Long, CommissionRecord> loadRecordMapForUpdate(List<CommissionWithdrawalItem> items) {
        List<Long> recordIds = items.stream()
                .map(CommissionWithdrawalItem::getCommissionRecordId)
                .collect(Collectors.toList());
        List<CommissionRecord> records = recordIds.isEmpty()
                ? new ArrayList<CommissionRecord>()
                : commissionRecordMapper.findByIdsForUpdate(recordIds);
        Map<Long, CommissionRecord> recordMap = new HashMap<Long, CommissionRecord>();
        for (CommissionRecord record : records) {
            recordMap.put(record.getId(), record);
        }
        return recordMap;
    }

    private Map<Long, BigDecimal> resolveOrderCommissionAllocations(Order order, Activity activity, User user, BigDecimal totalCommission) {
        Map<Long, BigDecimal> allocations = new LinkedHashMap<Long, BigDecimal>();
        BigDecimal total = money(totalCommission);
        if (total.signum() <= 0) {
            return allocations;
        }

        int depth = resolveEffectiveBuyerDepth(user);
        BigDecimal quantity = new BigDecimal(order.getQuantity() != null && order.getQuantity() > 0 ? order.getQuantity() : 1);
        BigDecimal user1Commission = money(activity != null ? activity.getBuyerCommissionAmount() : null).multiply(quantity);
        BigDecimal user2Commission = money(activity != null ? activity.getInviterCommissionAmount() : null).multiply(quantity);
        BigDecimal user3Commission = money(activity != null ? activity.getCommissionAmount() : null).multiply(quantity);
        long buyerId = order.getUserId() != null ? order.getUserId() : PLATFORM_USER_ID;

        if (depth <= 1) {
            mergeAllocation(allocations, buyerId, total);
            return allocations;
        }

        if (depth == 2) {
            BigDecimal buyerAmount = user2Commission.min(total);
            BigDecimal inviterAmount = total.subtract(buyerAmount);
            mergeAllocation(allocations, buyerId, buyerAmount);
            mergeAllocation(allocations, resolveDirectInviterUserId(user), inviterAmount);
            return allocations;
        }

        BigDecimal buyerAmount = user3Commission.min(total);
        BigDecimal operatorAmount = total.subtract(buyerAmount);
        mergeAllocation(allocations, buyerId, buyerAmount);
        mergeAllocation(allocations, PLATFORM_USER_ID, operatorAmount);
        return allocations;
    }

    private long resolveDirectInviterUserId(User user) {
        Long inviterId = user != null ? user.getInviterId() : null;
        if (inviterId == null || inviterId <= 0) {
            return PLATFORM_USER_ID;
        }
        User inviter = userMapper.findById(inviterId);
        if (!isScanEligible(inviter)) {
            return PLATFORM_USER_ID;
        }
        return inviterId;
    }

    private int resolveEffectiveBuyerDepth(User user) {
        int depth = resolveInviteDepth(user);
        if (depth <= 1) {
            return 1;
        }
        if (depth == 2) {
            return 2;
        }
        return 3;
    }

    private int resolveInviteDepth(User user) {
        int depth = resolveInviteDepth(user, new HashSet<Long>());
        return depth > 0 ? depth : 1;
    }

    private int resolveInviteDepth(User user, Set<Long> visited) {
        if (!isScanEligible(user)) {
            return 0;
        }
        Integer manualDepth = normalizeDepth(user != null ? user.getDepth() : null);
        if (manualDepth != null) {
            return manualDepth;
        }
        Long userId = user != null ? user.getId() : null;
        if (userId != null && !visited.add(userId)) {
            return 1;
        }
        Long inviterId = user.getInviterId();
        if (inviterId == null || inviterId <= 0) {
            return 1;
        }
        User inviter = userMapper.findById(inviterId);
        int inviterDepth = resolveInviteDepth(inviter, visited);
        if (inviterDepth <= 0) {
            return 1;
        }
        return Math.min(inviterDepth + 1, INVITE_DEPTH_MAX);
    }

    private boolean isScanEligible(User user) {
        return user != null && user.getScanUser() != null && user.getScanUser() == 1;
    }

    private Integer normalizeDepth(Integer depth) {
        if (depth == null || depth < 1 || depth > INVITE_DEPTH_MAX) {
            return null;
        }
        return depth;
    }

    private void mergeAllocation(Map<Long, BigDecimal> allocations, long userId, BigDecimal amount) {
        BigDecimal value = money(amount);
        if (value.signum() <= 0) {
            return;
        }
        BigDecimal current = allocations.containsKey(userId) ? money(allocations.get(userId)) : BigDecimal.ZERO;
        allocations.put(userId, current.add(value));
    }

    private void enforceWithdrawRules(long userId, BigDecimal amount) {
        CommissionProperties.Withdraw rule = commissionProperties.getWithdraw();
        BigDecimal minAmount = money(rule.getMinAmount());
        if (minAmount.signum() > 0 && amount.compareTo(minAmount) < 0) {
            throw new IllegalArgumentException("提现金额不能低于" + minAmount.setScale(2, RoundingMode.HALF_UP).toPlainString() + "元");
        }
        BigDecimal singleMaxAmount = money(rule.getSingleMaxAmount());
        if (singleMaxAmount.signum() > 0 && amount.compareTo(singleMaxAmount) > 0) {
            throw new IllegalArgumentException("单笔提现金额不能超过" + singleMaxAmount.setScale(2, RoundingMode.HALF_UP).toPlainString() + "元");
        }
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        Integer maxCount = rule.getDailyMaxCount();
        if (maxCount != null && maxCount > 0) {
            int count = commissionWithdrawalMapper.countByUserAndRequestedAt(userId, start, end);
            if (count >= maxCount) {
                throw new IllegalArgumentException("今日提现次数已达上限");
            }
        }
        BigDecimal maxAmount = money(rule.getDailyMaxAmount());
        if (maxAmount.signum() > 0) {
            BigDecimal dailyTotal = money(commissionWithdrawalMapper.sumAmountByUserAndRequestedAt(userId, start, end));
            if (dailyTotal.add(amount).compareTo(maxAmount) > 0) {
                throw new IllegalArgumentException("今日提现总额已达上限");
            }
        }
    }

    private boolean isTransferSuccess(String state) {
        if (!StringUtils.hasText(state)) {
            return false;
        }
        return "SUCCESS".equalsIgnoreCase(state.trim());
    }

    private boolean isTransferFailed(String state) {
        if (!StringUtils.hasText(state)) {
            return false;
        }
        String value = state.trim().toUpperCase();
        return "FAILED".equals(value) || "FAIL".equals(value) || "CANCELLED".equals(value);
    }

    private boolean isTransferProcessing(String state) {
        if (!StringUtils.hasText(state)) {
            return false;
        }
        String value = state.trim().toUpperCase();
        return "ACCEPTED".equals(value)
                || "PROCESSING".equals(value)
                || "TRANSFERING".equals(value)
                || "CANCELING".equals(value);
    }

    private void refreshAccount(long userId) {
        CommissionAccount account = commissionAccountMapper.calculateByUserId(userId);
        if (account == null) {
            account = new CommissionAccount();
            account.setUserId(userId);
            account.setWithdrawableBalance(BigDecimal.ZERO);
            account.setPendingBalance(BigDecimal.ZERO);
            account.setWithdrawingBalance(BigDecimal.ZERO);
            account.setWithdrawnTotal(BigDecimal.ZERO);
            account.setReversedTotal(BigDecimal.ZERO);
        } else {
            account.setUserId(userId);
            account.setWithdrawableBalance(money(account.getWithdrawableBalance()));
            account.setPendingBalance(money(account.getPendingBalance()));
            account.setWithdrawingBalance(money(account.getWithdrawingBalance()));
            account.setWithdrawnTotal(money(account.getWithdrawnTotal()));
            account.setReversedTotal(money(account.getReversedTotal()));
        }
        commissionAccountMapper.upsert(account);
    }

    private CommissionRecordResponse toRecordResponse(CommissionRecord record) {
        User user = record.getUserId() != null ? userMapper.findById(record.getUserId()) : null;
        return toRecordResponse(record, user);
    }

    private CommissionRecordResponse toRecordResponse(CommissionRecord record, Map<Long, User> userMap) {
        User user = record.getUserId() != null ? userMap.get(record.getUserId()) : null;
        return toRecordResponse(record, user);
    }

    private CommissionRecordResponse toRecordResponse(CommissionRecord record, User user) {
        CommissionRecordResponse response = new CommissionRecordResponse();
        response.setId(record.getId());
        response.setUserId(record.getUserId());
        response.setUserPhone(user != null ? trimToNull(user.getPhone()) : null);
        response.setOrderId(record.getOrderId());
        response.setOrderNo(record.getOrderNo());
        response.setActivityId(record.getActivityId());
        response.setActivityTitle(record.getActivityTitle());
        response.setScanUserAtOrderTime(record.getScanUserAtOrderTime());
        response.setPriceAmount(money(record.getPriceAmount()));
        response.setMerchantSettlementAmount(money(record.getMerchantSettlementAmount()));
        response.setPlatformOperationFee(money(record.getPlatformOperationFee()));
        response.setCommissionAmount(money(record.getCommissionAmount()));
        response.setAvailableAmount(money(record.getAvailableAmount()));
        response.setFrozenAmount(money(record.getFrozenAmount()));
        response.setWithdrawnAmount(money(record.getWithdrawnAmount()));
        response.setStatus(record.getStatus());
        response.setStatusText(recordStatusText(record.getStatus()));
        response.setOccurTime(record.getOccurTime());
        response.setAvailableAt(record.getAvailableAt());
        response.setReversedAt(record.getReversedAt());
        response.setReversalReason(record.getReversalReason());
        return response;
    }

    private CommissionWithdrawalResponse toWithdrawalResponse(CommissionWithdrawal withdrawal) {
        User user = withdrawal.getUserId() != null ? userMapper.findById(withdrawal.getUserId()) : null;
        return toWithdrawalResponse(withdrawal, user);
    }

    private CommissionWithdrawalResponse toWithdrawalResponse(CommissionWithdrawal withdrawal, Map<Long, User> userMap) {
        User user = withdrawal.getUserId() != null ? userMap.get(withdrawal.getUserId()) : null;
        return toWithdrawalResponse(withdrawal, user);
    }

    private CommissionWithdrawalResponse toWithdrawalResponse(CommissionWithdrawal withdrawal, User user) {
        CommissionWithdrawalResponse response = new CommissionWithdrawalResponse();
        response.setId(withdrawal.getId());
        response.setWithdrawNo(withdrawal.getWithdrawNo());
        response.setUserId(withdrawal.getUserId());
        response.setUserPhone(user != null ? trimToNull(user.getPhone()) : null);
        response.setAmount(money(withdrawal.getAmount()));
        response.setStatus(withdrawal.getStatus());
        response.setStatusText(withdrawStatusText(withdrawal.getStatus()));
        response.setFailReason(withdrawal.getFailReason());
        response.setTransferBillNo(withdrawal.getTransferBillNo());
        response.setTransferState(withdrawal.getTransferState());
        response.setTransferPackageInfo(withdrawal.getTransferPackageInfo());
        response.setTransferMchId(wechatTransferService.resolveMchId());
        response.setTransferAppId(wechatTransferService.resolvePayAppId());
        response.setOperatorName(withdrawal.getOperatorName());
        response.setOperatorNote(withdrawal.getOperatorNote());
        response.setRequestedAt(withdrawal.getRequestedAt());
        response.setProcessedAt(withdrawal.getProcessedAt());
        return response;
    }

    private Map<Long, User> buildUserMapFromRecordUsers(List<Long> rawUserIds) {
        List<Long> userIds = rawUserIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return new HashMap<Long, User>();
        }
        return userMapper.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private String recordStatusText(String status) {
        if (RECORD_PENDING.equals(status)) {
            return "待结算";
        }
        if (RECORD_WITHDRAWABLE.equals(status)) {
            return "可提现";
        }
        if (RECORD_WITHDRAWING.equals(status)) {
            return "提现中";
        }
        if (RECORD_WITHDRAWN.equals(status)) {
            return "已提现";
        }
        if (RECORD_REVERSED.equals(status)) {
            return "已冲销";
        }
        return status;
    }

    private String withdrawStatusText(String status) {
        if (WITHDRAW_PENDING.equals(status)) {
            return "待处理";
        }
        if (WITHDRAW_PROCESSING.equals(status)) {
            return "处理中";
        }
        if (WITHDRAW_SUCCESS.equals(status)) {
            return "成功";
        }
        if (WITHDRAW_FAILED.equals(status)) {
            return "失败";
        }
        return status;
    }

    private boolean isUserVisibleWithdrawalStatus(String status) {
        return WITHDRAW_SUCCESS.equals(status) || WITHDRAW_FAILED.equals(status);
    }

    private String buildWelfareTitle(String platformName) {
        String name = trimToNull(platformName);
        if (name == null) {
            return "悬赏任务好评奖励";
        }
        return name + "好评奖励";
    }

    private String generateWithdrawNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = 100 + random.nextInt(900);
        return "W" + time + suffix;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal money(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private int toFen(BigDecimal amount) {
        if (amount == null) {
            return 0;
        }
        return amount.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }
}
