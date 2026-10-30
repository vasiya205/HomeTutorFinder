package com.example.bookingservice.api;

import java.math.BigDecimal;

public record BookingStatsResponse(
        long totalBookings,
        long pending,
        long accepted,
        long completed,
        long rejected,
        long cancelled,
        long paidBookings,
        BigDecimal totalRevenue
) {}
