package com.leo.slotserver.engine;

import com.leo.slotserver.model.SpinContext;
import com.leo.slotserver.model.WinResult;

import java.util.List;

/**
 * 贏分計算器介面 — Strategy Pattern
 * <p>
 * 定義統一的算獎方法，不同計算方式各自實作：
 * - {@link WaysEvaluator} — Ways 路制
 * - {@link LinesEvaluator} — Lines 線制
 * - {@link ScatterPayEvaluator} — Scatter Pay 全盤收集
 * <p>
 * Engine 透過組合（Composition）持有一個 WinEvaluator，
 * 而非繼承不同的算獎邏輯 — 符合「組合優於繼承」原則。
 * </p>
 */
public interface WinEvaluator {

    /**
     * 計算盤面贏分
     *
     * @param grid    盤面 [reel][row]
     * @param context 旋轉上下文（含 lineBet、倍率等）
     * @return 所有中獎結果
     */
    List<WinResult> evaluate(int[][] grid, SpinContext context);
}
