package com.untitled.service;

import com.untitled.dto.UserProfileResponse;
import com.untitled.mapper.OrderMapper;
import com.untitled.model.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProfileService {
    private final OrderMapper orderMapper;

    public ProfileService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public BigDecimal getTotalSpend(long userId) {
        BigDecimal sum = orderMapper.sumUsedPayAmount(userId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public int getPriceLevel(long userId) {
        return 1;
    }

    public UserProfileResponse toProfile(User user) {
        BigDecimal totalSpend = getTotalSpend(user.getId());
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(user.getId());
        profile.setOpenId(user.getOpenId());
        profile.setNickname(user.getNickname());
        profile.setAvatarUrl(user.getAvatarUrl());
        profile.setPhone(user.getPhone());
        profile.setTotalSpend(totalSpend);
        profile.setPriceLevel(1);
        profile.setRemainToUnlock(BigDecimal.ZERO);
        profile.setScanUser(user.getScanUser() != null && user.getScanUser() == 1);
        return profile;
    }
}
