package com.leo.slotserver.service;

import com.leo.slotserver.engine.SlotEngineFactory;
import com.leo.slotserver.exception.GameNotFoundException;
import com.leo.slotserver.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GameService 單元測試
 */
class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        // 用空的 ApplicationContext，因為這裡只測 Service 邏輯
        GenericApplicationContext context = new GenericApplicationContext();
        context.refresh();
        SlotEngineFactory factory = new SlotEngineFactory(context);
        gameService = new GameService(factory);
    }

    @Test
    @DisplayName("不存在的遊戲應拋出 GameNotFoundException")
    void unknownGame_shouldThrow() {
        assertThrows(GameNotFoundException.class, () ->
                gameService.spin("non-existent-game", 10.0, "player1"));
    }

    @Test
    @DisplayName("餘額不足應拋出 InsufficientBalanceException")
    void insufficientBalance_shouldThrow() {
        assertThrows(InsufficientBalanceException.class, () ->
                gameService.spin("any-game", 999999.0, "player1"));
    }

    @Test
    @DisplayName("預設餘額應為 10000")
    void defaultBalance_shouldBe10000() {
        double balance = gameService.getBalance("new-player");
        assertEquals(10000.0, balance);
    }
}
