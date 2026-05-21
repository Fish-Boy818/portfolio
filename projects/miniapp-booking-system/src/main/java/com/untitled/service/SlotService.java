package com.untitled.service;

import com.untitled.dto.SlotCreateRequest;
import com.untitled.dto.SlotResponse;
import com.untitled.dto.SlotUpdateRequest;
import com.untitled.mapper.ActivitySlotMapper;
import com.untitled.model.ActivitySlot;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SlotService {
    private final ActivitySlotMapper slotMapper;

    public SlotService(ActivitySlotMapper slotMapper) {
        this.slotMapper = slotMapper;
    }

    public List<SlotResponse> list(long activityId, LocalDate slotDate) {
        return slotMapper.findByActivityId(activityId, slotDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SlotResponse> listAll(Long activityId) {
        return slotMapper.findAll(activityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<SlotResponse> get(long id) {
        ActivitySlot slot = slotMapper.findById(id);
        if (slot == null) {
            return Optional.empty();
        }
        return Optional.of(toResponse(slot));
    }

    public SlotResponse create(SlotCreateRequest request) {
        if (request.getActivityId() == null) {
            throw new IllegalArgumentException("活动ID不能为空");
        }
        ActivitySlot slot = new ActivitySlot();
        slot.setActivityId(request.getActivityId());
        slot.setSlotDate(request.getSlotDate());
        slot.setSlotTime(request.getSlotTime());
        slot.setCapacity(request.getCapacity());
        slot.setBooked(0);
        slot.setStatus("active");
        slotMapper.insert(slot);
        return toResponse(slotMapper.findById(slot.getId()));
    }

    public Optional<SlotResponse> update(long id, SlotUpdateRequest request) {
        ActivitySlot slot = slotMapper.findById(id);
        if (slot == null) {
            return Optional.empty();
        }
        slot.setActivityId(request.getActivityId());
        slot.setSlotDate(request.getSlotDate());
        slot.setSlotTime(request.getSlotTime());
        slot.setCapacity(request.getCapacity());
        slot.setStatus(request.getStatus() != null ? request.getStatus() : slot.getStatus());
        slotMapper.update(slot);
        return Optional.of(toResponse(slotMapper.findById(id)));
    }

    public boolean delete(long id) {
        return slotMapper.delete(id) > 0;
    }

    private SlotResponse toResponse(ActivitySlot slot) {
        SlotResponse response = new SlotResponse();
        response.setId(slot.getId());
        response.setActivityId(slot.getActivityId());
        response.setSlotDate(slot.getSlotDate());
        response.setSlotTime(slot.getSlotTime());
        response.setCapacity(slot.getCapacity());
        response.setBooked(slot.getBooked());
        int remaining = slot.getCapacity() != null && slot.getBooked() != null
                ? Math.max(0, slot.getCapacity() - slot.getBooked())
                : 0;
        response.setRemaining(remaining);
        response.setStatus(slot.getStatus());
        return response;
    }
}
