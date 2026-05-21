package com.untitled.mapper;

import com.untitled.model.ProfileContent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ProfileContentMapper {
    @Select("select id, notice_title, notice_content, about_us_content, tab_home_text, tab_category_text, tab_orders_text, tab_welfare_text, tab_profile_text, platform_service_phone, review_mode_enabled, site_activity_limit, created_at, updated_at " +
            "from profile_contents order by id asc")
    List<ProfileContent> findAll();

    @Select("select id, notice_title, notice_content, about_us_content, tab_home_text, tab_category_text, tab_orders_text, tab_welfare_text, tab_profile_text, platform_service_phone, review_mode_enabled, site_activity_limit, created_at, updated_at " +
            "from profile_contents where id = #{id}")
    ProfileContent findById(long id);

    @Select("select id, notice_title, notice_content, about_us_content, tab_home_text, tab_category_text, tab_orders_text, tab_welfare_text, tab_profile_text, platform_service_phone, review_mode_enabled, site_activity_limit, created_at, updated_at " +
            "from profile_contents order by id asc limit 1")
    ProfileContent findFirst();

    @Insert("insert into profile_contents (id, notice_title, notice_content, about_us_content, tab_home_text, tab_category_text, tab_orders_text, tab_welfare_text, tab_profile_text, platform_service_phone, review_mode_enabled, site_activity_limit, created_at, updated_at) " +
            "values (#{id}, #{noticeTitle}, #{noticeContent}, #{aboutUsContent}, #{tabHomeText}, #{tabCategoryText}, #{tabOrdersText}, #{tabWelfareText}, #{tabProfileText}, #{platformServicePhone}, #{reviewModeEnabled}, #{siteActivityLimit}, now(), now())")
    int insert(ProfileContent content);

    @Update("update profile_contents set notice_title = #{noticeTitle}, notice_content = #{noticeContent}, " +
            "about_us_content = #{aboutUsContent}, tab_home_text = #{tabHomeText}, tab_category_text = #{tabCategoryText}, " +
            "tab_orders_text = #{tabOrdersText}, tab_welfare_text = #{tabWelfareText}, tab_profile_text = #{tabProfileText}, " +
            "platform_service_phone = #{platformServicePhone}, review_mode_enabled = #{reviewModeEnabled}, site_activity_limit = #{siteActivityLimit}, updated_at = now() where id = #{id}")
    int update(ProfileContent content);
}
