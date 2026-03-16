package com.leo.slotserver.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 主遊戲結果
 * <p>
 * 一次主遊戲旋轉 = 初始停輪 + N 次連消。
 * rounds[0] 是初始停輪結果，rounds[1..N] 是連消結果。
 * </p>
 */
@Data
@Builder
public class BaseResult {
    private List<CascadeRound> rounds;      // 所有消除結果（含初始停輪）
    private double totalWin;                // 主遊戲總贏分
    private int scatterCount;               // Scatter 總數
    private boolean freeSpinTriggered;      // 是否觸發免費遊戲
    private int freeSpinCount;              // 觸發的免費次數

    /**
     * 取得初始停輪盤面
     */
    public int[][] getInitialGrid() {
        if (rounds == null || rounds.isEmpty()) return null;
        return rounds.get(0).getGrid();
    }

    /**
     * 連消次數（不含初始停輪）
     */
    public int getCascadeCount() {
        if (rounds == null) return 0;
        return Math.max(0, rounds.size() - 1);
    }
}
