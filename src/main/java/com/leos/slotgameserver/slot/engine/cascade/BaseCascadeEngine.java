package com.leos.slotgameserver.slot.engine.cascade;

import com.leos.slotgameserver.slot.core.GameState;
import com.leos.slotgameserver.slot.core.SlotConfig;
import com.leos.slotgameserver.slot.engine.wheel.WheelEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 基礎消除掉落引擎
 * 中獎符號消除，上方符號下落，頂部補新符號
 */
public class BaseCascadeEngine implements CascadeEngine {
    
    protected final SlotConfig config;
    protected final WheelEngine wheelEngine;
    protected final Random random = new Random();
    
    public BaseCascadeEngine(SlotConfig config, WheelEngine wheelEngine) {
        this.config = config;
        this.wheelEngine = wheelEngine;
    }
    
    @Override
    public int[][] cascade(int[][] grid, int[][] winGrid, GameState gameState, int wheelIndex, int roundIndex) {
        int maxReel = config.getMaxReel();
        int[] gridSize = config.getGridSize();
        
        int[][] newGrid = new int[maxReel][];
        
        for (int reel = 0; reel < maxReel; reel++) {
            newGrid[reel] = new int[gridSize[reel]];
            
            // 收集未中獎的符號（從下往上）
            List<Integer> remaining = new ArrayList<>();
            for (int row = gridSize[reel] - 1; row >= 0; row--) {
                if (winGrid[reel][row] == 0) {
                    remaining.add(0, grid[reel][row]);
                }
            }
            
            // 計算需要補充的數量
            int needNew = gridSize[reel] - remaining.size();
            
            // 生成新符號
            List<Integer> newSymbols = generateNewSymbols(gameState, wheelIndex, reel, needNew, roundIndex);
            
            // 填充盤面：新符號在上，保留符號在下
            int idx = 0;
            for (int symbol : newSymbols) {
                newGrid[reel][idx++] = symbol;
            }
            for (int symbol : remaining) {
                newGrid[reel][idx++] = symbol;
            }
        }
        
        return newGrid;
    }
    
    /**
     * 生成新的掉落符號
     * 子類可覆寫以實現特殊掉落邏輯
     */
    protected List<Integer> generateNewSymbols(GameState gameState, int wheelIndex, int reel, int count, int roundIndex) {
        List<Integer> symbols = new ArrayList<>();
        int[][] wheel = wheelEngine.getWheel(gameState, wheelIndex);
        
        if (wheel == null || wheel[reel] == null) {
            return symbols;
        }
        
        for (int i = 0; i < count; i++) {
            int idx = random.nextInt(wheel[reel].length);
            symbols.add(wheel[reel][idx]);
        }
        
        return symbols;
    }
}
