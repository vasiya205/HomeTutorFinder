package com.example.authservice.api;

import jakarta.validation.constraints.NotNull;

public record AccountEnabledRequest(@NotNull(message = "enabled is required") Boolean enabled) {}
