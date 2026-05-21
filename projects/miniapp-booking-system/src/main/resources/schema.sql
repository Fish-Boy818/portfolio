drop table if exists commission_withdrawal_items;
drop table if exists commission_withdrawals;
drop table if exists commission_records;
drop table if exists commission_accounts;
drop table if exists bounty_tasks;
drop table if exists welfare_submissions;
drop table if exists admin_credentials;
drop table if exists orders;
drop table if exists activity_slots;
drop table if exists activities;
drop table if exists categories;
drop table if exists profile_contents;
drop table if exists clubs;
drop table if exists users;
drop table if exists banners;

create table if not exists users (
  id bigint primary key auto_increment,
  open_id varchar(64) not null unique,
  nickname varchar(64),
  avatar_url varchar(255),
  phone varchar(32),
  depth tinyint,
  is_scan_user tinyint not null default 0,
  scan_activated_at datetime,
  inviter_id bigint,
  invited_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  index idx_user_inviter (inviter_id)
);

create table if not exists clubs (
  id bigint primary key auto_increment,
  name varchar(128) not null,
  location varchar(128),
  address varchar(255),
  phone varchar(32),
  open_time varchar(64),
  douyin_url varchar(255),
  tags text,
  cover varchar(255),
  license_image varchar(255),
  gallery text,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table if not exists profile_contents (
  id bigint primary key,
  notice_title varchar(128) not null default '',
  notice_content text,
  about_us_content text,
  platform_service_phone varchar(32) not null default '',
  review_mode_enabled tinyint not null default 0,
  site_activity_limit int not null default 6,
  tab_home_text varchar(32) not null default '首页',
  tab_category_text varchar(32) not null default '分类',
  tab_orders_text varchar(32) not null default '订单',
  tab_welfare_text varchar(32) not null default '悬赏',
  tab_profile_text varchar(32) not null default '我的',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table if not exists admin_credentials (
  id bigint primary key,
  username varchar(64) not null,
  password varchar(128) not null,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table if not exists categories (
  id bigint primary key auto_increment,
  `key` varchar(32) not null,
  name varchar(64) not null,
  sort int not null default 0,
  status varchar(20) not null default 'active',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_category_key (`key`),
  index idx_category_status (status),
  index idx_category_sort (sort)
);

create table if not exists activities (
  id bigint primary key auto_increment,
  club_id bigint not null,
  title varchar(128) not null,
  subtitle varchar(255),
  category varchar(32),
  base_price decimal(10,2) not null,
  original_price decimal(10,2),
  merchant_settlement_price decimal(10,2) not null default 0,
  platform_operation_fee decimal(10,2) not null default 0,
  commission_rate decimal(5,2) not null default 100.00,
  commission_amount decimal(10,2),
  buyer_commission_amount decimal(10,2),
  inviter_commission_amount decimal(10,2),
  audience varchar(64),
  description text,
  bundle text,
  expire_date date,
  status varchar(20) not null default 'active',
  cover varchar(255),
  gallery text,
  detail_images text,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  index idx_activity_club (club_id)
);

create table if not exists activity_slots (
  id bigint primary key auto_increment,
  activity_id bigint not null,
  slot_date date not null,
  slot_time varchar(32) not null,
  capacity int not null,
  booked int not null default 0,
  status varchar(20) not null default 'active',
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  index idx_slot_activity (activity_id)
);

create table if not exists orders (
  id bigint primary key auto_increment,
  order_no varchar(32) not null unique,
  user_id bigint not null,
  activity_id bigint not null,
  club_id bigint not null,
  slot_id bigint,
  phone varchar(32),
  title varchar(128) not null,
  club_name varchar(128),
  club_location varchar(128),
  slot_date date,
  slot_time varchar(32),
  status varchar(20) not null,
  price_level int not null,
  unit_price decimal(10,2) not null,
  original_price decimal(10,2),
  quantity int not null,
  pay_amount decimal(10,2) not null,
  is_scan_user_at_order_time tinyint not null default 0,
  merchant_settlement_amount decimal(10,2) not null default 0,
  platform_operation_fee decimal(10,2) not null default 0,
  user_commission_amount decimal(10,2) not null default 0,
  verify_code varchar(32),
  paid_at datetime,
  verified_at datetime,
  refund_at datetime,
  cancelled_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  index idx_order_user (user_id),
  index idx_order_activity (activity_id)
);

create table if not exists commission_accounts (
  user_id bigint primary key,
  withdrawable_balance decimal(10,2) not null default 0,
  pending_balance decimal(10,2) not null default 0,
  withdrawing_balance decimal(10,2) not null default 0,
  withdrawn_total decimal(10,2) not null default 0,
  reversed_total decimal(10,2) not null default 0,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp
);

create table if not exists commission_records (
  id bigint primary key auto_increment,
  user_id bigint not null,
  order_id bigint not null,
  order_no varchar(32) not null,
  activity_id bigint,
  activity_title varchar(128),
  is_scan_user_at_order_time tinyint not null default 0,
  price_amount decimal(10,2) not null default 0,
  merchant_settlement_amount decimal(10,2) not null default 0,
  platform_operation_fee decimal(10,2) not null default 0,
  commission_amount decimal(10,2) not null default 0,
  available_amount decimal(10,2) not null default 0,
  frozen_amount decimal(10,2) not null default 0,
  withdrawn_amount decimal(10,2) not null default 0,
  status varchar(20) not null default 'pending',
  occur_time datetime not null default current_timestamp,
  available_at datetime,
  reversed_at datetime,
  reversal_reason varchar(255),
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_commission_order_user (order_id, user_id),
  index idx_commission_user (user_id),
  index idx_commission_status (status),
  index idx_commission_order_no (order_no)
);

create table if not exists welfare_submissions (
  id bigint primary key auto_increment,
  submission_no varchar(40) not null unique,
  user_id bigint not null,
  bounty_task_id bigint,
  bounty_location varchar(128),
  platform_name varchar(64) not null,
  review_text varchar(255),
  screenshot_url varchar(255) not null,
  status varchar(20) not null default 'pending',
  reward_amount decimal(10,2) not null default 0,
  review_note varchar(255),
  reviewed_by varchar(64),
  reviewed_at datetime,
  reward_record_id bigint,
  rewarded_at datetime,
  delete_image_at datetime,
  image_deleted tinyint not null default 0,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  index idx_welfare_user (user_id),
  index idx_welfare_status (status),
  index idx_welfare_delete (delete_image_at)
);

create table if not exists bounty_tasks (
  id bigint primary key auto_increment,
  location varchar(128) not null,
  commission_min decimal(10,2) not null default 0,
  commission_max decimal(10,2) not null default 0,
  cover_image_url varchar(255) not null,
  detail_image_url varchar(255),
  steps_json text,
  step1_text text,
  step1_image_url varchar(255),
  step2_text text,
  step2_image_url varchar(255),
  step3_text text,
  step3_image_url varchar(255),
  status varchar(20) not null default 'active',
  sort int not null default 0,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  index idx_bounty_status_sort (status, sort)
);

create table if not exists bounty_claims (
  id bigint primary key auto_increment,
  bounty_task_id bigint not null,
  user_id bigint not null,
  status varchar(20) not null default 'accepted',
  latest_submission_id bigint,
  accepted_at datetime,
  submitted_at datetime,
  reviewed_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_bounty_claim_user_task (bounty_task_id, user_id),
  index idx_bounty_claim_user (user_id),
  index idx_bounty_claim_status (status)
);

create table if not exists commission_withdrawals (
  id bigint primary key auto_increment,
  withdraw_no varchar(40) not null unique,
  user_id bigint not null,
  amount decimal(10,2) not null,
  status varchar(20) not null default 'pending',
  idem_key varchar(64),
  fail_reason varchar(255),
  transfer_bill_no varchar(64),
  transfer_state varchar(32),
  transfer_package_info varchar(1024),
  operator_name varchar(64),
  operator_note varchar(255),
  requested_at datetime not null default current_timestamp,
  processed_at datetime,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  unique key uk_withdraw_user_idem (user_id, idem_key),
  index idx_withdraw_user (user_id),
  index idx_withdraw_status (status)
);

create table if not exists commission_withdrawal_items (
  id bigint primary key auto_increment,
  withdrawal_id bigint not null,
  commission_record_id bigint not null,
  amount decimal(10,2) not null,
  created_at datetime not null default current_timestamp,
  index idx_withdraw_item_withdrawal (withdrawal_id),
  index idx_withdraw_item_record (commission_record_id)
);

create table if not exists banners (
  id bigint primary key auto_increment,
  title varchar(128),
  subtitle varchar(255),
  image_url varchar(255),
  status varchar(20) not null default 'active',
  sort int not null default 0,
  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,
  index idx_banner_status (status),
  index idx_banner_sort (sort)
);
