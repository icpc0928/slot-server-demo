package com.leo.slotserver.model;

import lombok.Builder;
import lombok.Data;

/**
 * Spin 上下文 — 每次旋轉的輸入參數
 * 將所有旋轉需要的資訊封裝在一起，避免方法參數過多
 */
@Data
@Builder
public class SpinContext {
    private String gameId;
    private double betAmount;
    private double lineBet;        // betAmount / betMultiplier
    private boolean isFreeSpinMode;
    private int cascadeLevel;      // 連消第幾次 (0-based)
    private int freeSpinsRemaining;
    private int currentMultiplier;
}
