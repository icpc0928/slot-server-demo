package com.leo.slotserver.engine;

import com.leo.slotserver.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AbstractSlotEngine 整合測試
 * 測試 Template Method 流程的正確性（使用簡單的 Ways 引擎）
 */
class AbstractSlotEngineTest {

    private GameConfig config;
    private TestWaysEngine engine;

    /**
     * 用於測試的簡單 Ways 引擎
     */
    static class TestWaysEngine extends AbstractSlotEngine {
        private final WaysEvaluator waysEvaluator;

        TestWaysEngine(GameConfig config) {
            super(config);
            this.waysEvaluator = new WaysEvaluator(config);
        }

        @Override
        protected List<WinResult> evaluateWins(int[][] grid, SpinContext context) {
            return waysEvaluator.evaluate(grid, context);
        }
    }

    @BeforeEach
    void setUp() {
        config = new GameConfig();
        config.setGameId("test-game");
        config.setGameName("Test Game");
        config.setReels(5);
        config.setRows(3);
        config.setBetMultiplier(20);
        config.setMaxWinCap(10000);
        config.setWildSymbolId(11);
        config.setScatterSymbolId(12);
        config.setScatterToTrigger(3);
        config.setFreeSpinCount(10);
        config.setHasCascade(false);

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

        // 簡單輪帶
        Map<Integer, int[]> strips = Map.of(
                0, new int[]{0, 1, 0, 1, 0, 1, 12},
                1, new int[]{0, 1, 0, 1, 0, 1, 11},
                2, new int[]{0, 1, 0, 1, 0, 1, 12},
                3, new int[]{0, 1, 0, 1, 0, 1, 11},
                4, new int[]{0, 1, 0, 1, 0, 1, 12}
        );
        config.setBaseReelStrips(strips);
        config.setFreeReelStrips(strips);

        engine = new TestWaysEngine(config);
    }

    @Test
    @DisplayName("play() 應回傳完整的 RoundResult")
    void play_shouldReturnValidResult() {
        RoundResult result = engine.play(20.0);

        assertNotNull(result);
        assertEquals("test-game", result.getGameId());
        assertEquals(20.0, result.getBetAmount());
        assertNotNull(result.getBaseResult());
        assertNotNull(result.getBaseResult().getRounds());
        assertFalse(result.getBaseResult().getRounds().isEmpty(), "Should have at least 1 round");
        assertTrue(result.getTotalWin() >= 0, "Total win should be non-negative");
    }

    @Test
    @DisplayName("BaseResult 的第一個 round 應有盤面")
    void baseResult_shouldHaveGrid() {
        RoundResult result = engine.play(20.0);
        BaseResult base = result.getBaseResult();

        int[][] grid = base.getInitialGrid();
        assertNotNull(grid);
        assertEquals(5, grid.length, "Should have 5 reels");
        assertEquals(3, grid[0].length, "Should have 3 rows");
    }

    @Test
    @DisplayName("贏分不應超過 maxWinCap")
    void totalWin_shouldNotExceedCap() {
        // 跑 1000 次確認不超過 cap
        for (int i = 0; i < 1000; i++) {
            RoundResult result = engine.play(20.0);
            double maxWin = 20.0 * config.getMaxWinCap();
            assertTrue(result.getTotalWin() <= maxWin,
                    "Total win " + result.getTotalWin() + " exceeds cap " + maxWin);
        }
    }

    @Test
    @DisplayName("winMultiplier 應等於 totalWin / betAmount")
    void winMultiplier_shouldBeCorrect() {
        RoundResult result = engine.play(20.0);
        double expected = result.getTotalWin() / 20.0;
        assertEquals(expected, result.getWinMultiplier(), 0.001);
    }

    @Test
    @DisplayName("無連消時 BaseResult 應只有一個 round")
    void noCascade_shouldHaveOneRound() {
        config.setHasCascade(false);
        engine = new TestWaysEngine(config);

        RoundResult result = engine.play(20.0);
        assertEquals(0, result.getBaseResult().getCascadeCount(),
                "Without cascade, should have 0 cascade rounds");
    }

    @Test
    @DisplayName("CascadeRound 的 roundIndex 應正確遞增")
    void cascadeRound_indexShouldIncrement() {
        config.setHasCascade(true);
        config.setCascadeMultipliers(new int[]{1, 2, 3, 5});
        engine = new TestWaysEngine(config);

        // 跑多次找一個有連消的
        for (int i = 0; i < 100; i++) {
            RoundResult result = engine.play(20.0);
            List<CascadeRound> rounds = result.getBaseResult().getRounds();

            for (int j = 0; j < rounds.size(); j++) {
                assertEquals(j, rounds.get(j).getRoundIndex(),
                        "Round index should match position");
            }

            // 最後一個 round 的 hasNextCascade 應為 false
            assertFalse(rounds.get(rounds.size() - 1).isHasNextCascade());
        }
    }

    @Test
    @DisplayName("多次 play 結果應有隨機性")
    void play_shouldBeRandom() {
        double firstWin = engine.play(20.0).getTotalWin();
        boolean hasDifferent = false;

        for (int i = 0; i < 50; i++) {
            if (engine.play(20.0).getTotalWin() != firstWin) {
                hasDifferent = true;
                break;
            }
        }

        assertTrue(hasDifferent, "50 spins should produce different results");
    }

    @Test
    @DisplayName("getGameId 和 getConfig 應回傳正確值")
    void getters_shouldReturnCorrectValues() {
        assertEquals("test-game", engine.getGameId());
        assertEquals(config, engine.getConfig());
    }

    private GameConfig.SymbolDefinition createSymbol(int id, String name, SymbolType type) {
        GameConfig.SymbolDefinition def = new GameConfig.SymbolDefinition();
        def.setId(id);
        def.setName(name);
        def.setType(type);
        return def;
    }
}
