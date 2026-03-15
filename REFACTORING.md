# 重構記錄：消除 Factory 的 switch-case

## 📅 日期
2026-03-15

## 🎯 目標
消除 `SlotEngineFactory` 中的 switch-case，改用 Spring 自動掃描機制，完全符合開放封閉原則 (OCP)。

## 🔧 實作方案

### 方案選擇
採用**自定義註解 + Spring IoC 自動掃描**方案，原因：
1. **最符合 Spring 設計理念** — 充分利用 Spring 的依賴注入和自動掃描
2. **擴展性最佳** — 新增遊戲完全不需修改 Factory 程式碼
3. **面試友善** — 展示註解、反射、設計模式的綜合運用
4. **可維護性高** — gameId 與引擎類別的對應關係就在類別宣告上，一目了然

### 核心變更

#### 1. 新增 `@SlotGame` 註解
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface SlotGame {
    String value(); // gameId
}
```

#### 2. 重構 `SlotEngineFactory`
**Before:**
```java
private SlotEngine createEngine(GameConfig config) {
    return switch (config.getGameId()) {
        case "fortune-gods" -> new FortuneGodsEngine(config);
        case "super-ace" -> new SuperAceEngine(config);
        case "gates-of-olympus-1000" -> new GatesOfOlympusEngine(config);
        default -> throw new GameNotFoundException("Unknown game: " + config.getGameId());
    };
}
```

**After:**
```java
@PostConstruct
public void scanEngines() {
    Map<String, Object> beans = applicationContext.getBeansWithAnnotation(SlotGame.class);
    for (Object bean : beans.values()) {
        Class<?> clazz = bean.getClass();
        SlotGame annotation = clazz.getAnnotation(SlotGame.class);
        if (annotation != null && SlotEngine.class.isAssignableFrom(clazz)) {
            engineClassMap.put(annotation.value(), engineClass);
        }
    }
}

private SlotEngine createEngine(GameConfig config) {
    Class<? extends SlotEngine> engineClass = engineClassMap.get(config.getGameId());
    Constructor<? extends SlotEngine> constructor = engineClass.getConstructor(GameConfig.class);
    return constructor.newInstance(config);
}
```

#### 3. 更新引擎類別
```java
// Before
public class FortuneGodsEngine extends AbstractSlotEngine { ... }

// After
@SlotGame("fortune-gods")
public class FortuneGodsEngine extends AbstractSlotEngine { ... }
```

## ✅ 驗證
```bash
./gradlew clean compileJava --no-daemon
# BUILD SUCCESSFUL in 9s
```

## 📊 改進效果

| 指標 | Before | After | 改進 |
|------|--------|-------|------|
| **新增遊戲需修改的檔案** | Factory + Engine + Config | Engine + Config | -1 |
| **switch-case 數量** | 1 | 0 | -100% |
| **符合 OCP 程度** | 部分 | 完全 | ✅ |
| **gameId 維護位置** | Factory switch | Engine 註解 | 更內聚 |

## 🎓 技術亮點（面試可說）

### 1. **設計模式應用**
- **Factory Pattern** — 封裝物件建立邏輯
- **Template Method** — AbstractSlotEngine 定義遊戲流程骨架
- **Strategy Pattern** — 不同 Evaluator 策略（Ways/Lines/ScatterPay）

### 2. **SOLID 原則**
- **開放封閉原則 (OCP)** — 對擴展開放，對修改封閉
- **依賴反轉原則 (DIP)** — 依賴 SlotEngine 介面，不依賴具體實作
- **單一職責原則 (SRP)** — Factory 只負責建立，Engine 只負責遊戲邏輯

### 3. **Spring 框架特性**
- **IoC 容器** — ApplicationContext 管理所有引擎
- **註解驅動開發** — 自定義 @SlotGame 註解
- **自動掃描** — @PostConstruct 初始化時自動掃描

### 4. **反射機制**
- `getBeansWithAnnotation()` — 查找所有標記註解的 Bean
- `Constructor.newInstance()` — 動態建立引擎實例
- 平衡了靈活性與效能（僅啟動時掃描一次）

## 🚀 未來擴展
新增遊戲只需三步驟：
1. 繼承 `AbstractSlotEngine` 實作遊戲邏輯
2. 加上 `@SlotGame("new-game-id")` 註解
3. 建立對應的 YAML 設定檔

完全不需修改 Factory 或任何現有程式碼！

## 🔗 Commit
- Hash: `93b70d4`
- GitHub: https://github.com/icpc0928/slot-server-demo/commit/93b70d4
