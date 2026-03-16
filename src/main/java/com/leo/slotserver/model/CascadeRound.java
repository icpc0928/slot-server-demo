package com.leo.slotserver.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 單次消除結果（一消）
 * <p>
 * 連消機制中，每一次消除都是一個 CascadeRound：
 * 包含當下的盤面、中獎資訊、倍率。
 * 主遊戲的第一消就是初始停輪結果。
 * </p>
 */
@Data
@Builder
public class CascadeRound {
    private int roundIndex;             // 第幾消（0 = 初始停輪）
    private int[][] grid;               // 當下盤面 [reel][row]
    private List<WinResult> wins;       // 本消中獎
    private double totalWin;            // 本消贏分
    private int multiplier;             // 本消倍率
    private int scatterCount;           // 本消 Scatter 數量
    private boolean hasNextCascade;     // 是否還有下一消

    public boolean hasWin() {
        return totalWin > 0;
    }
}
