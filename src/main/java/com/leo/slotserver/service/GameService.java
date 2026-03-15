package com.leo.slotserver.service;

import com.leo.slotserver.dto.GameInfoDTO;
import com.leo.slotserver.dto.SpinResponseDTO;
import com.leo.slotserver.engine.SlotEngine;
import com.leo.slotserver.engine.SlotEngineFactory;
import com.leo.slotserver.exception.InsufficientBalanceException;
import com.leo.slotserver.model.RoundResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 遊戲服務層 — 業務邏輯
 * <p>
 * 負責：餘額管理、呼叫引擎、組裝回應
 * 不負責：遊戲數學計算（那是 Engine 的事）
 * </p>
 */
@Service
public class GameService {

    private final SlotEngineFactory engineFactory;

    // 簡化版餘額管理 — 實際專案應用 DB
    private final ConcurrentHashMap<String, Double> balances = new ConcurrentHashMap<>();
    private static final double DEFAULT_BALANCE = 10000.0;

    public GameService(SlotEngineFactory engineFactory) {
        this.engineFactory = engineFactory;
    }

    /**
     * 執行一次旋轉
     */
    public SpinResponseDTO spin(String gameId, double betAmount, String playerId) {
        // 1. 檢查餘額
        double balance = getBalance(playerId);
        if (balance < betAmount) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance: %.2f < %.2f", balance, betAmount));
        }

        // 2. 扣款
        balance -= betAmount;

        // 3. 執行遊戲
        SlotEngine engine = engineFactory.getEngine(gameId);
        RoundResult result = engine.play(betAmount);

        // 4. 加入贏分
        balance += result.getTotalWin();
        balances.put(playerId, balance);

        // 5. 組裝回應
        return SpinResponseDTO.fromRoundResult(result, balance);
    }

    /**
     * 取得所有遊戲列表
     */
    public List<GameInfoDTO> listGames() {
        return engineFactory.getRegisteredGameIds().stream()
                .map(id -> {
                    SlotEngine engine = engineFactory.getEngine(id);
                    return GameInfoDTO.fromConfig(engine.getConfig());
                })
                .toList();
    }

    /**
     * 取得遊戲資訊
     */
    public GameInfoDTO getGameInfo(String gameId) {
        SlotEngine engine = engineFactory.getEngine(gameId);
        return GameInfoDTO.fromConfig(engine.getConfig());
    }

    /**
     * 取得餘額
     */
    public double getBalance(String playerId) {
        return balances.computeIfAbsent(playerId, k -> DEFAULT_BALANCE);
    }
}
