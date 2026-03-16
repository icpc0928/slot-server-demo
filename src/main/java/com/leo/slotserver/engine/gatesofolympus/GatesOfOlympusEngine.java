package com.leo.slotserver.engine.gatesofolympus;

import com.leo.slotserver.engine.AbstractSlotEngine;
import com.leo.slotserver.engine.ScatterPayEvaluator;
import com.leo.slotserver.engine.SlotGame;
import com.leo.slotserver.model.*;

import java.util.List;

/**
 * Gates of Olympus 1000 引擎
 * <p>
 * 特色：6x5 Scatter Pay（全盤收集）+ Tumble（連消）+ 累進倍率
 * <p>
 * 與 Ways/Lines 不同，本遊戲不看符號是否在連續軸上，
 * 而是統計全盤面上相同符號的總數量，達到門檻即中獎。
 * 例如：盤面上出現 8 個紅寶石 → 查賠率表 → 計算贏分
 * <p>
 * 使用 ScatterPayEvaluator 而非 WaysEvaluator，
 * 體現不同計算策略可以自由組合到不同引擎中。
 * </p>
 */
@SlotGame("gates-of-olympus-1000")
public class GatesOfOlympusEngine extends AbstractSlotEngine {

    private final ScatterPayEvaluator scatterPayEvaluator;

    public GatesOfOlympusEngine(GameConfig config) {
        super(config);
        this.scatterPayEvaluator = new ScatterPayEvaluator(config);
    }

    @Override
    protected List<WinResult> evaluateWins(int[][] grid, SpinContext context) {
        return scatterPayEvaluator.evaluate(grid, context);
    }

    /**
     * Gates of Olympus 的 Free Spin 次數根據 Scatter 數量決定：
     * 4 個 = 15 次, 5 個 = 20 次, 6 個 = 25 次
     */
    @Override
    protected int determineFreeSpinCount(int scatterCount) {
        return switch (scatterCount) {
            case 4 -> 15;
            case 5 -> 20;
            case 6 -> 25;
            default -> config.getFreeSpinCount();
        };
    }
}
