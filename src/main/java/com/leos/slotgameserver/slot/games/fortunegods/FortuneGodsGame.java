package com.leos.slotgameserver.slot.games.fortunegods;

import com.leos.slotgameserver.slot.core.GameState;
import com.leos.slotgameserver.slot.core.PlayMode;
import com.leos.slotgameserver.slot.core.result.GameAward;
import com.leos.slotgameserver.slot.core.result.Round;
import com.leos.slotgameserver.slot.core.result.SpinResult;

import java.util.*;

import static com.leos.slotgameserver.slot.games.fortunegods.FortuneGodsConfig.*;
import static com.leos.slotgameserver.slot.games.fortunegods.FortuneGodsWheels.*;
import static com.leos.slotgameserver.slot.games.fortunegods.FortuneGodsRoundHelper.*;

/**
 * Fortune Gods 完整遊戲邏輯
 */
public class FortuneGodsGame {
    
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
        int wheelProbIndex = getWheelProbIndex(gameState, playMode, prob);
        int[][] wheel = getProbWheel(gameState, wheelProbIndex);
        int[][] topWheel = getTopWheel(gameState, wheelProbIndex);
        
        List<Round> baseRounds = new ArrayList<>();
        Round lastRound = null;
        int roundCount = 0;
        
        do {
            Round round = getRoundResult(lineBet, gameState, playMode, wheel, topWheel,
                    lastRound, roundCount, prob, scCountBase, 1, wheelProbIndex);
            baseRounds.add(round);
            totalBaseWin += round.getTotalWin();
            
            scCountBase -= getScSet(round);
            lastRound = round;
            roundCount++;
        } while (lastRound.isHasRespin());
        
        boolean isFree = lastRound.isFree();
        int freeSpinCount = lastRound.getFreeSpinCount();
        
        // 檢查最大贏分
        if (totalBaseWin > maxWin) {
            totalBaseWin = maxWin;
            isFree = false;
        }
        
        result.setBaseRounds(baseRounds);
        result.setTotalBaseWin(totalBaseWin);
        result.setTriggeredFree(isFree);
        result.setFreeSpinCount(freeSpinCount);
        
        // === 免費遊戲 ===
        if (isFree && totalBaseWin < maxWin) {
            List<List<Round>> freeSpinRounds = new ArrayList<>();
            gameState = GameState.FREE_GAME;
            int multiplier = FREE_SPIN_MULTIPLIER;
            int remainingSpins = freeSpinCount;
            long defaultBet = lineBet * BET_TIMES;
            
            while (remainingSpins > 0) {
                // 檢查是否強制中獎
                int spinIndex = freeSpinCount - remainingSpins;
                boolean forceWin = shouldForceWin(totalFreeWin, defaultBet, remainingSpins, playMode);
                long minWinRequired = forceWin ? (long)(defaultBet * FREE_FORCE_WIN_MIN_MULTIPLIER) : 0;
                
                List<Round> spinRounds;
                long spinTotalWin;
                int retryCount = 0;
                final int MAX_RETRY = 1000;
                
                do {
                    spinRounds = new ArrayList<>();
                    wheelProbIndex = getWheelProbIndex(gameState, playMode, prob);
                    wheel = getProbWheel(gameState, wheelProbIndex);
                    topWheel = getTopWheel(gameState, wheelProbIndex);
                    int scCountFree = getScCount(gameState, playMode, prob);
                    
                    Round tempLastRound = lastRound;
                    roundCount = 0;
                    spinTotalWin = 0;
                    
                    do {
                        Round round = getRoundResult(lineBet, gameState, playMode, wheel, topWheel,
                                tempLastRound, roundCount, prob, scCountFree, multiplier, wheelProbIndex);
                        spinRounds.add(round);
                        spinTotalWin += round.getTotalWin();
                        
                        scCountFree -= getScSet(round);
                        
                        // 免中免
                        if (round.isFree()) {
                            remainingSpins += round.getFreeSpinCount();
                        }
                        
                        tempLastRound = round;
                        roundCount++;
                    } while (tempLastRound.isHasRespin());
                    
                    lastRound = tempLastRound;
                    retryCount++;
                } while (forceWin && spinTotalWin < minWinRequired && retryCount < MAX_RETRY);
                
                freeSpinRounds.add(spinRounds);
                totalFreeWin += spinTotalWin;
                remainingSpins--;
            }
            
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
    
    // ========================================
    // 單 Round 處理
    // ========================================
    
    private Round getRoundResult(long lineBet, GameState gameState, PlayMode playMode,
                                  int[][] wheel, int[][] topWheel, Round lastRound,
                                  int roundCount, int prob, int scNeeds, int multiplier, int wheelProbIndex) {
        Round round = new Round();
        round.setGameState(gameState);
        round.setRoundIndex(roundCount);
        setCurrentMultiplier(round, multiplier);
        
        long accWin = lastRound != null ? lastRound.getAccWin() : 0;
        
        // Scatter 自然分布
        int sc = 0;
        if (scNeeds > 0) {
            sc = calculateNaturalScatterDistribution(scNeeds, roundCount);
        }
        
        // 生成盤面
        int[][] mainGrid;
        int[] topReel = new int[MAX_REEL];
        int[] winCountReel = new int[MAX_REEL];
        List<MultiSizeSymbol> multiSizeSymbols = new ArrayList<>();
        int[][] lockMain = initMainGrid();
        int[] lockTop = new int[MAX_REEL];
        
        if (roundCount == 0) {
            // 第一消
            mainGrid = generateFirstMainGrid(wheel);
            
            // 檢查橫財神觸發
            boolean forceHorizontalFortune = checkForceHorizontalFortune(gameState, playMode, prob, wheelProbIndex);
            if (forceHorizontalFortune) {
                setupGradualHorizontalFortune(topReel, mainGrid, round, prob, lockMain, lockTop);
            } else {
                generateTopReel(topReel, topWheel);
            }
            
            // 放置 Scatter
            boolean isInGradualMode = isGradualHorizontalFortuneMode(round) || isHorizontalFortuneTriggered(round);
            if (sc > 0) {
                placeScatters(mainGrid, topReel, sc, isInGradualMode, lockMain, lockTop);
            }
            
            // 生成多格符號
            generateMultiSizeSymbols(mainGrid, multiSizeSymbols, lockMain);
            
        } else {
            // 連消
            mainGrid = initMainGrid();
            int[][] lastMainGrid = deepCopyGrid(lastRound.getGrid());
            int[][] winGrid = deepCopyGrid(lastRound.getWinGrid());
            int[] lastTopReel = getTopReel(lastRound).clone();
            int[] topReelWinPos = getTopReelWinPos(lastRound);
            if (topReelWinPos != null) topReelWinPos = topReelWinPos.clone();
            
            List<MultiSizeSymbol> lastMultiSymbols = getMultiSizeSymbols(lastRound);
            
            // 繼承漸進橫財神模式
            boolean isGradualMode = isGradualHorizontalFortuneMode(lastRound);
            setGradualHorizontalFortuneMode(round, isGradualMode);
            
            // 處理銀框/金框轉換
            processFrameWinningInCascade(lastMainGrid, winGrid, lastMultiSymbols, multiSizeSymbols, round);
            
            // 主盤面掉落
            for (int i = 0; i < MAX_REEL; i++) {
                winCountReel[i] = 0;
                for (int j = 0; j < MAIN_GRID_SIZE[i]; j++) {
                    if (winGrid[i][j] == 1) winCountReel[i]++;
                }
                
                int fillIndex = MAIN_GRID_SIZE[i] - 1;
                for (int j = MAIN_GRID_SIZE[i] - 1; j >= 0; j--) {
                    if (winGrid[i][j] == 0) {
                        mainGrid[i][fillIndex--] = lastMainGrid[i][j];
                    }
                }
                
                int cascadeWheelIndex = getCascadeWheelIndex(gameState, playMode, roundCount);
                generateNewSymbolsFromWheel(mainGrid, i, winCountReel[i], gameState, cascadeWheelIndex);
            }
            
            // 附加軸掉落
            cascadeTopReel(topReel, lastTopReel, topReelWinPos, gameState, playMode, prob, roundCount,
                    wheelProbIndex, isHorizontalFortuneTriggered(lastRound), isGradualMode, round);
            
            // 補足 Scatter
            boolean skipTopReelForScatter = isHorizontalFortuneTriggered(round) || isGradualMode;
            if (sc > 0) {
                placeScattersInCascade(mainGrid, winCountReel, sc, topReel, getLastTopReelWinCount(lastRound), skipTopReelForScatter, lockMain, lockTop);
            }
        }
        
        round.setGrid(mainGrid);
        setTopReel(round, topReel);
        round.setWinCountPerReel(winCountReel);
        setMultiSizeSymbols(round, multiSizeSymbols);
        setScSet(round, sc);
        
        // 計算獎項
        Set<Integer> totalWinPos = new HashSet<>();
        Set<Integer> totalTopWinPos = new HashSet<>();
        List<GameAward> awards = calculateWaysAward(lineBet, mainGrid, topReel, multiSizeSymbols,
                multiplier, totalWinPos, totalTopWinPos, isHorizontalFortuneTriggered(round));
        round.setAwards(awards);
        
        // 設置中獎盤面
        setWinGrid(round, totalWinPos, totalTopWinPos);
        
        // 計算贏分
        long totalWin = awards.stream().mapToLong(GameAward::win).sum();
        round.setTotalWin(totalWin);
        
        // 標記中獎的多格符號
        if (totalWin > 0) {
            markWinningMultiSymbols(round);
        }
        
        accWin += totalWin;
        round.setAccWin(accWin);
        
        // 是否繼續連消
        boolean hasRespin = totalWin > 0;
        round.setHasRespin(hasRespin);
        
        // 最後一消補足 Scatter
        if (!hasRespin && scNeeds - sc > 0) {
            gridSettingLeftSc(round, scNeeds - sc, roundCount);
        }
        
        // 判斷免費遊戲觸發
        if (!hasRespin) {
            int scatterCount = countScatterInGrid(mainGrid, topReel, multiSizeSymbols);
            round.setScatterCount(scatterCount);
            if (scatterCount >= FREE_SPIN_NEED) {
                round.setFree(true);
                int extraScatter = scatterCount - FREE_SPIN_NEED;
                round.setFreeSpinCount(BASE_FREE_SPIN_COUNT + extraScatter * EXTRA_FREE_PER_SCATTER);
            }
        }
        
        return round;
    }
    
    // ========================================
    // 盤面生成
    // ========================================
    
    private int[][] generateFirstMainGrid(int[][] wheel) {
        int[][] mainGrid = initMainGrid();
        for (int i = 0; i < MAX_REEL; i++) {
            int index = random.nextInt(wheel[i].length);
            for (int j = 0; j < MAIN_GRID_SIZE[i]; j++) {
                mainGrid[i][j] = wheel[i][(index + j) % wheel[i].length];
            }
        }
        return mainGrid;
    }
    
    private void generateTopReel(int[] topReel, int[][] topWheel) {
        for (int i = 1; i <= 4; i++) {
            topReel[i] = topWheel[i - 1][random.nextInt(topWheel[i - 1].length)];
        }
    }
    
    private void setupGradualHorizontalFortune(int[] topReel, int[][] mainGrid, Round round,
                                               int prob, int[][] lockMain, int[] lockTop) {
        int[] countWeight = GRADUAL_HORIZONTAL_FORTUNE_COUNT_WEIGHT.getOrDefault(prob,
                GRADUAL_HORIZONTAL_FORTUNE_COUNT_WEIGHT.get(DEFAULT_PROB));
        int initialCount = getIndexByWeight(countWeight);
        
        if (initialCount >= 4) {
            // 直接觸發完整橫財神
            for (int i = 1; i <= 4; i++) {
                topReel[i] = HORIZONTAL_FORTUNE;
            }
            setHorizontalFortuneTriggered(round, true);
        } else {
            // 漸進模式
            setGradualHorizontalFortuneMode(round, true);
            List<Integer> positions = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
            Collections.shuffle(positions);
            
            // 放置橫財神
            for (int i = 0; i < initialCount; i++) {
                topReel[positions.get(i)] = HORIZONTAL_FORTUNE;
                lockTop[positions.get(i)] = 1;
            }
            
            // 放置一般符號（低價值優先）
            for (int i = initialCount; i < 4; i++) {
                int pos = positions.get(i);
                topReel[pos] = getRandomGradualNormalSymbol();
                lockTop[pos] = 1;
            }
            
            // 確保附加軸一般符號能中獎
            ensureTopReelWins(mainGrid, topReel, lockMain, lockTop);
        }
    }
    
    private void ensureTopReelWins(int[][] mainGrid, int[] topReel, int[][] lockMain, int[] lockTop) {
        for (int reel = 1; reel <= 4; reel++) {
            if (topReel[reel] != HORIZONTAL_FORTUNE && topReel[reel] != SCATTER) {
                int symbol = topReel[reel];
                // 確保前三軸有這個符號
                for (int r = 0; r < 3; r++) {
                    boolean found = false;
                    for (int row = 0; row < MAIN_GRID_SIZE[r]; row++) {
                        if (mainGrid[r][row] == symbol || mainGrid[r][row] == WILD) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        int randomRow = random.nextInt(MAIN_GRID_SIZE[r]);
                        mainGrid[r][randomRow] = symbol;
                        lockMain[r][randomRow] = 1;
                    }
                }
            }
        }
    }
    
    private int getRandomGradualNormalSymbol() {
        return getIndexByWeight(GRADUAL_NORMAL_SYMBOL_WEIGHT);
    }
    
    private void cascadeTopReel(int[] topReel, int[] lastTopReel, int[] topReelWinPos,
                                 GameState gameState, PlayMode playMode, int prob, int roundCount,
                                 int wheelProbIndex, boolean horizontalFortuneTriggered,
                                 boolean isGradualMode, Round round) {
        if (topReelWinPos == null) {
            System.arraycopy(lastTopReel, 0, topReel, 0, lastTopReel.length);
            return;
        }
        
        List<Integer> remaining = new ArrayList<>();
        int winCount = 0;
        
        for (int i = 1; i <= 4; i++) {
            if (topReelWinPos[i] == 0) {
                remaining.add(lastTopReel[i]);
            } else {
                winCount++;
            }
        }
        
        int idx = 1;
        for (int symbol : remaining) {
            topReel[idx++] = symbol;
        }
        
        if (isGradualMode) {
            for (int i = 0; i < winCount; i++) {
                topReel[idx++] = HORIZONTAL_FORTUNE;
            }
            if (isTopReelAllHorizontalFortune(topReel)) {
                setHorizontalFortuneTriggered(round, true);
                setGradualHorizontalFortuneMode(round, false);
            }
        } else {
            int[][] topWheel = getTopWheel(gameState, wheelProbIndex);
            for (int i = 0; i < winCount; i++) {
                int topReelPos = idx - 1;
                topReel[idx++] = topWheel[topReelPos][random.nextInt(topWheel[topReelPos].length)];
            }
        }
        setLastTopReelWinCount(round, winCount);
    }
    
    private boolean isTopReelAllHorizontalFortune(int[] topReel) {
        for (int i = 1; i <= 4; i++) {
            if (topReel[i] != HORIZONTAL_FORTUNE) return false;
        }
        return true;
    }
    
    private void generateNewSymbolsFromWheel(int[][] mainGrid, int reel, int newCount, GameState gameState, int wheelIndex) {
        if (newCount <= 0) return;
        int[][] wheel = getProbWheel(gameState, wheelIndex);
        for (int j = 0; j < newCount; j++) {
            mainGrid[reel][j] = wheel[reel][random.nextInt(wheel[reel].length)];
        }
    }
    
    // ========================================
    // 多格符號
    // ========================================
    
    private void generateMultiSizeSymbols(int[][] mainGrid, List<MultiSizeSymbol> multiSizeSymbols, int[][] lockMain) {
        for (int reel = 1; reel <= 4; reel++) {
            int row = 0;
            while (row < MAIN_GRID_SIZE[reel]) {
                int symbol = mainGrid[reel][row];
                if (symbol == WILD || symbol == HORIZONTAL_FORTUNE || lockMain[reel][row] == 1) {
                    row++;
                    continue;
                }
                
                int size = getSymbolSize(symbol);
                if (row + size > MAIN_GRID_SIZE[reel]) {
                    size = MAIN_GRID_SIZE[reel] - row;
                }
                
                for (int i = 1; i < size; i++) {
                    if (mainGrid[reel][row + i] == SCATTER || lockMain[reel][row + i] == 1) {
                        size = i;
                        break;
                    }
                }
                
                if (size > 1) {
                    for (int i = 1; i < size; i++) {
                        mainGrid[reel][row + i] = symbol;
                    }
                    MultiSizeSymbol ms = new MultiSizeSymbol(reel, row, size, symbol);
                    if (rollSilverFrame(symbol)) {
                        ms.setSilverFrame(true);
                    }
                    multiSizeSymbols.add(ms);
                }
                row += size;
            }
        }
    }
    
    private int getSymbolSize(int symbol) {
        if (symbol >= MULTI_SIZE_WEIGHT.length) return 1;
        return getIndexByWeight(MULTI_SIZE_WEIGHT[symbol]) + 1;
    }
    
    private boolean rollSilverFrame(int symbol) {
        if (symbol >= SILVER_FRAME_PROB.length) return false;
        return random.nextInt(10000) < SILVER_FRAME_PROB[symbol];
    }
    
    private void processFrameWinningInCascade(int[][] lastMainGrid, int[][] winGrid,
                                               List<MultiSizeSymbol> lastMultiSymbols,
                                               List<MultiSizeSymbol> newMultiSymbols, Round round) {
        if (lastMultiSymbols == null || lastMultiSymbols.isEmpty()) return;
        
        for (MultiSizeSymbol ms : lastMultiSymbols) {
            int reel = ms.getReel();
            int startRow = ms.getStartRow();
            int size = ms.getSize();
            
            if (ms.isGoldFrame() && ms.isWinning()) {
                for (int i = 0; i < size; i++) {
                    lastMainGrid[reel][startRow + i] = WILD;
                    winGrid[reel][startRow + i] = 0;
                }
                setConvertedToWild(round, true);
            } else if (ms.isSilverFrame() && ms.isWinning()) {
                int newSymbol = getRandomNormalSymbolExcept(ms.getSymbol());
                for (int i = 0; i < size; i++) {
                    lastMainGrid[reel][startRow + i] = newSymbol;
                    winGrid[reel][startRow + i] = 0;
                }
                MultiSizeSymbol goldMs = new MultiSizeSymbol(reel, startRow, size, newSymbol);
                goldMs.setGoldFrame(true);
                newMultiSymbols.add(goldMs);
            } else if (!ms.isWinning()) {
                newMultiSymbols.add(ms.copy());
            }
        }
        
        // 更新位置
        for (MultiSizeSymbol ms : newMultiSymbols) {
            int reel = ms.getReel();
            int startRow = ms.getStartRow();
            int size = ms.getSize();
            int removedBelow = 0;
            for (int j = startRow + size; j < winGrid[reel].length; j++) {
                if (winGrid[reel][j] == 1) removedBelow++;
            }
            ms.setStartRow(startRow + removedBelow);
        }
    }
    
    private int getRandomNormalSymbolExcept(int excludeSymbol) {
        int newSymbol;
        do {
            newSymbol = random.nextInt(11);
        } while (newSymbol == excludeSymbol);
        return newSymbol;
    }
    
    private void markWinningMultiSymbols(Round round) {
        int[][] winGrid = round.getWinGrid();
        List<MultiSizeSymbol> multiSymbols = getMultiSizeSymbols(round);
        
        for (MultiSizeSymbol multi : multiSymbols) {
            int reel = multi.getReel();
            int startRow = multi.getStartRow();
            int size = multi.getSize();
            
            for (int i = 0; i < size; i++) {
                if (winGrid[reel][startRow + i] == 1) {
                    multi.setWinning(true);
                    break;
                }
            }
        }
    }
    
    // ========================================
    // Scatter 處理
    // ========================================
    
    private void placeScatters(int[][] mainGrid, int[] topReel, int count, boolean forceHorizontalFortune, int[][] lockMain, int[] lockTop) {
        List<int[]> positions = new ArrayList<>();
        for (int i = 0; i < MAX_REEL; i++) {
            for (int j = 0; j < MAIN_GRID_SIZE[i]; j++) {
                if (mainGrid[i][j] != SCATTER && lockMain[i][j] == 0) {
                    positions.add(new int[]{i, j});
                }
            }
        }
        if (!forceHorizontalFortune) {
            for (int i = 1; i <= 4; i++) {
                if (topReel[i] != SCATTER && lockTop[i] == 0) {
                    positions.add(new int[]{-1, i});
                }
            }
        }
        
        Collections.shuffle(positions);
        for (int i = 0; i < Math.min(count, positions.size()); i++) {
            int[] pos = positions.get(i);
            if (pos[0] == -1) {
                topReel[pos[1]] = SCATTER;
            } else {
                mainGrid[pos[0]][pos[1]] = SCATTER;
            }
        }
    }
    
    private void placeScattersInCascade(int[][] mainGrid, int[] winCountReel, int count, int[] topReel,
                                         int lastTopWinCount, boolean horizontalFortuneTriggered, int[][] lockMain, int[] lockTop) {
        List<int[]> positions = new ArrayList<>();
        for (int i = 0; i < MAX_REEL; i++) {
            for (int j = 0; j < winCountReel[i]; j++) {
                if (mainGrid[i][j] != SCATTER && lockMain[i][j] == 0) {
                    positions.add(new int[]{i, j});
                }
            }
        }
        if (!horizontalFortuneTriggered) {
            for (int i = 1; i <= lastTopWinCount; i++) {
                int topCanChangeIndex = topReel.length - i;
                if (topReel[topCanChangeIndex] != SCATTER && lockTop[topCanChangeIndex] == 0) {
                    positions.add(new int[]{-1, topCanChangeIndex});
                }
            }
        }
        
        Collections.shuffle(positions);
        for (int i = 0; i < Math.min(count, positions.size()); i++) {
            int[] pos = positions.get(i);
            if (pos[0] == -1) {
                topReel[pos[1]] = SCATTER;
            } else {
                mainGrid[pos[0]][pos[1]] = SCATTER;
            }
        }
    }
    
    private void gridSettingLeftSc(Round round, int scLeft, int roundCount) {
        int[][] mainGrid = round.getGrid();
        int[] winCountReel = round.getWinCountPerReel();
        int[] topReel = getTopReel(round);
        int lastTopWinCount = getLastTopReelWinCount(round);
        boolean horizontalFortuneTriggered = isHorizontalFortuneTriggered(round);
        
        List<int[]> positions = new ArrayList<>();
        
        if (roundCount == 0) {
            Set<Integer> msPositions = new HashSet<>();
            for (MultiSizeSymbol ms : getMultiSizeSymbols(round)) {
                int reel = ms.getReel();
                int row = ms.getStartRow();
                int size = ms.getSize();
                for (int i = 0; i < size; i++) {
                    msPositions.add(reel * MAIN_GRID_SIZE[reel] + row + i);
                }
            }
            
            for (int i = 0; i < MAX_REEL; i++) {
                for (int j = 0; j < MAIN_GRID_SIZE[i]; j++) {
                    if (mainGrid[i][j] != SCATTER && !msPositions.contains(i * MAIN_GRID_SIZE[i] + j)) {
                        positions.add(new int[]{i, j});
                    }
                }
            }
        } else {
            for (int i = 0; i < MAX_REEL; i++) {
                for (int j = 0; j < winCountReel[i]; j++) {
                    if (mainGrid[i][j] != SCATTER) {
                        positions.add(new int[]{i, j});
                    }
                }
            }
        }
        
        if (!horizontalFortuneTriggered) {
            for (int i = 1; i <= lastTopWinCount; i++) {
                int topCanChangeIndex = topReel.length - i;
                positions.add(new int[]{-1, topCanChangeIndex});
            }
        }
        
        Collections.shuffle(positions);
        int addedSC = 0;
        for (int i = 0; i < Math.min(scLeft, positions.size()); i++) {
            int[] pos = positions.get(i);
            if (pos[0] == -1) {
                topReel[pos[1]] = SCATTER;
            } else {
                mainGrid[pos[0]][pos[1]] = SCATTER;
            }
            addedSC++;
        }
        
        setScSet(round, getScSet(round) + addedSC);
    }
    
    private int countScatterInGrid(int[][] mainGrid, int[] topReel, List<MultiSizeSymbol> multiSymbols) {
        int count = 0;
        Set<Integer> countedPositions = new HashSet<>();
        
        // 檢查多格符號中的 Scatter（只算一次）
        for (MultiSizeSymbol ms : multiSymbols) {
            if (ms.getSymbol() == SCATTER) {
                count++;
                for (int i = 0; i < ms.getSize(); i++) {
                    countedPositions.add(ms.getReel() * 100 + ms.getStartRow() + i);
                }
            }
        }
        
        // 主盤面
        for (int i = 0; i < MAX_REEL; i++) {
            for (int j = 0; j < MAIN_GRID_SIZE[i]; j++) {
                if (mainGrid[i][j] == SCATTER && !countedPositions.contains(i * 100 + j)) {
                    count++;
                }
            }
        }
        
        // 附加軸
        for (int i = 1; i <= 4; i++) {
            if (topReel[i] == SCATTER) count++;
        }
        
        return count;
    }
    
    // ========================================
    // 中獎計算
    // ========================================
    
    private List<GameAward> calculateWaysAward(long lineBet, int[][] mainGrid, int[] topReel,
                                                List<MultiSizeSymbol> multiSymbols, int multiplier,
                                                Set<Integer> totalWinPos, Set<Integer> totalTopWinPos,
                                                boolean horizontalFortuneTriggered) {
        List<GameAward> awards = new ArrayList<>();
        
        for (int symbol = 0; symbol <= 10; symbol++) {
            Set<Integer> winPos = new HashSet<>();
            Set<Integer> topWinPos = new HashSet<>();
            
            int reelCount = 0;
            int ways = 1;
            
            for (int reel = 0; reel < MAX_REEL; reel++) {
                int count = countSymbolInReel(reel, symbol, mainGrid, topReel, multiSymbols,
                        winPos, topWinPos, horizontalFortuneTriggered);
                
                if (count == 0) break;
                reelCount++;
                ways *= count;
            }
            
            if (reelCount >= 3 && PAY_TABLE[symbol][reelCount] > 0) {
                long win = lineBet * PAY_TABLE[symbol][reelCount] * ways * multiplier;
                awards.add(new GameAward(symbol, reelCount, ways, lineBet, PAY_TABLE[symbol][reelCount], multiplier, win));
                totalWinPos.addAll(winPos);
                totalTopWinPos.addAll(topWinPos);
            }
        }
        
        return awards;
    }
    
    private int countSymbolInReel(int reel, int targetSymbol, int[][] mainGrid, int[] topReel,
                                   List<MultiSizeSymbol> multiSymbols, Set<Integer> winPos,
                                   Set<Integer> topWinPos, boolean horizontalFortuneTriggered) {
        int count = 0;
        Set<MultiSizeSymbol> countedMultiSymbols = new HashSet<>();
        
        for (int row = 0; row < MAIN_GRID_SIZE[reel]; row++) {
            int symbol = mainGrid[reel][row];
            if (symbol == targetSymbol || symbol == WILD) {
                MultiSizeSymbol multi = findMultiSymbolAt(reel, row, multiSymbols);
                if (multi != null) {
                    if (!countedMultiSymbols.contains(multi)) {
                        countedMultiSymbols.add(multi);
                        count++;
                    }
                    winPos.add(reel * MAIN_GRID_SIZE[reel] + row);
                } else {
                    count++;
                    winPos.add(reel * MAIN_GRID_SIZE[reel] + row);
                }
            }
        }
        
        // 附加軸
        if (reel >= 1 && reel <= 4) {
            int topSymbol = topReel[reel];
            if (topSymbol == targetSymbol || topSymbol == WILD || 
                (horizontalFortuneTriggered && topSymbol == HORIZONTAL_FORTUNE)) {
                topWinPos.add(reel);
                count++;
            }
        }
        
        return count;
    }
    
    private MultiSizeSymbol findMultiSymbolAt(int reel, int row, List<MultiSizeSymbol> multiSymbols) {
        for (MultiSizeSymbol multi : multiSymbols) {
            if (multi.containsPosition(reel, row)) return multi;
        }
        return null;
    }
    
    private void setWinGrid(Round round, Set<Integer> totalWinPos, Set<Integer> totalTopWinPos) {
        int[] topReelWinPos = new int[MAX_REEL];
        int[][] winGrid = initWinGrid();
        
        for (int pos : totalWinPos) {
            int reel = pos / MAIN_GRID_SIZE[0];
            int row = pos % MAIN_GRID_SIZE[0];
            winGrid[reel][row] = 1;
        }
        
        for (int pos : totalTopWinPos) {
            topReelWinPos[pos] = 1;
        }
        
        round.setWinGrid(winGrid);
        setTopReelWinPos(round, topReelWinPos);
    }
    
    // ========================================
    // 輔助方法
    // ========================================
    
    private int getScCount(GameState gameState, PlayMode playMode, int prob) {
        Map<Integer, int[]> weights;
        if (gameState == GameState.BASE_GAME) {
            weights = (playMode == PlayMode.NORMAL) ? SC_WEIGHT_N_BASE : SC_WEIGHT_BF_BASE;
        } else {
            weights = (playMode == PlayMode.NORMAL) ? SC_WEIGHT_N_FREE : SC_WEIGHT_BF_FREE;
        }
        int[] weight = weights.getOrDefault(prob, weights.get(DEFAULT_PROB));
        return getIndexByWeight(weight);
    }
    
    private int getWheelProbIndex(GameState gameState, PlayMode playMode, int prob) {
        Map<Integer, int[]> weights;
        if (playMode == PlayMode.NORMAL) {
            weights = (gameState == GameState.BASE_GAME) ? N_BASE_WEIGHT : N_FREE_WEIGHT;
        } else {
            weights = (gameState == GameState.BASE_GAME) ? BF_BASE_WEIGHT : BF_FREE_WEIGHT;
        }
        int[] weight = weights.getOrDefault(prob, weights.get(DEFAULT_PROB));
        return getIndexByWeight(weight);
    }
    
    private int getCascadeWheelIndex(GameState gameState, PlayMode playMode, int roundCount) {
        int[][] probs;
        if (playMode == PlayMode.NORMAL) {
            probs = (gameState == GameState.BASE_GAME) ? CASCADE_WHEEL_PROB_BASE : CASCADE_WHEEL_PROB_FREE;
        } else {
            probs = (gameState == GameState.BASE_GAME) ? CASCADE_WHEEL_PROB_BF_BASE : CASCADE_WHEEL_PROB_BF_FREE;
        }
        int idx = Math.min(roundCount, probs.length - 1);
        if (idx == 0) idx = 1;
        return getIndexByWeight(probs[idx]);
    }
    
    private int[][] getProbWheel(GameState gameState, int wheelIndex) {
        if (gameState == GameState.BASE_GAME) {
            return switch (wheelIndex) {
                case 0 -> BASE_WHEEL_0;
                case 1 -> BASE_WHEEL_1;
                case 2 -> BASE_WHEEL_2;
                case 3 -> BASE_WHEEL_3;
                default -> BASE_WHEEL_0;
            };
        } else {
            return switch (wheelIndex) {
                case 0 -> FREE_WHEEL_0;
                case 1 -> FREE_WHEEL_1;
                case 2 -> FREE_WHEEL_2;
                case 3 -> FREE_WHEEL_3;
                default -> FREE_WHEEL_0;
            };
        }
    }
    
    private int[][] getTopWheel(GameState gameState, int wheelIndex) {
        if (gameState == GameState.BASE_GAME) {
            return switch (wheelIndex) {
                case 0 -> BASE_TOP_WHEEL_0;
                case 1 -> BASE_TOP_WHEEL_1;
                case 2 -> BASE_TOP_WHEEL_2;
                case 3 -> BASE_TOP_WHEEL_3;
                default -> BASE_TOP_WHEEL_0;
            };
        } else {
            return switch (wheelIndex) {
                case 0 -> FREE_TOP_WHEEL_0;
                case 1 -> FREE_TOP_WHEEL_1;
                case 2 -> FREE_TOP_WHEEL_2;
                case 3 -> FREE_TOP_WHEEL_3;
                default -> FREE_TOP_WHEEL_0;
            };
        }
    }
    
    private boolean checkForceHorizontalFortune(GameState gameState, PlayMode playMode, int prob, int wheelProbIndex) {
        Map<Integer, int[]> probTable;
        if (playMode == PlayMode.NORMAL) {
            probTable = (gameState == GameState.BASE_GAME) ? HORIZONTAL_FORTUNE_PROB_BASE : HORIZONTAL_FORTUNE_PROB_FREE;
        } else {
            probTable = (gameState == GameState.BASE_GAME) ? HORIZONTAL_FORTUNE_PROB_BF_BASE : HORIZONTAL_FORTUNE_PROB_BF_FREE;
        }
        
        int[] probs = probTable.getOrDefault(prob, probTable.get(DEFAULT_PROB));
        if (probs == null || wheelProbIndex >= probs.length) return false;
        
        return random.nextInt(10000) < probs[wheelProbIndex];
    }
    
    private boolean shouldForceWin(long accumulatedWin, long totalBet, int remainingSpins, PlayMode playMode) {
        long threshold = (long)(totalBet * FREE_FORCE_WIN_MULTIPLIER_THRESHOLD);
        if (accumulatedWin >= threshold) return false;
        
        float[] forceWinProb = (playMode == PlayMode.NORMAL) ? FREE_FORCE_WIN_PROB_N : FREE_FORCE_WIN_PROB_BF;
        int probIndex = remainingSpins - 1;
        if (probIndex < 0 || probIndex >= forceWinProb.length) return false;
        
        return random.nextFloat() < forceWinProb[probIndex];
    }
    
    private int calculateNaturalScatterDistribution(int scRemaining, int roundCount) {
        if (scRemaining <= 0) return 0;
        
        int scThisRound = 0;
        double baseProb = switch (roundCount) {
            case 0 -> 0.72;
            case 1 -> 0.30;
            case 2 -> 0.25;
            default -> 0.20;
        };
        
        for (int i = 0; i < scRemaining; i++) {
            double adjustedProb = baseProb + (random.nextDouble() * 0.2 - 0.1);
            adjustedProb = Math.max(0.15, Math.min(0.90, adjustedProb));
            if (random.nextDouble() < adjustedProb) {
                scThisRound++;
            }
        }
        
        return scThisRound;
    }
    
    private int[][] initMainGrid() {
        int[][] grid = new int[MAX_REEL][];
        for (int i = 0; i < MAX_REEL; i++) {
            grid[i] = new int[MAIN_GRID_SIZE[i]];
        }
        return grid;
    }
    
    private int[][] initWinGrid() {
        int[][] grid = new int[MAX_REEL][];
        for (int i = 0; i < MAX_REEL; i++) {
            grid[i] = new int[MAIN_GRID_SIZE[i]];
        }
        return grid;
    }
    
    private int[][] deepCopyGrid(int[][] source) {
        if (source == null) return null;
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }
    
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
