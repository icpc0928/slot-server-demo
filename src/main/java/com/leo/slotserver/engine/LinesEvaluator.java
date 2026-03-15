package com.leo.slotserver.engine;

import com.leo.slotserver.model.GameConfig;
import com.leo.slotserver.model.SpinContext;
import com.leo.slotserver.model.WinResult;

import java.util.*;

/**
 * Lines 計算器 — 共用的線制贏分計算邏輯
 * <p>
 * 依介面隔離原則 (ISP)，Lines 和 Ways 計算器分開，
 * 遊戲引擎只依賴自己需要的計算器。
 * </p>
 */
public class LinesEvaluator {

    private final GameConfig config;
    private final int[][] paylines;

    /**
     * @param config   遊戲設定
     * @param paylines 線的定義：paylines[lineIndex][reelIndex] = row position
     */
    public LinesEvaluator(GameConfig config, int[][] paylines) {
        this.config = config;
        this.paylines = paylines;
    }

    /**
     * 計算所有線的中獎
     */
    public List<WinResult> evaluate(int[][] grid, SpinContext context) {
        List<WinResult> results = new ArrayList<>();

        for (int lineIndex = 0; lineIndex < paylines.length; lineIndex++) {
            WinResult result = evaluateLine(grid, paylines[lineIndex], context);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    private WinResult evaluateLine(int[][] grid, int[] line, SpinContext context) {
        // 取得線上第一個非 Wild 符號作為基準
        int baseSymbol = -1;
        int matchCount = 0;
        List<int[]> positions = new ArrayList<>();

        for (int reel = 0; reel < config.getReels() && reel < line.length; reel++) {
            int row = line[reel];
            int symbol = grid[reel][row];

            if (baseSymbol == -1) {
                if (isWild(symbol)) {
                    matchCount++;
                    positions.add(new int[]{reel, row});
                    continue;
                }
                baseSymbol = symbol;
            }

            if (symbol == baseSymbol || isWild(symbol)) {
                matchCount++;
                positions.add(new int[]{reel, row});
            } else {
                break;
            }
        }

        if (baseSymbol == -1 || matchCount < 3) return null;

        int[] paytable = config.getPaytable().get(baseSymbol);
        if (paytable == null || matchCount >= paytable.length) return null;

        int payValue = paytable[matchCount];
        if (payValue <= 0) return null;

        double payout = payValue * context.getLineBet();
        String symbolName = getSymbolName(baseSymbol);

        return WinResult.builder()
                .symbolId(baseSymbol)
                .symbolName(symbolName)
                .matchCount(matchCount)
                .ways(1)
                .payout(payout)
                .multiplier(1)
                .positions(positions)
                .build();
    }

    private boolean isWild(int symbolId) {
        return symbolId == config.getWildSymbolId();
    }

    private String getSymbolName(int symbolId) {
        return config.getSymbols().stream()
                .filter(s -> s.getId() == symbolId)
                .map(GameConfig.SymbolDefinition::getName)
                .findFirst()
                .orElse("SYMBOL_" + symbolId);
    }
}
