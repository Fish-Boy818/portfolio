package com.untitled.service;

import com.untitled.dto.OrderCreateRequest;
import com.untitled.dto.OrderResponse;
import com.untitled.dto.OrderUpdateRequest;
import com.untitled.mapper.ActivityMapper;
import com.untitled.mapper.ActivitySlotMapper;
import com.untitled.mapper.ClubMapper;
import com.untitled.mapper.CommissionRecordMapper;
import com.untitled.mapper.OrderMapper;
import com.untitled.mapper.UserMapper;
import com.untitled.model.Activity;
import com.untitled.model.ActivitySlot;
import com.untitled.model.Club;
import com.untitled.model.CommissionRecord;
import com.untitled.model.Order;
import com.untitled.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final long UNPAID_TIMEOUT_MINUTES = 5L;
    private static final long DUPLICATE_ORDER_REUSE_MINUTES = 2L;

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;
    private final ActivitySlotMapper slotMapper;
    private final ClubMapper clubMapper;
    private final CommissionRecordMapper commissionRecordMapper;
    private final CommissionService commissionService;
    private final Random random = new Random();

    public OrderService(OrderMapper orderMapper,
                        UserMapper userMapper,
                        ActivityMapper activityMapper,
                        ActivitySlotMapper slotMapper,
                        ClubMapper clubMapper,
                        CommissionRecordMapper commissionRecordMapper,
                        CommissionService commissionService) {
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.activityMapper = activityMapper;
        this.slotMapper = slotMapper;
        this.clubMapper = clubMapper;
        this.commissionRecordMapper = commissionRecordMapper;
        this.commissionService = commissionService;
    }

    public List<OrderResponse> list(Long userId, String status) {
        cancelExpiredUnpaidOrders();
        List<Order> orders = orderMapper.findAll(userId, status);
        if (orders.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Set<Long> activityIds = orders.stream()
                .map(Order::getActivityId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, Activity> activityMap = new HashMap<>();
        Map<Long, String> coverMap = new HashMap<>();
        Map<Long, List<CommissionRecord>> commissionRecordMap = new HashMap<>();
        if (!activityIds.isEmpty()) {
            List<Activity> activities = activityMapper.findByIds(activityIds.stream().collect(Collectors.toList()));
            for (Activity activity : activities) {
                activityMap.put(activity.getId(), activity);
                coverMap.put(activity.getId(), activity.getCover());
            }
        }
        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            commissionRecordMap = commissionRecordMapper.findByOrderIds(orderIds).stream()
                    .collect(Collectors.groupingBy(CommissionRecord::getOrderId));
        }
        final Map<Long, List<CommissionRecord>> orderCommissionRecordMap = commissionRecordMap;
        if (userId != null) {
            User user = userMapper.findById(userId);
            return orders.stream()
                    .map(order -> {
                        OrderResponse response = toResponse(order, user, activityMap.get(order.getActivityId()), orderCommissionRecordMap.get(order.getId()));
                        response.setCover(coverMap.get(order.getActivityId()));
                        return response;
                    })
                    .collect(Collectors.toList());
        }
        List<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        java.util.Map<Long, User> userMap = userIds.isEmpty()
                ? new HashMap<Long, User>()
                : userMapper.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return orders.stream()
                .map(order -> {
                    OrderResponse response = toResponse(order, userMap.get(order.getUserId()), activityMap.get(order.getActivityId()), orderCommissionRecordMap.get(order.getId()));
                    response.setCover(coverMap.get(order.getActivityId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    public Optional<OrderResponse> get(long id) {
        cancelIfExpired(id);
        Order order = orderMapper.findById(id);
        if (order == null) {
            return Optional.empty();
        }
        User user = userMapper.findById(order.getUserId());
        Activity activity = activityMapper.findById(order.getActivityId());
        OrderResponse response = toResponse(order, user, activity, commissionRecordMapper.findByOrderId(order.getId()));
        if (activity != null) {
            response.setCover(activity.getCover());
        }
        return Optional.of(response);
    }

    public void assertRefundAllowed(long orderId) {
        commissionService.assertOrderRefundable(orderId);
    }

    @Transactional
    public Optional<OrderResponse> updateStatus(long id, OrderUpdateRequest request) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return Optional.empty();
        }
        String target = request.getStatus();
        if ("paid".equals(target)) {
            return pay(id);
        }
        if ("used".equals(target)) {
            return verify(id);
        }
        if ("refund".equals(target)) {
            return refund(id);
        }
        if ("cancelled".equals(target)) {
            return cancel(id);
        }
        if ("unpaid".equals(target)) {
            return Optional.of(toResponse(order, userMapper.findById(order.getUserId()), activityMapper.findById(order.getActivityId()), commissionRecordMapper.findByOrderId(order.getId())));
        }
        throw new IllegalArgumentException("不支持的订单状态");
    }

    @Transactional
    public synchronized OrderResponse create(OrderCreateRequest request) {
        // 作用：创建活动预约订单；方法：校验用户和库存后生成待支付订单并保存
        User user = userMapper.findById(request.getUserId());
        // 作用：查询下单用户；方法：根据请求中的 userId 读取用户信息
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String phone = request.getPhone() == null ? "" : request.getPhone().trim();
        // 作用：规范化手机号；方法：对前端手机号去空格并处理空值
        if (phone.isEmpty()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!phone.matches("^1\\d{10}$")) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        Activity activity = activityMapper.findById(request.getActivityId());
        // 作用：查询活动信息；方法：根据 activityId 读取用户选择的活动
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        BigDecimal unitPrice = resolveSalePrice(activity); // 作用：计算订单单价；方法：从活动价格配置中解析用户实际支付单价
        BigDecimal payAmount = unitPrice.multiply(new BigDecimal(request.getQuantity())); // 作用：计算订单总金额；方法：用单价乘购买数量得到应付金额
        ActivitySlot slot = null;
        if (request.getSlotId() != null) {
            slot = slotMapper.findById(request.getSlotId()); // 作用：查询预约时段；方法：根据 slotId 读取活动时段记录
            if (slot == null || !slot.getActivityId().equals(activity.getId())) {
                throw new IllegalArgumentException("时段不存在");
            }
            if (slot.getStatus() != null && !"active".equals(slot.getStatus())) {
                throw new IllegalArgumentException("时段不可用");
            }
            int remaining = slot.getCapacity() - slot.getBooked(); // 作用：计算剩余库存；方法：用容量减已预约数量得到剩余名额
            if (remaining < request.getQuantity()) {
                throw new IllegalArgumentException("名额不足");
            }
        }
        Order duplicate = orderMapper.findRecentUnpaidDuplicate(
                user.getId(),
                activity.getId(),
                request.getSlotId(),
                phone,
                request.getQuantity(),
                payAmount,
                LocalDateTime.now().minusMinutes(DUPLICATE_ORDER_REUSE_MINUTES)
        );
        if (duplicate != null) {
            return toResponse(duplicate, user, activity, commissionRecordMapper.findByOrderId(duplicate.getId()));
        }
        if (slot != null) {
            reserveSlotCapacity(slot.getId(), request.getQuantity()); // 作用：锁定库存；方法：确认不是重复订单后再扣减可用名额避免重复占用
        }
        Club club = clubMapper.findById(activity.getClubId()); // 作用：查询所属俱乐部；方法：根据活动中的 clubId 读取门店信息
        BigDecimal original = BigDecimal.ZERO;
        BigDecimal quantity = new BigDecimal(request.getQuantity());
        BigDecimal merchantSettlementAmount = money(activity.getMerchantSettlementPrice()).multiply(quantity); // 作用：计算商家结算金额；方法：用商家结算单价乘购买数量
        BigDecimal platformConfigured = money(activity.getPlatformOperationFee());
        BigDecimal buyerCommission = money(normalizeCommissionAmount(activity.getBuyerCommissionAmount()));
        BigDecimal commissionAmount = buyerCommission.multiply(quantity); // 作用：计算返利金额；方法：用返利单价乘购买数量得到总返利
        BigDecimal platformOperationFee = platformConfigured.multiply(quantity); // 作用：计算平台留存；方法：用平台留存单价乘购买数量得到总平台费用
        BigDecimal settlementCheck = payAmount.subtract(merchantSettlementAmount).subtract(platformOperationFee).subtract(commissionAmount);
        if (settlementCheck.signum() != 0) {
            throw new IllegalArgumentException("下单失败：价格配置不成立，请在管理端调整平台留存或用户1佣金后重试");
        }
        if (platformOperationFee.signum() < 0) {
            throw new IllegalArgumentException("下单失败：价格配置不成立，请在管理端降低用户1佣金或提高用户支付价");
        }
        boolean scanUser = user.getScanUser() != null && user.getScanUser() == 1;

        Order order = new Order();
        order.setOrderNo(generateOrderNo()); // 作用：生成订单号；方法：调用订单号生成规则构造业务流水号
        order.setUserId(user.getId());
        order.setActivityId(activity.getId());
        order.setClubId(activity.getClubId());
        order.setSlotId(slot != null ? slot.getId() : null);
        order.setPhone(phone);
        order.setTitle(activity.getTitle());
        order.setClubName(club != null ? club.getName() : null);
        order.setClubLocation(club != null ? club.getLocation() : null);
        order.setSlotDate(slot != null ? slot.getSlotDate() : null);
        order.setSlotTime(slot != null ? slot.getSlotTime() : null);
        order.setStatus("unpaid"); // 作用：设置订单初始状态；方法：把新订单标记为待支付
        order.setPriceLevel(1);
        order.setUnitPrice(unitPrice);
        order.setOriginalPrice(original);
        order.setQuantity(request.getQuantity());
        order.setPayAmount(payAmount);
        order.setScanUserAtOrderTime(scanUser ? 1 : 0);
        order.setMerchantSettlementAmount(merchantSettlementAmount);
        order.setPlatformOperationFee(platformOperationFee);
        order.setUserCommissionAmount(commissionAmount);
        orderMapper.insert(order); // 作用：保存订单主记录；方法：调用 Mapper 将订单写入数据库
        commissionService.onOrderCreated(order, activity, user); // 作用：生成返利明细；方法：在订单创建后同步创建佣金记录
        if (!phone.equals(user.getPhone())) {
            user.setPhone(phone);
            userMapper.update(user);
        }
        return toResponse(orderMapper.findById(order.getId()), user, activity, commissionRecordMapper.findByOrderId(order.getId())); // 作用：返回订单结果；方法：查询最新订单并组装为响应对象
    }

    @Transactional
    public Optional<OrderResponse> pay(long id) {
        cancelIfExpired(id);
        Order order = orderMapper.findById(id);
        if (order == null) {
            return Optional.empty();
        }
        if (!"unpaid".equals(order.getStatus())) {
            return Optional.of(toResponse(order, userMapper.findById(order.getUserId()), activityMapper.findById(order.getActivityId()), commissionRecordMapper.findByOrderId(order.getId())));
        }
        order.setStatus("paid");
        order.setPaidAt(LocalDateTime.now());
        if (order.getVerifyCode() == null || order.getVerifyCode().isEmpty()) {
            order.setVerifyCode(generateVerifyCode());
        }
        orderMapper.update(order);
        return Optional.of(toResponse(orderMapper.findById(id), userMapper.findById(order.getUserId()), activityMapper.findById(order.getActivityId()), commissionRecordMapper.findByOrderId(id)));
    }

    @Transactional
    public Optional<OrderResponse> verify(long id) { // 作用：核销已支付订单；方法：校验订单状态后更新为已使用并触发返利流转
        Order order = orderMapper.findById(id); // 作用：查询待核销订单；方法：根据订单 id 读取当前订单记录
        if (order == null) {
            return Optional.empty();
        }
        if (!"paid".equals(order.getStatus())) {
            return Optional.of(toResponse(order, userMapper.findById(order.getUserId()), activityMapper.findById(order.getActivityId()), commissionRecordMapper.findByOrderId(order.getId())));
        }
        order.setStatus("used"); // 作用：更新订单状态；方法：把已支付订单改为已核销状态
        order.setVerifiedAt(LocalDateTime.now()); // 作用：记录核销时间；方法：使用当前时间写入 verifiedAt 字段
        orderMapper.update(order); // 作用：保存核销结果；方法：调用 Mapper 更新订单记录
        commissionService.onOrderVerified(order); // 作用：更新返利状态；方法：把该订单返利从待结算改为可提现
        return Optional.of(toResponse(orderMapper.findById(id), userMapper.findById(order.getUserId()), activityMapper.findById(order.getActivityId()), commissionRecordMapper.findByOrderId(id)));
        // 作用：返回核销后订单详情；方法：重新查询订单并组装响应对象
    }

    @Transactional
    public Optional<OrderResponse> refund(long id) {
        return refund(id, true, "订单退款冲销佣金");
    }

    @Transactional
    public Optional<OrderResponse> refund(long id, boolean reclaimCommission, String reversalReason) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return Optional.empty();
        }
        boolean fromPaid = "paid".equals(order.getStatus());
        boolean fromUsed = "used".equals(order.getStatus());
        if (!fromPaid && !fromUsed) {
            return Optional.of(toResponse(order, userMapper.findById(order.getUserId()), activityMapper.findById(order.getActivityId()), commissionRecordMapper.findByOrderId(order.getId())));
        }
        if (reclaimCommission) {
            String reason = reversalReason == null ? "" : reversalReason.trim();
            if (reason.isEmpty()) {
                reason = "订单退款冲销佣金";
            }
            commissionService.onOrderReversed(order, reason);
        }
        order.setStatus("refund");
        order.setRefundAt(LocalDateTime.now());
        orderMapper.update(order);
        if (order.getSlotId() != null && fromPaid) {
            releaseSlotCapacity(order.getSlotId(), order.getQuantity());
        }
        return Optional.of(toResponse(orderMapper.findById(id), userMapper.findById(order.getUserId()), activityMapper.findById(order.getActivityId()), commissionRecordMapper.findByOrderId(id)));
    }

    @Transactional
    public Optional<OrderResponse> cancel(long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return Optional.empty();
        }
        if (!"unpaid".equals(order.getStatus())) {
            return Optional.of(toResponse(order, userMapper.findById(order.getUserId()), activityMapper.findById(order.getActivityId()), commissionRecordMapper.findByOrderId(order.getId())));
        }
        commissionService.onOrderReversed(order, "订单取消冲销佣金");
        order.setStatus("cancelled");
        order.setCancelledAt(LocalDateTime.now());
        orderMapper.update(order);
        if (order.getSlotId() != null) {
            releaseSlotCapacity(order.getSlotId(), order.getQuantity());
        }
        return Optional.of(toResponse(orderMapper.findById(id), userMapper.findById(order.getUserId()), activityMapper.findById(order.getActivityId()), commissionRecordMapper.findByOrderId(id)));
    }

    @Transactional
    public boolean delete(long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return false;
        }
        if (commissionService.hasCommissionRecord(id)) {
            throw new IllegalArgumentException("存在佣金记录的订单不可删除");
        }
        if (("unpaid".equals(order.getStatus()) || "paid".equals(order.getStatus())) && order.getSlotId() != null) {
            releaseSlotCapacity(order.getSlotId(), order.getQuantity());
        }
        return orderMapper.delete(id) > 0;
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int suffix = 100 + random.nextInt(900);
        return time + suffix;
    }

    private String generateVerifyCode() {
        int a = 1000 + random.nextInt(9000);
        int b = 1000 + random.nextInt(9000);
        return a + " " + b;
    }

    private OrderResponse toResponse(Order order, User user, Activity activity, List<CommissionRecord> commissionRecords) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setActivityId(order.getActivityId());
        response.setClubId(order.getClubId());
        response.setSlotId(order.getSlotId());
        response.setPhone(order.getPhone());
        response.setUserPhone(user != null ? user.getPhone() : null);
        response.setTitle(order.getTitle());
        response.setClubName(order.getClubName());
        response.setClubLocation(order.getClubLocation());
        response.setSlotDate(order.getSlotDate());
        response.setSlotTime(order.getSlotTime());
        response.setStatus(order.getStatus());
        response.setStatusText(mapStatus(order.getStatus()));
        response.setPriceLevel(1);
        response.setUnitPrice(order.getUnitPrice());
        response.setOriginalPrice(BigDecimal.ZERO);
        response.setQuantity(order.getQuantity());
        response.setPayAmount(order.getPayAmount());
        response.setScanUserAtOrderTime(order.getScanUserAtOrderTime());
        response.setMerchantSettlementAmount(order.getMerchantSettlementAmount());
        response.setPlatformOperationFee(order.getPlatformOperationFee());
        response.setUserCommissionAmount(order.getUserCommissionAmount());
        populateCommissionBreakdown(response, order, user, activity, commissionRecords);
        response.setVerifyCode(order.getVerifyCode());
        response.setPaidAt(order.getPaidAt());
        response.setVerifiedAt(order.getVerifiedAt());
        response.setRefundAt(order.getRefundAt());
        response.setCancelledAt(order.getCancelledAt());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    private void populateCommissionBreakdown(OrderResponse response,
                                             Order order,
                                             User user,
                                             Activity activity,
                                             List<CommissionRecord> commissionRecords) {
        BigDecimal totalCommission = money(order.getUserCommissionAmount());
        if (order.getScanUserAtOrderTime() == null || order.getScanUserAtOrderTime() != 1 || totalCommission.signum() <= 0) {
            response.setUser1CommissionAmount(BigDecimal.ZERO);
            response.setUser2CommissionAmount(BigDecimal.ZERO);
            response.setUser3CommissionAmount(BigDecimal.ZERO);
            response.setOperatorCommissionAmount(BigDecimal.ZERO);
            return;
        }
        if (applyBreakdownFromRecords(response, order, totalCommission, commissionRecords)) {
            return;
        }
        int depth = resolveEffectiveBuyerDepth(user);
        BigDecimal quantity = new BigDecimal(order.getQuantity() != null && order.getQuantity() > 0 ? order.getQuantity() : 1);
        BigDecimal user2Commission = money(activity != null ? activity.getInviterCommissionAmount() : null).multiply(quantity);
        BigDecimal user3Commission = money(activity != null ? activity.getCommissionAmount() : null).multiply(quantity);

        BigDecimal user1Amount = BigDecimal.ZERO;
        BigDecimal user2Amount = BigDecimal.ZERO;
        BigDecimal user3Amount = BigDecimal.ZERO;
        BigDecimal operatorAmount = BigDecimal.ZERO;

        if (depth <= 1) {
            user1Amount = totalCommission;
        } else if (depth == 2) {
            user2Amount = user2Commission.min(totalCommission);
            user1Amount = totalCommission.subtract(user2Amount);
        } else {
            user3Amount = user3Commission.min(totalCommission);
            operatorAmount = totalCommission.subtract(user3Amount);
        }

        response.setUser1CommissionAmount(user1Amount.max(BigDecimal.ZERO));
        response.setUser2CommissionAmount(user2Amount.max(BigDecimal.ZERO));
        response.setUser3CommissionAmount(user3Amount.max(BigDecimal.ZERO));
        response.setOperatorCommissionAmount(operatorAmount.max(BigDecimal.ZERO));
    }

    private boolean applyBreakdownFromRecords(OrderResponse response,
                                              Order order,
                                              BigDecimal totalCommission,
                                              List<CommissionRecord> commissionRecords) {
        if (commissionRecords == null || commissionRecords.isEmpty()) {
            return false;
        }
        long buyerId = order.getUserId() != null ? order.getUserId() : -1L;
        BigDecimal buyerAmount = BigDecimal.ZERO;
        BigDecimal inviterAmount = BigDecimal.ZERO;
        BigDecimal operatorAmount = BigDecimal.ZERO;
        for (CommissionRecord record : commissionRecords) {
            if (record == null) {
                continue;
            }
            BigDecimal amount = money(record.getCommissionAmount());
            Long recordUserId = record.getUserId();
            if (recordUserId != null && recordUserId == buyerId) {
                buyerAmount = buyerAmount.add(amount);
            } else if (recordUserId != null && recordUserId == 0L) {
                operatorAmount = operatorAmount.add(amount);
            } else {
                inviterAmount = inviterAmount.add(amount);
            }
        }
        if (buyerAmount.signum() <= 0 && inviterAmount.signum() <= 0 && operatorAmount.signum() <= 0) {
            return false;
        }

        BigDecimal user1Amount = BigDecimal.ZERO;
        BigDecimal user2Amount = BigDecimal.ZERO;
        BigDecimal user3Amount = BigDecimal.ZERO;
        if (operatorAmount.signum() > 0) {
            user3Amount = buyerAmount;
        } else if (inviterAmount.signum() > 0) {
            user1Amount = inviterAmount;
            user2Amount = buyerAmount;
        } else {
            user1Amount = buyerAmount.signum() > 0 ? buyerAmount : totalCommission;
        }

        response.setUser1CommissionAmount(user1Amount.max(BigDecimal.ZERO));
        response.setUser2CommissionAmount(user2Amount.max(BigDecimal.ZERO));
        response.setUser3CommissionAmount(user3Amount.max(BigDecimal.ZERO));
        response.setOperatorCommissionAmount(operatorAmount.max(BigDecimal.ZERO));
        return true;
    }

    private int resolveEffectiveBuyerDepth(User user) {
        if (user == null || user.getScanUser() == null || user.getScanUser() != 1) {
            return 0;
        }
        Integer manualDepth = user.getDepth();
        if (manualDepth != null && manualDepth >= 1 && manualDepth <= 3) {
            return manualDepth;
        }
        int depth = 1;
        User current = user;
        Set<Long> visited = new java.util.HashSet<>();
        while (depth < 3 && current != null) {
            Long currentId = current.getId();
            if (currentId != null && !visited.add(currentId)) {
                break;
            }
            Long inviterId = current.getInviterId();
            if (inviterId == null || inviterId <= 0) {
                break;
            }
            User inviter = userMapper.findById(inviterId);
            if (inviter == null || inviter.getScanUser() == null || inviter.getScanUser() != 1) {
                break;
            }
            depth++;
            current = inviter;
        }
        return depth;
    }

    private String mapStatus(String status) {
        if ("unpaid".equals(status)) {
            return "待支付";
        }
        if ("paid".equals(status)) {
            return "已支付";
        }
        if ("used".equals(status)) {
            return "已核销";
        }
        if ("refund".equals(status)) {
            return "已退款";
        }
        if ("cancelled".equals(status)) {
            return "已取消";
        }
        return status;
    }

    private BigDecimal money(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal normalizeCommissionAmount(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal amount = value.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return amount;
    }

    private BigDecimal resolveSalePrice(Activity activity) {
        BigDecimal original = activity.getOriginalPrice();
        if (original != null && original.signum() > 0) {
            return original;
        }
        return money(activity.getBasePrice());
    }

    private void reserveSlotCapacity(long slotId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        int updated = slotMapper.updateBookedSafely(slotId, quantity);
        if (updated <= 0) {
            throw new IllegalArgumentException("名额不足");
        }
    }

    private void releaseSlotCapacity(long slotId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        int updated = slotMapper.updateBookedSafely(slotId, -quantity);
        if (updated <= 0) {
            throw new IllegalArgumentException("时段名额回滚失败，请稍后重试");
        }
    }

    @Scheduled(fixedDelayString = "${app.order.auto-cancel-interval-ms:30000}")
    public void autoCancelExpiredUnpaidOrders() {
        cancelExpiredUnpaidOrders();
    }

    @Transactional
    public int cancelExpiredUnpaidOrders() {
        LocalDateTime expireBefore = LocalDateTime.now().minusMinutes(UNPAID_TIMEOUT_MINUTES);
        List<Long> expiredIds = orderMapper.findExpiredUnpaidIds(expireBefore);
        if (expiredIds == null || expiredIds.isEmpty()) {
            return 0;
        }
        int cancelled = 0;
        for (Long orderId : expiredIds) {
            if (orderId == null) {
                continue;
            }
            Optional<OrderResponse> response = cancel(orderId);
            if (response.isPresent() && "cancelled".equals(response.get().getStatus())) {
                cancelled++;
            }
        }
        if (cancelled > 0) {
            log.info("auto-cancelled unpaid orders, count={}", cancelled);
        }
        return cancelled;
    }

    @Transactional
    public boolean cancelIfExpired(long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            return false;
        }
        if (!isUnpaidExpired(order)) {
            return false;
        }
        Optional<OrderResponse> response = cancel(orderId);
        return response.isPresent() && "cancelled".equals(response.get().getStatus());
    }

    private boolean isUnpaidExpired(Order order) {
        if (order == null || !"unpaid".equals(order.getStatus())) {
            return false;
        }
        if (order.getCreatedAt() == null) {
            return false;
        }
        LocalDateTime deadline = order.getCreatedAt().plusMinutes(UNPAID_TIMEOUT_MINUTES);
        return !deadline.isAfter(LocalDateTime.now());
    }

}
