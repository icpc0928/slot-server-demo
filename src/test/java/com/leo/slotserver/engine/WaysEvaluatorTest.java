package com.leo.slotserver.engine;

import com.leo.slotserver.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ways 計算器單元測試
 * 驗證贏分計算的數學正確性
 */
class WaysEvaluatorTest {

    private WaysEvaluator evaluator;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        config = new GameConfig();
        config.setReels(5);
        config.setRows(3);
        config.setWildSymbolId(11);
        config.setScatterSymbolId(12);
        config.setSymbols(List.of(
                createSymbol(0, "A", SymbolType.REGULAR),
                createSymbol(1, "K", SymbolType.REGULAR),
                createSymbol(11, "WILD", SymbolType.WILD),
                createSymbol(12, "SCATTER", SymbolType.SCATTER)
        ));
        config.setPaytable(Map.of(
                0, new int[]{0, 0, 0, 5, 15, 50},
                1, new int[]{0, 0, 0, 3, 10, 30}
        ));
        evaluator = new WaysEvaluator(config);
    }

    @Test
    @DisplayName("3 軸連續相同符號應計算正確贏分")
    void threeOfAKind_shouldPayCorrectly() {
        int[][] grid = {
                {0, 1, 0},
                {0, 0, 1},
                {0, 1, 1},
                {1, 1, 1},
                {1, 0, 1},
        };

        SpinContext context = SpinContext.builder()
                .lineBet(1.0)
                .currentMultiplier(1)
                .build();

        List<WinResult> results = evaluator.evaluate(grid, context);

        WinResult aWin = results.stream()
                .filter(r -> r.getSymbolId() == 0)
                .findFirst()
                .orElse(null);

        assertNotNull(aWin, "Symbol A should have a win");
        assertEquals(3, aWin.getMatchCount());
        assertEquals(4, aWin.getWays());
        assertEquals(5 * 1.0 * 4, aWin.getPayout());
    }

    @Test
    @DisplayName("Wild 應替代任何普通符號")
    void wildShouldSubstitute() {
        int[][] grid = {
                {0, 1, 0},
                {11, 0, 1},
                {0, 1, 0},
                {1, 1, 1},
                {1, 0, 1},
        };

        SpinContext context = SpinContext.builder()
                .lineBet(1.0)
                .currentMultiplier(1)
                .build();

        List<WinResult> results = evaluator.evaluate(grid, context);

        WinResult aWin = results.stream()
                .filter(r -> r.getSymbolId() == 0)
                .findFirst()
                .orElse(null);

        assertNotNull(aWin, "Symbol A should win with Wild substitution");
        assertEquals(3, aWin.getMatchCount());
    }

    @Test
    @DisplayName("少於 3 軸不應中獎")
    void lessThanThreeReels_shouldNotWin() {
        int[][] grid = {
                {0, 0, 0},
                {0, 0, 0},
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1},
        };

        SpinContext context = SpinContext.builder()
                .lineBet(1.0)
                .currentMultiplier(1)
                .build();

        List<WinResult> results = evaluator.evaluate(grid, context);

        boolean hasAWin = results.stream().anyMatch(r -> r.getSymbolId() == 0);
        assertFalse(hasAWin, "2-reel match should not pay");
    }

    @Test
    @DisplayName("倍率應正確套用")
    void multiplier_shouldApply() {
        int[][] grid = {
                {0, 0, 0},
                {0, 0, 0},
                {0, 0, 0},
                {1, 1, 1},
                {1, 1, 1},
        };

        SpinContext context = SpinContext.builder()
                .lineBet(1.0)
                .currentMultiplier(3)
                .build();

        List<WinResult> results = evaluator.evaluate(grid, context);

        WinResult aWin = results.stream()
                .filter(r -> r.getSymbolId() == 0)
                .findFirst()
                .orElse(null);

        assertNotNull(aWin);
        assertEquals(3, aWin.getMultiplier());
        assertEquals(5 * 1.0 * 27 * 3, aWin.getPayout());
    }

    private GameConfig.SymbolDefinition createSymbol(int id, String name, SymbolType type) {
        GameConfig.SymbolDefinition def = new GameConfig.SymbolDefinition();
        def.setId(id);
        def.setName(name);
        def.setType(type);
        return def;
    }
}
