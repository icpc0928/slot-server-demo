package com.leo.slotserver.controller;

import com.leo.slotserver.dto.GameInfoDTO;
import com.leo.slotserver.dto.SpinRequestDTO;
import com.leo.slotserver.dto.SpinResponseDTO;
import com.leo.slotserver.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 遊戲 API Controller
 * <p>
 * 薄 Controller：只負責接收請求和回傳結果，
 * 所有業務邏輯在 Service 層，遊戲數學在 Engine 層。
 * </p>
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * 取得所有遊戲列表
     */
    @GetMapping
    public ResponseEntity<List<GameInfoDTO>> listGames() {
        return ResponseEntity.ok(gameService.listGames());
    }

    /**
     * 取得單一遊戲資訊
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameInfoDTO> getGame(@PathVariable String gameId) {
        return ResponseEntity.ok(gameService.getGameInfo(gameId));
    }

    /**
     * 執行旋轉
     */
    @PostMapping("/spin")
    public ResponseEntity<SpinResponseDTO> spin(@Valid @RequestBody SpinRequestDTO request) {
        String playerId = "default-player";
        SpinResponseDTO response = gameService.spin(
                request.getGameId(),
                request.getBetAmount(),
                playerId
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 取得玩家餘額
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Double>> getBalance() {
        double balance = gameService.getBalance("default-player");
        return ResponseEntity.ok(Map.of("balance", balance));
    }
}
