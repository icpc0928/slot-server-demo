package com.leo.slotserver.engine.gatesofolympus;

import com.leo.slotserver.engine.AbstractSlotEngine;
import com.leo.slotserver.engine.WaysEvaluator;
import com.leo.slotserver.model.*;

import java.util.List;

/**
 * Gates of Olympus 1000 引擎
 * <p>
 * 特色：6x5 Ways + Tumble（連消）+ 累進倍率
 * 連消倍率：每次連消倍率遞增，最高可達極大倍數
 * </p>
 */
public class GatesOfOlympusEngine extends AbstractSlotEngine {

    private final WaysEvaluator waysEvaluator;

    public GatesOfOlympusEngine(GameConfig config) {
        super(config);
        this.waysEvaluator = new WaysEvaluator(config);
    }

    @Override
    protected List<WinResult> evaluateWins(int[][] grid, SpinContext context) {
        return waysEvaluator.evaluate(grid, context);
    }

    /**
     * Gates of Olympus 的 Free Spin 次數根據 Scatter 數量決定：
     * 4 個 = 15 次, 5 個 = 20 次, 6 個 = 25 次
     */
    @Override
    protected int determineFreeSpinCount(SpinResult triggerSpin) {
        return switch (triggerSpin.getScatterCount()) {
            case 4 -> 15;
            case 5 -> 20;
            case 6 -> 25;
            default -> config.getFreeSpinCount();
        };
    }
}
