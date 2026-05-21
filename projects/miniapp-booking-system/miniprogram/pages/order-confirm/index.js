const { request } = require("../../utils/api");
const { ensureAuth, loginWithPhoneCode, loginForDevtools } = require("../../utils/auth");

const USER_AGREEMENT_TEXT = "欢迎使用御乾小程序。你在使用本服务时，应遵守法律法规及平台规则，不得进行违规或侵权行为。你需对账号下的操作与订单行为负责。";
const PRIVACY_POLICY_TEXT = "为完成登录、下单、支付与售后服务，我们会处理必要信息（微信标识、手机号、订单与支付结果）。你可在“我的”页面申请查询、更正或删除相关信息。";
const DEFAULT_ACTIVITY_COVER = "/assets/images/activities/activity-1.png";

const SAFETY_TEXT = `〇、前言
我们重视您的健康和安全，因此请您在决定是否出行之前仔细阅读安全须知。为了确保您和家人、朋友的安全，请在旅游过程中严格遵守各项安全注意事项。同时特别提醒，除了安全须知外，针对具体产品，您还应仔细阅读产品展示页面的预订须知、安全提示以及预订说明中的其他安全提示内容，以及商家在线上/线下可能告知您需要注意的其他事项。请注意，以下内容涉及的疾病仅为简要示例，如果您或其他计划出行的人员有其他疾病可能不适合参加旅游活动，请主动告知或者咨询平台与商家。
一、特殊人群须知
（一）老年人
为了确保您的安全和家人的安全，考虑到老年人在体力和身体状况等方面与年轻人不同，请在预订旅行之前谨慎选择目的地和游玩项目，并携带必要物品。除了遵守其他安全须知外，还应遵循以下具体内容：
出行选择适宜的旅游目的地。建议选择短途的名胜古迹旅游或休闲度假地旅游，避免远途旅行。
游玩选择适宜的项目。尽量选择轻松的观光类旅游产品，避免过度消耗体力或存在一定危险性的活动，例如爬山、骑行、漂流、滑雪等，请谨慎选择或避免参与。
携带必要的药品和急救医疗器械：
患有慢性病如高血压、糖尿病、冠心病等的人，请携带必要的药品。
出行前准备常见疾病的治疗药品，例如防晕车、晕船药品，止泻、消炎、通便药品，防过敏药品以及伤湿止痛膏、酒精、药棉、红药水等，以防水土不服或发生其他意外情况。
携带适当的衣物和其他用品。在出发前了解旅游目的地的天气和气候特点，准备足够的衣物和防雨用具。选择舒适、透气的鞋子。
请确保有家人陪同，并避免单独外出。在游览过程中，建议结伴而行，相互照料。
避免爬高、弹跳或进行其他高速、剧烈的活动，建议携带手杖或其他辅助出行用品。如果发生跌倒、发病或其他意外情况，请进行简单处理后尽快前往附近医院就诊。
（二）儿童
考虑到儿童的身体抵抗力、适应新环境能力较弱，缺乏自我控制和自我照顾能力，以及好奇心强，容易发生意外事故，未满18周岁的旅游者应由家属陪同。在遵守其他安全须知的基础上，请遵守以下内容：
选择公共卫生条件良好的地区作为旅游目的地，以预防呼吸道感染、腹泻或其他疾病。如果发生相关情况，应及时就医。
出行前咨询医师，并携带必要的药品，如退烧药、止泻药、防过敏药、感冒药等，同时了解紧急救助措施。还应携带防蚊虫叮咬或其他治疗过敏的药品或外用药膏。
家长应紧跟儿童，并确保儿童始终在视野和可触碰的范围内，不得让儿童离开。
如果参加团队旅行，不得随意离开团队，并严格遵守导游、向导、领队、园区员工等工作人员的指示。
在活动过程中，不得私自进行爬高、水上、高速或剧烈的活动，以免发生跌倒、溺水、骨折或扭伤等情况。
如果发生跌倒、骨折、扭伤等运动伤害，应给予适当的休息、冰敷、抬高受伤部位，并避免处理不当，以防延长康复时间或加重症状。
（三）孕妇、病患及行动不便者
为了确保您和其他计划出行的人员的人身安全，我们建议您在出行前进行身体检查。请注意，如果您符合以下情况中的任何一种，由于我们的服务能力有限，我们原则上无法提供接待服务。如果您需要出行，请遵循医生的建议，并提前咨询相关平台和商家，谨慎决定是否出行。
精神病患者，例如癫痫及各种精神疾病。
呼吸系统疾病患者，例如肺气肿、肺心病等疾病。
脑血管疾病患者，例如脑栓塞、脑出血、脑肿瘤等疾病。
传染性疾病患者，例如传染性肝炎、活动期肺结核、伤寒等传染病。
心血管疾病患者，例如严重高血压、心功能不全、心肌缺氧、心肌梗塞等疾病。
严重贫血病患者。
大中型手术的恢复期病患者。
孕妇及行动不便者。
二、高危险项目安全须知
（一）爬山
患有精神病、颈椎病、高血压、心脏病、癫痫病、腰椎病、骨折及运动障碍的患者、饮酒、孕妇，以及其他商家建议不宜参加的游客，严禁参与登山活动。为确保安全，请遵守以下具体内容：
提前制定行程计划，并告知家人，并结伴出行。
服从领队或向导人员的指导，并遵守团队规定。
在活动前要密切关注天气状况，避免在恶劣天气下进行登山活动。同时，带好衣物以备早晚温差，防止感冒。
登山时不要过于追求速度，应量力而行，切忌与他人比拼速度或进行危险动作如跳跃等。
在登山过程中，如果遇到恶劣天气、缺少食物或发生疾病等山难事件，请立即与当地警方或景区管理人员联系寻求援助。
（二）漂流
请注意，以下人群严禁参与漂流活动：患有精神病、颈椎病、高血压、心脏病、癫痫病、腰椎病、骨折及运动障碍的患者、饮酒的游客、孕妇、身高不足1.2米的儿童，以及其他商家建议不适合参加的游客。
为确保安全，请遵守以下具体内容：
严禁携带易破碎、易燃、易爆等危险物品进入河道。
在漂流前，请穿上漂流服或泳装，戴上软底鞋，以防滑倒和擦伤，因为岸边可能会湿滑。
在漂流过程中，请正确穿戴安全帽、救生衣等，并注意航道引导标志。
在漂流过程中，请注意以下事项：
(1) 不要过度打水仗或嬉戏。
(2) 不要进入河道游泳或玩水，以免造成划伤。
(3) 不得脱下安全帽和救生衣。
(4) 不要随意将橡皮艇、安全帽交给他人或随意丢弃。
(5) 其他需要注意的事项。
在整个漂流过程中，严禁离开艇体玩耍或进入危险地带，以免发生意外事故。
（三）温泉
饮酒、过度疲劳、孕妇、生理期女性、术后未痊愈、患有精神病、中重度高血压、严重心脏病、恶性肿瘤、癫痫病、呼吸功能障碍、传染病（如急性肝炎）、急性病症（如急性胃肠炎等）、出血症等不适宜参加温泉活动。为确保安全，请遵守以下具体内容：
浸泡前后均需饮用适量的温开水以补充水分。
不宜在饥饿或饭后立即进行温泉浸泡，建议饭后间隔40分钟以上再入池。
入池前需摘下贵重金属饰品，以免受硫化影响而变色。
浸泡时间不宜过长，每个循环不宜超过15分钟，如果出现心跳加速、呼吸急促等现象，应立即停止浸泡。
如有需要，请及时联系现场管理人员。
（四）滑雪
请注意，以下人群严禁参与滑雪活动：患有精神病、颈椎病、高血压、心脏病、癫痫病、腰椎病、骨折及运动障碍的患者、饮酒者、孕妇、身高低于1.2米的儿童以及商家建议不适宜参加的游客。为确保安全，请遵守以下具体内容：
滑雪时要注意保暖，易冻伤的部位包括手指、脚、耳朵、鼻尖、生殖器等。
在滑雪前进行准备活动和热身运动，以避免受伤。
了解滑雪场的情况，包括高度、宽度、长度、坡度和走向等，记住雪场设施的分布位置，认清警示标志，并严格遵守滑雪场的安全管理规定。
选择质量合格的装备，并在滑雪前仔细检查滑雪板、滑雪杖，包括是否有折裂、固定器联接是否牢固、附件是否齐备等。如果在滑行中发现装备异常，请立即停止使用。
选择安全的滑雪路线，切记不要越过雪场界限或离开营地，对于陌生的雪区，应当寻求向导的帮助。
事先了解滑雪的相关规则，在滑雪时与他人保持一定的间距，避免碰撞。如果需要停下休息，请离开雪道，以免影响他人。
在整个活动过程中，请严格听从导游或工作人员的指示，尽量避免单独出行。如果必须单独出发，请告知同伴或雪场管理人员。
（五）高空
恐高症、心脏病、高血压、贫血、颈椎疾病患者、精神病患者、孕妇、高龄老人、儿童和饮酒游客不适宜参加，此外高空游玩项目具有惊险性和刺激性，游客必须根据自身身体状况选择是否参加刺激性项目。如果身体状况或精神状况无法适应刺激性项目，请避免参加。为确保安全，请遵守以下具体内容：
滑翔：
(1) 初学者应避免在山坡进行滑翔，尽量选择没有障碍物、宽广的平地作为安全区域进行练习。
(2) 注意关注与气象有关的资料，避免在大风、雨雪或能见度低的恶劣天气下进行活动。
(3) 选择合格的滑翔设备，并避免任意改动。如果进行改动，必须经过试飞员试验后才能使用。
(4) 在进行滑翔之前，向当地的飞行员请教并听取他们的意见。
(5) 遵守商家的相关安全管理规定，听取安全建议，以避免发生危险情况。
过山车：
(1) 在乘坐过程中，禁止携带可能掉落或易折断的物品，包括但不限于拖鞋、戒指、手镯、眼镜、项链、耳环或其他佩饰品。
(2) 建议佩戴保护装备以保护头部、神经、颈椎和脊柱等，避免受伤。
热气球：
(1) 请根据商家的建议选择合适的体验时间，避免在大风天气下进行热气球飞行。
(2) 请穿着合适的服装、帽子和运动鞋等装备，避免穿裙装、高跟鞋或凉鞋等。
(3) 热气球的结构特殊，即使发生突然熄火，也不会急速下降。如果遇到突发情况，请保持冷静，并听从飞行员的指挥。
（六）快艇
请注意，以下人群禁止参与快艇项目：饮酒者、严重心脏病患者、精神病患者、高血压患者、高度近视者、颈椎病患者、腰椎病患者、骨折患者等。为确保安全，请遵守以下具体内容：
不要携带易燃、易爆、腐蚀性等危险物品。
必须穿戴救生衣，并找到安全绳。如果发生翻艇落水，请保持冷静并积极配合驾驶员的救护措施。
上艇时不要站在缆绳附近，头部和手部不要伸出边缘外，以避免发生危险。
对于老人和儿童，应根据商家的引导和身体状况评估决定是否参与项目。请不要坐在船头，因为船头颠簸剧烈，以免发生意外伤害。
参与快艇活动的人员未经许可不得离开艇体下水。
（七）浮潜
请注意，以下人群禁止从事潜水项目：饮酒的游客、患有耳、鼻疾病、癫痫症、精神病、结核病、糖尿病、肾脏病、心脏病、气喘、高（低）血压等疾病的游客。为确保安全，请遵守以下具体内容：
准备合格的潜水用具，包括面镜、呼吸管、蛙鞋和防水手表等。
在教练员或工作人员的陪同下，听从导游或工作人员的指示，在商家指定区域活动。
如发生紧急情况，请立即就近求援。
如果同伴或其他人发生危险，请谨慎评估自身的救援能力，首先向他人求助，并将可提供浮力的器具传递给溺水者。如果前往救援，请尽量携带浮具，切勿贸然进行救援。
（八）滑沙、滑草
请注意，患有精神病、颈椎病、高血压、心脏病、癫痫病、腰椎病、骨折及运动障碍的患者、饮酒者、孕妇、身高低于1.2米的儿童不适宜参加此活动。为确保安全，请遵守以下具体内容：
在乘坐前建议穿戴护具或专门用具，以防受伤。
乘坐时，双脚应踩住滑板下沿两侧，双手扶住把手，身体微微向前倾斜。
在下滑过程中，严禁乱动或做出其他危险动作，以防发生意外。
在整个活动过程中，请严格听从工作人员的指示。
（九）攀岩
患有精神病、颈椎病、高血压、心脏病、癫痫病、腰椎病、骨折及运动障碍的患者、饮酒、孕妇，以及其他商家建议不宜参加的游客，严禁参与攀岩活动。为确保安全，请遵守以下具体内容：
选择舒适、有弹性的衣物，并穿着防滑鞋，同时佩戴防护用具如帽子、手套、护膝、护腕等。
在活动前进行适当的热身运动，攀岩过程中要量力而行，避免发生损伤。
选择质量合格的攀岩装备，并在攀岩之前仔细检查攀岩用品的完好性和可用性。
（十）骑马
患有精神病、颈椎病、高血压、心脏病、癫痫病、腰椎病、骨折及运动障碍的患者、饮酒、孕妇，身体状况不佳的老人以及其他被管理人员建议不宜参加的游客，严禁参与骑马活动。为确保安全，请遵守以下具体内容：
骑马前要仔细听从马信的讲解和安排。
为避免马匹受惊，请不要打开太阳伞进入马场，同时在接近马匹时避免做剧烈或夸张的动作。
骑马时必须佩戴头盔，并穿着贴身的衣物和合适的鞋子。
骑马时，前脚掌要踩好马镫，并牢牢抓住绳子，切勿将整只脚都伸入马镫中，否则可能被马镫卡住，导致受伤。
三、特殊目的地风险提示
在高原地区，海拔高，气压低，含氧量低，易导致人体缺氧，对人的体质要求较高。因此对于糖尿病患者、各种血液病患者、睡眠中容易出现呼吸暂停的患者，高血压、心脏病、糖尿病、癫痫、精神分裂症等其他精神性疾病患者，重症感冒、呼吸道感染的患者，以往患过高原病及其他严重慢性疾病等的患者，儿童、老年人，肺、脑、肝、肾有明显病变以及严重贫血的患者，严禁进入高原。此外，建议在进入高原之前，请您务必进行严格的体格检查，并遵守以下内容：
在前往高原地区之前，尽量提前适应海拔环境。如果可能的话，您可以选择逐渐增加海拔高度的行程，给身体更多的时间来适应高原环境。如果乘坐汽车或火车进入高原地区，每天上升高度控制在400-600米以内，初进高原时不宜过快，采取逐步升高的方式，使身体逐渐适应高原环境。
尽可能准备氧气和防治急性高原病的药物，如硝苯吡啶（又称心痛定）、氨茶碱等。具体用药以医嘱为准。同时，备有防治感冒的药物、抗菌素和维生素类药物等，以备不时之需。
高原地区的早晚温差可能超过15℃，需携带足够的防寒衣物和抗紫外线的防护用品。
保证充足的睡眠，避免摄入油腻食物和酒精，宜选择清淡、富含维生素、易消化的饮食，多喝水、多吃水果。
初到高原地区或进入高原后，避免过度体力活动和剧烈活动，避免情绪激动。
初到高原的几天内，避免频繁洗浴，以免受凉引发感冒。感冒常常是急性高原肺水肿的主要诱因（在缺氧状态下不易痊愈）。
如果出现严重的高原反应症状，应立即处理，并按医嘱及时服用药物。在严重情况下，应使用氧气。如果出现严重的胸闷、剧烈咳嗽、呼吸困难、咳粉红色泡沫痰，或出现反应迟钝、意识淡漠、甚至昏迷等症状，除了采取上述处理措施外，应立即前往附近医院抢救，或尽快转移到海拔较低的地区进行治疗。
在整个活动过程中，务必听从导游或工作人员的指示，注意安全。
在高原地区旅行时，存在高原反应的风险，建议购买高原保险险种。
请注意，这些建议只是一般性的指导，具体的预防措施和注意事项应根据个人的健康状况和医生的建议来确定。在计划前往高原地区之前，请与专业医生进行详细咨询和评估。`;

Page({
  data: {
    productId: 1,
    payAmount: 0,
    unitPrice: 0,
    quantity: 1,
    status: "loading",
    errorMessage: "订单信息加载失败",
    emptyMessage: "",
    product: null,
    userId: null,
    phone: "",
    requiresPhoneLogin: false,
    privacyAgreed: false,
    showSafety: false,
    paying: false,
    safetyText: SAFETY_TEXT,
    safetyLines: []
  },
  onLoad(options) {
    const id = Number(options.id) || 1;
    this.setData({
      productId: id,
      safetyLines: this.buildSafetyLines(SAFETY_TEXT)
    }, () => {
      this.loadData();
    });
  },
  buildSafetyLines(text) {
    if (!text) {
      return [];
    }
    return String(text)
      .split("\n")
      .map((line) => String(line || ""));
  },
  loadData() {
    this.setData({ status: "loading" });
    ensureAuth()
      .catch(() => null)
      .then((user) => {
        const userId = user ? user.id : null;
        const phone = user && user.phone ? user.phone : "";
        const requiresPhoneLogin = !this.isRealWechatUser(user);
        return request({ url: `/api/activities/${this.data.productId}`, data: { userId } })
          .then((product) => {
            if (!product) {
              this.setData({ status: "empty", emptyMessage: "商品已下架或不存在" });
              return;
            }
            const mapped = {
              ...product,
              coverImage: this.resolveProductCover(product),
              store: product.clubLocation || product.clubName || ""
            };
            const unitPrice = Number(mapped.price || 0);
            const quantity = this.data.quantity || 1;
            this.setData({
              product: mapped,
              unitPrice,
              quantity,
              payAmount: Number((unitPrice * quantity).toFixed(2)),
              status: "ready",
              userId,
              phone,
              requiresPhoneLogin
            });
          });
      })
      .catch((err) => {
        this.setData({ status: "error", errorMessage: String(err || "加载失败") });
      });
  },
  isRealWechatUser(user) {
    const openId = user && user.openId ? String(user.openId) : "";
    return (
      !!(user && user.id) &&
      !!openId &&
      !openId.startsWith("manual-") &&
      !openId.startsWith("dev-")
    );
  },
  updatePayAmount(nextQuantity) {
    const quantity = Math.max(1, Number(nextQuantity) || 1);
    const unitPrice = Number(this.data.unitPrice || 0);
    this.setData({
      quantity,
      payAmount: Number((unitPrice * quantity).toFixed(2))
    });
  },
  onQtyMinus() {
    this.updatePayAmount(this.data.quantity - 1);
  },
  onQtyPlus() {
    this.updatePayAmount(this.data.quantity + 1);
  },
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value });
  },
  onTogglePrivacyAgree() {
    this.setData({ privacyAgreed: !this.data.privacyAgreed });
  },
  onOpenUserAgreement() {
    wx.showModal({
      title: "用户协议",
      content: USER_AGREEMENT_TEXT,
      showCancel: false,
      confirmText: "我已知晓"
    });
  },
  onOpenPrivacyPolicy() {
    wx.showModal({
      title: "隐私政策",
      content: PRIVACY_POLICY_TEXT,
      showCancel: false,
      confirmText: "我已知晓"
    });
  },
  ensurePrivacyAgreed() {
    if (this.data.privacyAgreed) {
      return true;
    }
    wx.showToast({ title: "请先勾选并同意协议", icon: "none" });
    return false;
  },
  ensureRealWechatLogin(phoneCode) {
    return ensureAuth()
      .catch(() => null)
      .then((user) => {
        if (this.isRealWechatUser(user)) {
          this.setData({
            userId: user.id,
            phone: user.phone || this.data.phone || "",
            requiresPhoneLogin: false
          });
          return user;
        }
        if (!phoneCode) {
          return loginForDevtools().then((profile) => {
            if (!profile || !profile.id) {
              throw new Error("__NEED_PHONE_AUTH__");
            }
            this.setData({
              userId: profile.id,
              phone: profile.phone || this.data.phone || "",
              requiresPhoneLogin: false
            });
            return profile;
          }).catch(() => {
            throw new Error("__NEED_PHONE_AUTH__");
          });
        }
        return loginWithPhoneCode(phoneCode).then((profile) => {
          if (!profile || !profile.id) {
            throw new Error("登录失败，请重试");
          }
          this.setData({
            userId: profile.id,
            phone: profile.phone || this.data.phone || "",
            requiresPhoneLogin: false
          });
          return profile;
        });
      });
  },
  submitPay(user) {
    const phone = String(this.data.phone || "").trim();
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: "请输入正确手机号", icon: "none" });
      return Promise.reject(new Error("__PHONE_INVALID__"));
    }
    if (this.data.paying) {
      return Promise.reject(new Error("__PAYING__"));
    }
    this.setData({ paying: true });
    const payload = {
      userId: user.id,
      activityId: this.data.productId,
      quantity: this.data.quantity,
      phone
    };
    return this.createOrder(payload)
      .then((order) => {
        return request({ url: `/api/pay/orders/${order.id}/jsapi`, method: "POST" })
          .then((payParams) => ({ order, payParams }));
      })
      .then(({ order, payParams }) => {
        return new Promise((resolve, reject) => {
          wx.requestPayment({
            timeStamp: String(payParams.timeStamp || ""),
            nonceStr: payParams.nonceStr || "",
            package: payParams.packageValue || payParams.package || "",
            signType: payParams.signType || "RSA",
            paySign: payParams.paySign || "",
            success: () => {
              wx.showToast({ title: "支付成功", icon: "success" });
              this.confirmPaidAndRedirect(order.id, 10);
              resolve(order);
            },
            fail: (err) => {
              const message = err && err.errMsg ? String(err.errMsg) : "";
              this.setData({ paying: false });
              if (message.indexOf("cancel") > -1) {
                wx.showToast({ title: "已取消支付", icon: "none" });
                reject(new Error("__PAY_CANCELLED__"));
                return;
              }
              wx.showToast({ title: "支付失败，请重试", icon: "none" });
              reject(new Error("__PAY_FAILED__"));
            }
          });
        });
      })
      .catch((err) => {
        this.setData({ paying: false });
        throw err;
      });
  },
  onPay() {
    if (this.data.status !== "ready" || this.data.paying) {
      return;
    }
    this.ensureRealWechatLogin()
      .then((user) => {
        if (!user || !user.id) {
          throw new Error("登录失败，请重试");
        }
        return this.submitPay(user);
      })
      .catch((err) => {
        const message = String(err || "支付失败");
        if (message === "__NEED_PHONE_AUTH__") {
          this.setData({ requiresPhoneLogin: true });
          wx.showToast({ title: "请先授权手机号登录", icon: "none" });
          return;
        }
        if (message === "__PHONE_INVALID__") {
          return;
        }
        if (message === "Error: __PAYING__" || message === "Error: __PAY_CANCELLED__" || message === "Error: __PAY_FAILED__") {
          return;
        }
        if (message.indexOf("取消") > -1) {
          wx.showToast({ title: "已取消授权", icon: "none" });
          return;
        }
        wx.showToast({ title: message, icon: "none" });
      });
  },
  onPhoneAuthAndPay(e) {
    if (this.data.paying || !this.ensurePrivacyAgreed()) {
      return;
    }
    const detail = (e && e.detail) || {};
    if (!detail.code) {
      wx.showToast({ title: "你已取消手机号授权", icon: "none" });
      return;
    }
    this.ensureRealWechatLogin(detail.code)
      .then((user) => {
        if (!user || !user.id) {
          throw new Error("登录失败，请重试");
        }
        return this.submitPay(user);
      })
      .catch((err) => {
        const message = String(err || "支付失败");
        if (message === "__PHONE_INVALID__") {
          return;
        }
        if (message === "Error: __PAYING__" || message === "Error: __PAY_CANCELLED__" || message === "Error: __PAY_FAILED__") {
          return;
        }
        if (message.indexOf("取消") > -1) {
          wx.showToast({ title: "已取消授权", icon: "none" });
          return;
        }
        wx.showToast({ title: message, icon: "none" });
      });
  },
  createOrder(payload) {
    return request({ url: "/api/orders", method: "POST", data: payload })
      .catch((err) => {
        const message = String(err || "");
        const needSlotId =
          message.indexOf("slotId") > -1 &&
          (message.indexOf("不能为空") > -1 ||
            message.indexOf("不能为null") > -1 ||
            message.toLowerCase().indexOf("must not be null") > -1);
        if (!needSlotId) {
          throw err;
        }
        return this.pickFallbackSlotId(payload.activityId, payload.quantity)
          .then((slotId) => {
            if (!slotId) {
              throw new Error("当前活动未配置可用时段，请联系管理员补充时段");
            }
            return request({
              url: "/api/orders",
              method: "POST",
              data: { ...payload, slotId }
            });
          });
      });
  },
  pickFallbackSlotId(activityId, quantity) {
    return request({ url: `/api/activities/${activityId}/slots` })
      .then((slots) => {
        const list = Array.isArray(slots) ? slots : [];
        const target = list.find((item) => {
          if (!item || item.status !== "active") {
            return false;
          }
          const remaining = Number(item.remaining);
          if (!Number.isNaN(remaining)) {
            return remaining >= quantity;
          }
          const capacity = Number(item.capacity || 0);
          const booked = Number(item.booked || 0);
          return capacity - booked >= quantity;
        });
        return target && target.id ? Number(target.id) : null;
      });
  },
  confirmPaidAndRedirect(orderId, remain) {
    request({ url: `/api/orders/${orderId}/pay`, method: "POST" })
      .then(() => {
        wx.redirectTo({ url: `/pages/order-detail/index?id=${orderId}` });
      })
      .catch(() => {
        if (remain <= 1) {
          wx.redirectTo({ url: `/pages/order-detail/index?id=${orderId}` });
          return;
        }
        setTimeout(() => this.confirmPaidAndRedirect(orderId, remain - 1), 1000);
      });
  },
  openSafety() {
    this.setData({ showSafety: true });
  },
  closeSafety() {
    this.setData({ showSafety: false });
  },
  noop() {},
  onRetry() {
    this.loadData();
  },
  onGoBack() {
    wx.navigateBack();
  },
  resolveProductCover(product) {
    if (!product) {
      return DEFAULT_ACTIVITY_COVER;
    }
    const gallery = Array.isArray(product.gallery) ? product.gallery : [];
    const candidates = [product.cover, product.imageUrl, gallery[0]];
    for (let i = 0; i < candidates.length; i += 1) {
      const value = String(candidates[i] || "").trim();
      if (value) {
        return value;
      }
    }
    return DEFAULT_ACTIVITY_COVER;
  },
  onSummaryImageError() {
    const product = this.data.product || {};
    if (product.coverImage === DEFAULT_ACTIVITY_COVER) {
      return;
    }
    this.setData({ "product.coverImage": DEFAULT_ACTIVITY_COVER });
  }
});
