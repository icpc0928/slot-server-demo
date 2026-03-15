package com.leos.slotgameserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameResult {
    private List<String> symbols;  // 轉出的符號 ["🍒", "🍒", "🍒"]
    private boolean win;           // 是否中獎
    private int winAmount;         // 中獎金額
    private String message;        // 訊息
}
