package com.leo.slotserver.dto;

import com.leo.slotserver.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTO 轉換測試
 */
class SpinResponseDTOTest {

    @Test
    @DisplayName("fromRoundResult 應正確轉換所有欄位")
    void fromRoundResult_shouldMapCorrectly() {
        int[][] grid = {{0, 1, 2}, {3, 4, 5}};
        WinResult win = WinResult.builder()
                .symbolId(0)
                .symbolName("A")
                .matchCount(3)
                .ways(4)
                .payout(20.0)
                .multiplier(1)
                .positions(List.of(new int[]{0, 0}))
                .build();

        CascadeRound round = CascadeRound.builder()
                .roundIndex(0)
                .grid(grid)
                .wins(List.of(win))
                .totalWin(20.0)
                .multiplier(1)
                .scatterCount(0)
                .hasNextCascade(false)
                .build();

        BaseResult base = BaseResult.builder()
                .rounds(List.of(round))
                .totalWin(20.0)
                .scatterCount(0)
                .freeSpinTriggered(false)
                .freeSpinCount(0)
                .build();

        RoundResult result = RoundResult.builder()
                .gameId("test")
                .betAmount(10.0)
                .baseResult(base)
                .freeSpinResults(List.of())
                .baseWin(20.0)
                .freeSpinWin(0)
                .totalWin(20.0)
                .freeSpinTriggered(false)
                .totalFreeSpins(0)
                .winMultiplier(2.0)
                .build();

        SpinResponseDTO dto = SpinResponseDTO.fromRoundResult(result, 9990.0);

        assertEquals("test", dto.getGameId());
        assertEquals(10.0, dto.getBetAmount());
        assertEquals(20.0, dto.getTotalWin());
        assertEquals(20.0, dto.getBaseWin());
        assertEquals(0, dto.getFreeSpinWin());
        assertEquals(9990.0, dto.getBalance());
        assertFalse(dto.isFreeSpinTriggered());
        assertEquals(0, dto.getCascadeCount());
        assertNotNull(dto.getGrid());
        assertEquals(1, dto.getWins().size());
        assertEquals("A", dto.getWins().get(0).getSymbolName());
        assertEquals(3, dto.getWins().get(0).getMatchCount());
        assertEquals(4, dto.getWins().get(0).getWays());
    }

    @Test
    @DisplayName("有連消的結果應顯示正確的 cascadeCount")
    void withCascade_shouldShowCount() {
        CascadeRound round0 = CascadeRound.builder()
                .roundIndex(0).grid(new int[][]{{0}}).wins(List.of())
                .totalWin(10.0).multiplier(1).scatterCount(0).hasNextCascade(true).build();
        CascadeRound round1 = CascadeRound.builder()
                .roundIndex(1).grid(new int[][]{{0}}).wins(List.of())
                .totalWin(20.0).multiplier(2).scatterCount(0).hasNextCascade(true).build();
        CascadeRound round2 = CascadeRound.builder()
                .roundIndex(2).grid(new int[][]{{0}}).wins(List.of())
                .totalWin(0).multiplier(3).scatterCount(0).hasNextCascade(false).build();

        BaseResult base = BaseResult.builder()
                .rounds(List.of(round0, round1, round2))
                .totalWin(30.0).scatterCount(0)
                .freeSpinTriggered(false).freeSpinCount(0).build();

        RoundResult result = RoundResult.builder()
                .gameId("test").betAmount(10.0).baseResult(base)
                .freeSpinResults(List.of()).baseWin(30.0).freeSpinWin(0)
                .totalWin(30.0).freeSpinTriggered(false).totalFreeSpins(0)
                .winMultiplier(3.0).build();

        SpinResponseDTO dto = SpinResponseDTO.fromRoundResult(result, 10000.0);

        assertEquals(2, dto.getCascadeCount(), "3 rounds - 1 = 2 cascades");
    }
}
