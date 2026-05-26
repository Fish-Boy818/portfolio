package com.example.business.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.example.business.model.OrderSummary;
import io.micrometer.core.annotation.Timed;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    @Timed(value = "business.order.lookup", description = "Order lookup latency")
    @Cacheable(cacheNames = "orders", keyGenerator = "stableKeyGenerator")
    public OrderSummary getOrderSummary(String orderId) {
        simulateSlowBackend();
        BigDecimal amount = BigDecimal.valueOf(Math.abs(orderId.hashCode() % 100000) / 100.0);
        return new OrderSummary(orderId, "PAID", amount, Instant.now());
    }

    private void simulateSlowBackend() {
        try {
            TimeUnit.MILLISECONDS.sleep(120);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading order", e);
        }
    }
}
