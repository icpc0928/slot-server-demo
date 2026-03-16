package com.leo.slotserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.leo.slotserver.engine.SlotEngineFactory;
import com.leo.slotserver.model.GameConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * 遊戲註冊設定 — 啟動時自動載入所有遊戲設定檔
 * <p>
 * 遊戲設定從 YAML 外部化，新增遊戲只需加設定檔 + 實作引擎，
 * 不需要修改此設定類別。
 * </p>
 */
@Component
public class GameRegistryConfig {

    private static final Logger log = LoggerFactory.getLogger(GameRegistryConfig.class);
    private final SlotEngineFactory engineFactory;

    public GameRegistryConfig(SlotEngineFactory engineFactory) {
        this.engineFactory = engineFactory;
    }

    @PostConstruct
    public void registerGames() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:games/*.yml");

        for (Resource resource : resources) {
            try {
                //讀取各款遊戲的config並做註冊
                GameConfig config = yamlMapper.readValue(resource.getInputStream(), GameConfig.class);
                engineFactory.register(config);
                log.info("✅ Registered game: {} ({})", config.getGameId(), config.getGameName());
            } catch (Exception e) {
                log.error("❌ Failed to load game config: {}", resource.getFilename(), e);
            }
        }

        log.info("📊 Total games registered: {}", engineFactory.getRegisteredGameIds().size());
    }
}
