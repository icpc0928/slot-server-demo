package com.leo.slotserver.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 完整回合結果 — 一次 play() 的完整輸出
 * <p>
 * 結構：
 *   RoundResult
 *   ├── BaseResult (主遊戲)
 *   │   └── List<CascadeRound>  (初始停輪 + N 次連消)
 *   ├── List<FreeSpinResult>    (每一轉免費遊戲)
 *   │   └── List<CascadeRound>  (初始停輪 + N 次連消)
 *   └── 總計資訊
 * </p>
 */
@Data
@Builder
public class RoundResult {
    private String gameId;
    private double betAmount;

    // 主遊戲結果
    private BaseResult baseResult;

    // 免費遊戲結果（每一轉一個 FreeSpinResult）
    private List<FreeSpinResult> freeSpinResults;

    // 總計
    private double baseWin;                      // 主遊戲贏分
    private double freeSpinWin;                  // 免費遊戲總贏分
    private double totalWin;                     // 最終總贏分
    private boolean freeSpinTriggered;           // 是否觸發免費遊戲
    private int totalFreeSpins;                  // 免費遊戲總次數（含 retrigger）
    private double winMultiplier;                // 總贏分 / 投注 倍數
}
