package com.leo.slotserver.engine;

import com.leo.slotserver.exception.GameNotFoundException;
import com.leo.slotserver.model.GameConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 遊戲引擎工廠 — Factory Pattern + Classpath Scanning
 * <p>
 * 啟動時自動掃描所有標記 {@link SlotGame} 註解的引擎類別，
 * 並建立 gameId → engineClass 的映射。
 * <p>
 * 當 GameConfig 載入後，呼叫 register() 透過反射建立引擎實例。
 * 新增遊戲只需加 @SlotGame 註解 + YAML 設定檔，不修改此類別 (OCP)。
 * </p>
 */
@Component
public class SlotEngineFactory {

    private static final Logger log = LoggerFactory.getLogger(SlotEngineFactory.class);

    private final Map<String, SlotEngine> engines = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends SlotEngine>> engineClassMap = new ConcurrentHashMap<>();

    /**
     * 啟動時掃描 classpath，找出所有標記 @SlotGame 的類別
     */
    @PostConstruct
    public void scanEngines() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SlotGame.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents("com.leo.slotserver.engine");

        for (BeanDefinition bd : candidates) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                SlotGame annotation = clazz.getAnnotation(SlotGame.class);

                if (annotation != null && SlotEngine.class.isAssignableFrom(clazz)) {
                    String gameId = annotation.value();
                    @SuppressWarnings("unchecked")
                    Class<? extends SlotEngine> engineClass = (Class<? extends SlotEngine>) clazz;
                    engineClassMap.put(gameId, engineClass);
                    log.info("Scanned engine: {} -> {}", gameId, clazz.getSimpleName());
                }
            } catch (ClassNotFoundException e) {
                log.error("Failed to load engine class: {}", bd.getBeanClassName(), e);
            }
        }

        log.info("Total engines scanned: {}", engineClassMap.size());
    }

    /**
     * 註冊遊戲引擎（載入 GameConfig 後呼叫）
     */
    public void register(GameConfig config) {
        SlotEngine engine = createEngine(config);
        engines.put(config.getGameId(), engine);
    }

    /**
     * 取得遊戲引擎
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
     * 透過反射建立引擎實例 — 根據 @SlotGame 自動對應，不需 switch-case
     */
    private SlotEngine createEngine(GameConfig config) {
        String gameId = config.getGameId();
        Class<? extends SlotEngine> engineClass = engineClassMap.get(gameId);

        if (engineClass == null) {
            throw new GameNotFoundException(
                    "No engine found for game: " + gameId +
                    ". Ensure the engine class has @SlotGame(\"" + gameId + "\") annotation.");
        }

        try {
            Constructor<? extends SlotEngine> constructor = engineClass.getConstructor(GameConfig.class);
            return constructor.newInstance(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create engine for game: " + gameId, e);
        }
    }
}
