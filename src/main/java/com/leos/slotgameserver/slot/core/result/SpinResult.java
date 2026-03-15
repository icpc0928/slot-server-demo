package com.leos.slotgameserver.slot.core.result;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 完整的轉盤結果
 */
@Data
public class SpinResult {
    
    // === 下注資訊 ===
    private long totalBet;
    private long lineBet;
    
    // === 主遊戲結果 ===
    private List<Round> baseRounds;
    private long totalBaseWin;
    private boolean triggeredFree;
    private int freeSpinCount;
    
    // === 免費遊戲結果 ===
    private List<List<Round>> freeSpinRounds;   // 外層 List = 每次免費，內層 List = 該次的消除回合
    private long totalFreeWin;
    
    // === 總計 ===
    private long totalWin;
    
    // === 擴展欄位（供具體遊戲使用）===
    private Map<String, Object> extra;
}
