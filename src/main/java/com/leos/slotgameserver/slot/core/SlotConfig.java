package com.leos.slotgameserver.slot.core;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 老虎機遊戲配置
 * 包含遊戲的基本設定，由具體遊戲實現提供
 */
@Data
@Builder
public class SlotConfig {
    
    // === 基本配置 ===
    private int gameId;
    private String gameName;
    private int maxReel;                    // 軸數
    private int[] gridSize;                 // 每軸格數
    private int betTimes;                   // 押注倍數
    private boolean hasLines;               // 是否有線（false = Ways）
    
    // === 符號配置 ===
    private int wildSymbol;                 // Wild 符號 ID
    private int scatterSymbol;              // Scatter 符號 ID
    private Map<Integer, String> symbolMap; // 符號對應表
    private int[][] payTable;               // 賠付表 [symbol][reelCount]
    
    // === 免費遊戲配置 ===
    private int freeSpinNeed;               // 觸發免費遊戲所需 Scatter 數
    private int baseFreeSpinCount;          // 基礎免費次數
    private int extraFreePerScatter;        // 每多一個 Scatter 額外次數
    
    // === 限制 ===
    private int maxTotalWinLimit;           // 最大贏分限制（總下注的倍數）
    
    // === 遊戲模式 ===
    private boolean supportBuyFree;         // 是否支援買免費
    private int buyFreeMultiplier;          // 買免費倍數
}
