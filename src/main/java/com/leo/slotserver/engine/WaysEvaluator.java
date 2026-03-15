package com.leo.slotserver.engine;

import com.leo.slotserver.model.GameConfig;
import com.leo.slotserver.model.SpinContext;
import com.leo.slotserver.model.WinResult;

import java.util.*;

/**
 * Ways 計算器 — 共用的 Ways 贏分計算邏輯
 * 單一責任原則：只負責 Ways 模式的贏分計算。
 * 可以被任何使用 Ways 計算的遊戲引擎共用。
 */
public class WaysEvaluator {

    private final GameConfig config;

    public WaysEvaluator(GameConfig config) {
        this.config = config;
    }

    /**
     * 計算 Ways 中獎
     * Ways 計算：從左到右，每軸至少出現 1 個該符號（或 Wild），
     * 路數 = 各軸出現次數的乘積
     */
    public List<WinResult> evaluate(int[][] grid, SpinContext context) {
        List<WinResult> results = new ArrayList<>();
        Set<Integer> evaluatedSymbols = getEvaluableSymbols(grid);

        for (int symbolId : evaluatedSymbols) {
            WinResult result = evaluateSymbol(grid, symbolId, context);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    private WinResult evaluateSymbol(int[][] grid, int symbolId, SpinContext context) {
        int consecutiveReels = 0;
        int totalWays = 1;
        List<int[]> allPositions = new ArrayList<>();

        for (int reel = 0; reel < config.getReels(); reel++) {
            List<int[]> matchPositions = new ArrayList<>();
            for (int row = 0; row < grid[reel].length; row++) {
                if (grid[reel][row] == symbolId || isWild(grid[reel][row])) {
                    matchPositions.add(new int[]{reel, row});
                }
            }

            if (matchPositions.isEmpty()) break;

            consecutiveReels++;
            totalWays *= matchPositions.size();
            allPositions.addAll(matchPositions);
        }

        // 最少需要 3 軸連續
        if (consecutiveReels < 3) return null;

        int[] paytable = config.getPaytable().get(symbolId);
        if (paytable == null || consecutiveReels >= paytable.length) return null;

        int payValue = paytable[consecutiveReels];
        if (payValue <= 0) return null;

        double payout = payValue * context.getLineBet() * totalWays;
        int multiplier = context.getCurrentMultiplier();
        payout *= multiplier;

        String symbolName = getSymbolName(symbolId);

        return WinResult.builder()
                .symbolId(symbolId)
                .symbolName(symbolName)
                .matchCount(consecutiveReels)
                .ways(totalWays)
                .payout(payout)
                .multiplier(multiplier)
                .positions(allPositions)
                .build();
    }

    /**
     * 取得盤面上所有需要評估的符號（排除 Wild 和 Scatter）
     */
    private Set<Integer> getEvaluableSymbols(int[][] grid) {
        Set<Integer> symbols = new HashSet<>();
        for (int[] reel : grid) {
            for (int symbol : reel) {
                if (!isWild(symbol) && !isScatter(symbol)) {
                    symbols.add(symbol);
                }
            }
        }
        return symbols;
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
