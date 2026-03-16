package com.leo.slotserver.engine.fortunegods;

import com.leo.slotserver.engine.AbstractSlotEngine;
import com.leo.slotserver.engine.SlotGame;
import com.leo.slotserver.engine.WaysEvaluator;
import com.leo.slotserver.model.*;

import java.util.List;
import java.util.Map;

/**
 * Fortune Gods (贏財神) 引擎
 * <p>
 * 特色：6x5 Ways + Cascade（連消）+ 多格符號 + 橫財神模式
 * <p>
 * 覆寫 generateGrid() 展示 Template Method 的彈性：
 * 父類別的盤面生成是「每格獨立隨機」，
 * 但 Fortune Gods 有多格符號（同一個符號佔據 2x2 甚至 3x3 的範圍），
 * 因此需要自定義盤面生成邏輯。
 * </p>
 */
@SlotGame("fortune-gods")
public class FortuneGodsEngine extends AbstractSlotEngine {

    private final WaysEvaluator waysEvaluator;

    // 多格符號出現的機率（百分比），只在第 2~5 軸出現
    private static final int MEGA_SYMBOL_CHANCE = 15;
    // 多格符號最大尺寸
    private static final int MAX_MEGA_SIZE = 3;

    public FortuneGodsEngine(GameConfig config) {
        super(config);
        this.waysEvaluator = new WaysEvaluator(config);
    }

    /**
     * 覆寫盤面生成 — 加入多格符號邏輯
     * <p>
     * 一般老虎機每格獨立隨機填入符號，但 Fortune Gods 有「多格符號」：
     * 一個符號可能佔據 2x2 或 3x3 的範圍，視覺上是一個大符號。
     * <p>
     * 流程：
     * 1. 先用父類別的方式生成基礎盤面
     * 2. 對第 2~5 軸，隨機決定是否產生多格符號
     * 3. 如果產生，把該區域填入相同符號
     * </p>
     */
    @Override
    protected int[][] generateGrid(SpinContext context) {
        // Step 1: 用父類別的邏輯先生成基礎盤面
        int[][] grid = super.generateGrid(context);

        // Step 2: 嘗試在第 2~5 軸放置多格符號（index 1~4）
        for (int reel = 1; reel < config.getReels() - 1; reel++) {
            if (random.nextInt(100) < MEGA_SYMBOL_CHANCE) {
                placeMegaSymbol(grid, reel);
            }
        }

        return grid;
    }

    /**
     * 在指定軸放置多格符號
     * <p>
     * 隨機決定尺寸（2x2 或 3x3）和起始位置，
     * 然後將該區域的所有格子填入同一個普通符號。
     * Wild 和 Scatter 不會變成多格。
     * </p>
     */
    private void placeMegaSymbol(int[][] grid, int startReel) {
        // 隨機決定尺寸：2 或 3
        int size = 2 + random.nextInt(MAX_MEGA_SIZE - 1);

        // 確保不超出盤面邊界
        int maxStartRow = config.getRows() - size;
        int maxEndReel = Math.min(startReel + size, config.getReels());
        if (maxStartRow < 0) return;

        int startRow = random.nextInt(maxStartRow + 1);

        // 選一個普通符號作為多格符號（排除 Wild 和 Scatter）
        int symbolId = pickRegularSymbol(grid, startReel);
        if (symbolId < 0) return;

        // 填入多格符號
        for (int reel = startReel; reel < maxEndReel; reel++) {
            for (int row = startRow; row < startRow + size && row < config.getRows(); row++) {
                grid[reel][row] = symbolId;
            }
        }
    }

    /**
     * 從盤面上挑一個普通符號（非 Wild、非 Scatter）
     */
    private int pickRegularSymbol(int[][] grid, int reel) {
        for (int row = 0; row < grid[reel].length; row++) {
            int symbol = grid[reel][row];
            if (!isWild(symbol) && !isScatter(symbol)) {
                return symbol;
            }
        }
        return -1;
    }

    @Override
    protected List<WinResult> evaluateWins(int[][] grid, SpinContext context) {
        return waysEvaluator.evaluate(grid, context);
    }

    /**
     * Fortune Gods 的 Free Spin：
     * 3 個 Scatter = 10 次
     * 4 個 Scatter = 15 次
     * 5+ 個 Scatter = 20 次
     */
    @Override
    protected int determineFreeSpinCount(SpinResult triggerSpin) {
        return switch (triggerSpin.getScatterCount()) {
            case 3 -> 10;
            case 4 -> 15;
            default -> 20;
        };
    }
}
