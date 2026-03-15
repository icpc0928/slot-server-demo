package com.leos.slotgameserver.slot.core.result;

import com.leos.slotgameserver.slot.core.GameState;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 單一消除回合結果
 */
@Data
public class Round {
    
    // === 遊戲狀態 ===
    private GameState gameState;
    private int roundIndex;                 // 第幾消（0 開始）
    
    // === 盤面 ===
    private int[][] grid;                   // 主盤面
    private int[][] winGrid;                // 中獎位置
    private int[] winCountPerReel;          // 每軸消除數量
    
    // === 獎項 ===
    private List<GameAward> awards = new ArrayList<>();
    private long totalWin;                  // 本回合贏分
    private long accWin;                    // 累積贏分
    
    // === 狀態標記 ===
    private boolean hasRespin;              // 是否繼續連消
    private boolean isFree;                 // 是否觸發免費遊戲
    private int freeSpinCount;              // 獲得的免費次數
    private int scatterCount;               // Scatter 數量
    
    // === 擴展欄位（供具體遊戲使用）===
    private Map<String, Object> extra = new HashMap<>();  // 遊戲專用資料放這裡
}
