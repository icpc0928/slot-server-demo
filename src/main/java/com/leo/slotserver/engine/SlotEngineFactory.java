package com.leo.slotserver.engine;

import com.leo.slotserver.exception.GameNotFoundException;
import com.leo.slotserver.model.GameConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 遊戲引擎工廠 — Factory Pattern + Spring IoC
 * <p>
 * 職責：根據 gameId 建立對應的遊戲引擎實例。
 * Controller 和 Service 不需要知道具體的引擎實作類別，
 * 只需要透過工廠取得 SlotEngine 介面 — 依賴反轉原則 (DIP)。
 * </p>
 * <p>
 * 自動掃描機制：
 * 啟動時自動掃描所有標記 {@link SlotGame} 註解的引擎類別並註冊。
 * 新增遊戲只需：
 * 1. 繼承 AbstractSlotEngine
 * 2. 加上 @SlotGame("game-id") 註解
 * 3. 建立對應的 YAML 設定檔
 * 不需修改此工廠任何程式碼 — 開放封閉原則 (OCP)
 * </p>
 */
@Component
public class SlotEngineFactory {

    private static final Logger log = LoggerFactory.getLogger(SlotEngineFactory.class);
    
    private final ApplicationContext applicationContext;
    private final Map<String, SlotEngine> engines = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends SlotEngine>> engineClassMap = new ConcurrentHashMap<>();

    public SlotEngineFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 啟動時自動掃描所有標記 @SlotGame 的引擎類別
     */
    @PostConstruct
    public void scanEngines() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(SlotGame.class);
        
        for (Object bean : beans.values()) {
            Class<?> clazz = bean.getClass();
            SlotGame annotation = clazz.getAnnotation(SlotGame.class);
            
            if (annotation != null && SlotEngine.class.isAssignableFrom(clazz)) {
                String gameId = annotation.value();
                @SuppressWarnings("unchecked")
                Class<? extends SlotEngine> engineClass = (Class<? extends SlotEngine>) clazz;
                engineClassMap.put(gameId, engineClass);
                log.info("🎰 Scanned engine: {} -> {}", gameId, clazz.getSimpleName());
            }
        }
        
        log.info("Total engines scanned: {}", engineClassMap.size());
    }

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
     * 根據設定建立對應的引擎 — 透過反射自動建立實例
     * <p>
     * 不再使用 switch-case！
     * 透過 @SlotGame 註解自動對應 gameId 和引擎類別
     * </p>
     */
    private SlotEngine createEngine(GameConfig config) {
        String gameId = config.getGameId();
        Class<? extends SlotEngine> engineClass = engineClassMap.get(gameId);
        
        if (engineClass == null) {
            throw new GameNotFoundException("Unknown game: " + gameId + " (no engine class found)");
        }
        
        try {
            // 透過反射建立實例（建構子接受 GameConfig）
            Constructor<? extends SlotEngine> constructor = engineClass.getConstructor(GameConfig.class);
            return constructor.newInstance(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create engine for game: " + gameId, e);
        }
    }
}
