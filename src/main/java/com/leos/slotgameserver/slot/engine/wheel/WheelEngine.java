package com.leos.slotgameserver.slot.engine.wheel;

import com.leos.slotgameserver.slot.core.GameState;
import com.leos.slotgameserver.slot.core.PlayMode;

/**
 * 輪帶引擎介面
 * 負責輪帶的選擇和盤面生成
 */
public interface WheelEngine {
    
    /**
     * 選擇要使用的輪帶索引
     * @param gameState 遊戲狀態
     * @param playMode 遊戲模式
     * @param prob 機率設定
     * @return 輪帶索引
     */
    int selectWheelIndex(GameState gameState, PlayMode playMode, int prob);
    
    /**
     * 根據輪帶生成盤面
     * @param gameState 遊戲狀態
     * @param wheelIndex 輪帶索引
     * @return 生成的盤面
     */
    int[][] generateGrid(GameState gameState, int wheelIndex);
    
    /**
     * 取得特定輪帶
     * @param gameState 遊戲狀態
     * @param wheelIndex 輪帶索引
     * @return 輪帶資料
     */
    int[][] getWheel(GameState gameState, int wheelIndex);
}
