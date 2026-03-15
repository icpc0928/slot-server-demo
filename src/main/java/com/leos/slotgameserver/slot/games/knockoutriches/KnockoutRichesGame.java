package com.leos.slotgameserver.slot.games.knockoutriches;

import com.leos.slotgameserver.slot.core.GameState;
import com.leos.slotgameserver.slot.core.PlayMode;
import com.leos.slotgameserver.slot.core.result.GameAward;
import com.leos.slotgameserver.slot.core.result.Round;
import com.leos.slotgameserver.slot.core.result.SpinResult;

import java.util.*;

import static com.leos.slotgameserver.slot.games.knockoutriches.KnockoutRichesConfig.*;
import static com.leos.slotgameserver.slot.games.knockoutriches.KnockoutRichesWheels.*;
import static com.leos.slotgameserver.slot.games.knockoutriches.KnockoutRichesRoundHelper.*;

/**
 * Knockout Riches 9 Icon 完整遊戲邏輯
 */
public class KnockoutRichesGame {
    
    private final Random random = new Random();
    
    public SpinResult spin(long lineBet, int prob, PlayMode playMode) {
        SpinResult result = new SpinResult();
        result.setLineBet(lineBet);
        result.setTotalBet(getTotalBet(lineBet, playMode));
        
        long maxWin = getMaxWin(lineBet);
        long totalBaseWin = 0;
        long totalFreeWin = 0;
        
        // === 主遊戲 ===
        GameState gameState = GameState.BASE_GAME;
        int scCountBase = getScCount(gameState, playMode, prob);
        int wheelIndex = getWheelIndex(gameState, playMode, prob, false);
        int[][] wheel = getWheel(gameState, wheelIndex);
        
        List<Round> baseRounds = new ArrayList<>();
        Round lastRound = null;
        int[] accStackCount = new int[MAX_REEL];
        int roundCount = 0;
        
        do {
            if (roundCount == STACK_TRIGGER) {
                accStackCount = initAccStackCount();
            }
            
            Round round = getRoundResult(lineBet, gameState, playMode, wheel, lastRound, 
                    roundCount, prob, scCountBase, accStackCount, 0, false);
            baseRounds.add(round);
            totalBaseWin += round.getTotalWin();
            
            accStackCount = getAccStackCount(round);
            scCountBase -= round.getScatterCount();
            lastRound = round;
            roundCount++;
        } while (lastRound.isHasRespin());
        
        boolean isFree = lastRound.isFree();
        int freeSpinCount = lastRound.getFreeSpinCount();
        
        // 記分板獎勵
        long sbWin = 0;
        if (baseRounds.size() > STACK_TRIGGER) {
            sbWin = calculateScoreBoard(lineBet, accStackCount, playMode);
            totalBaseWin += sbWin;
        }
        
        // 檢查最大贏分
        boolean isLimit = totalBaseWin > maxWin;
        if (isLimit) {
            totalBaseWin = maxWin;
            isFree = false;
        }
        
        result.setBaseRounds(baseRounds);
        result.setTotalBaseWin(totalBaseWin);
        result.setTriggeredFree(isFree);
        result.setFreeSpinCount(freeSpinCount);
        
        // === 免費遊戲 ===
        if (isFree) {
            List<List<Round>> freeSpinRounds = new ArrayList<>();
            gameState = GameState.FREE_GAME;
            accStackCount = initAccStackCount();
            int remainingSpins = freeSpinCount;
            boolean isFreeWinEnough = false;
            int sbWays = 1;
            
            while (remainingSpins > 0) {
                List<Round> spinRounds = new ArrayList<>();
                wheelIndex = getWheelIndex(gameState, playMode, prob, sbWays >= BUY_FREE_SB_WAYS_CHANGE_WEIGHT);
                wheel = getWheel(gameState, wheelIndex);
                int scCountFree = getScCount(gameState, playMode, prob);
                
                lastRound = baseRounds.get(baseRounds.size() - 1);
                roundCount = 0;
                
                do {
                    boolean isGuaranteeWin = false;
                    if (playMode == PlayMode.BUY_FREE && roundCount == 0 && !isFreeWinEnough) {
                        isGuaranteeWin = isBuyFreeNeedWin(freeSpinCount - remainingSpins, freeSpinCount);
                    }
                    
                    Round round = getRoundResult(lineBet, gameState, playMode, wheel, lastRound,
                            roundCount, prob, scCountFree, accStackCount, sbWin, isGuaranteeWin);
                    spinRounds.add(round);
                    totalFreeWin += round.getTotalWin();
                    
                    accStackCount = getAccStackCount(round);
                    scCountFree -= round.getScatterCount();
                    
                    // 免中免
                    if (round.isFree()) {
                        remainingSpins += round.getFreeSpinCount();
                    }
                    
                    sbWays = calculateWays(accStackCount);
                    if (sbWays >= BUY_FREE_SB_WAYS_CONDITION) {
                        isFreeWinEnough = true;
                    }
                    
                    lastRound = round;
                    roundCount++;
                } while (lastRound.isHasRespin());
                
                freeSpinRounds.add(spinRounds);
                remainingSpins--;
            }
            
            // 免費遊戲記分板
            long freeSbWin = calculateScoreBoard(lineBet, accStackCount, playMode);
            totalFreeWin += freeSbWin;
            
            // 檢查最大贏分
            if (totalBaseWin + totalFreeWin > maxWin) {
                totalFreeWin = maxWin - totalBaseWin;
            }
            
            result.setFreeSpinRounds(freeSpinRounds);
        }
        
        result.setTotalFreeWin(totalFreeWin);
        result.setTotalWin(totalBaseWin + totalFreeWin);
        
        return result;
    }
    
    private Round getRoundResult(long lineBet, GameState gameState, PlayMode playMode,
                                  int[][] wheel, Round lastRound, int roundCount,
                                  int prob, int scNeeds, int[] accStackCount,
                                  long baseSBWin, boolean isGuaranteeWin) {
        Round round = new Round();
        round.setGameState(gameState);
        round.setRoundIndex(roundCount);
        
        long accWin = lastRound != null ? lastRound.getAccWin() : 0;
        
        if (gameState == GameState.FREE_GAME && (lastRound == null || lastRound.getGameState() == GameState.BASE_GAME)) {
            accWin += baseSBWin;
        }
        
        // Scatter 分配
        int sc = 0;
        if (roundCount == 0 && scNeeds > 0) {
            sc = random.nextDouble() > 0.5 ? scNeeds : scNeeds - 1;
        }
        
        // 生成盤面
        int[][] grid;
        int[] winCountReel = new int[MAX_REEL];
        
        if (roundCount == 0) {
            grid = generateFirstGrid(wheel, playMode, gameState, sc, isGuaranteeWin);
        } else {
            grid = generateCascadeGrid(lastRound.getGrid(), lastRound.getWinGrid(), 
                    gameState, playMode, prob, roundCount, sc, isGuaranteeWin, winCountReel);
        }
        
        round.setGrid(grid);
        setAccStackCount(round, Arrays.copyOf(accStackCount, accStackCount.length));
        
        // 計算獎項
        List<GameAward> awards = calculateAwards(lineBet, grid);
        round.setAwards(awards);
        
        // 計算贏分
        long totalWin = awards.stream().mapToLong(GameAward::win).sum();
        round.setTotalWin(totalWin);
        
        // 設置中獎盤面
        int[][] winGrid = calculateWinGrid(grid, awards);
        round.setWinGrid(winGrid);
        round.setWinCountPerReel(winCountReel);
        
        accWin += totalWin;
        round.setAccWin(accWin);
        
        // 更新記分板
        if (totalWin > 0) {
            updateAccStackCount(gameState, round, roundCount, winGrid, accStackCount);
        }
        setAccStackCount(round, accStackCount);
        
        // 是否繼續連消
        boolean hasRespin = totalWin > 0;
        round.setHasRespin(hasRespin);
        
        // 補足 Scatter
        if (!hasRespin && scNeeds - sc > 0) {
            placeLeftScatters(grid, scNeeds - sc, roundCount, winCountReel);
            sc = scNeeds;
        }
        round.setScatterCount(sc);
        
        // 最後一消判斷免費遊戲
        if (!hasRespin) {
            int scatterTotal = countScattersInGrid(grid);
            if (scatterTotal >= FREE_SPIN_NEED) {
                round.setFree(true);
                round.setFreeSpinCount(FREE_SPIN_COUNT + (scatterTotal - FREE_SPIN_NEED) * FREE_SPIN_COUNT);
            }
        }
        
        return round;
    }
    
    // === 盤面生成 ===
    
    private int[][] generateFirstGrid(int[][] wheel, PlayMode playMode, GameState gameState, int sc, boolean isGuaranteeWin) {
        int[][] grid = new int[MAX_REEL][];
        
        for (int i = 0; i < MAX_REEL; i++) {
            grid[i] = new int[GRID_SIZE[i]];
            int index = random.nextInt(wheel[i].length);
            for (int j = 0; j < GRID_SIZE[i]; j++) {
                grid[i][j] = wheel[i][(index + j) % wheel[i].length];
            }
        }
        
        // 放置 Scatter
        if (sc > 0) {
            placeScatters(grid, sc);
        }
        
        return grid;
    }
    
    private int[][] generateCascadeGrid(int[][] lastGrid, int[][] winGrid, GameState gameState,
                                         PlayMode playMode, int prob, int roundCount, int sc,
                                         boolean isGuaranteeWin, int[] winCountReel) {
        int[][] grid = new int[MAX_REEL][];
        
        for (int i = 0; i < MAX_REEL; i++) {
            grid[i] = new int[GRID_SIZE[i]];
            winCountReel[i] = 0;
            
            // 計算每軸消除數量
            for (int j = 0; j < GRID_SIZE[i]; j++) {
                if (winGrid[i][j] == 1) {
                    winCountReel[i]++;
                }
            }
            
            // 保留未中獎的符號（下落）
            int fillIdx = GRID_SIZE[i] - 1;
            for (int j = GRID_SIZE[i] - 1; j >= 0; j--) {
                if (winGrid[i][j] == 0) {
                    grid[i][fillIdx--] = lastGrid[i][j];
                }
            }
            
            // 生成新符號
            int cascadeIndex = getCascadeIndex(gameState, playMode, prob, roundCount);
            for (int j = 0; j < winCountReel[i]; j++) {
                grid[i][j] = getNewSymbol(cascadeIndex, i);
            }
        }
        
        // 強制中獎（買免費模式）
        if (isGuaranteeWin || isGuaranteedToWin(gameState, playMode, roundCount)) {
            setGuaranteeWinGrid(grid, winCountReel);
        }
        
        // 放置 Scatter
        if (sc > 0) {
            placeScattersInCascade(grid, winCountReel, sc);
        }
        
        return grid;
    }
    
    // === 算獎 ===
    
    private List<GameAward> calculateAwards(long lineBet, int[][] grid) {
        List<GameAward> awards = new ArrayList<>();
        
        for (int symbol = 0; symbol < WILD; symbol++) {
            int reelCount = 0;
            int ways = 1;
            
            for (int reel = 0; reel < MAX_REEL; reel++) {
                int count = 0;
                for (int row = 0; row < GRID_SIZE[reel]; row++) {
                    if (grid[reel][row] == symbol || grid[reel][row] == WILD) {
                        count++;
                    }
                }
                if (count == 0) break;
                reelCount++;
                ways *= count;
            }
            
            if (reelCount >= 3 && PAY_TABLE[symbol][reelCount] > 0) {
                long win = lineBet * PAY_TABLE[symbol][reelCount] * ways;
                awards.add(new GameAward(symbol, reelCount, ways, lineBet, PAY_TABLE[symbol][reelCount], win));
            }
        }
        
        return awards;
    }
    
    private int[][] calculateWinGrid(int[][] grid, List<GameAward> awards) {
        int[][] winGrid = new int[MAX_REEL][];
        for (int i = 0; i < MAX_REEL; i++) {
            winGrid[i] = new int[GRID_SIZE[i]];
        }
        
        for (GameAward award : awards) {
            int symbol = award.symbol();
            int reelCount = award.reelCount();
            
            for (int reel = 0; reel < reelCount; reel++) {
                for (int row = 0; row < GRID_SIZE[reel]; row++) {
                    if (grid[reel][row] == symbol || grid[reel][row] == WILD) {
                        winGrid[reel][row] = 1;
                    }
                }
            }
        }
        
        return winGrid;
    }
    
    // === 記分板 ===
    
    private long calculateScoreBoard(long lineBet, int[] accStackCount, PlayMode playMode) {
        int ways = calculateWays(accStackCount);
        if (ways <= 0) return 0;
        
        int[] iconWeight = getScoreBoardIconWeight(ways, playMode);
        int icon = getIndexByWeight(iconWeight);
        
        return lineBet * PAY_TABLE[icon][MAX_REEL] * ways;
    }
    
    private int[] getScoreBoardIconWeight(int ways, PlayMode playMode) {
        int[][] weights = (playMode == PlayMode.NORMAL) ? SCOREBOARD_ICON_WEIGHT : SCOREBOARD_ICON_WEIGHT_BUY_FREE;
        
        for (int i = 0; i < SCOREBOARD_WAYS_LEVEL.length; i++) {
            if (ways <= SCOREBOARD_WAYS_LEVEL[i]) {
                return weights[i];
            }
        }
        return weights[weights.length - 1];
    }
    
    private int calculateWays(int[] accStackCount) {
        int ways = 1;
        for (int count : accStackCount) {
            if (count > 0) ways *= count;
        }
        return ways;
    }
    
    private void updateAccStackCount(GameState gameState, Round round, int roundCount, 
                                      int[][] winGrid, int[] accStackCount) {
        if (gameState == GameState.BASE_GAME && roundCount < STACK_TRIGGER) return;
        
        for (int i = 0; i < MAX_REEL; i++) {
            if (MAX_REEL_STACK[i] > 0) {
                int winCount = 0;
                for (int j = 0; j < GRID_SIZE[i]; j++) {
                    if (winGrid[i][j] == 1) winCount++;
                }
                accStackCount[i] = Math.min(MAX_REEL_STACK[i], accStackCount[i] + winCount);
            }
        }
    }
    
    private int[] initAccStackCount() {
        return new int[]{0, 1, 1, 1, 0};
    }
    
    // === 輪帶選擇 ===
    
    private int getWheelIndex(GameState gameState, PlayMode playMode, int prob, boolean isBuyFreeChange) {
        Map<Integer, int[]> weights = getWheelWeights(gameState, playMode, isBuyFreeChange);
        int[] weight = weights.getOrDefault(prob, weights.get(DEFAULT_PROB));
        return getIndexByWeight(weight);
    }
    
    private Map<Integer, int[]> getWheelWeights(GameState gameState, PlayMode playMode, boolean isBuyFreeChange) {
        if (playMode == PlayMode.NORMAL) {
            return gameState == GameState.BASE_GAME ? N_BASE_WEIGHT : N_FREE_WEIGHT;
        } else {
            if (gameState == GameState.BASE_GAME) {
                return BF_BASE_WEIGHT;
            }
            return isBuyFreeChange ? BF_FREE_WEIGHT_CHANGE : BF_FREE_WEIGHT;
        }
    }
    
    private int[][] getWheel(GameState gameState, int index) {
        if (gameState == GameState.BASE_GAME) {
            return switch (index) {
                case 0 -> BASE_WHEEL_0;
                case 1 -> BASE_WHEEL_1;
                case 2 -> BASE_WHEEL_2;
                case 3 -> BASE_WHEEL_3;
                case 4 -> BASE_WHEEL_4;
                case 5 -> BASE_WHEEL_5;
                default -> BASE_WHEEL_0;
            };
        } else {
            return switch (index) {
                case 0 -> FREE_WHEEL_0;
                case 1 -> FREE_WHEEL_1;
                case 2 -> FREE_WHEEL_2;
                case 3 -> FREE_WHEEL_3;
                case 4 -> FREE_WHEEL_4;
                case 5 -> FREE_WHEEL_5;
                case 6 -> FREE_WHEEL_6;
                default -> FREE_WHEEL_0;
            };
        }
    }
    
    // === Scatter ===
    
    private int getScCount(GameState gameState, PlayMode playMode, int prob) {
        Map<Integer, int[]> weights;
        if (gameState == GameState.BASE_GAME) {
            weights = (playMode == PlayMode.NORMAL) ? SC_WEIGHT_N_BASE : SC_WEIGHT_BUY_FREE;
        } else {
            weights = (playMode == PlayMode.NORMAL) ? SC_WEIGHT_N_FREE : SC_WEIGHT_BUY_FREE;
        }
        int[] weight = weights.getOrDefault(prob, weights.get(DEFAULT_PROB));
        return getIndexByWeight(weight);
    }
    
    private void placeScatters(int[][] grid, int count) {
        List<int[]> positions = new ArrayList<>();
        for (int i = 0; i < MAX_REEL; i++) {
            positions.add(new int[]{i, random.nextInt(GRID_SIZE[i])});
        }
        Collections.shuffle(positions);
        
        for (int i = 0; i < Math.min(count, positions.size()); i++) {
            int[] pos = positions.get(i);
            grid[pos[0]][pos[1]] = SCATTER;
        }
    }
    
    private void placeScattersInCascade(int[][] grid, int[] winCountReel, int count) {
        List<int[]> positions = new ArrayList<>();
        for (int i = 0; i < MAX_REEL; i++) {
            for (int j = 0; j < winCountReel[i]; j++) {
                if (grid[i][j] != SCATTER) {
                    positions.add(new int[]{i, j});
                }
            }
        }
        Collections.shuffle(positions);
        
        for (int i = 0; i < Math.min(count, positions.size()); i++) {
            int[] pos = positions.get(i);
            grid[pos[0]][pos[1]] = SCATTER;
        }
    }
    
    private void placeLeftScatters(int[][] grid, int count, int roundCount, int[] winCountReel) {
        List<int[]> positions = new ArrayList<>();
        
        if (roundCount == 0) {
            for (int i = 0; i < MAX_REEL; i++) {
                for (int j = 0; j < GRID_SIZE[i]; j++) {
                    if (grid[i][j] != SCATTER) {
                        positions.add(new int[]{i, j});
                    }
                }
            }
        } else {
            for (int i = 0; i < MAX_REEL; i++) {
                for (int j = 0; j < winCountReel[i]; j++) {
                    if (grid[i][j] != SCATTER) {
                        positions.add(new int[]{i, j});
                    }
                }
            }
        }
        
        Collections.shuffle(positions);
        for (int i = 0; i < Math.min(count, positions.size()); i++) {
            int[] pos = positions.get(i);
            grid[pos[0]][pos[1]] = SCATTER;
        }
    }
    
    private int countScattersInGrid(int[][] grid) {
        int count = 0;
        for (int[] reel : grid) {
            for (int symbol : reel) {
                if (symbol == SCATTER) count++;
            }
        }
        return count;
    }
    
    // === 消除權重 ===
    
    private int getCascadeIndex(GameState gameState, PlayMode playMode, int prob, int roundCount) {
        int[][] cascadeProb;
        if (gameState == GameState.BASE_GAME) {
            cascadeProb = CASCADE_PROB_BASE;
        } else if (playMode == PlayMode.NORMAL) {
            cascadeProb = CASCADE_PROB_FREE;
        } else {
            cascadeProb = CASCADE_PROB_BUY_FREE;
        }
        
        int idx = Math.min(roundCount, cascadeProb.length - 1);
        if (idx == 0) idx = 1;
        return getIndexByWeight(cascadeProb[idx]);
    }
    
    private int getNewSymbol(int cascadeIndex, int reel) {
        int[][] weights = CASCADE_WEIGHTS[cascadeIndex];
        int[] reelWeights = new int[weights.length];
        for (int i = 0; i < weights.length; i++) {
            reelWeights[i] = weights[i][reel];
        }
        return getIndexByWeight(reelWeights);
    }
    
    // === 強制中獎 ===
    
    private boolean isGuaranteedToWin(GameState gameState, PlayMode playMode, int roundCount) {
        if (gameState != GameState.BASE_GAME || roundCount < 1 || playMode != PlayMode.NORMAL) {
            return false;
        }
        int idx = Math.min(roundCount, BASE_CASCADE_WIN_PROB.length - 1);
        return random.nextDouble() < BASE_CASCADE_WIN_PROB[idx];
    }
    
    private boolean isBuyFreeNeedWin(int currentSpin, int totalSpins) {
        int lastRoundCount = totalSpins - currentSpin;
        if (lastRoundCount > BUY_FREE_WIN_PROB.length) return false;
        return random.nextDouble() < BUY_FREE_WIN_PROB[lastRoundCount - 1];
    }
    
    private void setGuaranteeWinGrid(int[][] grid, int[] winCountReel) {
        // 統計前三軸的符號
        Map<Integer, Integer> symbolCount = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            Set<Integer> reelSymbols = new HashSet<>();
            for (int j = 0; j < GRID_SIZE[i]; j++) {
                int symbol = grid[i][j];
                if (symbol != SCATTER && symbol != WILD) {
                    reelSymbols.add(symbol);
                }
            }
            for (int symbol : reelSymbols) {
                symbolCount.merge(symbol, 1, Integer::sum);
            }
        }
        
        // 找出出現最多的符號
        int targetSymbol = symbolCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
        
        // 確保前三軸都有這個符號
        for (int i = 0; i < 3; i++) {
            boolean hasSymbol = false;
            for (int j = 0; j < GRID_SIZE[i]; j++) {
                if (grid[i][j] == targetSymbol || grid[i][j] == WILD) {
                    hasSymbol = true;
                    break;
                }
            }
            if (!hasSymbol && winCountReel[i] > 0) {
                int randomIdx = random.nextInt(winCountReel[i]);
                grid[i][randomIdx] = targetSymbol;
            }
        }
    }
    
    // === 工具方法 ===
    
    private int getIndexByWeight(int[] weights) {
        int total = 0;
        for (int w : weights) total += w;
        if (total == 0) return 0;
        
        int rand = random.nextInt(total);
        int acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (rand < acc) return i;
        }
        return 0;
    }
    
    private long getTotalBet(long lineBet, PlayMode playMode) {
        int multiplier = (playMode == PlayMode.BUY_FREE) ? BUY_FREE_MULTIPLIER : 1;
        return lineBet * BET_TIMES * multiplier;
    }
    
    private long getMaxWin(long lineBet) {
        return lineBet * BET_TIMES * MAX_WIN_LIMIT;
    }
}
