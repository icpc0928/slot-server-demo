package com.leo.slotserver.engine;

import com.leo.slotserver.engine.fortunegods.FortuneGodsEngine;
import com.leo.slotserver.engine.gatesofolympus.GatesOfOlympusEngine;
import com.leo.slotserver.engine.superace.SuperAceEngine;
import com.leo.slotserver.exception.GameNotFoundException;
import com.leo.slotserver.model.GameConfig;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 遊戲引擎工廠 — Factory Pattern
 * <p>
 * 職責：根據 gameId 建立對應的遊戲引擎實例。
 * Controller 和 Service 不需要知道具體的引擎實作類別，
 * 只需要透過工廠取得 SlotEngine 介面 — 依賴反轉原則 (DIP)。
 * </p>
 * <p>
 * 新增遊戲只需：
 * 1. 實作 AbstractSlotEngine
 * 2. 在此工廠註冊
 * 不影響任何其他程式碼 — 開放封閉原則 (OCP)
 * </p>
 */
@Component
public class SlotEngineFactory {

    private final Map<String, SlotEngine> engines = new ConcurrentHashMap<>();

    /**
     * 註冊遊戲引擎
     */
    public void register(GameConfig config) {
        SlotEngine engine = createEngine(config);
        engines.put(config.getGameId(), engine);
    }

    /**
     * 取得遊戲引擎
     *
     * @throws GameNotFoundException 如果遊戲不存在
     */
    public SlotEngine getEngine(String gameId) {
        SlotEngine engine = engines.get(gameId);
        if (engine == null) {
            throw new GameNotFoundException("Game not found: " + gameId);
        }
        return engine;
    }

    /**
     * 取得所有已註冊的遊戲 ID
     */
    public Set<String> getRegisteredGameIds() {
        return Collections.unmodifiableSet(engines.keySet());
    }

    /**
     * 根據設定建立對應的引擎 — 內部工廠方法
     */
    private SlotEngine createEngine(GameConfig config) {
        return switch (config.getGameId()) {
            case "fortune-gods" -> new FortuneGodsEngine(config);
            case "super-ace" -> new SuperAceEngine(config);
            case "gates-of-olympus-1000" -> new GatesOfOlympusEngine(config);
            default -> throw new GameNotFoundException("Unknown game: " + config.getGameId());
        };
    }
}
