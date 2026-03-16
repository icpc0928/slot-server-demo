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

        // Step 1: 主遊戲（初始停輪 + 連消）
        BaseResult baseResult = executeBaseGame(context);

        // Step 2: 免費遊戲（如果觸發）
        List<FreeSpinResult> freeSpinResults = new ArrayList<>();
        double freeSpinWin = 0;
        int totalFreeSpinCount = 0;

        if (baseResult.isFreeSpinTriggered()) {
            totalFreeSpinCount = baseResult.getFreeSpinCount();
            SpinContext freeContext = SpinContext.builder()
                    .gameId(config.getGameId())
                    .betAmount(betAmount)
                    .lineBet(lineBet)
                    .isFreeSpinMode(true)
                    .cascadeLevel(0)
                    .currentMultiplier(1)
                    .build();

            freeSpinResults = processFreeSpins(freeContext, totalFreeSpinCount);
            freeSpinWin = freeSpinResults.stream().mapToDouble(FreeSpinResult::getTotalWin).sum();
            totalFreeSpinCount = freeSpinResults.size();
        }

        // Step 3: 加總
        double totalWin = baseResult.getTotalWin() + freeSpinWin;
        double maxWin = betAmount * config.getMaxWinCap();
        totalWin = Math.min(totalWin, maxWin);  // Win cap

        return RoundResult.builder()
                .gameId(config.getGameId())
                .betAmount(betAmount)
                .baseResult(baseResult)
                .freeSpinResults(freeSpinResults)
                .baseWin(baseResult.getTotalWin())
                .freeSpinWin(freeSpinWin)
                .totalWin(totalWin)
                .freeSpinTriggered(baseResult.isFreeSpinTriggered())
                .totalFreeSpins(totalFreeSpinCount)
                .winMultiplier(betAmount > 0 ? totalWin / betAmount : 0)
                .build();
    }

    // ==========================================
    // 主遊戲流程
    // ==========================================

    /**
     * 執行主遊戲：初始停輪 + 連消循環
     */
    protected BaseResult executeBaseGame(SpinContext context) {
        List<CascadeRound> rounds = new ArrayList<>();
        int cascadeLevel = 0;
        double totalWin = 0;
        int totalScatterCount = 0;

        // 初始停輪
        int[][] grid = generateGrid(context);
        CascadeRound firstRound = buildCascadeRound(grid, context, cascadeLevel);
        rounds.add(firstRound);
        totalWin += firstRound.getTotalWin();
        totalScatterCount += firstRound.getScatterCount();

        // 連消循環
        if (config.isHasCascade()) {
            int[][] currentGrid = grid;
            CascadeRound currentRound = firstRound;

            while (currentRound.hasWin()) {
                cascadeLevel++;
                currentGrid = cascadeGrid(currentRound);
                SpinContext cascadeContext = SpinContext.builder()
                        .gameId(context.getGameId())
                        .betAmount(context.getBetAmount())
                        .lineBet(context.getLineBet())
                        .isFreeSpinMode(context.isFreeSpinMode())
                        .cascadeLevel(cascadeLevel)
                        .currentMultiplier(getCascadeMultiplier(cascadeLevel))
                        .build();

                currentRound = buildCascadeRound(currentGrid, cascadeContext, cascadeLevel);
                if (currentRound.hasWin()) {
                    rounds.add(currentRound);
                    totalWin += currentRound.getTotalWin();
                    totalScatterCount += currentRound.getScatterCount();
                }
            }
        }

        // 標記最後一消的 hasNextCascade = false
        if (!rounds.isEmpty()) {
            rounds.get(rounds.size() - 1).setHasNextCascade(false);
        }

        boolean triggered = totalScatterCount >= config.getScatterToTrigger();
        int freeCount = triggered ? determineFreeSpinCount(totalScatterCount) : 0;

        return BaseResult.builder()
                .rounds(rounds)
                .totalWin(totalWin)
                .scatterCount(totalScatterCount)
                .freeSpinTriggered(triggered)
                .freeSpinCount(freeCount)
                .build();
    }

    /**
     * 建立單次消除結果
     */
    private CascadeRound buildCascadeRound(int[][] grid, SpinContext context, int cascadeLevel) {
        List<WinResult> wins = evaluateWins(grid, context);
        int multiplier = context.getCurrentMultiplier();
        double totalWin = wins.stream().mapToDouble(WinResult::getPayout).sum();
        int scatterCount = countScatters(grid);

        return CascadeRound.builder()
                .roundIndex(cascadeLevel)
                .grid(deepCopyGrid(grid))
                .wins(wins)
                .totalWin(totalWin)
                .multiplier(multiplier)
                .scatterCount(scatterCount)
                .hasNextCascade(totalWin > 0 && config.isHasCascade())
                .build();
    }

    // ==========================================
    // 免費遊戲流程
    // ==========================================

    /**
     * 處理免費遊戲：每一轉都跑初始停輪 + 連消
     */
    protected List<FreeSpinResult> processFreeSpins(SpinContext context, int totalSpins) {
        List<FreeSpinResult> results = new ArrayList<>();
        int remaining = totalSpins;
        int spinIndex = 0;

        while (remaining > 0) {
            SpinContext freeContext = SpinContext.builder()
                    .gameId(context.getGameId())
                    .betAmount(context.getBetAmount())
                    .lineBet(context.getLineBet())
                    .isFreeSpinMode(true)
                    .cascadeLevel(0)
                    .currentMultiplier(1)
                    .build();

            // 每一轉免費遊戲：停輪 + 連消（結構同主遊戲）
            List<CascadeRound> rounds = new ArrayList<>();
            int cascadeLevel = 0;
            double spinTotalWin = 0;
            int spinScatterCount = 0;

            int[][] grid = generateGrid(freeContext);
            CascadeRound firstRound = buildCascadeRound(grid, freeContext, cascadeLevel);
            rounds.add(firstRound);
            spinTotalWin += firstRound.getTotalWin();
            spinScatterCount += firstRound.getScatterCount();

            // 連消
            if (config.isHasCascade()) {
                CascadeRound currentRound = firstRound;
                while (currentRound.hasWin()) {
                    cascadeLevel++;
                    int[][] newGrid = cascadeGrid(currentRound);
                    SpinContext cascadeContext = SpinContext.builder()
                            .gameId(context.getGameId())
                            .betAmount(context.getBetAmount())
                            .lineBet(context.getLineBet())
                            .isFreeSpinMode(true)
                            .cascadeLevel(cascadeLevel)
                            .currentMultiplier(getCascadeMultiplier(cascadeLevel))
                            .build();

                    currentRound = buildCascadeRound(newGrid, cascadeContext, cascadeLevel);
                    if (currentRound.hasWin()) {
                        rounds.add(currentRound);
                        spinTotalWin += currentRound.getTotalWin();
                        spinScatterCount += currentRound.getScatterCount();
                    }
                }
            }

            if (!rounds.isEmpty()) {
                rounds.get(rounds.size() - 1).setHasNextCascade(false);
            }

            // Retrigger 判定
            boolean retrigger = spinScatterCount >= config.getScatterToTrigger();
            if (retrigger) {
                remaining += determineFreeSpinCount(spinScatterCount);
            }

            results.add(FreeSpinResult.builder()
                    .spinIndex(spinIndex)
                    .rounds(rounds)
                    .totalWin(spinTotalWin)
                    .currentMultiplier(1)
                    .scatterCount(spinScatterCount)
                    .retrigger(retrigger)
                    .build());

            spinIndex++;
            remaining--;
        }
        return results;
    }

    // ==========================================
    // 可覆寫的 Hook Methods
    // ==========================================

    /**
     * 生成盤面 — 根據輪帶權重隨機產生。子類可覆寫以自定義盤面生成邏輯。
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
     * 計算贏分 — 子類必須實作（Lines vs Ways vs ScatterPay 不同）
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
     * 決定免費遊戲次數 — 子類可覆寫
     */
    protected int determineFreeSpinCount(int scatterCount) {
        return config.getFreeSpinCount();
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
    protected int[][] cascadeGrid(CascadeRound round) {
        int[][] grid = deepCopyGrid(round.getGrid());
        Set<String> winPositions = new HashSet<>();

        for (WinResult win : round.getWins()) {
            if (win.getPositions() != null) {
                for (int[] pos : win.getPositions()) {
                    winPositions.add(pos[0] + "," + pos[1]);
                }
            }
        }

        for (int reel = 0; reel < config.getReels(); reel++) {
            List<Integer> remaining = new ArrayList<>();
            for (int row = 0; row < config.getRows(); row++) {
                if (!winPositions.contains(reel + "," + row)) {
                    remaining.add(grid[reel][row]);
                }
            }
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

    protected boolean isWild(int symbolId) {
        return symbolId == config.getWildSymbolId();
    }

    protected boolean isScatter(int symbolId) {
        return symbolId == config.getScatterSymbolId();
    }
}
