package com.leo.slotserver.engine;

import com.leo.slotserver.model.*;

import java.util.*;

/**
 * 抽象老虎機引擎 — Template Method Pattern
 * <p>
 * 定義老虎機的通用遊戲流程：
 *   1. 生成盤面 → 2. 計算贏分 → 3. 處理連消 → 4. 檢查 Feature → 5. 處理 Free Spin
 * <p>
 * 子類只需覆寫有差異的部分（如盤面生成、贏分計算方式）。
 * 不變的流程固定在此，變化的部分交給子類 — Template Method Pattern。
 * </p>
 */
public abstract class AbstractSlotEngine implements SlotEngine {

    protected final GameConfig config;
    protected final Random random = new Random();

    protected AbstractSlotEngine(GameConfig config) {
        this.config = config;
    }

    @Override
    public String getGameId() {
        return config.getGameId();
    }

    @Override
    public GameConfig getConfig() {
        return config;
    }

    // ==========================================
    // Template Method: 定義遊戲流程骨架
    // ==========================================

    @Override
    public RoundResult play(double betAmount) {
        double lineBet = betAmount / config.getBetMultiplier();

        SpinContext context = SpinContext.builder()
                .gameId(config.getGameId())
                .betAmount(betAmount)
                .lineBet(lineBet)
                .isFreeSpinMode(false)
                .cascadeLevel(0)
                .currentMultiplier(1)
                .build();

        // Step 1: Base Spin
        SpinResult baseSpin = executeSpin(context);
        double baseWin = baseSpin.getTotalWin();

        // Step 2: Cascade (if applicable)
        List<SpinResult> cascadeSpins = new ArrayList<>();
        if (config.isHasCascade() && baseSpin.hasWin()) {
            cascadeSpins = processCascade(baseSpin, context);
            baseWin += cascadeSpins.stream().mapToDouble(SpinResult::getTotalWin).sum();
        }

        // Step 3: Free Spin (if triggered)
        List<SpinResult> freeSpins = new ArrayList<>();
        double freeSpinWin = 0;
        int totalFreeSpinCount = 0;
        boolean freeSpinTriggered = checkFreeSpinTrigger(baseSpin);

        if (freeSpinTriggered) {
            totalFreeSpinCount = determineFreeSpinCount(baseSpin);
            SpinContext freeContext = SpinContext.builder()
                    .gameId(config.getGameId())
                    .betAmount(betAmount)
                    .lineBet(lineBet)
                    .isFreeSpinMode(true)
                    .cascadeLevel(0)
                    .freeSpinsRemaining(totalFreeSpinCount)
                    .currentMultiplier(1)
                    .build();

            freeSpins = processFreeSpins(freeContext);
            freeSpinWin = freeSpins.stream().mapToDouble(SpinResult::getTotalWin).sum();
        }

        // Step 4: Calculate total
        double totalWin = baseWin + freeSpinWin;
        double maxWin = betAmount * config.getMaxWinCap();
        totalWin = Math.min(totalWin, maxWin);  // Win cap

        return RoundResult.builder()
                .gameId(config.getGameId())
                .betAmount(betAmount)
                .baseSpin(baseSpin)
                .cascadeSpins(cascadeSpins)
                .freeSpins(freeSpins)
                .baseWin(baseWin)
                .freeSpinWin(freeSpinWin)
                .totalWin(totalWin)
                .freeSpinTriggered(freeSpinTriggered)
                .totalFreeSpins(totalFreeSpinCount)
                .winMultiplier(betAmount > 0 ? totalWin / betAmount : 0)
                .build();
    }

    // ==========================================
    // 可覆寫的 Hook Methods
    // ==========================================

    /**
     * 執行單次旋轉 — 子類可覆寫以自定義盤面生成邏輯
     */
    protected SpinResult executeSpin(SpinContext context) {
        int[][] grid = generateGrid(context);
        List<WinResult> wins = evaluateWins(grid, context);
        double totalWin = wins.stream().mapToDouble(WinResult::getPayout).sum();
        int scatterCount = countScatters(grid);

        return SpinResult.builder()
                .grid(grid)
                .wins(wins)
                .totalWin(totalWin)
                .scatterCount(scatterCount)
                .featureTriggered(scatterCount >= config.getScatterToTrigger())
                .cascadeLevel(context.getCascadeLevel())
                .build();
    }

    /**
     * 生成盤面 — 根據輪帶權重隨機產生
     */
    protected int[][] generateGrid(SpinContext context) {
        Map<Integer, int[]> strips = context.isFreeSpinMode()
                ? config.getFreeReelStrips()
                : config.getBaseReelStrips();

        int[][] grid = new int[config.getReels()][config.getRows()];
        for (int reel = 0; reel < config.getReels(); reel++) {
            int[] strip = strips.get(reel);
            int startPos = random.nextInt(strip.length);
            for (int row = 0; row < config.getRows(); row++) {
                grid[reel][row] = strip[(startPos + row) % strip.length];
            }
        }
        return grid;
    }

    /**
     * 計算贏分 — 子類必須實作（Lines vs Ways vs Cluster 不同）
     */
    protected abstract List<WinResult> evaluateWins(int[][] grid, SpinContext context);

    /**
     * 計算 Scatter 數量
     */
    protected int countScatters(int[][] grid) {
        int count = 0;
        int scatterId = config.getScatterSymbolId();
        for (int[] reel : grid) {
            for (int symbol : reel) {
                if (symbol == scatterId) count++;
            }
        }
        return count;
    }

    /**
     * 檢查是否觸發免費遊戲
     */
    protected boolean checkFreeSpinTrigger(SpinResult spinResult) {
        return spinResult.getScatterCount() >= config.getScatterToTrigger();
    }

    /**
     * 決定免費遊戲次數 — 子類可覆寫（如根據 Scatter 數量給不同次數）
     */
    protected int determineFreeSpinCount(SpinResult triggerSpin) {
        return config.getFreeSpinCount();
    }

    /**
     * 處理連消 — 子類可覆寫以自定義連消邏輯
     */
    protected List<SpinResult> processCascade(SpinResult initialSpin, SpinContext context) {
        List<SpinResult> cascadeResults = new ArrayList<>();
        SpinResult currentSpin = initialSpin;
        int cascadeLevel = 1;

        while (currentSpin.hasWin()) {
            int[][] newGrid = cascadeGrid(currentSpin);
            SpinContext cascadeContext = SpinContext.builder()
                    .gameId(context.getGameId())
                    .betAmount(context.getBetAmount())
                    .lineBet(context.getLineBet())
                    .isFreeSpinMode(context.isFreeSpinMode())
                    .cascadeLevel(cascadeLevel)
                    .currentMultiplier(getCascadeMultiplier(cascadeLevel))
                    .build();

            List<WinResult> wins = evaluateWins(newGrid, cascadeContext);
            double multiplier = getCascadeMultiplier(cascadeLevel);
            double totalWin = wins.stream().mapToDouble(WinResult::getPayout).sum() * multiplier;

            currentSpin = SpinResult.builder()
                    .grid(newGrid)
                    .wins(wins)
                    .totalWin(totalWin)
                    .scatterCount(countScatters(newGrid))
                    .cascadeLevel(cascadeLevel)
                    .build();

            if (currentSpin.hasWin()) {
                cascadeResults.add(currentSpin);
            }
            cascadeLevel++;
        }
        return cascadeResults;
    }

    /**
     * 連消倍率 — 根據連消次數查表
     */
    protected int getCascadeMultiplier(int cascadeLevel) {
        int[] multipliers = config.getCascadeMultipliers();
        if (multipliers == null || multipliers.length == 0) return 1;
        int index = Math.min(cascadeLevel, multipliers.length - 1);
        return multipliers[index];
    }

    /**
     * 連消後生成新盤面 — 移除中獎符號，掉落新符號
     */
    protected int[][] cascadeGrid(SpinResult spinResult) {
        int[][] grid = deepCopyGrid(spinResult.getGrid());
        Set<String> winPositions = new HashSet<>();

        // 收集所有中獎位置
        for (WinResult win : spinResult.getWins()) {
            if (win.getPositions() != null) {
                for (int[] pos : win.getPositions()) {
                    winPositions.add(pos[0] + "," + pos[1]);
                }
            }
        }

        // 移除中獎符號並掉落
        for (int reel = 0; reel < config.getReels(); reel++) {
            List<Integer> remaining = new ArrayList<>();
            for (int row = 0; row < config.getRows(); row++) {
                if (!winPositions.contains(reel + "," + row)) {
                    remaining.add(grid[reel][row]);
                }
            }
            // 從輪帶補充新符號
            int[] strip = config.getBaseReelStrips().get(reel);
            while (remaining.size() < config.getRows()) {
                remaining.add(0, strip[random.nextInt(strip.length)]);
            }
            for (int row = 0; row < config.getRows(); row++) {
                grid[reel][row] = remaining.get(row);
            }
        }
        return grid;
    }

    /**
     * 處理免費遊戲
     */
    protected List<SpinResult> processFreeSpins(SpinContext context) {
        List<SpinResult> results = new ArrayList<>();
        int remaining = context.getFreeSpinsRemaining();

        while (remaining > 0) {
            SpinContext freeContext = SpinContext.builder()
                    .gameId(context.getGameId())
                    .betAmount(context.getBetAmount())
                    .lineBet(context.getLineBet())
                    .isFreeSpinMode(true)
                    .cascadeLevel(0)
                    .currentMultiplier(1)
                    .build();

            SpinResult spin = executeSpin(freeContext);
            results.add(spin);

            // Cascade in free spin
            if (config.isHasCascade() && spin.hasWin()) {
                List<SpinResult> cascades = processCascade(spin, freeContext);
                results.addAll(cascades);
            }

            // Retrigger check
            if (checkFreeSpinTrigger(spin)) {
                remaining += determineFreeSpinCount(spin);
            }
            remaining--;
        }
        return results;
    }

    // ==========================================
    // Utility Methods
    // ==========================================

    protected int[][] deepCopyGrid(int[][] grid) {
        int[][] copy = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            copy[i] = Arrays.copyOf(grid[i], grid[i].length);
        }
        return copy;
    }

    /**
     * 檢查符號是否為 Wild（可替代其他符號）
     */
    protected boolean isWild(int symbolId) {
        return symbolId == config.getWildSymbolId();
    }

    /**
     * 檢查符號是否為 Scatter
     */
    protected boolean isScatter(int symbolId) {
        return symbolId == config.getScatterSymbolId();
    }
}
