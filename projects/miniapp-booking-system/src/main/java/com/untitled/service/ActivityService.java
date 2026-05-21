package com.untitled.service;

import com.untitled.dto.ActivityCreateRequest;
import com.untitled.dto.ActivityResponse;
import com.untitled.dto.ActivityUpdateRequest;
import com.untitled.mapper.ActivityMapper;
import com.untitled.mapper.BannerMapper;
import com.untitled.mapper.ClubMapper;
import com.untitled.mapper.UserMapper;
import com.untitled.model.Activity;
import com.untitled.model.Club;
import com.untitled.model.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ActivityService {
    private static final int INVITE_DEPTH_MAX = 3;
    private static final int ACTIVITY_TITLE_MAX_BYTES = 120;

    private final ActivityMapper activityMapper;
    private final ClubMapper clubMapper;
    private final UserMapper userMapper;
    private final StorageService storageService;
    private final BannerMapper bannerMapper;

    public ActivityService(ActivityMapper activityMapper,
                           ClubMapper clubMapper,
                           UserMapper userMapper,
                           StorageService storageService,
                           BannerMapper bannerMapper) {
        this.activityMapper = activityMapper;
        this.clubMapper = clubMapper;
        this.userMapper = userMapper;
        this.storageService = storageService;
        this.bannerMapper = bannerMapper;
    }

    /**
     * 作用：
     * 查询首页和分类页展示的活动列表数据。
     * 方法：
     * 先按条件查询活动，再读取俱乐部和当前用户信息，
     * 最后组装活动展示对象并返回前端。
     */
    public List<ActivityResponse> list(Long clubId, String category, String keyword, Long userId) {
        // 作用：查询前台活动列表；方法：按条件读取活动并补充俱乐部和用户信息
        List<Activity> list = activityMapper.findAll(clubId, category, keyword);
        // 作用：查询活动列表；方法：按俱乐部、分类和关键词从数据库筛选活动
        List<Club> clubs = clubMapper.findAll();
        // 作用：查询俱乐部信息；方法：读取全部俱乐部用于匹配活动归属
        java.util.Map<Long, Club> clubMap = clubs.stream().collect(Collectors.toMap(Club::getId, Function.identity()));
        // 作用：构建俱乐部映射；方法：把俱乐部列表转为 id 到对象的 Map
        User currentUser = userId != null ? userMapper.findById(userId) : null;
        // 作用：获取当前用户；方法：用户已登录时按 id 查询用户信息
        return list.stream()
                .filter(activity -> clubMap.containsKey(activity.getClubId()))
                // 作用：过滤异常活动；方法：只保留存在俱乐部归属的活动
                .map(activity -> toResponse(activity, clubMap.get(activity.getClubId()), currentUser))
                // 作用：转换展示对象；方法：组装活动、俱乐部和用户维度数据
                .collect(Collectors.toList()); // 作用：返回活动列表；方法：把流式结果收集为 List
    }

    public Optional<ActivityResponse> get(long id, Long userId) {
        Activity activity = activityMapper.findById(id);
        if (activity == null) {
            return Optional.empty();
        }
        Club club = clubMapper.findById(activity.getClubId());
        User currentUser = userId != null ? userMapper.findById(userId) : null;
        return Optional.of(toResponse(activity, club, currentUser));
    }

    public ActivityResponse create(ActivityCreateRequest request) {
        Activity activity = new Activity();
        applyRequest(activity, request);
        activityMapper.insert(activity);
        Club club = clubMapper.findById(activity.getClubId());
        return toResponse(activityMapper.findById(activity.getId()), club, null);
    }

    public Optional<ActivityResponse> update(long id, ActivityUpdateRequest request) {
        Activity activity = activityMapper.findById(id);
        if (activity == null) {
            return Optional.empty();
        }
        applyRequest(activity, request);
        activityMapper.update(activity);
        Club club = clubMapper.findById(activity.getClubId());
        return Optional.of(toResponse(activityMapper.findById(id), club, null));
    }

    public boolean delete(long id) {
        Activity activity = activityMapper.findById(id);
        if (activity == null) {
            return false;
        }
        int deleted = activityMapper.delete(id);
        if (deleted > 0) {
            cleanupActivityImages(activity);
        }
        return deleted > 0;
    }

    /**
     * 作用：
     * 处理后台新增活动时的字段赋值、价格拆分和图片整理。
     * 方法：
     * 先规范化封面、图集和详情图，再计算销售价、平台留存、佣金和商家结算价，
     * 最后统一写回活动对象，供新增接口保存。
     */
    private void applyRequest(Activity activity, ActivityCreateRequest request) { // 作用：处理新增活动字段；方法：整理图片、价格和返利字段后写回活动对象
        String cover = normalizeMedia(request.getCover()); // 作用：规范化封面地址；方法：统一清洗封面媒体链接
        List<String> gallery = normalizeMediaList(request.getGallery()).stream()
                .filter(url -> !url.equals(cover))
                .collect(Collectors.toList()); // 作用：整理活动图集；方法：过滤与封面重复的图片后收集为列表
        Set<String> occupied = new HashSet<>(); // 作用：记录已占用图片；方法：创建集合用于去重
        if (!cover.isEmpty()) {
            occupied.add(cover); // 作用：登记封面图片；方法：把封面地址加入已占用集合
        }
        occupied.addAll(gallery); // 作用：登记图集图片；方法：把图集地址加入已占用集合
        List<String> detailImages = normalizeMediaList(request.getDetailImages()).stream()
                .filter(url -> !occupied.contains(url))
                .collect(Collectors.toList()); // 作用：整理详情图；方法：过滤与封面和图集重复的图片后收集为列表
        BigDecimal salePrice = resolveInputSalePrice(request.getOriginalPrice(), request.getBasePrice()); // 作用：计算销售价；方法：统一从原价和基础价字段中解析有效售价
        String status = normalizeActivityStatus(request.getStatus());
        String title = normalizeTitle(request.getTitle());
        validateActivityBeforeSave(request.getClubId(), title, salePrice, status, cover);
        activity.setClubId(request.getClubId()); // 作用：设置所属俱乐部；方法：把请求中的 clubId 写入活动对象
        activity.setTitle(title); // 作用：设置活动标题；方法：把规范化后的请求标题写入活动对象
        activity.setSubtitle(request.getSubtitle()); // 作用：设置活动副标题；方法：把请求副标题写入活动对象
        activity.setCategory(request.getCategory()); // 作用：设置活动分类；方法：把请求分类写入活动对象
        activity.setOriginalPrice(salePrice); // 作用：保存原价；方法：把销售价写入 originalPrice 字段
        activity.setBasePrice(salePrice); // 作用：保存基础价；方法：把销售价同步写入 basePrice 字段
        BigDecimal platformFee = defaultMoney(request.getPlatformOperationFee()); // 作用：读取平台留存；方法：把空值金额标准化为 0
        BigDecimal buyerCommission = normalizeCommissionAmount(request.getBuyerCommissionAmount()); // 作用：读取一级返利；方法：标准化买家返利金额
        BigDecimal inviterCommission = normalizeCommissionAmount(request.getInviterCommissionAmount()); // 作用：读取邀请返利；方法：标准化邀请人返利金额
        BigDecimal thirdCommission = normalizeCommissionAmount(request.getCommissionAmount()); // 作用：读取其他返利；方法：标准化佣金金额
        activity.setPlatformOperationFee(platformFee); // 作用：保存平台留存；方法：把平台金额写入活动对象
        activity.setCommissionRate(normalizeCommissionRate(request.getCommissionRate())); // 作用：保存佣金比例；方法：标准化佣金比例后写入活动对象
        activity.setCommissionAmount(thirdCommission); // 作用：保存佣金金额；方法：把第三层返利写入活动对象
        activity.setBuyerCommissionAmount(buyerCommission); // 作用：保存买家返利；方法：把一级返利金额写入活动对象
        activity.setInviterCommissionAmount(inviterCommission); // 作用：保存邀请返利；方法：把二级返利金额写入活动对象
        activity.setMerchantSettlementPrice(resolveMerchantSettlementPrice(
                salePrice,
                platformFee,
                buyerCommission,
                inviterCommission,
                thirdCommission
        )); // 作用：计算商家结算价；方法：按销售价减平台留存和返利金额得到商家结算值
        activity.setAudience(request.getAudience()); // 作用：保存适用人群；方法：把 audience 字段写入活动对象
        activity.setDescription(request.getDescription()); // 作用：保存活动描述；方法：把描述文案写入活动对象
        activity.setBundle(request.getBundle()); // 作用：保存套餐说明；方法：把 bundle 字段写入活动对象
        activity.setExpireDate(request.getExpireDate()); // 作用：保存有效期；方法：把过期日期写入活动对象
        activity.setStatus(status); // 作用：保存活动状态；方法：有状态则用请求值，否则默认 active
        activity.setCover(cover); // 作用：保存封面图；方法：把清洗后的封面地址写入活动对象
        activity.setGallery(join(gallery)); // 作用：保存图集；方法：把图集列表拼接成字符串写入活动对象
        activity.setDetailImages(join(detailImages)); // 作用：保存详情图；方法：把详情图列表拼接成字符串写入活动对象
    }

    private void applyRequest(Activity activity, ActivityUpdateRequest request) {
        String cover = normalizeMedia(request.getCover());
        List<String> gallery = normalizeMediaList(request.getGallery()).stream()
                .filter(url -> !url.equals(cover))
                .collect(Collectors.toList());
        Set<String> occupied = new HashSet<>();
        if (!cover.isEmpty()) {
            occupied.add(cover);
        }
        occupied.addAll(gallery);
        List<String> detailImages = normalizeMediaList(request.getDetailImages()).stream()
                .filter(url -> !occupied.contains(url))
                .collect(Collectors.toList());
        BigDecimal salePrice = resolveInputSalePrice(request.getOriginalPrice(), request.getBasePrice());
        String status = normalizeActivityStatus(request.getStatus());
        String title = normalizeTitle(request.getTitle());
        validateActivityBeforeSave(request.getClubId(), title, salePrice, status, cover);
        activity.setClubId(request.getClubId());
        activity.setTitle(title);
        activity.setSubtitle(request.getSubtitle());
        activity.setCategory(request.getCategory());
        activity.setOriginalPrice(salePrice);
        activity.setBasePrice(salePrice);
        BigDecimal platformFee = defaultMoney(request.getPlatformOperationFee());
        BigDecimal buyerCommission = normalizeCommissionAmount(request.getBuyerCommissionAmount());
        BigDecimal inviterCommission = normalizeCommissionAmount(request.getInviterCommissionAmount());
        BigDecimal thirdCommission = normalizeCommissionAmount(request.getCommissionAmount());
        activity.setPlatformOperationFee(platformFee);
        activity.setCommissionRate(normalizeCommissionRate(request.getCommissionRate()));
        activity.setCommissionAmount(thirdCommission);
        activity.setBuyerCommissionAmount(buyerCommission);
        activity.setInviterCommissionAmount(inviterCommission);
        activity.setMerchantSettlementPrice(resolveMerchantSettlementPrice(
                salePrice,
                platformFee,
                buyerCommission,
                inviterCommission,
                thirdCommission
        ));
        activity.setAudience(request.getAudience());
        activity.setDescription(request.getDescription());
        activity.setBundle(request.getBundle());
        activity.setExpireDate(request.getExpireDate());
        activity.setStatus(status);
        activity.setCover(cover);
        activity.setGallery(join(gallery));
        activity.setDetailImages(join(detailImages));
    }

    private ActivityResponse toResponse(Activity activity, Club club, User currentUser) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setClubId(activity.getClubId());
        response.setClubName(club != null ? club.getName() : null);
        response.setClubLocation(club != null ? club.getLocation() : null);
        response.setTitle(activity.getTitle());
        response.setSubtitle(activity.getSubtitle());
        response.setCategory(activity.getCategory());
        response.setStatus(activity.getStatus());
        List<String> gallery = split(activity.getGallery());
        response.setCover(normalizeMedia(activity.getCover()));
        response.setGallery(gallery);
        response.setAudience(activity.getAudience());
        response.setDescription(activity.getDescription());
        response.setBundle(activity.getBundle());
        response.setExpireDate(activity.getExpireDate());
        response.setBasePrice(activity.getBasePrice());
        response.setOriginalPrice(activity.getOriginalPrice());
        response.setMerchantSettlementPrice(defaultMoney(activity.getMerchantSettlementPrice()));
        response.setPlatformOperationFee(defaultMoney(activity.getPlatformOperationFee()));
        response.setCommissionRate(normalizeCommissionRate(activity.getCommissionRate()));
        response.setCommissionAmount(normalizeCommissionAmount(activity.getCommissionAmount()));
        response.setBuyerCommissionAmount(normalizeCommissionAmount(activity.getBuyerCommissionAmount()));
        response.setInviterCommissionAmount(normalizeCommissionAmount(activity.getInviterCommissionAmount()));
        response.setDetailImages(split(activity.getDetailImages()));
        BigDecimal salePrice = resolveSalePrice(activity);
        response.setPriceLevel(1);
        response.setPrice(salePrice);
        response.setOriginal(BigDecimal.ZERO);
        boolean showCommission = currentUser != null && currentUser.getScanUser() != null && currentUser.getScanUser() == 1;
        response.setShowCommission(showCommission);
        if (showCommission) {
            BigDecimal expected = resolveExpectedCommissionByDisplayPrice(activity, response.getPrice(), currentUser);
            response.setExpectedCommission(expected);
            response.setCommissionTipText(buildCommissionTipText(activity, currentUser, expected));
        } else {
            response.setExpectedCommission(null);
            response.setCommissionTipText(null);
        }
        return response;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void validateCommissionRule(BigDecimal originalPrice,
                                        BigDecimal platformOperationFee,
                                        BigDecimal buyerCommissionAmount,
                                        BigDecimal inviterCommissionAmount,
                                        BigDecimal thirdCommissionAmount) {
        BigDecimal fee = defaultMoney(platformOperationFee);
        BigDecimal user1Commission = normalizeCommissionAmount(buyerCommissionAmount);
        BigDecimal user2Commission = normalizeCommissionAmount(inviterCommissionAmount);
        BigDecimal user3Commission = normalizeCommissionAmount(thirdCommissionAmount);
        if (fee.signum() < 0) {
            throw new IllegalArgumentException("填写有误：平台留存不能小于0");
        }
        if (user1Commission != null && user1Commission.signum() < 0) {
            throw new IllegalArgumentException("填写有误：用户1佣金金额不能小于0");
        }
        if (user2Commission != null && user2Commission.signum() < 0) {
            throw new IllegalArgumentException("填写有误：用户2佣金金额不能小于0");
        }
        if (user3Commission != null && user3Commission.signum() < 0) {
            throw new IllegalArgumentException("填写有误：用户3佣金金额不能小于0");
        }
        if (defaultMoney(user2Commission).compareTo(defaultMoney(user1Commission)) > 0) {
            throw new IllegalArgumentException("填写有误：用户2佣金不能大于用户1佣金");
        }
        if (defaultMoney(user3Commission).compareTo(defaultMoney(user1Commission)) > 0) {
            throw new IllegalArgumentException("填写有误：用户3佣金不能大于用户1佣金");
        }

        BigDecimal fixedTotal = fee.add(defaultMoney(user1Commission));
        BigDecimal original = defaultMoney(originalPrice);

        if (original.subtract(fixedTotal).signum() < 0) {
            throw new IllegalArgumentException("价格不成立：请确保 用户支付价 >= 平台留存 + 用户1佣金");
        }
    }

    private BigDecimal resolveMerchantSettlementPrice(BigDecimal originalPrice,
                                                      BigDecimal platformOperationFee,
                                                      BigDecimal buyerCommissionAmount,
                                                      BigDecimal inviterCommissionAmount,
                                                      BigDecimal thirdCommissionAmount) {
        validateCommissionRule(
                originalPrice,
                platformOperationFee,
                buyerCommissionAmount,
                inviterCommissionAmount,
                thirdCommissionAmount
        );
        return defaultMoney(originalPrice)
                .subtract(defaultMoney(platformOperationFee))
                .subtract(defaultMoney(buyerCommissionAmount))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCommissionRate(BigDecimal value) {
        BigDecimal rate = value != null ? value : new BigDecimal("100");
        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCommissionAmount(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveExpectedCommissionByDisplayPrice(Activity activity, BigDecimal displayPrice, User currentUser) {
        BigDecimal totalCommission = resolveDisplayTotalCommission(activity, displayPrice);
        if (totalCommission.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal buyer = resolveBuyerCommissionByDepth(activity, currentUser, totalCommission);
        BigDecimal max = defaultMoney(displayPrice).subtract(defaultMoney(activity.getMerchantSettlementPrice()));
        if (max.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return buyer.min(max);
    }

    private BigDecimal resolveDisplayTotalCommission(Activity activity, BigDecimal displayPrice) {
        BigDecimal user1Commission = normalizeCommissionAmount(activity.getBuyerCommissionAmount());
        return defaultMoney(user1Commission);
    }

    private BigDecimal resolveBuyerCommissionByDepth(Activity activity, User currentUser, BigDecimal totalCommission) {
        int depth = resolveEffectiveBuyerDepth(currentUser);
        if (depth <= 1) {
            return totalCommission;
        }
        BigDecimal expected = depth == 2
                ? defaultMoney(normalizeCommissionAmount(activity.getInviterCommissionAmount()))
                : defaultMoney(normalizeCommissionAmount(activity.getCommissionAmount()));
        if (expected.compareTo(totalCommission) > 0) {
            expected = totalCommission;
        }
        return expected;
    }

    private String buildCommissionTipText(Activity activity, User currentUser, BigDecimal expectedCommission) {
        BigDecimal amount = defaultMoney(expectedCommission).setScale(2, RoundingMode.HALF_UP);
        int chainDepth = resolveEffectiveBuyerDepth(currentUser);
        if (chainDepth <= 1) {
            return "预计佣金：¥" + amount.toPlainString();
        }
        return "预计自购佣金：¥" + amount.toPlainString();
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
        if (!isScanUser(user)) {
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

    private boolean isScanUser(User user) {
        return user != null && user.getScanUser() != null && user.getScanUser() == 1;
    }

    private Integer normalizeDepth(Integer depth) {
        if (depth == null || depth < 1 || depth > INVITE_DEPTH_MAX) {
            return null;
        }
        return depth;
    }

    private BigDecimal resolveSalePrice(Activity activity) {
        BigDecimal original = activity.getOriginalPrice();
        if (original != null && original.signum() > 0) {
            return original;
        }
        return defaultMoney(activity.getBasePrice());
    }

    private BigDecimal resolveInputSalePrice(BigDecimal originalPrice, BigDecimal basePrice) {
        BigDecimal original = defaultMoney(originalPrice);
        if (original.signum() > 0) {
            return originalPrice;
        }
        BigDecimal base = defaultMoney(basePrice);
        if (base.signum() > 0) {
            return basePrice;
        }
        throw new IllegalArgumentException("请至少填写一个商品售价");
    }

    private String normalizeTitle(String title) {
        return title == null ? "" : title.trim();
    }

    private String normalizeActivityStatus(String status) {
        String value = status == null ? "" : status.trim();
        if (value.isEmpty()) {
            return "active";
        }
        if ("active".equals(value) || "inactive".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("活动状态只能选择上架或下架");
    }

    private void validateActivityBeforeSave(Long clubId, String title, BigDecimal salePrice, String status, String cover) {
        if (clubId == null || clubMapper.findById(clubId) == null) {
            throw new IllegalArgumentException("请选择有效的俱乐部");
        }
        if (title.isEmpty()) {
            throw new IllegalArgumentException("活动标题不能为空");
        }
        if (title.getBytes(StandardCharsets.UTF_8).length > ACTIVITY_TITLE_MAX_BYTES) {
            throw new IllegalArgumentException("活动标题过长，请控制在 40 个中文以内，长卖点请填写到副标题或详情");
        }
        if (salePrice == null || salePrice.signum() <= 0) {
            throw new IllegalArgumentException("用户支付价必须大于0");
        }
        if ("active".equals(status) && (cover == null || cover.trim().isEmpty())) {
            throw new IllegalArgumentException("活动上架前请先上传封面图；未准备好可先选择下架保存");
        }
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(v -> v != null && !v.trim().isEmpty())
                .collect(Collectors.joining(","));
    }

    private List<String> split(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeMedia(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> normalizeMediaList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(
                values.stream()
                        .map(this::normalizeMedia)
                        .filter(v -> !v.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    private void cleanupActivityImages(Activity activity) {
        List<String> urls = new ArrayList<>();
        if (activity.getCover() != null) {
            urls.add(activity.getCover());
        }
        urls.addAll(split(activity.getGallery()));
        urls.addAll(split(activity.getDetailImages()));
        urls.stream()
                .filter(url -> url != null && !url.trim().isEmpty())
                .distinct()
                .forEach(url -> deleteIfUnused(url, activity.getId()));
    }

    private void deleteIfUnused(String url, long activityId) {
        try {
            if (activityMapper.countImageUsage(url, activityId) > 0) {
                return;
            }
            if (clubMapper.countImageUsage(url) > 0) {
                return;
            }
            if (bannerMapper.countImageUsage(url) > 0) {
                return;
            }
            storageService.delete(url);
        } catch (Exception ignored) {
            // ignore delete failures to avoid breaking deletion
        }
    }
}
