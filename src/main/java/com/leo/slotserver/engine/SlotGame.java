package com.leo.slotserver.engine;

import java.lang.annotation.*;

/**
 * 標記 Slot 引擎並指定支援的 gameId
 * <p>
 * 使用方式：
 * <pre>
 * @SlotGame("fortune-gods")
 * public class FortuneGodsEngine extends AbstractSlotEngine { ... }
 * </pre>
 * <p>
 * 工廠會自動掃描所有標記此註解的類別並註冊，
 * 新增遊戲不需修改工廠程式碼 — 符合開放封閉原則 (OCP)
 * </p>
 * <p>
 * 注意：不加 @Component，因為 Engine 不由 Spring 管理，
 * 而是由 SlotEngineFactory 透過反射建立（需要傳入 GameConfig）。
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SlotGame {
    /**
     * 遊戲 ID（必須與 YAML 設定檔中的 gameId 一致）
     */
    String value();
}
