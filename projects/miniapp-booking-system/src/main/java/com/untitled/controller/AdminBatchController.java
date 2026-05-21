package com.untitled.controller;

import com.untitled.dto.ActivityCreateRequest;
import com.untitled.dto.ApiResponse;
import com.untitled.dto.BatchResult;
import com.untitled.dto.ClubCreateRequest;
import com.untitled.dto.SlotCreateRequest;
import com.untitled.dto.UserCreateRequest;
import com.untitled.service.ActivityService;
import com.untitled.service.ClubService;
import com.untitled.service.SlotService;
import com.untitled.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/batch")
@Validated
public class AdminBatchController {
    private final UserService userService;
    private final ClubService clubService;
    private final ActivityService activityService;
    private final SlotService slotService;

    public AdminBatchController(UserService userService,
                                ClubService clubService,
                                ActivityService activityService,
                                SlotService slotService) {
        this.userService = userService;
        this.clubService = clubService;
        this.activityService = activityService;
        this.slotService = slotService;
    }

    @PostMapping("/users")
    public ApiResponse<BatchResult> importUsers(@RequestBody List<UserCreateRequest> requests) {
        BatchResult result = new BatchResult();
        result.setTotal(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            try {
                UserCreateRequest request = requests.get(i);
                if (request.getNickname() == null || request.getNickname().trim().isEmpty()) {
                    throw new IllegalArgumentException("昵称不能为空");
                }
                userService.createUser(request);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception ex) {
                result.setFailed(result.getFailed() + 1);
                result.addError("第" + (i + 1) + "条用户导入失败: " + ex.getMessage());
            }
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/clubs")
    public ApiResponse<BatchResult> importClubs(@RequestBody List<ClubCreateRequest> requests) {
        BatchResult result = new BatchResult();
        result.setTotal(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            try {
                ClubCreateRequest request = requests.get(i);
                if (request.getName() == null || request.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("名称不能为空");
                }
                clubService.create(request);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception ex) {
                result.setFailed(result.getFailed() + 1);
                result.addError("第" + (i + 1) + "条俱乐部导入失败: " + ex.getMessage());
            }
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/activities")
    public ApiResponse<BatchResult> importActivities(@RequestBody List<ActivityCreateRequest> requests) {
        BatchResult result = new BatchResult();
        result.setTotal(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            try {
                ActivityCreateRequest request = requests.get(i);
                if (request.getClubId() == null) {
                    throw new IllegalArgumentException("俱乐部ID不能为空");
                }
                if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                    throw new IllegalArgumentException("标题不能为空");
                }
                if (request.getBasePrice() == null && request.getOriginalPrice() == null) {
                    throw new IllegalArgumentException("售价不能为空");
                }
                activityService.create(request);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception ex) {
                result.setFailed(result.getFailed() + 1);
                result.addError("第" + (i + 1) + "条活动导入失败: " + ex.getMessage());
            }
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/slots")
    public ApiResponse<BatchResult> importSlots(@RequestBody List<SlotCreateRequest> requests) {
        BatchResult result = new BatchResult();
        result.setTotal(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            try {
                SlotCreateRequest request = requests.get(i);
                if (request.getActivityId() == null) {
                    throw new IllegalArgumentException("活动ID不能为空");
                }
                if (request.getSlotDate() == null) {
                    throw new IllegalArgumentException("日期不能为空");
                }
                if (request.getSlotTime() == null || request.getSlotTime().trim().isEmpty()) {
                    throw new IllegalArgumentException("时段不能为空");
                }
                if (request.getCapacity() == null) {
                    throw new IllegalArgumentException("容量不能为空");
                }
                slotService.create(request);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception ex) {
                result.setFailed(result.getFailed() + 1);
                result.addError("第" + (i + 1) + "条时段导入失败: " + ex.getMessage());
            }
        }
        return ApiResponse.ok(result);
    }
}
