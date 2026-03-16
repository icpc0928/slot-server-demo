package com.leo.slotserver.engine;

import com.leo.slotserver.model.GameConfig;
import com.leo.slotserver.model.SpinContext;
import com.leo.slotserver.model.WinResult;

import java.util.*;

/**
 * Scatter Pay 計算器 — 全盤收集模式
 * <p>
 * 與 Ways/Lines 不同，不需要符號在連續軸上，
 * 只要盤面上同一符號出現 N 個以上即算中獎。
 * 適用於 Gates of Olympus、Sweet Bonanza 等遊戲。
 * </p>
 */
public class ScatterPayEvaluator implements WinEvaluator {

    private final GameConfig config;

    public ScatterPayEvaluator(GameConfig config) {
        this.config = config;
    }

    /**
     * 計算全盤收集中獎
     * <p>
     * 邏輯：統計盤面上每個符號的出現次數和位置，
     * 達到最低中獎數量（通常 8 個）即計算贏分。
     * </p>
     */
    public List<WinResult> evaluate(int[][] grid, SpinContext context) {
        // 1. 統計每個符號的數量和位置
        Map<Integer, List<int[]>> symbolPositions = new HashMap<>();

        for (int reel = 0; reel < grid.length; reel++) {
            for (int row = 0; row < grid[reel].length; row++) {
                int symbol = grid[reel][row];
                if (!isWild(symbol) && !isScatter(symbol)) {
                    symbolPositions.computeIfAbsent(symbol, k -> new ArrayList<>())
                            .add(new int[]{reel, row});
                }
            }
        }

        // Wild 位置（可替代任何符號，計入每個符號的數量）
        List<int[]> wildPositions = new ArrayList<>();
        for (int reel = 0; reel < grid.length; reel++) {
            for (int row = 0; row < grid[reel].length; row++) {
                if (isWild(grid[reel][row])) {
                    wildPositions.add(new int[]{reel, row});
                }
            }
        }

        // 2. 逐符號計算中獎
        List<WinResult> results = new ArrayList<>();
        for (Map.Entry<Integer, List<int[]>> entry : symbolPositions.entrySet()) {
            int symbolId = entry.getKey();
            List<int[]> positions = entry.getValue();
            int count = positions.size();

            // 查賠率表（index = 出現數量）
            int[] paytable = config.getPaytable().get(symbolId);
            if (paytable == null) continue;

            // 找最高中獎（從最高數量往下找）
            int payValue = 0;
            int matchCount = count;

            // 加上 Wild 數量
            int totalCount = count + wildPositions.size();

            // 限制不超過賠率表長度
            matchCount = Math.min(totalCount, paytable.length - 1);

            if (matchCount >= 0 && matchCount < paytable.length) {
                payValue = paytable[matchCount];
            }

            if (payValue <= 0) continue;

            // 合併位置（符號 + Wild）
            List<int[]> allPositions = new ArrayList<>(positions);
            allPositions.addAll(wildPositions);

            double payout = payValue * context.getLineBet();
            int multiplier = context.getCurrentMultiplier();
            payout *= multiplier;

            String symbolName = getSymbolName(symbolId);

            results.add(WinResult.builder()
                    .symbolId(symbolId)
                    .symbolName(symbolName)
                    .matchCount(matchCount)
                    .ways(1)
                    .payout(payout)
                    .multiplier(multiplier)
                    .positions(allPositions)
                    .build());
        }

        return results;
    }

    private boolean isWild(int symbolId) {
        return symbolId == config.getWildSymbolId();
    }

    private boolean isScatter(int symbolId) {
        return symbolId == config.getScatterSymbolId();
    }

    private String getSymbolName(int symbolId) {
        return config.getSymbols().stream()
                .filter(s -> s.getId() == symbolId)
                .map(GameConfig.SymbolDefinition::getName)
                .findFirst()
                .orElse("SYMBOL_" + symbolId);
    }
}
