package com.example.bookingservice.api;

import com.example.bookingservice.domain.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record BookingStatusRequest(@NotNull(message = "status is required") BookingStatus status) {}
