package com.leos.slotgameserver.slot.engine.award;

import com.leos.slotgameserver.slot.core.SlotConfig;
import com.leos.slotgameserver.slot.core.result.GameAward;

import java.util.*;

/**
 * Ways 算獎計算器
 * 計算 4096 Ways 等無線玩法的獎項
 */
public class WaysAwardCalculator implements AwardCalculator {
    
    private int[][] winGrid;
    private int multiplier = 1;
    
    public WaysAwardCalculator() {}
    
    public WaysAwardCalculator(int multiplier) {
        this.multiplier = multiplier;
    }
    
    @Override
    public List<GameAward> calculate(long lineBet, int[][] grid, SlotConfig config) {
        List<GameAward> awards = new ArrayList<>();
        int maxReel = config.getMaxReel();
        int[] gridSize = config.getGridSize();
        int wild = config.getWildSymbol();
        int scatter = config.getScatterSymbol();
        int[][] payTable = config.getPayTable();
        
        // 初始化中獎盤面
        winGrid = new int[maxReel][];
        for (int i = 0; i < maxReel; i++) {
            winGrid[i] = new int[gridSize[i]];
        }
        
        Set<Integer> totalWinPos = new HashSet<>();
        
        // 遍歷所有可中獎符號
        for (int symbol = 0; symbol < payTable.length; symbol++) {
            // 跳過 Wild 和 Scatter
            if (symbol == wild || symbol == scatter) continue;
            
            Set<Integer> winPos = new HashSet<>();
            int reelCount = 0;
            int ways = 1;
            
            // 從左到右計算連續軸
            int pos = 0;
            for (int reel = 0; reel < maxReel; reel++) {
                int count = 0;
                
                for (int row = 0; row < gridSize[reel]; row++) {
                    int gridSymbol = grid[reel][row];
                    if (gridSymbol == symbol || gridSymbol == wild) {
                        count++;
                        winPos.add(pos + row);
                    }
                }
                
                pos += gridSize[reel];
                
                if (count == 0) {
                    break;
                }
                
                reelCount++;
                ways *= count;
            }
            
            // 檢查是否有獎
            if (reelCount >= 3 && reelCount < payTable[symbol].length && payTable[symbol][reelCount] > 0) {
                long win = lineBet * payTable[symbol][reelCount] * ways * multiplier;
                awards.add(new GameAward(symbol, reelCount, ways, lineBet, payTable[symbol][reelCount], multiplier, win));
                totalWinPos.addAll(winPos);
            }
        }
        
        // 設置中獎盤面
        for (int winPosValue : totalWinPos) {
            int tempPos = winPosValue;
            for (int reel = 0; reel < maxReel; reel++) {
                if (tempPos < gridSize[reel]) {
                    winGrid[reel][tempPos] = 1;
                    break;
                }
                tempPos -= gridSize[reel];
            }
        }
        
        return awards;
    }
    
    @Override
    public int[][] getWinGrid() {
        return winGrid;
    }
    
    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }
}
