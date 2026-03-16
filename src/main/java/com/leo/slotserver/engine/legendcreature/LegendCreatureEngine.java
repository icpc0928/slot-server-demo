package com.leo.slotserver.engine.legendcreature;

import com.leo.slotserver.engine.AbstractSlotEngine;
import com.leo.slotserver.engine.WinEvaluator;
import com.leo.slotserver.engine.LinesEvaluator;
import com.leo.slotserver.engine.SlotGame;
import com.leo.slotserver.model.*;

import java.util.List;

/**
 * Legendary Creature 引擎
 * <p>
 * 最經典的老虎機類型：3x3 單線
 * 只計算中間一條線（3 個符號），結構最單純。
 * Free Spin 中 Wild 帶有隨機倍率（1x~5x）
 * </p>
 */
@SlotGame("legend-creature")
public class LegendCreatureEngine extends AbstractSlotEngine {

    private final WinEvaluator winEvaluator;

    // 3x3 單線：只看中間一行
    private static final int[][] PAYLINES = {
            {1, 1, 1},  // Line 1: 中間一排
    };

    public LegendCreatureEngine(GameConfig config) {
        super(config);
        this.winEvaluator = new LinesEvaluator(config, PAYLINES);
    }

    @Override
    protected List<WinResult> evaluateWins(int[][] grid, SpinContext context) {
        return winEvaluator.evaluate(grid, context);
    }

    /**
     * Legendary Creature 固定 5 次 Free Spin
     */
    @Override
    protected int determineFreeSpinCount(int scatterCount) {
        return 5;
    }
}
