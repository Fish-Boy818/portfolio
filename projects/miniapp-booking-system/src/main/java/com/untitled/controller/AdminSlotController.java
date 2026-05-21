package com.untitled.controller;

import com.untitled.dto.ApiResponse;
import com.untitled.dto.SlotCreateRequest;
import com.untitled.dto.SlotResponse;
import com.untitled.dto.SlotUpdateRequest;
import com.untitled.service.SlotService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/slots")
@Validated
public class AdminSlotController {
    private final SlotService slotService;

    public AdminSlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    public ApiResponse<List<SlotResponse>> list(@RequestParam(required = false) Long activityId) {
        return ApiResponse.ok(slotService.listAll(activityId));
    }

    @PostMapping
    public ApiResponse<SlotResponse> create(@Valid @RequestBody SlotCreateRequest request) {
        return ApiResponse.ok(slotService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SlotResponse> update(@PathVariable long id, @Valid @RequestBody SlotUpdateRequest request) {
        return slotService.update(id, request)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("时段不存在"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        if (slotService.delete(id)) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.fail("时段不存在");
    }
}
