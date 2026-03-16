package com.leo.slotserver.dto;

import com.leo.slotserver.model.*;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Spin 回應 DTO — 只暴露前端需要的資料
 */
@Data
@Builder
public class SpinResponseDTO {
    private String gameId;
    private double betAmount;
    private int[][] grid;                    // 初始停輪盤面
    private List<WinInfo> wins;              // 初始停輪中獎
    private int cascadeCount;                // 主遊戲連消次數
    private double baseWin;
    private double freeSpinWin;
    private double totalWin;
    private double winMultiplier;
    private boolean freeSpinTriggered;
    private int totalFreeSpins;
    private double balance;

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
     * 從 RoundResult 轉換
     */
    public static SpinResponseDTO fromRoundResult(RoundResult result, double balance) {
        BaseResult base = result.getBaseResult();
        int[][] initialGrid = base.getInitialGrid();

        List<WinInfo> winInfos = List.of();
        if (!base.getRounds().isEmpty()) {
            winInfos = base.getRounds().get(0).getWins().stream()
                    .map(w -> WinInfo.builder()
                            .symbolName(w.getSymbolName())
                            .matchCount(w.getMatchCount())
                            .ways(w.getWays())
                            .payout(w.getPayout())
                            .multiplier(w.getMultiplier())
                            .build())
                    .toList();
        }

        return SpinResponseDTO.builder()
                .gameId(result.getGameId())
                .betAmount(result.getBetAmount())
                .grid(initialGrid)
                .wins(winInfos)
                .cascadeCount(base.getCascadeCount())
                .baseWin(result.getBaseWin())
                .freeSpinWin(result.getFreeSpinWin())
                .totalWin(result.getTotalWin())
                .winMultiplier(result.getWinMultiplier())
                .freeSpinTriggered(result.isFreeSpinTriggered())
                .totalFreeSpins(result.getTotalFreeSpins())
                .balance(balance)
                .build();
    }
}
