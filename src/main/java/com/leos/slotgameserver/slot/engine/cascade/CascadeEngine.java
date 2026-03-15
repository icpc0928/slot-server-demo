package com.leos.slotgameserver.slot.engine.cascade;

import com.leos.slotgameserver.slot.core.GameState;

/**
 * 消除掉落引擎介面
 */
public interface CascadeEngine {
    
    /**
     * 執行消除掉落
     * @param grid 當前盤面
     * @param winGrid 中獎位置
     * @param gameState 遊戲狀態
     * @param wheelIndex 輪帶索引
     * @param roundIndex 消除回合數
     * @return 掉落後的新盤面
     */
    int[][] cascade(int[][] grid, int[][] winGrid, GameState gameState, int wheelIndex, int roundIndex);
}
