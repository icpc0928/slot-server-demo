package com.leo.slotserver.dto;

import com.leo.slotserver.model.GameConfig;
import lombok.Builder;
import lombok.Data;

/**
 * 遊戲資訊 DTO — 前端用來顯示遊戲列表
 */
@Data
@Builder
public class GameInfoDTO {
    private String gameId;
    private String gameName;
    private int reels;
    private int rows;
    private String evalType;
    private int betMultiplier;
    private boolean hasCascade;
    private int maxWinCap;

    public static GameInfoDTO fromConfig(GameConfig config) {
        return GameInfoDTO.builder()
                .gameId(config.getGameId())
                .gameName(config.getGameName())
                .reels(config.getReels())
                .rows(config.getRows())
                .evalType(config.getEvalType().name())
                .betMultiplier(config.getBetMultiplier())
                .hasCascade(config.isHasCascade())
                .maxWinCap(config.getMaxWinCap())
                .build();
    }
}
