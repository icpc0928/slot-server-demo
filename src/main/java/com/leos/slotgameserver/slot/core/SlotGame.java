package com.leos.slotgameserver.slot.core;

import com.leos.slotgameserver.slot.core.result.Round;
import com.leos.slotgameserver.slot.core.result.SpinResult;
import com.leos.slotgameserver.slot.engine.award.AwardCalculator;
import com.leos.slotgameserver.slot.engine.cascade.CascadeEngine;
import com.leos.slotgameserver.slot.engine.wheel.WheelEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * 老虎機遊戲抽象基類
 * 定義遊戲的主流程，具體遊戲繼承並實現差異化邏輯
 */
public abstract class SlotGame {
    
    protected final SlotConfig config;
    protected final WheelEngine wheelEngine;
    protected final AwardCalculator awardCalculator;
    protected final CascadeEngine cascadeEngine;
    
    protected SlotGame(SlotConfig config, 
                       WheelEngine wheelEngine,
                       AwardCalculator awardCalculator,
                       CascadeEngine cascadeEngine) {
        this.config = config;
        this.wheelEngine = wheelEngine;
        this.awardCalculator = awardCalculator;
        this.cascadeEngine = cascadeEngine;
    }
    
    /**
     * 取得遊戲配置
     */
    public SlotConfig getConfig() {
        return config;
    }
    
    /**
     * 計算總下注金額
     */
    public long calculateTotalBet(long lineBet, PlayMode playMode) {
        int multiplier = (playMode == PlayMode.BUY_FREE) ? config.getBuyFreeMultiplier() : 1;
        return lineBet * config.getBetTimes() * multiplier;
    }
    
    /**
     * 計算最大贏分限制
     */
    public long calculateMaxWin(long lineBet) {
        return lineBet * config.getBetTimes() * config.getMaxTotalWinLimit();
    }
    
    /**
     * 主入口：取得遊戲結果
     */
    public SpinResult spin(long lineBet, int prob, PlayMode playMode) {
        SpinResult result = new SpinResult();
        result.setLineBet(lineBet);
        result.setTotalBet(calculateTotalBet(lineBet, playMode));
        
        long maxWin = calculateMaxWin(lineBet);
        
        // === 主遊戲 ===
        List<Round> baseRounds = playBaseGame(lineBet, prob, playMode);
        result.setBaseRounds(baseRounds);
        
        // 計算主遊戲總贏分
        long totalBaseWin = baseRounds.stream()
                .mapToLong(Round::getTotalWin)
                .sum();
        
        // 加上特殊獎勵（如記分板等）
        totalBaseWin += calculateBaseGameBonus(baseRounds, lineBet, playMode);
        
        // 檢查最大贏分限制
        if (totalBaseWin > maxWin) {
            totalBaseWin = maxWin;
        }
        result.setTotalBaseWin(totalBaseWin);
        
        // 檢查是否觸發免費遊戲
        Round lastRound = baseRounds.get(baseRounds.size() - 1);
        boolean triggeredFree = lastRound.isFree();
        result.setTriggeredFree(triggeredFree);
        result.setFreeSpinCount(lastRound.getFreeSpinCount());
        
        // === 免費遊戲 ===
        long totalFreeWin = 0;
        if (triggeredFree && totalBaseWin < maxWin) {
            List<List<Round>> freeSpinRounds = playFreeGame(
                    lineBet, prob, playMode, 
                    lastRound.getFreeSpinCount(), 
                    lastRound
            );
            result.setFreeSpinRounds(freeSpinRounds);
            
            // 計算免費遊戲總贏分
            for (List<Round> spinRounds : freeSpinRounds) {
                totalFreeWin += spinRounds.stream()
                        .mapToLong(Round::getTotalWin)
                        .sum();
            }
            
            // 加上免費遊戲特殊獎勵
            totalFreeWin += calculateFreeGameBonus(freeSpinRounds, lineBet, playMode);
            
            // 檢查最大贏分限制
            if (totalBaseWin + totalFreeWin > maxWin) {
                totalFreeWin = maxWin - totalBaseWin;
            }
        }
        result.setTotalFreeWin(totalFreeWin);
        
        // 總贏分
        result.setTotalWin(totalBaseWin + totalFreeWin);
        
        // 後處理（供子類擴展）
        postProcess(result, lineBet, prob, playMode);
        
        return result;
    }
    
    /**
     * 執行主遊戲
     */
    protected List<Round> playBaseGame(long lineBet, int prob, PlayMode playMode) {
        List<Round> rounds = new ArrayList<>();
        GameState gameState = GameState.BASE_GAME;
        
        // 選擇輪帶
        int wheelIndex = wheelEngine.selectWheelIndex(gameState, playMode, prob);
        
        // 決定本次 Scatter 數量
        int scatterCount = determineScatterCount(gameState, playMode, prob);
        
        Round lastRound = null;
        int roundIndex = 0;
        
        do {
            Round round = playRound(
                    lineBet, gameState, playMode, 
                    wheelIndex, lastRound, roundIndex, 
                    prob, scatterCount
            );
            rounds.add(round);
            
            // 更新 Scatter 剩餘數
            scatterCount -= round.getScatterCount();
            
            lastRound = round;
            roundIndex++;
        } while (lastRound.isHasRespin());
        
        return rounds;
    }
    
    /**
     * 執行免費遊戲
     */
    protected List<List<Round>> playFreeGame(long lineBet, int prob, PlayMode playMode,
                                              int freeSpinCount, Round baseLastRound) {
        List<List<Round>> allFreeRounds = new ArrayList<>();
        GameState gameState = GameState.FREE_GAME;
        
        int remainingSpins = freeSpinCount;
        
        while (remainingSpins > 0) {
            List<Round> spinRounds = new ArrayList<>();
            
            // 選擇輪帶
            int wheelIndex = wheelEngine.selectWheelIndex(gameState, playMode, prob);
            
            // 決定本次 Scatter 數量
            int scatterCount = determineScatterCount(gameState, playMode, prob);
            
            Round lastRound = null;
            int roundIndex = 0;
            
            do {
                Round round = playRound(
                        lineBet, gameState, playMode,
                        wheelIndex, lastRound, roundIndex,
                        prob, scatterCount
                );
                spinRounds.add(round);
                
                scatterCount -= round.getScatterCount();
                
                // 免中免：增加次數
                if (round.isFree()) {
                    remainingSpins += round.getFreeSpinCount();
                }
                
                lastRound = round;
                roundIndex++;
            } while (lastRound.isHasRespin());
            
            allFreeRounds.add(spinRounds);
            remainingSpins--;
        }
        
        return allFreeRounds;
    }
    
    /**
     * 執行單一回合（消除）
     * 這是核心邏輯，子類可覆寫以實現特殊機制
     */
    protected Round playRound(long lineBet, GameState gameState, PlayMode playMode,
                              int wheelIndex, Round lastRound, int roundIndex,
                              int prob, int scatterNeeded) {
        Round round = new Round();
        round.setGameState(gameState);
        round.setRoundIndex(roundIndex);
        
        // 生成盤面
        int[][] grid;
        if (roundIndex == 0) {
            // 首消：從輪帶生成
            grid = wheelEngine.generateGrid(gameState, wheelIndex);
        } else {
            // 連消：掉落補牌
            grid = cascadeEngine.cascade(lastRound.getGrid(), lastRound.getWinGrid(), 
                                         gameState, wheelIndex, roundIndex);
        }
        
        // 放置 Scatter
        int placedScatter = placeScatters(grid, scatterNeeded, roundIndex);
        round.setScatterCount(placedScatter);
        
        // 子類處理特殊盤面邏輯
        processGridSpecial(grid, round, gameState, playMode, roundIndex, prob);
        
        round.setGrid(grid);
        
        // 計算獎項
        var awards = awardCalculator.calculate(lineBet, grid, config);
        round.setAwards(awards);
        
        // 計算總贏分
        long totalWin = awards.stream().mapToLong(a -> a.win()).sum();
        round.setTotalWin(totalWin);
        
        // 設置中獎位置
        int[][] winGrid = awardCalculator.getWinGrid();
        round.setWinGrid(winGrid);
        
        // 累積贏分
        long accWin = (lastRound != null ? lastRound.getAccWin() : 0) + totalWin;
        round.setAccWin(accWin);
        
        // 判斷是否繼續連消
        round.setHasRespin(totalWin > 0);
        
        // 最後一消判斷免費遊戲觸發
        if (!round.isHasRespin()) {
            checkFreeSpinTrigger(round, grid);
        }
        
        return round;
    }
    
    // ========================================
    // 可覆寫的鉤子方法
    // ========================================
    
    /**
     * 決定本次 Scatter 數量
     */
    protected abstract int determineScatterCount(GameState gameState, PlayMode playMode, int prob);
    
    /**
     * 放置 Scatter 到盤面
     */
    protected abstract int placeScatters(int[][] grid, int count, int roundIndex);
    
    /**
     * 處理特殊盤面邏輯（子類實現）
     */
    protected void processGridSpecial(int[][] grid, Round round, 
                                       GameState gameState, PlayMode playMode,
                                       int roundIndex, int prob) {
        // 預設不做任何事，子類覆寫
    }
    
    /**
     * 檢查免費遊戲觸發
     */
    protected void checkFreeSpinTrigger(Round round, int[][] grid) {
        int scatterCount = countScatterInGrid(grid);
        if (scatterCount >= config.getFreeSpinNeed()) {
            round.setFree(true);
            int extraScatters = scatterCount - config.getFreeSpinNeed();
            round.setFreeSpinCount(config.getBaseFreeSpinCount() + 
                                   extraScatters * config.getExtraFreePerScatter());
        }
    }
    
    /**
     * 計算盤面中的 Scatter 數量
     */
    protected int countScatterInGrid(int[][] grid) {
        int count = 0;
        for (int[] reel : grid) {
            for (int symbol : reel) {
                if (symbol == config.getScatterSymbol()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * 計算主遊戲額外獎勵（如記分板）
     */
    protected long calculateBaseGameBonus(List<Round> rounds, long lineBet, PlayMode playMode) {
        return 0; // 預設無額外獎勵
    }
    
    /**
     * 計算免費遊戲額外獎勵
     */
    protected long calculateFreeGameBonus(List<List<Round>> freeRounds, long lineBet, PlayMode playMode) {
        return 0; // 預設無額外獎勵
    }
    
    /**
     * 後處理（設置額外結果資料）
     */
    protected void postProcess(SpinResult result, long lineBet, int prob, PlayMode playMode) {
        // 預設不做任何事，子類覆寫
    }
}
