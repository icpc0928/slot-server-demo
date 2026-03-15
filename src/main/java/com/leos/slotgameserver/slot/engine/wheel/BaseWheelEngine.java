package com.leos.slotgameserver.slot.engine.wheel;

import com.leos.slotgameserver.slot.core.GameState;
import com.leos.slotgameserver.slot.core.PlayMode;
import com.leos.slotgameserver.slot.core.SlotConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 基礎輪帶引擎實現
 */
public class BaseWheelEngine implements WheelEngine {
    
    protected final SlotConfig config;
    protected final Random random = new Random();
    
    // 輪帶表 [gameState][wheelIndex] -> wheel
    protected final Map<GameState, Map<Integer, int[][]>> wheels = new HashMap<>();
    
    // 輪帶權重表 [gameState][playMode][prob] -> weights
    protected final Map<GameState, Map<PlayMode, Map<Integer, int[]>>> wheelWeights = new HashMap<>();
    
    protected int defaultProb = 96;
    
    public BaseWheelEngine(SlotConfig config) {
        this.config = config;
    }
    
    /**
     * 註冊輪帶
     */
    public void registerWheel(GameState gameState, int wheelIndex, int[][] wheel) {
        wheels.computeIfAbsent(gameState, k -> new HashMap<>())
              .put(wheelIndex, wheel);
    }
    
    /**
     * 註冊輪帶權重
     */
    public void registerWheelWeight(GameState gameState, PlayMode playMode, int prob, int[] weight) {
        wheelWeights
            .computeIfAbsent(gameState, k -> new HashMap<>())
            .computeIfAbsent(playMode, k -> new HashMap<>())
            .put(prob, weight);
    }
    
    @Override
    public int selectWheelIndex(GameState gameState, PlayMode playMode, int prob) {
        var stateWeights = wheelWeights.get(gameState);
        if (stateWeights == null) return 0;
        
        var modeWeights = stateWeights.get(playMode);
        if (modeWeights == null) return 0;
        
        int[] weights = modeWeights.getOrDefault(prob, modeWeights.get(defaultProb));
        if (weights == null) return 0;
        
        return getIndexByWeight(weights);
    }
    
    @Override
    public int[][] generateGrid(GameState gameState, int wheelIndex) {
        int[][] wheel = getWheel(gameState, wheelIndex);
        if (wheel == null) {
            throw new IllegalStateException("Wheel not found: " + gameState + ", index=" + wheelIndex);
        }
        
        int[][] grid = new int[config.getMaxReel()][];
        int[] gridSize = config.getGridSize();
        
        for (int i = 0; i < config.getMaxReel(); i++) {
            grid[i] = new int[gridSize[i]];
            int startIndex = random.nextInt(wheel[i].length);
            
            for (int j = 0; j < gridSize[i]; j++) {
                grid[i][j] = wheel[i][(startIndex + j) % wheel[i].length];
            }
        }
        
        return grid;
    }
    
    @Override
    public int[][] getWheel(GameState gameState, int wheelIndex) {
        var stateWheels = wheels.get(gameState);
        if (stateWheels == null) return null;
        return stateWheels.get(wheelIndex);
    }
    
    /**
     * 根據權重隨機選擇索引
     */
    protected int getIndexByWeight(int[] weights) {
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        
        int rand = random.nextInt(total);
        int acc = 0;
        
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (rand < acc) {
                return i;
            }
        }
        
        return 0;
    }
}
