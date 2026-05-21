insert into clubs (id, name, location, address, phone, tags, cover, license_image, gallery)
values
  (1, '蓝鲸海上俱乐部', '三亚 · 亚龙湾', '三亚市亚龙湾海边码头', '13312340001', '摩托艇,冲浪,潜水', '/assets/images/clubs/club-1.png', '', '/assets/images/clubs/club-1.png,/assets/images/clubs/club-2.png,/assets/images/clubs/club-3.png'),
  (2, '珊瑚湾冲浪基地', '厦门 · 白城', '厦门市白城沙滩入口', '13312340002', '冲浪,桨板,日落', '/assets/images/clubs/club-2.png', '', '/assets/images/clubs/club-2.png,/assets/images/clubs/club-4.png,/assets/images/clubs/club-6.png'),
  (3, '疾风摩托艇中心', '青岛 · 石老人', '青岛市石老人海滨', '13312340003', '摩托艇,拖拽伞,教练', '/assets/images/clubs/club-3.png', '', '/assets/images/clubs/club-3.png,/assets/images/clubs/club-5.png,/assets/images/clubs/club-7.png'),
  (4, '日出码头水上会', '珠海 · 横琴', '珠海市横琴口岸码头', '13312340004', '香蕉船,亲子,沙滩', '/assets/images/clubs/club-4.png', '', '/assets/images/clubs/club-4.png,/assets/images/clubs/club-1.png,/assets/images/clubs/club-8.png'),
  (5, '银贝海洋运动', '北海 · 银滩', '北海市银滩东区', '13312340005', '摩托艇,滑水,摄影', '/assets/images/clubs/club-5.png', '', '/assets/images/clubs/club-5.png,/assets/images/clubs/club-6.png,/assets/images/clubs/club-9.png'),
  (6, '潮汐港运动站', '深圳 · 大鹏', '深圳市大鹏新区金沙湾', '13312340006', '冲浪,潜水,营地', '/assets/images/clubs/club-6.png', '', '/assets/images/clubs/club-6.png,/assets/images/clubs/club-2.png,/assets/images/clubs/club-10.png'),
  (7, '海风沙滩俱乐部', '湛江 · 南三岛', '湛江市南三岛海岸', '13312340007', '香蕉船,摩托艇,团建', '/assets/images/clubs/club-7.png', '', '/assets/images/clubs/club-7.png,/assets/images/clubs/club-3.png,/assets/images/clubs/club-8.png'),
  (8, '浪屿水上营地', '宁波 · 象山', '宁波市象山松兰山', '13312340008', '桨板,冲浪,露营', '/assets/images/clubs/club-8.png', '', '/assets/images/clubs/club-8.png,/assets/images/clubs/club-6.png,/assets/images/clubs/club-9.png'),
  (9, '深蓝潜行中心', '海南 · 万宁', '海南省万宁市日月湾', '13312340009', '潜水,冲浪,摄影', '/assets/images/clubs/club-9.png', '', '/assets/images/clubs/club-9.png,/assets/images/clubs/club-4.png,/assets/images/clubs/club-10.png'),
  (10, '阳光海上运动', '广州 · 南沙', '广州市南沙湾码头', '13312340010', '摩托艇,香蕉船,日落', '/assets/images/clubs/club-10.png', '', '/assets/images/clubs/club-10.png,/assets/images/clubs/club-5.png,/assets/images/clubs/club-1.png'),
  (11, '示例海上运动中心', '三亚后海', '三亚后海', '', '摩托艇,海上项目', '/uploads/4a546d26bf9d4810b47ed0bd6ce13f89.jpg', '', '')
on duplicate key update name = values(name), location = values(location), address = values(address),
  phone = values(phone), tags = values(tags), cover = values(cover), license_image = values(license_image), gallery = values(gallery);

insert into profile_contents (id, notice_title, notice_content, about_us_content, review_mode_enabled, site_activity_limit, tab_home_text, tab_category_text, tab_orders_text, tab_welfare_text, tab_profile_text)
values
  (1, '平台公告', '欢迎来到示例海上运动平台，最新活动与服务说明请以平台公告为准。', '示例平台专注海上运动体验服务，提供多城市俱乐部预约、活动下单与售后保障。', 0, 6, '首页', '分类', '订单', '悬赏', '我的')
on duplicate key update notice_title = values(notice_title), notice_content = values(notice_content), about_us_content = values(about_us_content),
  review_mode_enabled = values(review_mode_enabled),
  site_activity_limit = values(site_activity_limit),
  tab_home_text = values(tab_home_text), tab_category_text = values(tab_category_text), tab_orders_text = values(tab_orders_text),
  tab_welfare_text = values(tab_welfare_text), tab_profile_text = values(tab_profile_text);

insert into categories (id, `key`, name, sort, status)
values
  (1, 'jetski', '摩托艇', 1, 'active'),
  (2, 'surf', '冲浪', 2, 'active'),
  (3, 'banana', '香蕉船', 3, 'active'),
  (4, 'paddle', '桨板', 4, 'active'),
  (5, 'dive', '潜水', 5, 'active')
on duplicate key update name = values(name), sort = values(sort), status = values(status);

insert into activities (id, club_id, title, subtitle, category, base_price, original_price, audience, description, expire_date, status, cover, gallery)
values
  (198, 1, '佣金深度测试 · 20分钟', '未扫码/用户1/用户2/用户3 对照测试', 'jetski', 100.00, 100.00, '测试账号', '仅用于佣金深度测试。未扫码用户不显示佣金；用户1预计佣金 10 元；用户2预计自购佣金 5 元、邀请人佣金 5 元；用户3预计自购佣金 5 元。', '2027-12-31', 'active', '/assets/images/activities/activity-10.png', '/assets/images/activities/activity-10.png,/assets/images/activities/activity-1.png'),
  (199, 1, '支付联调测试 · 5分钟', '0.01元测试商品', 'jetski', 0.01, 0.01, '测试账号', '仅用于微信支付联调，请勿用于正式售卖。', '2027-12-31', 'active', '/assets/images/activities/activity-1.png', '/assets/images/activities/activity-1.png'),
  (200, 1, '三级返佣测试 · 20分钟', '用户1/用户2/用户3返佣验证', 'jetski', 100.00, 100.00, '测试账号', '仅用于当前返佣规则验证。用户1下单返 10 元；用户2下单返 5 元且用户1返 5 元；用户3及以后下单返 5 元，小程序运营返 5 元。', '2027-12-31', 'active', '/assets/images/activities/activity-3.png', '/assets/images/activities/activity-3.png,/assets/images/activities/activity-4.png'),
  (101, 1, '摩托艇体验 · 30分钟', '极速体验', 'jetski', 260.00, 320.00, '需穿救生衣', '极速冲浪体验，教练全程陪同并提供安全装备。', '2026-12-31', 'active', '/assets/images/activities/activity-1.png', '/assets/images/activities/activity-1.png,/assets/images/activities/activity-2.png,/assets/images/activities/activity-3.png'),
  (102, 1, '海湾巡航 · 60分钟', '海岸巡游', 'jetski', 360.00, 420.00, '适合新手', '沿海岸线巡航，风景优美，适合第一次体验。', '2026-12-31', 'active', '/assets/images/activities/activity-2.png', '/assets/images/activities/activity-2.png,/assets/images/activities/activity-3.png,/assets/images/activities/activity-4.png'),
  (103, 2, '冲浪课程 · 90分钟', '私教教学', 'surf', 420.00, 520.00, '建议有基础', '包含基础动作教学与板具使用指导，教练一对一带练。', '2026-12-31', 'active', '/assets/images/activities/activity-3.png', '/assets/images/activities/activity-3.png,/assets/images/activities/activity-4.png,/assets/images/activities/activity-5.png'),
  (104, 2, '冲浪进阶 · 120分钟', '进阶训练', 'surf', 480.00, 560.00, '需有基础', '进阶技巧训练，适合有冲浪经验的玩家。', '2026-12-31', 'active', '/assets/images/activities/activity-4.png', '/assets/images/activities/activity-4.png,/assets/images/activities/activity-5.png,/assets/images/activities/activity-6.png'),
  (105, 3, '摩托艇巡航 · 40分钟', '海湾巡游', 'jetski', 280.00, 350.00, '适合新手', '轻松巡航体验，感受海风与速度的结合。', '2026-12-31', 'active', '/assets/images/activities/activity-5.png', '/assets/images/activities/activity-5.png,/assets/images/activities/activity-6.png,/assets/images/activities/activity-7.png'),
  (106, 4, '香蕉船欢乐 · 20分钟', '团体畅玩', 'banana', 180.00, 220.00, '亲子推荐', '欢乐水上项目，适合家庭或朋友结伴体验。', '2026-12-31', 'active', '/assets/images/activities/activity-6.png', '/assets/images/activities/activity-6.png,/assets/images/activities/activity-7.png,/assets/images/activities/activity-8.png'),
  (107, 5, '滑水体验 · 30分钟', '速度挑战', 'jetski', 260.00, 320.00, '需穿救生衣', '滑水与速度挑战结合，刺激与乐趣并存。', '2026-12-31', 'active', '/assets/images/activities/activity-7.png', '/assets/images/activities/activity-7.png,/assets/images/activities/activity-8.png,/assets/images/activities/activity-9.png'),
  (108, 6, '潜水体验 · 45分钟', '深蓝探索', 'surf', 520.00, 620.00, '需会游泳', '基础潜水体验，配备专业教练与安全指导。', '2026-12-31', 'active', '/assets/images/activities/activity-8.png', '/assets/images/activities/activity-8.png,/assets/images/activities/activity-9.png,/assets/images/activities/activity-10.png'),
  (109, 7, '香蕉船团建 · 30分钟', '团队项目', 'banana', 220.00, 280.00, '团建推荐', '团队出游项目，含安全讲解与统一装备。', '2026-12-31', 'active', '/assets/images/activities/activity-9.png', '/assets/images/activities/activity-9.png,/assets/images/activities/activity-10.png,/assets/images/activities/activity-1.png'),
  (110, 8, '桨板体验 · 60分钟', '轻松慢享', 'surf', 240.00, 300.00, '亲子推荐', '适合亲子与轻度体验者的慢节奏水上项目。', '2026-12-31', 'active', '/assets/images/activities/activity-10.png', '/assets/images/activities/activity-10.png,/assets/images/activities/activity-1.png,/assets/images/activities/activity-2.png'),
  (111, 9, '潜水进阶 · 90分钟', '深海探索', 'surf', 680.00, 780.00, '需有基础', '进阶潜水训练，适合有潜水经验的玩家。', '2026-12-31', 'active', '/assets/images/activities/activity-1.png', '/assets/images/activities/activity-1.png,/assets/images/activities/activity-2.png,/assets/images/activities/activity-3.png'),
  (112, 10, '日落巡航 · 60分钟', '风景优选', 'jetski', 320.00, 380.00, '适合新手', '傍晚巡航体验，感受日落与海风。', '2026-12-31', 'active', '/assets/images/activities/activity-2.png', '/assets/images/activities/activity-2.png,/assets/images/activities/activity-3.png,/assets/images/activities/activity-4.png')
on duplicate key update title = values(title), subtitle = values(subtitle), category = values(category),
  base_price = values(base_price), original_price = values(original_price), audience = values(audience),
  description = values(description), expire_date = values(expire_date), status = values(status),
  cover = values(cover), gallery = values(gallery);

insert into activities (id, club_id, title, subtitle, category, base_price, original_price, merchant_settlement_price, platform_operation_fee, buyer_commission_amount, inviter_commission_amount, audience, description, bundle, expire_date, status, cover, gallery, detail_images)
values
  (198, 1, '佣金深度测试 · 20分钟', '未扫码/用户1/用户2/用户3 对照测试', 'jetski', 100.00, 100.00, 85.00, 5.00, 5.00, 5.00, '测试账号', '仅用于佣金深度测试。未扫码用户不显示佣金；用户1预计佣金 10 元；用户2预计自购佣金 5 元、邀请人佣金 5 元；用户3预计自购佣金 5 元。', '仅用于佣金测试，请勿正式售卖', '2027-12-31', 'active', '/assets/images/activities/activity-10.png', '/assets/images/activities/activity-10.png,/assets/images/activities/activity-1.png', '/assets/images/activities/activity-10.png,/assets/images/activities/activity-2.png'),
  (200, 1, '三级返佣测试 · 20分钟', '用户1/用户2/用户3返佣验证', 'jetski', 100.00, 100.00, 0.00, 90.00, 10.00, 5.00, '测试账号', '仅用于当前返佣规则验证。用户1下单返 10 元；用户2下单返 5 元且用户1返 5 元；用户3及以后下单返 5 元，小程序运营返 5 元。', '仅用于三级返佣测试，请勿正式售卖', '2027-12-31', 'active', '/assets/images/activities/activity-3.png', '/assets/images/activities/activity-3.png,/assets/images/activities/activity-4.png', '/assets/images/activities/activity-3.png,/assets/images/activities/activity-5.png')
on duplicate key update
  club_id = values(club_id),
  title = values(title),
  subtitle = values(subtitle),
  category = values(category),
  base_price = values(base_price),
  original_price = values(original_price),
  merchant_settlement_price = values(merchant_settlement_price),
  platform_operation_fee = values(platform_operation_fee),
  buyer_commission_amount = values(buyer_commission_amount),
  inviter_commission_amount = values(inviter_commission_amount),
  commission_amount = values(commission_amount),
  audience = values(audience),
  description = values(description),
  bundle = values(bundle),
  expire_date = values(expire_date),
  status = values(status),
  cover = values(cover),
  gallery = values(gallery),
  detail_images = values(detail_images);

insert into activity_slots (id, activity_id, slot_date, slot_time, capacity, booked, status)
values
  (1981, 198, '2027-12-31', '10:00 - 10:20', 999, 0, 'active'),
  (1991, 199, '2026-12-31', '09:00', 9999, 0, 'active'),
  (2001, 200, '2027-12-31', '10:30 - 10:50', 999, 0, 'active'),
  (1001, 101, '2026-06-08', '09:00 - 09:30', 10, 2, 'active'),
  (1002, 101, '2026-06-08', '10:30 - 11:00', 8, 5, 'active'),
  (1003, 101, '2026-06-09', '15:00 - 15:30', 12, 4, 'active'),
  (1004, 102, '2026-06-08', '09:30 - 10:30', 6, 1, 'active'),
  (1005, 102, '2026-06-09', '14:00 - 15:00', 6, 3, 'active'),
  (1006, 103, '2026-06-08', '10:00 - 11:30', 10, 6, 'active'),
  (1007, 103, '2026-06-09', '15:00 - 16:30', 10, 2, 'active'),
  (1008, 104, '2026-06-10', '09:00 - 11:00', 8, 2, 'active'),
  (1009, 105, '2026-06-08', '13:00 - 13:40', 12, 7, 'active'),
  (1010, 106, '2026-06-08', '16:30 - 17:00', 10, 5, 'active'),
  (1011, 107, '2026-06-09', '10:30 - 11:00', 8, 1, 'active'),
  (1012, 108, '2026-06-09', '13:00 - 13:45', 6, 2, 'active'),
  (1013, 109, '2026-06-10', '09:30 - 10:00', 12, 6, 'active'),
  (1014, 110, '2026-06-10', '14:00 - 15:00', 12, 4, 'active'),
  (1015, 111, '2026-06-10', '15:30 - 17:00', 6, 1, 'active'),
  (1016, 112, '2026-06-08', '17:00 - 18:00', 10, 3, 'active')
on duplicate key update slot_date = values(slot_date), slot_time = values(slot_time),
  capacity = values(capacity), booked = values(booked), status = values(status);

insert into banners (id, title, subtitle, image_url, status, sort)
values
  (1, '海上俱乐部 · 夏季畅玩', '摩托艇/冲浪/潜水一站式预订', '', 'active', 1),
  (2, '教练陪同 · 安全保障', '下单即享透明价格与安全保障', '', 'active', 2)
on duplicate key update title = values(title), subtitle = values(subtitle),
  image_url = values(image_url), status = values(status), sort = values(sort);

insert into bounty_tasks (
  id, location, commission_min, commission_max, cover_image_url, detail_image_url,
  step1_text, step1_image_url, step2_text, step2_image_url, step3_text, step3_image_url,
  status, sort
)
values
  (
    301, '三亚 · 后海', 18.00, 38.00,
    '/assets/images/activities/activity-4.png',
    '/assets/images/activities/activity-5.png',
    '到达后海指定点位后，先拍门店招牌和海边环境，确保地点清晰可辨认。',
    '/assets/images/clubs/club-1.png',
    '按要求完成内容发布，可选图文或短视频，内容真实自然，不要只传空白截图。',
    '/assets/images/activities/activity-6.png',
    '完成后回到悬赏页点击接取，再去悬赏任务上传截图和补充说明，等待后台审核发佣。',
    '/assets/images/activities/activity-7.png',
    'active', 1
  ),
  (
    302, '万宁 · 日月湾', 30.00, 88.00,
    '/assets/images/activities/activity-8.png',
    '/assets/images/activities/activity-9.png',
    '到达日月湾后，先拍摄现场位置、店招或海边标志物，证明任务地点真实到达。',
    '/assets/images/clubs/club-9.png',
    '根据任务要求发布一条带地点或体验内容的图文/视频，文字尽量完整，内容尽量清晰。',
    '/assets/images/activities/activity-10.png',
    '发布成功后截图保存，进入悬赏任务提交截图；如果是视频平台，可在补充说明里粘贴视频链接。',
    '/assets/images/activities/activity-3.png',
    'active', 2
  )
on duplicate key update
  location = values(location),
  commission_min = values(commission_min),
  commission_max = values(commission_max),
  cover_image_url = values(cover_image_url),
  detail_image_url = values(detail_image_url),
  step1_text = values(step1_text),
  step1_image_url = values(step1_image_url),
  step2_text = values(step2_text),
  step2_image_url = values(step2_image_url),
  step3_text = values(step3_text),
  step3_image_url = values(step3_image_url),
  status = values(status),
  sort = values(sort);
