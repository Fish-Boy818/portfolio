package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.CommissionAccountResponse;
import com.untitled.dto.CommissionRecordResponse;
import com.untitled.dto.CommissionScanActivateRequest;
import com.untitled.dto.CommissionWithdrawalCreateRequest;
import com.untitled.dto.CommissionWithdrawalResponse;
import com.untitled.dto.InviteBindRequest;
import com.untitled.dto.InviteOverviewResponse;
import com.untitled.dto.InviteRecordResponse;
import com.untitled.service.CommissionService;
import com.untitled.service.InviteService;
import com.untitled.service.UserAuthService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commission")
@Validated
public class CommissionController {
    private final CommissionService commissionService;
    private final InviteService inviteService;
    private final UserAuthService userAuthService;

    public CommissionController(CommissionService commissionService, InviteService inviteService, UserAuthService userAuthService) {
        this.commissionService = commissionService;
        this.inviteService = inviteService;
        this.userAuthService = userAuthService;
    }

    @PostMapping("/scan/activate")
    public ApiResponse<Map<String, Object>> activateScan(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                         @RequestBody(required = false) CommissionScanActivateRequest request) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        commissionService.activateScanQualification(currentUserId);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("scanUser", true);
        return ApiResponse.ok(result);
    }

    @GetMapping("/account")
    public ApiResponse<CommissionAccountResponse> account(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(commissionService.getAccount(currentUserId));
    }

    @GetMapping("/records")
    public ApiResponse<List<CommissionRecordResponse>> records(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                               @RequestParam(required = false) String status) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(commissionService.listUserRecords(currentUserId, status));
    }

    @GetMapping("/withdrawals")
    public ApiResponse<List<CommissionWithdrawalResponse>> withdrawals(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                                       @RequestParam(required = false) String status) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(commissionService.listUserWithdrawals(currentUserId, status));
    }

    @PostMapping("/withdrawals")
    public ApiResponse<CommissionWithdrawalResponse> createWithdrawal(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                                      @Valid @RequestBody CommissionWithdrawalCreateRequest request) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(commissionService.createWithdrawal(currentUserId, request.getAmount(), request.getIdemKey()));
    }

    @PostMapping("/withdrawals/{id}/sync")
    public ApiResponse<CommissionWithdrawalResponse> syncWithdrawal(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                                    @PathVariable long id) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(commissionService.syncWithdrawal(currentUserId, id));
    }

    @PostMapping("/withdrawals/{id}/cancel")
    public ApiResponse<Map<String, Object>> cancelWithdrawal(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                             @PathVariable long id) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        commissionService.cancelWithdrawalByUser(currentUserId, id);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("cancelled", true);
        return ApiResponse.ok(result);
    }

    @PostMapping("/withdraw/notify")
    public Map<String, String> withdrawNotify(@RequestBody String payload, HttpServletRequest request) {
        return commissionService.handleWithdrawNotify(
                payload,
                request.getHeader("Wechatpay-Timestamp"),
                request.getHeader("Wechatpay-Nonce"),
                request.getHeader("Wechatpay-Signature"),
                request.getHeader("Wechatpay-Serial")
        );
    }

    @PostMapping("/invite/bind")
    public ApiResponse<Map<String, Object>> bindInvite(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestBody(required = false) InviteBindRequest request) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        Long inviterId = request != null ? request.getInviterId() : null;
        String source = request != null ? request.getSource() : null;
        boolean bound = inviteService.bindInvite(currentUserId, inviterId, source);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("bound", bound);
        return ApiResponse.ok(result);
    }

    @GetMapping("/invite/me")
    public ApiResponse<InviteOverviewResponse> inviteOverview(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(inviteService.getOverview(currentUserId));
    }

    @GetMapping("/invite/records")
    public ApiResponse<List<InviteRecordResponse>> inviteRecords(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long currentUserId = userAuthService.resolveUserId(authorization);
        if (currentUserId == null) {
            return ApiResponse.fail("请先登录");
        }
        return ApiResponse.ok(inviteService.listMyInvites(currentUserId));
    }

    @GetMapping(value = "/invite/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> inviteQrcode(@RequestParam long inviterId) {
        byte[] bytes = inviteService.loadInviteQrcode(inviterId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }
}
