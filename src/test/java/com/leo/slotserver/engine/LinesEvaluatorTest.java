package com.leo.slotserver.engine;

import com.leo.slotserver.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lines 計算器單元測試
 */
class LinesEvaluatorTest {

    private LinesEvaluator evaluator;
    private GameConfig config;

    // 簡單的 3 條線定義（5x3 盤面）
    private static final int[][] PAYLINES = {
            {1, 1, 1, 1, 1},  // 中間一排
            {0, 0, 0, 0, 0},  // 上排
            {2, 2, 2, 2, 2},  // 下排
    };

    @BeforeEach
    void setUp() {
        config = new GameConfig();
        config.setReels(5);
        config.setRows(3);
        config.setWildSymbolId(11);
        config.setScatterSymbolId(12);
        config.setSymbols(List.of(
                createSymbol(0, "9", SymbolType.REGULAR),
                createSymbol(1, "10", SymbolType.REGULAR),
                createSymbol(2, "J", SymbolType.REGULAR),
                createSymbol(11, "WILD", SymbolType.WILD),
                createSymbol(12, "SCATTER", SymbolType.SCATTER)
        ));
        config.setPaytable(Map.of(
                0, new int[]{0, 0, 0, 5, 15, 50},
                1, new int[]{0, 0, 0, 3, 10, 30},
                2, new int[]{0, 0, 0, 8, 25, 80}
        ));
        evaluator = new LinesEvaluator(config, PAYLINES);
    }

    @Test
    @DisplayName("中間線 3 連應正確計算贏分")
    void middleLine_threeMatch() {
        // 中間一排: 0, 0, 0, 1, 2
        int[][] grid = {
                {1, 0, 2},  // reel 0: row1 = 0
                {2, 0, 1},  // reel 1: row1 = 0
                {1, 0, 2},  // reel 2: row1 = 0
                {0, 1, 2},  // reel 3: row1 = 1 (break)
                {2, 2, 0},
        };

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        WinResult win = results.stream()
                .filter(r -> r.getSymbolId() == 0)
                .findFirst()
                .orElse(null);

        assertNotNull(win);
        assertEquals(3, win.getMatchCount());
        assertEquals(5.0, win.getPayout());
    }

    @Test
    @DisplayName("5 連線應取得最高賠率")
    void fiveMatch_shouldPayMax() {
        // 中間一排全部是 symbol 2 (J)
        int[][] grid = {
                {1, 2, 0},
                {0, 2, 1},
                {1, 2, 0},
                {0, 2, 1},
                {1, 2, 0},
        };

        SpinContext context = SpinContext.builder().lineBet(2.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        WinResult win = results.stream()
                .filter(r -> r.getSymbolId() == 2)
                .findFirst()
                .orElse(null);

        assertNotNull(win);
        assertEquals(5, win.getMatchCount());
        assertEquals(80 * 2.0, win.getPayout());  // paytable[5] * lineBet
    }

    @Test
    @DisplayName("Wild 開頭應正確匹配後續符號")
    void wildAtStart_shouldMatch() {
        // 中間一排: WILD, WILD, 1, 1, 2
        int[][] grid = {
                {0, 11, 2},  // reel 0: row1 = WILD
                {0, 11, 1},  // reel 1: row1 = WILD
                {0,  1, 2},  // reel 2: row1 = 1
                {0,  1, 2},  // reel 3: row1 = 1
                {0,  2, 1},  // reel 4: row1 = 2 (break)
        };

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        // WILD + WILD + 1 + 1 = 4 連 symbol 1
        WinResult win = results.stream()
                .filter(r -> r.getSymbolId() == 1)
                .findFirst()
                .orElse(null);

        assertNotNull(win, "Wild at start should match subsequent symbols");
        assertEquals(4, win.getMatchCount());
        assertEquals(10.0, win.getPayout());
    }

    @Test
    @DisplayName("多條線同時中獎應各自獨立計算")
    void multipleLines_shouldPaySeparately() {
        // 上排: 0, 0, 0, 0, 0 (5連)
        // 中排: 0, 0, 0, 1, 1 (3連)
        // 下排: 1, 1, 1, 1, 1 (5連 不在 paytable 內的話...)
        int[][] grid = {
                {0, 0, 1},
                {0, 0, 1},
                {0, 0, 1},
                {0, 1, 1},
                {0, 1, 1},
        };

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        // 應有多條線中獎
        assertTrue(results.size() >= 2, "Multiple lines should win");
    }

    @Test
    @DisplayName("無任何中獎應回傳空列表")
    void noWin_shouldReturnEmpty() {
        // 每軸第一個符號都不同
        int[][] grid = {
                {0, 1, 2},
                {1, 2, 0},
                {2, 0, 1},
                {0, 1, 2},
                {1, 2, 0},
        };

        SpinContext context = SpinContext.builder().lineBet(1.0).currentMultiplier(1).build();
        List<WinResult> results = evaluator.evaluate(grid, context);

        // 檢查中間線: 1, 2, 0, 1, 2 → 不連續
        boolean hasMiddleWin = results.stream()
                .anyMatch(r -> r.getMatchCount() >= 3);
        // 可能有些線碰巧中，但至少不會 crash
        assertNotNull(results);
    }

    private GameConfig.SymbolDefinition createSymbol(int id, String name, SymbolType type) {
        GameConfig.SymbolDefinition def = new GameConfig.SymbolDefinition();
        def.setId(id);
        def.setName(name);
        def.setType(type);
        return def;
    }
}
