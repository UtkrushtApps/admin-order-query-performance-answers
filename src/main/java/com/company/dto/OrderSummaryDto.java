package com.company.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderSummaryDto(
        Long orderId,
        String customerEmail,
        BigDecimal totalAmount,
        OffsetDateTime createdAt,
        long itemCount) {
}
