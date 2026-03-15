package com.leo.slotserver.engine.fortunegods;

import com.leo.slotserver.engine.AbstractSlotEngine;
import com.leo.slotserver.engine.WaysEvaluator;
import com.leo.slotserver.model.*;

import java.util.List;

/**
 * Fortune Gods (贏財神) 引擎
 * <p>
 * 特色：6x5 Ways + Cascade（連消）+ 多格符號 + 橫財神模式
 * 展示：繼承 AbstractSlotEngine 的 Template Method，
 *       只覆寫有差異的部分（Free Spin 次數計算）
 * </p>
 */
public class FortuneGodsEngine extends AbstractSlotEngine {

    private final WaysEvaluator waysEvaluator;

    public FortuneGodsEngine(GameConfig config) {
        super(config);
        this.waysEvaluator = new WaysEvaluator(config);
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
