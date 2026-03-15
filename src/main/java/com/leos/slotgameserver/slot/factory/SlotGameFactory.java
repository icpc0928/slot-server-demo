package com.leos.slotgameserver.slot.factory;

import com.leos.slotgameserver.slot.games.knockoutriches.KnockoutRichesGame;
import com.leos.slotgameserver.slot.games.fortunegods.FortuneGodsGame;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 老虎機遊戲工廠
 * 根據 gameId 創建對應的遊戲實例
 */
@Component
public class SlotGameFactory {
    
    private final Map<Integer, Supplier<Object>> gameSuppliers = new HashMap<>();
    
    public SlotGameFactory() {
        // 註冊遊戲
        gameSuppliers.put(1001, KnockoutRichesGame::new);
        gameSuppliers.put(1002, FortuneGodsGame::new);
    }
    
    /**
     * 創建 KnockoutRiches 遊戲實例
     */
    public KnockoutRichesGame createKnockoutRiches() {
        return new KnockoutRichesGame();
    }
    
    /**
     * 創建 FortuneGods 遊戲實例
     */
    public FortuneGodsGame createFortuneGods() {
        return new FortuneGodsGame();
    }
    
    /**
     * 創建遊戲實例（通用）
     */
    public Object createGame(int gameId) {
        Supplier<Object> supplier = gameSuppliers.get(gameId);
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown game ID: " + gameId);
        }
        return supplier.get();
    }
    
    /**
     * 檢查遊戲是否已註冊
     */
    public boolean hasGame(int gameId) {
        return gameSuppliers.containsKey(gameId);
    }
    
    /**
     * 取得所有已註冊的遊戲 ID
     */
    public Set<Integer> getRegisteredGameIds() {
        return gameSuppliers.keySet();
    }
}
