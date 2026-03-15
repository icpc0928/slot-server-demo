package com.leos.slotgameserver.slot.engine.award;

import com.leos.slotgameserver.slot.core.SlotConfig;
import com.leos.slotgameserver.slot.core.result.GameAward;

import java.util.List;

/**
 * 獎項計算器介面
 */
public interface AwardCalculator {
    
    /**
     * 計算盤面獎項
     * @param lineBet 線注
     * @param grid 盤面
     * @param config 遊戲配置
     * @return 獎項列表
     */
    List<GameAward> calculate(long lineBet, int[][] grid, SlotConfig config);
    
    /**
     * 取得中獎位置盤面
     * @return 中獎位置（1=中獎, 0=未中獎）
     */
    int[][] getWinGrid();
}
