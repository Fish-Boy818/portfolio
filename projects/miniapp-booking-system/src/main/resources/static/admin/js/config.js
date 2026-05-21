function adminTabs() {
  return [
    {
      key: "banners",
      label: "首页轮播",
      columns: [
        { key: "title", label: "标题" },
        { key: "subtitle", label: "副标题" },
        { key: "imageUrl", label: "轮播媒体" },
        { key: "status", label: "状态" },
        { key: "sort", label: "排序" }
      ],
      listUrl: "/api/admin/banners",
      createUrl: "/api/admin/banners",
      updateUrl: (id) => `/api/admin/banners/${id}`,
      deleteUrl: (id) => `/api/admin/banners/${id}`,
      fields: [
        { key: "title", label: "标题" },
        { key: "subtitle", label: "副标题" },
        {
          key: "imageUrl",
          label: "轮播媒体",
          type: "image",
          help: "支持图片或 MP4 视频；视频将按原始时长播放"
        },
        {
          key: "status",
          label: "状态",
          type: "select",
          options: [
            { label: "上架", value: "active" },
            { label: "下架", value: "inactive" }
          ]
        },
        { key: "sort", label: "排序", type: "number" }
      ]
    },
    {
      key: "users",
      label: "用户",
      allowCreate: false,
      columns: [
        { key: "nickname", label: "昵称" },
        { key: "phone", label: "手机号" },
        { key: "depth", label: "层级" },
        { key: "scanUser", label: "扫码用户" },
        { key: "createdAt", label: "注册时间" },
        { key: "openId", label: "OpenID" }
      ],
      listUrl: "/api/admin/users",
      createUrl: "/api/admin/users",
      updateUrl: (id) => `/api/admin/users/${id}`,
      deleteUrl: (id) => `/api/admin/users/${id}`,
      batchUrl: "/api/admin/batch/users",
      fields: [
        { key: "nickname", label: "昵称", required: true },
        { key: "phone", label: "手机号" },
        {
          key: "scanUser",
          label: "扫码资格",
          type: "select",
          valueType: "number",
          options: [
            { label: "否", value: "0" },
            { label: "是", value: "1" }
          ],
          help: "只有扫码链路用户才有 1/2/3 级和邀请二维码"
        },
        {
          key: "depth",
          label: "层级",
          type: "select",
          valueType: "number",
          options: [
            { label: "1", value: "1" },
            { label: "2", value: "2" },
            { label: "3", value: "3" }
          ],
          help: "仅对扫码用户生效；不设置时按扫码邀请关系自动计算"
        },
        { key: "avatarUrl", label: "头像URL" },
        { key: "openId", label: "OpenID", createOnly: true }
      ]
    },
    {
      key: "clubs",
      label: "俱乐部",
      columns: [
        { key: "name", label: "名称" },
        { key: "cover", label: "封面" },
        { key: "licenseImage", label: "营业执照" },
        { key: "location", label: "位置" },
        { key: "douyinUrl", label: "抖音主页" },
        { key: "openTime", label: "营业时间" },
        { key: "address", label: "地址" }
      ],
      listUrl: "/api/admin/clubs",
      createUrl: "/api/admin/clubs",
      updateUrl: (id) => `/api/admin/clubs/${id}`,
      deleteUrl: (id) => `/api/admin/clubs/${id}`,
      batchUrl: "/api/admin/batch/clubs",
      fields: [
        { key: "name", label: "名称", required: true },
        { key: "location", label: "位置" },
        { key: "address", label: "地址" },
        { key: "phone", label: "电话" },
        { key: "douyinUrl", label: "抖音主页链接", placeholder: "https://www.douyin.com/user/..." },
        { key: "openTime", label: "营业时间", placeholder: "如：08:00-19:30" },
        { key: "tags", label: "标签", type: "list", placeholder: "标签1,标签2" },
        {
          key: "cover",
          label: "封面图",
          type: "image",
          help: "建议图片不超过 10MB"
        },
        {
          key: "licenseImage",
          label: "营业执照",
          type: "image",
          help: "用于官网点击俱乐部后展示，可在此上传或替换"
        },
        {
          key: "gallery",
          label: "图集",
          type: "images",
          help: "可多选，单张不超过 10MB"
        }
      ]
    },
    {
      key: "categories",
      label: "分类管理",
      columns: [
        { key: "key", label: "识别码" },
        { key: "name", label: "分类名称" },
        { key: "status", label: "状态" },
        { key: "sort", label: "排序" }
      ],
      listUrl: "/api/admin/categories",
      createUrl: "/api/admin/categories",
      updateUrl: (id) => `/api/admin/categories/${id}`,
      deleteUrl: (id) => `/api/admin/categories/${id}`,
      fields: [
        {
          key: "key",
          label: "分类标识",
          placeholder: "建议填写英文/拼音，如 jetski、surf",
          help: "用于系统识别的短码，建议填写英文或拼音，避免直接使用中文"
        },
        { key: "name", label: "分类名称", required: true, placeholder: "如：摩托艇" },
        {
          key: "status",
          label: "状态",
          type: "select",
          options: [
            { label: "启用", value: "active" },
            { label: "停用", value: "inactive" }
          ],
          help: "停用后小程序不展示该分类"
        },
        {
          key: "sort",
          label: "排序",
          type: "number",
          placeholder: "数字越小越靠前",
          help: "建议 1,2,3…"
        }
      ]
    },
    {
      key: "profileContent",
      label: "我的页设置",
      allowCreate: false,
      allowDelete: false,
      columns: [
        { key: "noticeTitle", label: "公告标题" },
        { key: "noticeContent", label: "公告内容" },
        { key: "aboutUsContent", label: "关于我们" },
        { key: "platformServicePhone", label: "平台电话" },
        { key: "reviewModeEnabled", label: "审核模式" },
        { key: "siteActivityLimit", label: "官网活动数" },
        { key: "tabHomeText", label: "首页导航" },
        { key: "tabCategoryText", label: "分类导航" },
        { key: "tabOrdersText", label: "订单导航" },
        { key: "tabWelfareText", label: "悬赏导航" },
        { key: "tabProfileText", label: "我的导航" },
        { key: "updatedAt", label: "更新时间" }
      ],
      listUrl: "/api/admin/profile-content",
      updateUrl: (id) => `/api/admin/profile-content/${id}`,
      fields: [
        { key: "noticeTitle", label: "公告标题", required: true, placeholder: "例如：平台公告" },
        {
          key: "noticeContent",
          label: "公告内容",
          type: "textarea",
          placeholder: "请输入首页公告内容，可换行"
        },
        {
          key: "aboutUsContent",
          label: "关于我们",
          type: "textarea",
          placeholder: "请输入关于我们介绍，可换行"
        },
        { key: "platformServicePhone", label: "平台接入电话", placeholder: "例如：400-123-4567 或 13800138000" },
        {
          key: "reviewModeEnabled",
          label: "审核模式开关",
          type: "select",
          valueType: "number",
          options: [
            { label: "关闭（正常功能）", value: "0" },
            { label: "开启（审核降级）", value: "1" }
          ],
          help: "开启后，小程序任务/悬赏入口会进入升级占位页"
        },
        {
          key: "siteActivityLimit",
          label: "官网活动展示数量",
          type: "number",
          placeholder: "例如：6",
          help: "控制官网“入驻商家活动推荐”最多展示多少个活动，建议 4-8 个"
        },
        { key: "tabHomeText", label: "首页导航名称", placeholder: "例如：首页" },
        { key: "tabCategoryText", label: "分类导航名称", placeholder: "例如：分类" },
        { key: "tabOrdersText", label: "订单导航名称", placeholder: "例如：订单" },
        { key: "tabWelfareText", label: "悬赏导航名称", placeholder: "例如：悬赏" },
        { key: "tabProfileText", label: "我的导航名称", placeholder: "例如：我的" }
      ]
    },
    {
      key: "adminAccount",
      label: "管理账号",
      allowCreate: false,
      allowDelete: false,
      columns: [
        { key: "username", label: "当前账号" },
        { key: "updatedAt", label: "更新时间" }
      ],
      listUrl: "/api/admin/admin-account",
      updateUrl: (id) => `/api/admin/admin-account/${id}`,
      fields: [
        { key: "username", label: "新账号", required: true, placeholder: "请输入新的管理账号" },
        {
          key: "currentPassword",
          label: "当前密码",
          type: "password",
          required: true,
          placeholder: "请输入当前密码"
        },
        {
          key: "password",
          label: "新密码",
          type: "password",
          required: true,
          placeholder: "请输入新的管理密码"
        },
        {
          key: "confirmPassword",
          label: "确认新密码",
          type: "password",
          required: true,
          placeholder: "请再次输入新的管理密码"
        }
      ]
    },
    {
      key: "activities",
      label: "活动",
      columns: [
        { key: "title", label: "标题" },
        { key: "cover", label: "封面" },
        { key: "clubId", label: "俱乐部" },
        { key: "originalPrice", label: "用户支付价(元)" },
        { key: "platformOperationFee", label: "平台留存(元)" },
        { key: "buyerCommissionAmount", label: "用户1佣金(下单人)" },
        { key: "inviterCommissionAmount", label: "用户2佣金(邀请人)" },
        { key: "commissionAmount", label: "用户3佣金(上上级)" },
        { key: "status", label: "状态" }
      ],
      listUrl: "/api/admin/activities",
      createUrl: "/api/admin/activities",
      updateUrl: (id) => `/api/admin/activities/${id}`,
      deleteUrl: (id) => `/api/admin/activities/${id}`,
      batchUrl: "/api/admin/batch/activities",
      fields: [
        {
          key: "clubId",
          label: "俱乐部",
          type: "select",
          optionsKey: "clubs",
          optionTemplate: "club",
          valueType: "number",
          required: true
        },
        { key: "title", label: "标题", required: true },
        { key: "subtitle", label: "副标题" },
        {
          key: "category",
          label: "分类",
          type: "select",
          optionsKey: "categories",
          optionTemplate: "category"
        },
        {
          key: "originalPrice",
          label: "用户支付价(元)",
          type: "number",
          required: true,
          help: "用户下单时看到并支付的价格"
        },
        {
          key: "platformOperationFee",
          label: "平台留存(元)",
          type: "number",
          required: true,
          placeholder: "可填 0，系统会按实际拆分校验"
        },
        {
          key: "buyerCommissionAmount",
          label: "用户1佣金金额(元)",
          type: "number",
          placeholder: "例如：5.00",
          help: "用户1=一级扫码用户自购时整单返佣；用户2/3场景会从这里拆分给上级或运营"
        },
        {
          key: "inviterCommissionAmount",
          label: "用户2佣金金额(元)",
          type: "number",
          placeholder: "例如：5.00",
          help: "用户2=二级扫码用户自购返佣；剩余佣金返给用户1"
        },
        {
          key: "commissionAmount",
          label: "用户3佣金金额(元)",
          type: "number",
          placeholder: "例如：3.00",
          help: "用户3=三级及以下扫码用户自购返佣；剩余佣金归小程序运营"
        },
        { key: "audience", label: "适用人群" },
        { key: "description", label: "描述", type: "textarea" },
        { key: "bundle", label: "商品搭配", type: "textarea", placeholder: "如：拍照+跟拍 | 救生衣+教练" },
        { key: "expireDate", label: "有效期", type: "date" },
        {
          key: "status",
          label: "状态",
          type: "select",
          options: [
            { label: "上架", value: "active" },
            { label: "下架", value: "inactive" }
          ]
        },
        {
          key: "cover",
          label: "封面图",
          type: "image",
          help: "建议图片不超过 10MB"
        },
        {
          key: "gallery",
          label: "图集",
          type: "images",
          help: "可多选，单张不超过 10MB"
        },
        {
          key: "detailImages",
          label: "详情图",
          type: "images",
          help: "用于详情页竖排展示，建议上传 2-6 张"
        }
      ]
    },
    {
      key: "bounties",
      label: "悬赏",
      columns: [
        { key: "location", label: "地点" },
        { key: "coverImageUrl", label: "封面" },
        { key: "commissionRangeText", label: "佣金区间" },
        { key: "totalClaims", label: "已接取" },
        { key: "pendingReviewClaims", label: "审核中" },
        { key: "completedClaims", label: "已完成" },
        { key: "statusText", label: "状态" },
        { key: "sort", label: "排序" }
      ],
      listUrl: "/api/admin/bounties",
      createUrl: "/api/admin/bounties",
      updateUrl: (id) => `/api/admin/bounties/${id}`,
      deleteUrl: (id) => `/api/admin/bounties/${id}`,
      fields: [
        { key: "location", label: "地点", required: true, placeholder: "例如：三亚 · 亚龙湾" },
        { key: "commissionMin", label: "最低佣金(元)", type: "number", required: true, placeholder: "例如：8" },
        { key: "commissionMax", label: "最高佣金(元)", type: "number", required: true, placeholder: "例如：20" },
        {
          key: "coverImageUrl",
          label: "列表封面",
          type: "image",
          required: true,
          help: "用于悬赏列表卡片展示"
        },
        {
          key: "detailImageUrl",
          label: "详情头图",
          type: "image",
          help: "用于悬赏详情页顶部展示；不填则沿用列表封面"
        },
        {
          key: "steps",
          label: "任务说明",
          type: "bountySteps",
          required: true,
          help: "不限制 1、2、3 条，可继续新增；每条都可填写文字，并可选上传配图"
        },
        {
          key: "status",
          label: "状态",
          type: "select",
          options: [
            { label: "启用", value: "active" },
            { label: "停用", value: "inactive" }
          ]
        },
        { key: "sort", label: "排序", type: "number", placeholder: "数字越小越靠前" }
      ]
    },
    {
      key: "commissions",
      label: "佣金明细",
      allowCreate: false,
      allowEdit: false,
      allowDelete: false,
      columns: [
        { key: "userPhone", label: "手机号" },
        { key: "orderNo", label: "订单号" },
        { key: "activityTitle", label: "活动" },
        { key: "statusText", label: "状态" },
        { key: "commissionAmount", label: "佣金" },
        { key: "availableAmount", label: "可提现" },
        { key: "frozenAmount", label: "提现中" }
      ],
      listUrl: "/api/admin/commissions",
      fields: []
    },
    {
      key: "welfareSubmissions",
      label: "悬赏审核",
      allowCreate: false,
      allowDelete: false,
      columns: [
        { key: "submissionNo", label: "提交单号" },
        { key: "userPhone", label: "手机号" },
        { key: "bountyLocationAttemptText", label: "悬赏地点" },
        { key: "reviewText", label: "补充说明" },
        { key: "screenshotUrl", label: "好评截图" },
        { key: "statusText", label: "审核状态" },
        { key: "reviewNote", label: "审核备注" },
        { key: "rewardAmount", label: "奖励佣金" },
        { key: "createdAt", label: "提交时间" }
      ],
      listUrl: "/api/admin/welfare-submissions",
      updateUrl: (id) => `/api/admin/welfare-submissions/${id}`,
      fields: [
        { key: "submissionNo", label: "提交单号", editOnly: true },
        { key: "userPhone", label: "手机号", editOnly: true },
        {
          key: "status",
          label: "审核状态",
          type: "select",
          options: [
            { label: "审核通过", value: "approved" },
            { label: "未通过", value: "rejected" }
          ],
          editOnly: true
        },
        {
          key: "rewardAmount",
          label: "奖励佣金(元)",
          type: "number",
          placeholder: "审核通过时填写，如 8.80",
          editOnly: true
        },
        {
          key: "reviewNote",
          label: "审核备注/驳回原因",
          type: "textarea",
          placeholder: "通过可写审核备注；驳回时必须填写原因",
          editOnly: true
        }
      ]
    },
    {
      key: "withdrawals",
      label: "提现单",
      allowCreate: false,
      allowDelete: false,
      columns: [
        { key: "withdrawNo", label: "提现单号" },
        { key: "userPhone", label: "手机号" },
        { key: "amount", label: "提现金额" },
        { key: "statusText", label: "状态" },
        { key: "failReason", label: "失败原因" },
        { key: "requestedAt", label: "申请时间" }
      ],
      listUrl: "/api/admin/withdrawals",
      updateUrl: (id) => `/api/admin/withdrawals/${id}`,
      fields: [
        { key: "withdrawNo", label: "提现单号", editOnly: true },
        { key: "userPhone", label: "手机号", editOnly: true },
        { key: "amount", label: "提现金额", editOnly: true },
        {
          key: "status",
          label: "处理状态",
          type: "select",
          options: [
            { label: "待处理", value: "pending" },
            { label: "处理中", value: "processing" },
            { label: "成功", value: "success" },
            { label: "失败", value: "failed" }
          ],
          editOnly: true
        },
        { key: "failReason", label: "失败原因", editOnly: true, placeholder: "失败时填写原因" },
        { key: "operatorName", label: "处理人", editOnly: true, placeholder: "可选" },
        { key: "operatorNote", label: "处理备注", type: "textarea", editOnly: true }
      ]
    },
    {
      key: "orders",
      label: "订单",
      allowEdit: false,
      columns: [
        { key: "orderNo", label: "订单号" },
        { key: "title", label: "活动" },
        { key: "clubName", label: "俱乐部" },
        { key: "userPhone", label: "用户手机号" },
        { key: "statusText", label: "状态" },
        { key: "createdAt", label: "下单时间" },
        { key: "verifiedAt", label: "核销时间" },
        { key: "payAmount", label: "金额" },
        { key: "user1CommissionAmount", label: "用户1" },
        { key: "user2CommissionAmount", label: "用户2" },
        { key: "user3CommissionAmount", label: "用户3" },
        { key: "operatorCommissionAmount", label: "小程序运营" },
        { key: "platformOperationFee", label: "平台留存" }
      ],
      listUrl: "/api/admin/orders",
      createUrl: "/api/admin/orders",
      updateUrl: (id) => `/api/admin/orders/${id}`,
      deleteUrl: (id) => `/api/admin/orders/${id}`,
      fields: [
        {
          key: "userId",
          label: "用户",
          type: "select",
          optionsKey: "users",
          optionTemplate: "user",
          valueType: "number",
          required: true,
          createOnly: true
        },
        {
          key: "activityId",
          label: "活动",
          type: "select",
          optionsKey: "activities",
          optionTemplate: "activity",
          valueType: "number",
          required: true,
          createOnly: true
        },
        {
          key: "phone",
          label: "手机号",
          required: true,
          createOnly: true
        },
        { key: "quantity", label: "数量", type: "number", required: true, createOnly: true },
        {
          key: "status",
          label: "状态",
          type: "select",
          options: [
            { label: "待支付", value: "unpaid" },
            { label: "已支付", value: "paid" },
            { label: "已核销", value: "used" },
            { label: "已退款", value: "refund" },
            { label: "已取消", value: "cancelled" }
          ],
          editOnly: true
        }
      ]
    }
  ];
}

window.AdminTabs = adminTabs;
