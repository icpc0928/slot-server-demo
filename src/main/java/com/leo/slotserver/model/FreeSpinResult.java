package com.leo.slotserver.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 單次免費旋轉結果
 * <p>
 * 每一轉免費遊戲 = 初始停輪 + N 次連消，結構同主遊戲。
 * 但免費遊戲可能有不同的倍率、不同的輪帶。
 * </p>
 */
@Data
@Builder
public class FreeSpinResult {
    private int spinIndex;                  // 第幾轉（0-based）
    private List<CascadeRound> rounds;      // 所有消除結果（含初始停輪）
    private double totalWin;                // 本轉總贏分
    private int currentMultiplier;          // 本轉使用的倍率
    private int scatterCount;               // 本轉 Scatter 數量
    private boolean retrigger;              // 是否重新觸發（加次數）

    /**
     * 取得初始停輪盤面
     */
    public int[][] getInitialGrid() {
        if (rounds == null || rounds.isEmpty()) return null;
        return rounds.get(0).getGrid();
    }

    /**
     * 連消次數
     */
    public int getCascadeCount() {
        if (rounds == null) return 0;
        return Math.max(0, rounds.size() - 1);
    }
}
