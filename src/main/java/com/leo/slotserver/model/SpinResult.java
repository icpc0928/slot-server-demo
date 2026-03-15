package com.leo.slotserver.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 單次旋轉結果 — 包含盤面、贏分、Feature 觸發
 */
@Data
@Builder
public class SpinResult {
    private int[][] grid;               // 盤面結果 [reel][row]
    private List<WinResult> wins;       // 所有中獎
    private double totalWin;            // 本次總贏分
    private int scatterCount;           // Scatter 數量
    private boolean featureTriggered;   // 是否觸發 Feature
    private int cascadeLevel;           // 連消層數

    public boolean hasWin() {
        return totalWin > 0;
    }
}
