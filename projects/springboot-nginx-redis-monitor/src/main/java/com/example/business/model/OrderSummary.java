package com.example.business.model;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummary(
    String orderId,
    String status,
    BigDecimal amount,
    Instant generatedAt
) {
}
