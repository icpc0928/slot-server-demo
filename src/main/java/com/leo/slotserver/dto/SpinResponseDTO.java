package com.leo.slotserver.dto;

import com.leo.slotserver.model.RoundResult;
import com.leo.slotserver.model.SpinResult;
import com.leo.slotserver.model.WinResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Spin 回應 DTO — 只暴露前端需要的資料
 * 內部的計算細節（如 SpinContext、lineBet）不會暴露
 */
@Data
@Builder
public class SpinResponseDTO {
    private String gameId;
    private double betAmount;
    private int[][] grid;                // 主盤面
    private List<WinInfo> wins;          // 中獎資訊
    private double baseWin;
    private double freeSpinWin;
    private double totalWin;
    private double winMultiplier;        // totalWin / betAmount
    private boolean freeSpinTriggered;
    private int totalFreeSpins;
    private int cascadeCount;
    private double balance;              // 剩餘餘額

    @Data
    @Builder
    public static class WinInfo {
        private String symbolName;
        private int matchCount;
        private int ways;
        private double payout;
        private int multiplier;
    }

    /**
     * 從 RoundResult 轉換 — 工廠方法
     */
    public static SpinResponseDTO fromRoundResult(RoundResult result, double balance) {
        List<WinInfo> winInfos = result.getBaseSpin().getWins().stream()
                .map(w -> WinInfo.builder()
                        .symbolName(w.getSymbolName())
                        .matchCount(w.getMatchCount())
                        .ways(w.getWays())
                        .payout(w.getPayout())
                        .multiplier(w.getMultiplier())
                        .build())
                .toList();

        return SpinResponseDTO.builder()
                .gameId(result.getGameId())
                .betAmount(result.getBetAmount())
                .grid(result.getBaseSpin().getGrid())
                .wins(winInfos)
                .baseWin(result.getBaseWin())
                .freeSpinWin(result.getFreeSpinWin())
                .totalWin(result.getTotalWin())
                .winMultiplier(result.getWinMultiplier())
                .freeSpinTriggered(result.isFreeSpinTriggered())
                .totalFreeSpins(result.getTotalFreeSpins())
                .cascadeCount(result.getCascadeSpins() != null ? result.getCascadeSpins().size() : 0)
                .balance(balance)
                .build();
    }
}
