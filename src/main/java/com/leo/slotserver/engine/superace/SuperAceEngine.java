package com.leo.slotserver.engine.superace;

import com.leo.slotserver.engine.AbstractSlotEngine;
import com.leo.slotserver.engine.LinesEvaluator;
import com.leo.slotserver.model.*;

import java.util.List;

/**
 * Super Ace 引擎
 * <p>
 * 特色：5x3 Lines（線制）— 展示與 Ways 引擎不同的計算策略
 * 使用 LinesEvaluator 而非 WaysEvaluator，體現 Strategy Pattern
 * </p>
 */
public class SuperAceEngine extends AbstractSlotEngine {

    private final LinesEvaluator linesEvaluator;

    // Super Ace 50 線定義（簡化版 — 前 20 線）
    private static final int[][] PAYLINES = {
            {1, 1, 1, 1, 1},  // Line 1: 中間一排
            {0, 0, 0, 0, 0},  // Line 2: 上排
            {2, 2, 2, 2, 2},  // Line 3: 下排
            {0, 1, 2, 1, 0},  // Line 4: V 形
            {2, 1, 0, 1, 2},  // Line 5: 倒 V
            {0, 0, 1, 2, 2},  // Line 6
            {2, 2, 1, 0, 0},  // Line 7
            {1, 0, 0, 0, 1},  // Line 8
            {1, 2, 2, 2, 1},  // Line 9
            {0, 1, 0, 1, 0},  // Line 10
            {2, 1, 2, 1, 2},  // Line 11
            {1, 0, 1, 0, 1},  // Line 12
            {1, 2, 1, 2, 1},  // Line 13
            {0, 1, 1, 1, 0},  // Line 14
            {2, 1, 1, 1, 2},  // Line 15
            {1, 1, 0, 1, 1},  // Line 16
            {1, 1, 2, 1, 1},  // Line 17
            {0, 2, 0, 2, 0},  // Line 18
            {2, 0, 2, 0, 2},  // Line 19
            {0, 2, 2, 2, 0},  // Line 20
    };

    public SuperAceEngine(GameConfig config) {
        super(config);
        this.linesEvaluator = new LinesEvaluator(config, PAYLINES);
    }

    @Override
    protected List<WinResult> evaluateWins(int[][] grid, SpinContext context) {
        return linesEvaluator.evaluate(grid, context);
    }

    /**
     * Super Ace 固定 10 次 Free Spin
     */
    @Override
    protected int determineFreeSpinCount(SpinResult triggerSpin) {
        return 10;
    }
}
