package com.leos.slotgameserver.controller;

import com.leos.slotgameserver.model.GameResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private static final List<String> SYMBOLS = List.of("🍒", "🍋", "🍊", "🍇", "⭐", "💎", "7️⃣");
    private final Random random = new Random();

    /**
     * 老虎機轉一次
     */
    @PostMapping("/spin")
    public GameResult spin(@RequestParam(defaultValue = "10") int bet) {
        // 隨機產生 3 個符號
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            result.add(SYMBOLS.get(random.nextInt(SYMBOLS.size())));
        }

        // 判斷是否中獎
        boolean win = false;
        int winAmount = 0;
        String message;

        if (result.get(0).equals(result.get(1)) && result.get(1).equals(result.get(2))) {
            // 三個一樣 - 大獎
            win = true;
            winAmount = bet * getMultiplier(result.get(0));
            message = "🎉 JACKPOT! 三個 " + result.get(0);
        } else if (result.get(0).equals(result.get(1)) || result.get(1).equals(result.get(2))) {
            // 兩個一樣 - 小獎
            win = true;
            winAmount = bet * 2;
            message = "👍 Nice! 兩個一樣";
        } else {
            message = "再試一次！";
        }

        return new GameResult(result, win, winAmount, message);
    }

    /**
     * 取得符號的倍率
     */
    private int getMultiplier(String symbol) {
        return switch (symbol) {
            case "7️⃣" -> 100;
            case "💎" -> 50;
            case "⭐" -> 20;
            case "🍇" -> 15;
            case "🍊" -> 10;
            case "🍋" -> 8;
            case "🍒" -> 5;
            default -> 5;
        };
    }

    /**
     * 健康檢查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "service", "slot-game-server",
            "timestamp", System.currentTimeMillis()
        );
    }
}
