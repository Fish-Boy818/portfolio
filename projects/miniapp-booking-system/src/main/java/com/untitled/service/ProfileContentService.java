package com.untitled.service;

import com.untitled.dto.ProfileContentResponse;
import com.untitled.dto.ProfileContentUpdateRequest;
import com.untitled.mapper.ProfileContentMapper;
import com.untitled.model.ProfileContent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProfileContentService {
    private static final long DEFAULT_ID = 1L;
    private static final String DEFAULT_NOTICE_TITLE = "平台公告";
    private static final String DEFAULT_NOTICE_CONTENT = "欢迎来到示例海上运动平台，最新活动与服务说明请以平台公告为准。";
    private static final String DEFAULT_ABOUT_US_CONTENT = "示例平台专注海上运动体验服务，提供多城市俱乐部预约、活动下单与售后保障。";
    private static final String DEFAULT_TAB_HOME_TEXT = "首页";
    private static final String DEFAULT_TAB_CATEGORY_TEXT = "分类";
    private static final String DEFAULT_TAB_ORDERS_TEXT = "订单";
    private static final String DEFAULT_TAB_WELFARE_TEXT = "悬赏";
    private static final String DEFAULT_TAB_PROFILE_TEXT = "我的";
    private static final String DEFAULT_PLATFORM_SERVICE_PHONE = "";
    private static final int DEFAULT_REVIEW_MODE_ENABLED = 0;
    private static final int DEFAULT_SITE_ACTIVITY_LIMIT = 6;

    private final ProfileContentMapper profileContentMapper;

    public ProfileContentService(ProfileContentMapper profileContentMapper) {
        this.profileContentMapper = profileContentMapper;
    }

    public List<ProfileContentResponse> listAll() {
        ensureDefaultRecord();
        List<ProfileContent> list = profileContentMapper.findAll();
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ProfileContentResponse getCurrent() {
        ensureDefaultRecord();
        ProfileContent content = profileContentMapper.findFirst();
        if (content == null) {
            content = createDefaultRecord();
        }
        return toResponse(content);
    }

    public Optional<ProfileContentResponse> get(long id) {
        ensureDefaultRecord();
        ProfileContent content = profileContentMapper.findById(id);
        if (content == null) {
            return Optional.empty();
        }
        return Optional.of(toResponse(content));
    }

    public ProfileContentResponse create(ProfileContentUpdateRequest request) {
        ensureDefaultRecord();
        ProfileContent content = profileContentMapper.findById(DEFAULT_ID);
        if (content == null) {
            content = createDefaultRecord();
        }
        applyRequest(content, request);
        profileContentMapper.update(content);
        return toResponse(profileContentMapper.findById(DEFAULT_ID));
    }

    public Optional<ProfileContentResponse> update(long id, ProfileContentUpdateRequest request) {
        // 作用：更新页面内容配置；方法：按 id 查询内容后写入最新公告和文案字段
        ensureDefaultRecord(); // 作用：保证默认记录存在；方法：在更新前检查并补齐默认页面内容
        ProfileContent content = profileContentMapper.findById(id); // 作用：查询待修改记录；方法：根据页面内容 id 读取数据库记录
        if (content == null) {
            return Optional.empty();
        }
        applyRequest(content, request); // 作用：写入页面字段；方法：把公告、关于我们和导航配置覆盖到实体对象中
        profileContentMapper.update(content); // 作用：保存页面配置；方法：调用 Mapper 更新数据库记录
        return Optional.of(toResponse(profileContentMapper.findById(id))); // 作用：返回最新配置；方法：重新查询更新后的记录并转换为响应对象
    }

    private void ensureDefaultRecord() { // 作用：保证系统存在默认页面内容记录；方法：先查询首条记录，不存在时自动创建默认数据
        ProfileContent first = profileContentMapper.findFirst(); // 作用：查询首条页面内容记录；方法：调用 Mapper 从数据库读取第一条配置数据
        if (first != null) {
            return; // 作用：结束初始化流程；方法：已存在默认记录时直接返回
        }
        createDefaultRecord(); // 作用：创建默认页面内容；方法：调用默认记录创建方法补齐初始化数据
    }

    private ProfileContent createDefaultRecord() { // 作用：创建默认页面内容记录；方法：构造默认对象并插入数据库
        ProfileContent content = new ProfileContent(); // 作用：创建页面内容对象；方法：实例化 ProfileContent 作为默认数据载体
        content.setId(DEFAULT_ID); // 作用：设置默认主键；方法：把预定义常量写入 id 字段
        content.setNoticeTitle(DEFAULT_NOTICE_TITLE); // 作用：设置默认公告标题；方法：写入系统预设公告标题
        content.setNoticeContent(DEFAULT_NOTICE_CONTENT); // 作用：设置默认公告内容；方法：写入系统预设公告文案
        content.setAboutUsContent(DEFAULT_ABOUT_US_CONTENT); // 作用：设置默认关于我们内容；方法：写入系统预设平台介绍
        content.setTabHomeText(DEFAULT_TAB_HOME_TEXT); // 作用：设置首页导航文案；方法：写入默认首页标签文本
        content.setTabCategoryText(DEFAULT_TAB_CATEGORY_TEXT); // 作用：设置分类导航文案；方法：写入默认分类标签文本
        content.setTabOrdersText(DEFAULT_TAB_ORDERS_TEXT); // 作用：设置订单导航文案；方法：写入默认订单标签文本
        content.setTabWelfareText(DEFAULT_TAB_WELFARE_TEXT); // 作用：设置福利导航文案；方法：写入默认福利标签文本
        content.setTabProfileText(DEFAULT_TAB_PROFILE_TEXT); // 作用：设置我的导航文案；方法：写入默认个人中心标签文本
        content.setPlatformServicePhone(DEFAULT_PLATFORM_SERVICE_PHONE); // 作用：设置默认客服电话；方法：写入系统默认服务电话
        content.setReviewModeEnabled(DEFAULT_REVIEW_MODE_ENABLED); // 作用：设置默认审核模式；方法：写入系统默认审核开关值
        content.setSiteActivityLimit(DEFAULT_SITE_ACTIVITY_LIMIT);
        profileContentMapper.insert(content); // 作用：保存默认页面内容；方法：调用 Mapper 将默认记录插入数据库
        return profileContentMapper.findById(DEFAULT_ID); // 作用：返回默认记录；方法：按默认 id 重新查询并返回最新数据
    }

    private void applyRequest(ProfileContent content, ProfileContentUpdateRequest request) {
        if (content == null || request == null) {
            return;
        }
        content.setNoticeTitle(normalizeSingleLine(request.getNoticeTitle(), DEFAULT_NOTICE_TITLE));
        content.setNoticeContent(normalizeMultiline(request.getNoticeContent()));
        content.setAboutUsContent(normalizeMultiline(request.getAboutUsContent()));
        content.setTabHomeText(normalizeSingleLine(request.getTabHomeText(), DEFAULT_TAB_HOME_TEXT));
        content.setTabCategoryText(normalizeSingleLine(request.getTabCategoryText(), DEFAULT_TAB_CATEGORY_TEXT));
        content.setTabOrdersText(normalizeSingleLine(request.getTabOrdersText(), DEFAULT_TAB_ORDERS_TEXT));
        content.setTabWelfareText(normalizeSingleLine(request.getTabWelfareText(), DEFAULT_TAB_WELFARE_TEXT));
        content.setTabProfileText(normalizeSingleLine(request.getTabProfileText(), DEFAULT_TAB_PROFILE_TEXT));
        content.setPlatformServicePhone(normalizeSingleLine(request.getPlatformServicePhone(), DEFAULT_PLATFORM_SERVICE_PHONE));
        content.setReviewModeEnabled(
                normalizeSwitch(
                        request.getReviewModeEnabled(),
                        normalizeSwitch(content.getReviewModeEnabled(), DEFAULT_REVIEW_MODE_ENABLED)
                )
        );
        content.setSiteActivityLimit(normalizeLimit(request.getSiteActivityLimit(), normalizeLimit(content.getSiteActivityLimit(), DEFAULT_SITE_ACTIVITY_LIMIT)));
    }

    private String normalizeSingleLine(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        return text;
    }

    private String normalizeMultiline(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").trim();
    }

    private int normalizeSwitch(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return value == 1 ? 1 : 0;
    }

    private int normalizeLimit(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value < 1) {
            return 1;
        }
        return Math.min(value, 50);
    }

    private ProfileContentResponse toResponse(ProfileContent content) {
        ProfileContentResponse response = new ProfileContentResponse();
        response.setId(content.getId());
        response.setNoticeTitle(content.getNoticeTitle());
        response.setNoticeContent(content.getNoticeContent());
        response.setAboutUsContent(content.getAboutUsContent());
        response.setTabHomeText(normalizeSingleLine(content.getTabHomeText(), DEFAULT_TAB_HOME_TEXT));
        response.setTabCategoryText(normalizeSingleLine(content.getTabCategoryText(), DEFAULT_TAB_CATEGORY_TEXT));
        response.setTabOrdersText(normalizeSingleLine(content.getTabOrdersText(), DEFAULT_TAB_ORDERS_TEXT));
        response.setTabWelfareText(normalizeSingleLine(content.getTabWelfareText(), DEFAULT_TAB_WELFARE_TEXT));
        response.setTabProfileText(normalizeSingleLine(content.getTabProfileText(), DEFAULT_TAB_PROFILE_TEXT));
        response.setPlatformServicePhone(normalizeSingleLine(content.getPlatformServicePhone(), DEFAULT_PLATFORM_SERVICE_PHONE));
        response.setReviewModeEnabled(normalizeSwitch(content.getReviewModeEnabled(), DEFAULT_REVIEW_MODE_ENABLED));
        response.setSiteActivityLimit(normalizeLimit(content.getSiteActivityLimit(), DEFAULT_SITE_ACTIVITY_LIMIT));
        response.setUpdatedAt(content.getUpdatedAt());
        return response;
    }
}
