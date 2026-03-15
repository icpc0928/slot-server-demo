package com.leo.slotserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Spin 請求 DTO — 與內部 Model 分離
 * 前端只需要知道 DTO，不會接觸到內部的 GameConfig 或 SpinContext
 */
@Data
public class SpinRequestDTO {
    @NotBlank(message = "gameId is required")
    private String gameId;

    @Positive(message = "betAmount must be positive")
    private double betAmount;
}
