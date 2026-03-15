package com.leo.slotserver.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 單次中獎結果
 */
@Data
@Builder
public class WinResult {
    private int symbolId;
    private String symbolName;
    private int matchCount;
    private int ways;              // Ways 計算時的路數
    private double payout;         // 實際贏分
    private int multiplier;        // 倍率
    private List<int[]> positions; // 中獎位置 [reel, row]
}
