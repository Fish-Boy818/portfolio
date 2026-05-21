package com.untitled.controller;

import com.untitled.dto.DashboardStats;
import com.untitled.mapper.CommissionRecordMapper;
import com.untitled.mapper.OrderMapper;
import com.untitled.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final OrderMapper orderMapper;
    private final CommissionRecordMapper commissionRecordMapper;
    private final UserMapper userMapper;

    public AdminDashboardController(OrderMapper orderMapper, CommissionRecordMapper commissionRecordMapper, UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.commissionRecordMapper = commissionRecordMapper;
        this.userMapper = userMapper;
    }

    @GetMapping("/stats")
    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();
        BigDecimal totalSales = orderMapper.sumTotalSales();
        BigDecimal verifiedAmount = orderMapper.sumVerifiedAmount();
        BigDecimal unverifiedAmount = orderMapper.sumUnverifiedAmount();
        BigDecimal withdrawnCommission = commissionRecordMapper.sumWithdrawnAmount();
        BigDecimal pendingCommission = commissionRecordMapper.sumPendingCommission();
        int totalUsers = userMapper.countAll();
        stats.setTotalSales(totalSales != null ? totalSales : BigDecimal.ZERO);
        stats.setTotalVerified(verifiedAmount != null ? verifiedAmount : BigDecimal.ZERO);
        stats.setTotalUnverified(unverifiedAmount != null ? unverifiedAmount : BigDecimal.ZERO);
        stats.setTotalWithdrawnCommission(withdrawnCommission != null ? withdrawnCommission : BigDecimal.ZERO);
        stats.setTotalPendingCommission(pendingCommission != null ? pendingCommission : BigDecimal.ZERO);
        stats.setTotalUsers(totalUsers);
        return stats;
    }
}
