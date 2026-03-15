package com.leo.slotserver.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 遊戲設定 — 從 YAML 設定檔載入
 * 單一責任原則 (SRP)：只負責存放遊戲的靜態配置
 */
@Data
public class GameConfig {
    private String gameId;
    private String gameName;
    private int reels;
    private int rows;
    private int betMultiplier;
    private EvalType evalType;    // LINES, WAYS, CLUSTER
    private int maxWinCap;        // 最大贏分倍數 (x total bet)

    // 符號定義
    private List<SymbolDefinition> symbols;
    private int wildSymbolId;
    private int scatterSymbolId;

    // 賠率表: symbolId -> [0, 0, 3x, 5x, 10x, 25x] (index = match count)
    private Map<Integer, int[]> paytable;

    // 輪帶: reelIndex -> symbol id array
    private Map<Integer, int[]> baseReelStrips;
    private Map<Integer, int[]> freeReelStrips;

    // Free Spin 設定
    private int scatterToTrigger;     // 需要幾個 Scatter 觸發
    private int freeSpinCount;        // 免費次數
    private boolean hasCascade;       // 是否有連消

    // 連消倍率表 (cascade index -> multiplier)
    private int[] cascadeMultipliers;

    public enum EvalType {
        LINES, WAYS, SCATTER_PAY, CLUSTER
    }

    @Data
    public static class SymbolDefinition {
        private int id;
        private String name;
        private SymbolType type;
    }
}
