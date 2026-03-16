package com.leo.slotserver.engine;

import com.leo.slotserver.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScatterPay 計算器單元測試
 * 驗證全盤收集模式的贏分計算
 */
class ScatterPayEvaluatorTest {

    private ScatterPayEvaluator evaluator;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        config = new GameConfig();
        config.setReels(6);
        config.setRows(5);
        config.setWildSymbolId(11);
        config.setScatterSymbolId(12);
        config.setSymbols(List.of(
                createSymbol(0, "CHALICE", SymbolType.REGULAR),
                createSymbol(1, "RING", SymbolType.REGULAR),
                createSymbol(11, "WILD", SymbolType.WILD),
                createSymbol(12, "SCATTER", SymbolType.SCATTER)
        ));
        // index = 出現個數, 8 個以上才中獎
        config.setPaytable(Map.of(
                0, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 10, 25, 50, 125, 500},
                1, new int[]{0, 0, 0, 0, 0, 0, 0, 0, 8, 20, 40, 100, 400}
        ));
        evaluator = new ScatterPayEvaluator(config);
    }

    @Test
    @DisplayName("8 個相同符號應中獎")
    void eightSymbols_shouldWin() {
        // 6x5 盤面，放 8 個 symbol 0
        int[][] grid = {
                {0, 0, 1, 1, 1},   // 2 個 symbol 0
                {0, 0, 1, 1, 1},   // 2 個
                {0, 0, 1, 1, 1},   // 2 個
                {0, 0, 1, 1, 1},   // 2 個 → 共 8 個
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
        };

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        WinResult win = results.stream()
                .filter(r -> r.getSymbolId() == 0)
                .findFirst()
                .orElse(null);

        assertNotNull(win, "8 symbols should trigger a win");
        assertEquals(8, win.getMatchCount());
        assertEquals(10.0, win.getPayout());  // paytable[8] * lineBet
    }

    @Test
    @DisplayName("7 個相同符號不應中獎")
    void sevenSymbols_shouldNotWin() {
        // 只有 7 個 symbol 0
        int[][] grid = {
                {0, 0, 1, 1, 1},   // 2 個
                {0, 0, 1, 1, 1},   // 2 個
                {0, 0, 1, 1, 1},   // 2 個
                {0, 1, 1, 1, 1},   // 1 個 → 共 7 個
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
        };

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        boolean hasWin = results.stream()
                .anyMatch(r -> r.getSymbolId() == 0);

        assertFalse(hasWin, "7 symbols should not trigger a win (need 8+)");
    }

    @Test
    @DisplayName("Wild 應加入符號數量計算")
    void wild_shouldAddToCount() {
        // 6 個 symbol 0 + 2 個 Wild = 8 個
        int[][] grid = {
                {0, 0, 1, 1, 1},   // 2 個 symbol 0
                {0, 0, 1, 1, 1},   // 2 個
                {0, 0, 1, 1, 1},   // 2 個 → 共 6 個 symbol 0
                {11, 1, 1, 1, 1},  // 1 個 Wild
                {11, 1, 1, 1, 1},  // 1 個 Wild → 共 2 個 Wild
                {1, 1, 1, 1, 1},
        };

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        WinResult win = results.stream()
                .filter(r -> r.getSymbolId() == 0)
                .findFirst()
                .orElse(null);

        assertNotNull(win, "6 symbols + 2 Wilds should trigger a win");
        assertEquals(8, win.getMatchCount());
    }

    @Test
    @DisplayName("12+ 個符號應取賠率表最高值")
    void twelveOrMore_shouldCapAtMax() {
        // 全盤 30 格都是 symbol 0（最多算到 index 12）
        int[][] grid = new int[6][5];
        // 預設全部是 0

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        WinResult win = results.stream()
                .filter(r -> r.getSymbolId() == 0)
                .findFirst()
                .orElse(null);

        assertNotNull(win);
        assertEquals(12, win.getMatchCount());  // capped at paytable length - 1
        assertEquals(500.0, win.getPayout());   // paytable[12]
    }

    @Test
    @DisplayName("倍率應正確套用")
    void multiplier_shouldApply() {
        // 8 個 symbol 0
        int[][] grid = {
                {0, 0, 1, 1, 1},
                {0, 0, 1, 1, 1},
                {0, 0, 1, 1, 1},
                {0, 0, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
        };

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(5).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        WinResult win = results.stream()
                .filter(r -> r.getSymbolId() == 0)
                .findFirst()
                .orElse(null);

        assertNotNull(win);
        assertEquals(5, win.getMultiplier());
        assertEquals(10.0 * 5, win.getPayout());  // paytable[8] * lineBet * multiplier
    }

    @Test
    @DisplayName("Scatter 不應被當作普通符號計算")
    void scatter_shouldBeExcluded() {
        // 全盤都是 Scatter
        int[][] grid = new int[6][5];
        for (int[] reel : grid) {
            java.util.Arrays.fill(reel, 12);
        }

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        assertTrue(results.isEmpty(), "Scatter-only grid should have no regular wins");
    }

    @Test
    @DisplayName("多種符號同時中獎應各自計算")
    void multipleSymbols_shouldPaySeparately() {
        // 8 個 symbol 0 + 8 個 symbol 1
        int[][] grid = {
                {0, 0, 0, 0, 1},   // 4 個 s0, 1 個 s1
                {0, 0, 0, 0, 1},   // 4 個 s0, 1 個 s1
                {1, 1, 1, 1, 1},   // 5 個 s1
                {1, 1, 1, 1, 1},   // 5 個 s1 → 共 12 個 s1
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
        };

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        // symbol 0: 8 個 → 中
        // symbol 1: 12+ 個 → 中
        assertEquals(2, results.size(), "Both symbols should win");
    }

    private GameConfig.SymbolDefinition createSymbol(int id, String name, SymbolType type) {
        GameConfig.SymbolDefinition def = new GameConfig.SymbolDefinition();
        def.setId(id);
        def.setName(name);
        def.setType(type);
        return def;
    }
}
