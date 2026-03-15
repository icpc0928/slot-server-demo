package com.leos.slotgameserver.slot.games.fortunegods;

import com.leos.slotgameserver.slot.core.result.Round;

import java.util.ArrayList;
import java.util.List;

/**
 * Fortune Gods 專用的 Round 資料存取 Helper
 */
public class FortuneGodsRoundHelper {
    
    private static final String TOP_REEL = "topReel";
    private static final String TOP_REEL_WIN_POS = "topReelWinPos";
    private static final String MULTI_SIZE_SYMBOLS = "multiSizeSymbols";
    private static final String HORIZONTAL_FORTUNE_TRIGGERED = "horizontalFortuneTriggered";
    private static final String GRADUAL_HORIZONTAL_FORTUNE_MODE = "gradualHorizontalFortuneMode";
    private static final String LAST_TOP_REEL_WIN_COUNT = "lastTopReelWinCount";
    private static final String CURRENT_MULTIPLIER = "currentMultiplier";
    private static final String CONVERTED_TO_WILD = "convertedToWild";
    private static final String SC_SET = "scSet";
    
    // === 附加軸 ===
    
    public static void setTopReel(Round round, int[] topReel) {
        round.getExtra().put(TOP_REEL, topReel);
    }
    
    public static int[] getTopReel(Round round) {
        Object value = round.getExtra().get(TOP_REEL);
        return value != null ? (int[]) value : null;
    }
    
    public static void setTopReelWinPos(Round round, int[] winPos) {
        round.getExtra().put(TOP_REEL_WIN_POS, winPos);
    }
    
    public static int[] getTopReelWinPos(Round round) {
        Object value = round.getExtra().get(TOP_REEL_WIN_POS);
        return value != null ? (int[]) value : null;
    }
    
    // === 多格符號 ===
    
    @SuppressWarnings("unchecked")
    public static void setMultiSizeSymbols(Round round, List<MultiSizeSymbol> symbols) {
        round.getExtra().put(MULTI_SIZE_SYMBOLS, symbols);
    }
    
    @SuppressWarnings("unchecked")
    public static List<MultiSizeSymbol> getMultiSizeSymbols(Round round) {
        Object value = round.getExtra().get(MULTI_SIZE_SYMBOLS);
        return value != null ? (List<MultiSizeSymbol>) value : new ArrayList<>();
    }
    
    // === 橫財神 ===
    
    public static void setHorizontalFortuneTriggered(Round round, boolean triggered) {
        round.getExtra().put(HORIZONTAL_FORTUNE_TRIGGERED, triggered);
    }
    
    public static boolean isHorizontalFortuneTriggered(Round round) {
        Object value = round.getExtra().get(HORIZONTAL_FORTUNE_TRIGGERED);
        return value != null && (boolean) value;
    }
    
    public static void setGradualHorizontalFortuneMode(Round round, boolean mode) {
        round.getExtra().put(GRADUAL_HORIZONTAL_FORTUNE_MODE, mode);
    }
    
    public static boolean isGradualHorizontalFortuneMode(Round round) {
        Object value = round.getExtra().get(GRADUAL_HORIZONTAL_FORTUNE_MODE);
        return value != null && (boolean) value;
    }
    
    // === 其他狀態 ===
    
    public static void setLastTopReelWinCount(Round round, int count) {
        round.getExtra().put(LAST_TOP_REEL_WIN_COUNT, count);
    }
    
    public static int getLastTopReelWinCount(Round round) {
        Object value = round.getExtra().get(LAST_TOP_REEL_WIN_COUNT);
        return value != null ? (int) value : 0;
    }
    
    public static void setCurrentMultiplier(Round round, int multiplier) {
        round.getExtra().put(CURRENT_MULTIPLIER, multiplier);
    }
    
    public static int getCurrentMultiplier(Round round) {
        Object value = round.getExtra().get(CURRENT_MULTIPLIER);
        return value != null ? (int) value : 1;
    }
    
    public static void setConvertedToWild(Round round, boolean converted) {
        round.getExtra().put(CONVERTED_TO_WILD, converted);
    }
    
    public static boolean isConvertedToWild(Round round) {
        Object value = round.getExtra().get(CONVERTED_TO_WILD);
        return value != null && (boolean) value;
    }
    
    public static void setScSet(Round round, int scSet) {
        round.getExtra().put(SC_SET, scSet);
    }
    
    public static int getScSet(Round round) {
        Object value = round.getExtra().get(SC_SET);
        return value != null ? (int) value : 0;
    }
}
