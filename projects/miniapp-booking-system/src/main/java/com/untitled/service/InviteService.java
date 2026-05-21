package com.untitled.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.untitled.config.WechatProperties;
import com.untitled.dto.InviteOverviewResponse;
import com.untitled.dto.InviteRecordResponse;
import com.untitled.mapper.UserMapper;
import com.untitled.model.User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InviteService {
    private final UserMapper userMapper;
    private final WechatAuthService wechatAuthService;
    private final WechatProperties wechatProperties;

    public InviteService(UserMapper userMapper,
                         WechatAuthService wechatAuthService,
                         WechatProperties wechatProperties) {
        this.userMapper = userMapper;
        this.wechatAuthService = wechatAuthService;
        this.wechatProperties = wechatProperties;
    }

    public boolean bindInvite(long currentUserId, Long inviterId, String source) {
        // 作用：绑定邀请关系；方法：校验邀请双方身份后写入 inviter 关系
        if (inviterId == null || inviterId <= 0) {
            return false;
        }
        if (currentUserId == inviterId.longValue()) {
            return false;
        }
        User current = userMapper.findById(currentUserId);
        // 作用：查询当前用户；方法：根据当前登录用户 id 读取用户信息
        if (current == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (current.getInviterId() != null && current.getInviterId() > 0) {
            return false;
        }
        User inviter = userMapper.findById(inviterId);
        // 作用：查询邀请人信息；方法：根据 inviterId 读取邀请人数据
        if (inviter == null) {
            return false;
        }
        if (inviter.getScanUser() == null || inviter.getScanUser() != 1) {
            throw new IllegalArgumentException("仅扫码用户可邀请他人");
        }
        boolean bound = userMapper.bindInviter(currentUserId, inviterId) > 0;
        // 作用：建立邀请关系；方法：在数据库中把当前用户的 inviterId 更新为邀请人
        if (bound) {
            current.setInviterId(inviterId);
            current.setInvitedAt(current.getInvitedAt() != null ? current.getInvitedAt() : LocalDateTime.now());
            current.setScanUser(1); // 作用：激活扫码身份；方法：绑定成功后把当前用户标记为扫码用户
            if (current.getScanActivatedAt() == null) {
                current.setScanActivatedAt(LocalDateTime.now());
            }
            if (current.getDepth() == null || current.getDepth() < 1 || current.getDepth() > 3) {
                current.setDepth(null);
            }
            userMapper.update(current); // 作用：回写邀请状态；方法：把更新后的用户信息保存回数据库
        }
        return bound; // 作用：返回绑定结果；方法：把数据库更新是否成功作为布尔值返回
    }

    public InviteOverviewResponse getOverview(long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        boolean scanUser = user.getScanUser() != null && user.getScanUser() == 1;
        InviteOverviewResponse response = new InviteOverviewResponse();
        response.setUserId(user.getId());
        response.setInviterId(user.getInviterId());
        response.setScanUser(scanUser);
        response.setInviteCount(userMapper.countByInviterId(user.getId()));
        response.setInviteCode(buildInviteCode(user.getId()));
        if (scanUser) {
            response.setQrCodePath("/api/commission/invite/qrcode?inviterId=" + user.getId());
        } else {
            response.setQrCodePath("");
        }
        return response;
    }

    public List<InviteRecordResponse> listMyInvites(long userId) {
        return userMapper.findByInviterId(userId).stream()
                .map(this::toRecord)
                .collect(Collectors.toList());
    }

    public byte[] loadInviteQrcode(long inviterId) {
        User inviter = userMapper.findById(inviterId);
        if (inviter == null) {
            throw new IllegalArgumentException("邀请用户不存在");
        }
        if (inviter.getScanUser() == null || inviter.getScanUser() != 1) {
            throw new IllegalArgumentException("当前用户暂无邀请资格");
        }
        WechatProperties.Invite invite = wechatProperties.getInvite();
        String scene = "i_" + inviterId;
        String page = invite != null ? invite.getPage() : "pages/home/index";
        Integer width = invite != null ? invite.getWidth() : 430;
        String envVersion = invite != null ? invite.getEnvVersion() : "release";
        try {
            return wechatAuthService.getUnlimitedCode(scene, page, width, envVersion);
        } catch (Exception ignored) {
            return buildFallbackQrcode(inviterId, width, invite != null ? invite.getFallbackUrl() : "");
        }
    }

    private InviteRecordResponse toRecord(User user) {
        InviteRecordResponse response = new InviteRecordResponse();
        response.setUserId(user.getId());
        response.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : "微信用户");
        response.setPhone(maskPhone(user.getPhone()));
        response.setInvitedAt(user.getInvitedAt());
        return response;
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }
        String value = phone.trim();
        if (value.matches("^1\\d{10}$")) {
            return value.substring(0, 3) + "****" + value.substring(7);
        }
        return value;
    }

    private String buildInviteCode(long userId) {
        return "I" + userId;
    }

    private byte[] buildFallbackQrcode(long inviterId, Integer width, String fallbackUrl) {
        try {
            String url = buildFallbackUrl(inviterId, fallbackUrl);
            int size = width != null && width > 0 ? width : 430;
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException ex) {
            throw new IllegalArgumentException("生成邀请二维码失败");
        } catch (Exception ex) {
            throw new IllegalArgumentException("生成邀请二维码失败");
        }
    }

    private String buildFallbackUrl(long inviterId, String fallbackUrl) {
        String base = StringUtils.hasText(fallbackUrl) ? fallbackUrl.trim() : "https://example.com/invite";
        String delimiter = base.contains("?") ? "&" : "?";
        return base + delimiter + "inviterId=" + inviterId;
    }
}
