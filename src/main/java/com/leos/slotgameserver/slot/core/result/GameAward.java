package com.leos.slotgameserver.slot.core.result;

/**
 * 單一獎項結果
 */
public record GameAward(
    int symbol,         // 中獎符號
    int reelCount,      // 連續軸數
    int ways,           // Ways 數
    long lineBet,       // 線注
    int payTable,       // 賠付表倍數
    int multiplier,     // 額外倍數
    long win            // 贏分
) {
    // 簡化建構子（無額外倍數）
    public GameAward(int symbol, int reelCount, int ways, long lineBet, int payTable, long win) {
        this(symbol, reelCount, ways, lineBet, payTable, 1, win);
    }
}
