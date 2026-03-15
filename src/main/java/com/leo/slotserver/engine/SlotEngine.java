package com.leo.slotserver.engine;

import com.leo.slotserver.model.GameConfig;
import com.leo.slotserver.model.RoundResult;
import com.leo.slotserver.model.SpinContext;

/**
 * 老虎機引擎介面 — Strategy Pattern
 * <p>
 * 每個遊戲實作此介面，提供統一的遊戲操作方式。
 * 新增遊戲只需實作此介面，不需修改任何現有程式碼 (OCP)。
 * </p>
 */
public interface SlotEngine {

    /**
     * 取得遊戲 ID
     */
    String getGameId();

    /**
     * 取得遊戲設定
     */
    GameConfig getConfig();

    /**
     * 執行一個完整回合（Base Spin + Cascade + Free Spin）
     *
     * @param betAmount 投注金額
     * @return 完整回合結果
     */
    RoundResult play(double betAmount);
}
