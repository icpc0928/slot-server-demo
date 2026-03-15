package com.leos.slotgameserver.slot.games.fortunegods;

import lombok.Data;

/**
 * 多格符號
 * 在第2-5軸上，符號可能佔用 2-4 格
 */
@Data
public class MultiSizeSymbol {
    private int reel;           // 軸編號 (1-4)
    private int startRow;       // 起始行
    private int size;           // 佔用格數 (2-4)
    private int symbol;         // 符號 ID
    
    // === 框框狀態 ===
    private boolean silverFrame;     // 銀框（中獎後變金框）
    private boolean goldFrame;       // 金框（中獎後變 WILD）
    private boolean convertedToWild; // 已轉換為 WILD
    
    // === 中獎狀態 ===
    private boolean winning;         // 本輪是否中獎
    
    public MultiSizeSymbol(int reel, int startRow, int size, int symbol) {
        this.reel = reel;
        this.startRow = startRow;
        this.size = size;
        this.symbol = symbol;
    }
    
    /**
     * 檢查指定位置是否在這個多格符號範圍內
     */
    public boolean containsPosition(int reel, int row) {
        if (this.reel != reel) return false;
        return row >= startRow && row < startRow + size;
    }
    
    /**
     * 深拷貝
     */
    public MultiSizeSymbol copy() {
        MultiSizeSymbol copy = new MultiSizeSymbol(reel, startRow, size, symbol);
        copy.silverFrame = this.silverFrame;
        copy.goldFrame = this.goldFrame;
        copy.convertedToWild = this.convertedToWild;
        copy.winning = this.winning;
        return copy;
    }
}
