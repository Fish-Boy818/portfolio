package com.untitled.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class SchemaMigrationRunner {
    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationRunner.class);
    private final JdbcTemplate jdbcTemplate;
    private final AdminProperties adminProperties;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate, AdminProperties adminProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.adminProperties = adminProperties;
    }

    @PostConstruct
    public void migrateColumns() {
        migrateUsersDepthColumn();
        migrateClubsDouyinUrlColumn();
        migrateClubsLicenseImageColumn();
        seedSampleClub();
        migrateAdminCredentialsTable();
        migrateProfileContentsTable();
        migrateWelfareSubmissionsTable();
        migrateBountyTasksTable();
        migrateBountyClaimsTable();
    }

    private void migrateUsersDepthColumn() {
        try {
            if (!columnExists("users", "depth")) {
                jdbcTemplate.execute("alter table users add column depth tinyint null after phone");
                log.info("schema migrated: users.depth added");
                return;
            }
            Integer nullable = jdbcTemplate.queryForObject(
                    "select case when is_nullable = 'YES' then 1 else 0 end from information_schema.columns " +
                            "where table_schema = database() and table_name = 'users' and column_name = 'depth' limit 1",
                    Integer.class
            );
            if (nullable != null && nullable == 0) {
                jdbcTemplate.execute("alter table users modify column depth tinyint null");
                log.info("schema migrated: users.depth set nullable");
            }
        } catch (Exception ex) {
            log.warn("schema migration for users.depth skipped: {}", ex.getMessage());
        }
    }

    private void migrateClubsDouyinUrlColumn() {
        try {
            if (!columnExists("clubs", "douyin_url")) {
                jdbcTemplate.execute("alter table clubs add column douyin_url varchar(255) null after open_time");
                log.info("schema migrated: clubs.douyin_url added");
            }
        } catch (Exception ex) {
            log.warn("schema migration for clubs.douyin_url skipped: {}", ex.getMessage());
        }
    }

    private void migrateClubsLicenseImageColumn() {
        try {
            if (!columnExists("clubs", "license_image")) {
                jdbcTemplate.execute("alter table clubs add column license_image varchar(255) null after cover");
                log.info("schema migrated: clubs.license_image added");
            }
        } catch (Exception ex) {
            log.warn("schema migration for clubs.license_image skipped: {}", ex.getMessage());
        }
    }

    private void seedSampleClub() {
        try {
            Integer count = jdbcTemplate.queryForObject("select count(1) from clubs where name = ?", Integer.class, "示例海上运动中心");
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.update(
                    "insert into clubs (name, location, address, phone, open_time, tags, cover, license_image, gallery, created_at, updated_at) " +
                            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())",
                    "示例海上运动中心",
                    "三亚后海",
                    "三亚后海",
                    "",
                    "",
                    "摩托艇,海上项目",
                    "/uploads/4a546d26bf9d4810b47ed0bd6ce13f89.jpg",
                    "",
                    ""
            );
            log.info("schema migrated: default sample club created");
        } catch (Exception ex) {
            log.warn("default sample club skipped: {}", ex.getMessage());
        }
    }

    private void migrateProfileContentsTable() {
        try {
            jdbcTemplate.execute(
                    "create table if not exists profile_contents (" +
                            "id bigint primary key, " +
                            "notice_title varchar(128) not null default '', " +
                            "notice_content text, " +
                            "about_us_content text, " +
                            "platform_service_phone varchar(32) not null default '', " +
                            "review_mode_enabled tinyint not null default 0, " +
                            "site_activity_limit int not null default 6, " +
                            "tab_home_text varchar(32) not null default '首页', " +
                            "tab_category_text varchar(32) not null default '分类', " +
                            "tab_orders_text varchar(32) not null default '订单', " +
                            "tab_welfare_text varchar(32) not null default '悬赏', " +
                            "tab_profile_text varchar(32) not null default '我的', " +
                            "created_at datetime not null default current_timestamp, " +
                            "updated_at datetime not null default current_timestamp on update current_timestamp" +
                            ")"
            );
            if (!columnExists("profile_contents", "platform_service_phone")) {
                jdbcTemplate.execute("alter table profile_contents add column platform_service_phone varchar(32) not null default '' after about_us_content");
                log.info("schema migrated: profile_contents.platform_service_phone added");
            }
            ensureProfileContentColumn("review_mode_enabled", "tinyint not null default 0", "platform_service_phone");
            ensureProfileContentColumn("site_activity_limit", "int not null default 6", "review_mode_enabled");
            ensureProfileContentColumn("tab_home_text", "varchar(32) not null default '首页'", "site_activity_limit");
            ensureProfileContentColumn("tab_category_text", "varchar(32) not null default '分类'", "tab_home_text");
            ensureProfileContentColumn("tab_orders_text", "varchar(32) not null default '订单'", "tab_category_text");
            ensureProfileContentColumn("tab_welfare_text", "varchar(32) not null default '悬赏'", "tab_orders_text");
            ensureProfileContentColumn("tab_profile_text", "varchar(32) not null default '我的'", "tab_welfare_text");
            Integer count = jdbcTemplate.queryForObject("select count(1) from profile_contents", Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.update(
                        "insert into profile_contents (id, notice_title, notice_content, about_us_content, platform_service_phone, review_mode_enabled, site_activity_limit, tab_home_text, tab_category_text, tab_orders_text, tab_welfare_text, tab_profile_text, created_at, updated_at) " +
                                "values (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())",
                        "平台公告",
                        "欢迎来到示例海上运动平台，最新活动与服务说明请以平台公告为准。",
                        "示例平台专注海上运动体验服务，提供多城市俱乐部预约、活动下单与售后保障。",
                        "",
                        0,
                        6,
                        "首页",
                        "分类",
                        "订单",
                        "悬赏",
                        "我的"
                );
                log.info("schema migrated: profile_contents default row created");
            }
            jdbcTemplate.update("update profile_contents set review_mode_enabled = 0 where review_mode_enabled is null");
            jdbcTemplate.update("update profile_contents set site_activity_limit = 6 where site_activity_limit is null or site_activity_limit < 1");
            jdbcTemplate.update("update profile_contents set tab_welfare_text = '悬赏' where tab_welfare_text is null or trim(tab_welfare_text) = '' or tab_welfare_text = '玩福利'");
        } catch (Exception ex) {
            log.warn("schema migration for profile_contents skipped: {}", ex.getMessage());
        }
    }

    private void migrateWelfareSubmissionsTable() {
        try {
            jdbcTemplate.execute(
                    "create table if not exists welfare_submissions (" +
                            "id bigint primary key auto_increment, " +
                            "submission_no varchar(40) not null unique, " +
                            "user_id bigint not null, " +
                            "platform_name varchar(64) not null, " +
                            "review_text varchar(255), " +
                            "screenshot_url varchar(2000) not null, " +
                            "status varchar(20) not null default 'pending', " +
                            "reward_amount decimal(10,2) not null default 0, " +
                            "review_note varchar(255), " +
                            "reviewed_by varchar(64), " +
                            "reviewed_at datetime, " +
                            "reward_record_id bigint, " +
                            "rewarded_at datetime, " +
                            "delete_image_at datetime, " +
                            "image_deleted tinyint not null default 0, " +
                            "created_at datetime not null default current_timestamp, " +
                            "updated_at datetime not null default current_timestamp on update current_timestamp" +
                            ")"
            );
            ensureWelfareSubmissionColumn("bounty_task_id", "bigint null", "user_id");
            ensureWelfareSubmissionColumn("bounty_location", "varchar(128) null", "bounty_task_id");
        } catch (Exception ex) {
            log.warn("schema migration for welfare_submissions skipped: {}", ex.getMessage());
        }
    }

    private void migrateBountyTasksTable() {
        try {
            jdbcTemplate.execute(
                    "create table if not exists bounty_tasks (" +
                            "id bigint primary key auto_increment, " +
                            "location varchar(128) not null, " +
                            "commission_min decimal(10,2) not null default 0, " +
                            "commission_max decimal(10,2) not null default 0, " +
                            "cover_image_url varchar(255) not null, " +
                            "detail_image_url varchar(255), " +
                            "steps_json text, " +
                            "step1_text text, " +
                            "step1_image_url varchar(255), " +
                            "step2_text text, " +
                            "step2_image_url varchar(255), " +
                            "step3_text text, " +
                            "step3_image_url varchar(255), " +
                            "status varchar(20) not null default 'active', " +
                            "sort int not null default 0, " +
                            "created_at datetime not null default current_timestamp, " +
                            "updated_at datetime not null default current_timestamp on update current_timestamp" +
                            ")"
            );
            ensureBountyTaskColumn("steps_json", "text", "detail_image_url");
        } catch (Exception ex) {
            log.warn("schema migration for bounty_tasks skipped: {}", ex.getMessage());
        }
    }

    private void migrateBountyClaimsTable() {
        try {
            jdbcTemplate.execute(
                    "create table if not exists bounty_claims (" +
                            "id bigint primary key auto_increment, " +
                            "bounty_task_id bigint not null, " +
                            "user_id bigint not null, " +
                            "status varchar(20) not null default 'accepted', " +
                            "latest_submission_id bigint, " +
                            "accepted_at datetime, " +
                            "submitted_at datetime, " +
                            "reviewed_at datetime, " +
                            "created_at datetime not null default current_timestamp, " +
                            "updated_at datetime not null default current_timestamp on update current_timestamp, " +
                            "unique key uk_bounty_claim_user_task (bounty_task_id, user_id), " +
                            "index idx_bounty_claim_user (user_id), " +
                            "index idx_bounty_claim_status (status)" +
                            ")"
            );
        } catch (Exception ex) {
            log.warn("schema migration for bounty_claims skipped: {}", ex.getMessage());
        }
    }

    private void migrateAdminCredentialsTable() {
        try {
            jdbcTemplate.execute(
                    "create table if not exists admin_credentials (" +
                            "id bigint primary key, " +
                            "username varchar(64) not null, " +
                            "password varchar(128) not null, " +
                            "created_at datetime not null default current_timestamp, " +
                            "updated_at datetime not null default current_timestamp on update current_timestamp" +
                            ")"
            );
            Integer count = jdbcTemplate.queryForObject("select count(1) from admin_credentials", Integer.class);
            if (count == null || count == 0) {
                jdbcTemplate.update(
                        "insert into admin_credentials (id, username, password, created_at, updated_at) values (1, ?, ?, now(), now())",
                        defaultAdminUsername(),
                        defaultAdminPassword()
                );
                log.info("schema migrated: admin_credentials default row created");
            }
        } catch (Exception ex) {
            log.warn("schema migration for admin_credentials skipped: {}", ex.getMessage());
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from information_schema.columns " +
                        "where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class,
                table,
                column
        );
        return count != null && count > 0;
    }

    private void ensureProfileContentColumn(String column, String definition, String afterColumn) {
        if (!columnExists("profile_contents", column)) {
            jdbcTemplate.execute("alter table profile_contents add column " + column + " " + definition + " after " + afterColumn);
            log.info("schema migrated: profile_contents.{} added", column);
        }
    }

    private void ensureWelfareSubmissionColumn(String column, String definition, String afterColumn) {
        if (!columnExists("welfare_submissions", column)) {
            jdbcTemplate.execute("alter table welfare_submissions add column " + column + " " + definition + " after " + afterColumn);
            log.info("schema migrated: welfare_submissions.{} added", column);
        }
    }

    private void ensureBountyTaskColumn(String column, String definition, String afterColumn) {
        if (!columnExists("bounty_tasks", column)) {
            jdbcTemplate.execute("alter table bounty_tasks add column " + column + " " + definition + " after " + afterColumn);
            log.info("schema migrated: bounty_tasks.{} added", column);
        }
    }

    private String defaultAdminUsername() {
        String value = trimToNull(adminProperties.getUsername());
        return value != null ? value : "admin";
    }

    private String defaultAdminPassword() {
        String value = trimToNull(adminProperties.getPassword());
        return value != null ? value : "admin123456";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
