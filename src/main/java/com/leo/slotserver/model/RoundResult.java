package com.leo.slotserver.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 完整回合結果 — 包含 Base Spin + 所有連消 + Free Spin
 */
@Data
@Builder
public class RoundResult {
    private String gameId;
    private double betAmount;
    private SpinResult baseSpin;                 // 主遊戲結果
    private List<SpinResult> cascadeSpins;       // 連消結果
    private List<SpinResult> freeSpins;          // 免費遊戲結果
    private double baseWin;                      // 主遊戲贏分
    private double freeSpinWin;                  // 免費遊戲總贏分
    private double totalWin;                     // 最終總贏分
    private boolean freeSpinTriggered;           // 是否觸發免費遊戲
    private int totalFreeSpins;                  // 免費遊戲總次數（含 retrigger）
    private double winMultiplier;                // 總贏分 / 投注 倍數
}
