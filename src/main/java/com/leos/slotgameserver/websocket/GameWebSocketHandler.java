package com.leos.slotgameserver.websocket;

import com.leos.slotgameserver.slot.core.PlayMode;
import com.leos.slotgameserver.slot.core.result.GameAward;
import com.leos.slotgameserver.slot.core.result.Round;
import com.leos.slotgameserver.slot.core.result.SpinResult;
import com.leos.slotgameserver.slot.games.fortunegods.FortuneGodsGame;
import com.leos.slotgameserver.slot.games.fortunegods.MultiSizeSymbol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.leos.slotgameserver.slot.games.fortunegods.FortuneGodsRoundHelper.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    
    // 儲存玩家 session 和餘額
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> balances = new ConcurrentHashMap<>();
    
    // 遊戲實例
    private final Map<String, FortuneGodsGame> fortuneGodsGames = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        balances.put(sessionId, 10000L);  // 初始餘額 10000
        
        log.info("玩家連線: {}", sessionId);
        
        // 發送歡迎訊息和初始餘額
        sendMessage(session, Map.of(
            "type", "connected",
            "sessionId", sessionId,
            "balance", 10000L,
            "message", "歡迎來到 Slot Game！"
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        JsonNode json = objectMapper.readTree(message.getPayload());
        String action = json.has("action") ? json.get("action").asText() : "";

        switch (action) {
            case "init" -> handleInit(session, sessionId, json);
            case "spin" -> handleSpin(session, sessionId, json);
            case "balance" -> handleBalance(session, sessionId);
            default -> sendMessage(session, Map.of(
                "type", "error",
                "message", "未知的操作: " + action
            ));
        }
    }

    private void handleInit(WebSocketSession session, String sessionId, JsonNode json) throws IOException {
        String gameId = json.has("gameId") ? json.get("gameId").asText() : "";
        
        if ("fortune-gods".equals(gameId)) {
            fortuneGodsGames.put(sessionId, new FortuneGodsGame());
            log.info("玩家 {} 初始化 Fortune Gods", sessionId);
        }
        
        sendMessage(session, Map.of(
            "type", "connected",
            "gameId", gameId,
            "balance", balances.getOrDefault(sessionId, 10000L),
            "message", "遊戲已載入！"
        ));
    }

    private void handleSpin(WebSocketSession session, String sessionId, JsonNode json) throws IOException {
        String gameId = json.has("gameId") ? json.get("gameId").asText() : "";
        
        if ("fortune-gods".equals(gameId)) {
            handleFortuneGodsSpin(session, sessionId, json);
        } else {
            sendMessage(session, Map.of(
                "type", "error",
                "message", "未知的遊戲: " + gameId
            ));
        }
    }

    private void handleFortuneGodsSpin(WebSocketSession session, String sessionId, JsonNode json) throws IOException {
        long lineBet = json.has("lineBet") ? json.get("lineBet").asLong() : 1L;
        String playModeStr = json.has("playMode") ? json.get("playMode").asText() : "NORMAL";
        PlayMode playMode = "BUY_FREE".equals(playModeStr) ? PlayMode.BUY_FREE : PlayMode.NORMAL;
        int prob = 96;
        
        long balance = balances.getOrDefault(sessionId, 0L);
        long totalBet = lineBet * 20 * (playMode == PlayMode.BUY_FREE ? 80 : 1);
        
        // 檢查餘額
        if (totalBet > balance) {
            sendMessage(session, Map.of(
                "type", "error",
                "message", "餘額不足！"
            ));
            return;
        }
        
        // 取得或建立遊戲實例
        FortuneGodsGame game = fortuneGodsGames.computeIfAbsent(sessionId, k -> new FortuneGodsGame());
        
        // 執行 spin
        SpinResult result = game.spin(lineBet, prob, playMode);
        
        // 更新餘額
        balance = balance - result.getTotalBet() + result.getTotalWin();
        balances.put(sessionId, balance);
        
        // 轉換結果為前端格式
        Map<String, Object> response = new HashMap<>();
        response.put("type", "spin_result");
        response.put("balance", balance);
        response.put("result", convertSpinResult(result));
        
        sendMessage(session, response);
    }

    private Map<String, Object> convertSpinResult(SpinResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("totalBet", result.getTotalBet());
        map.put("totalWin", result.getTotalWin());
        map.put("totalBaseWin", result.getTotalBaseWin());
        map.put("totalFreeWin", result.getTotalFreeWin());
        map.put("triggeredFree", result.isTriggeredFree());
        map.put("freeSpinCount", result.getFreeSpinCount());
        
        // 轉換 baseRounds
        List<Map<String, Object>> baseRounds = new ArrayList<>();
        if (result.getBaseRounds() != null) {
            for (Round round : result.getBaseRounds()) {
                baseRounds.add(convertRound(round));
            }
        }
        map.put("baseRounds", baseRounds);
        
        return map;
    }

    private Map<String, Object> convertRound(Round round) {
        Map<String, Object> map = new HashMap<>();
        map.put("grid", round.getGrid());
        map.put("winGrid", round.getWinGrid());
        map.put("totalWin", round.getTotalWin());
        map.put("accWin", round.getAccWin());
        map.put("hasRespin", round.isHasRespin());
        map.put("roundIndex", round.getRoundIndex());
        map.put("scatterCount", round.getScatterCount());
        map.put("free", round.isFree());
        map.put("freeSpinCount", round.getFreeSpinCount());
        
        // 附加軸
        int[] topReel = getTopReel(round);
        map.put("topReel", topReel != null ? topReel : new int[]{0, -1, -1, -1, -1, 0});
        
        // 附加軸中獎位置
        int[] topReelWinPos = getTopReelWinPos(round);
        map.put("topReelWinPos", topReelWinPos != null ? topReelWinPos : new int[6]);
        
        // 多格符號
        List<MultiSizeSymbol> multiSymbols = getMultiSizeSymbols(round);
        List<Map<String, Object>> multiSymbolsList = new ArrayList<>();
        if (multiSymbols != null) {
            for (MultiSizeSymbol ms : multiSymbols) {
                Map<String, Object> msMap = new HashMap<>();
                msMap.put("reel", ms.getReel());
                msMap.put("startRow", ms.getStartRow());
                msMap.put("size", ms.getSize());
                msMap.put("symbol", ms.getSymbol());
                msMap.put("winning", ms.isWinning());
                msMap.put("silverFrame", ms.isSilverFrame());
                msMap.put("goldFrame", ms.isGoldFrame());
                multiSymbolsList.add(msMap);
            }
        }
        map.put("multiSizeSymbols", multiSymbolsList);
        
        // 獎項
        List<Map<String, Object>> awards = new ArrayList<>();
        if (round.getAwards() != null) {
            for (GameAward award : round.getAwards()) {
                Map<String, Object> awardMap = new HashMap<>();
                awardMap.put("symbol", award.symbol());
                awardMap.put("reelCount", award.reelCount());
                awardMap.put("ways", award.ways());
                awardMap.put("pay", award.payTable());
                awardMap.put("multiplier", award.multiplier());
                awardMap.put("win", award.win());
                awards.add(awardMap);
            }
        }
        map.put("awards", awards);
        
        // 其他狀態
        map.put("horizontalFortuneTriggered", isHorizontalFortuneTriggered(round));
        map.put("gradualHorizontalFortuneMode", isGradualHorizontalFortuneMode(round));
        
        return map;
    }

    private void handleBalance(WebSocketSession session, String sessionId) throws IOException {
        sendMessage(session, Map.of(
            "type", "balance",
            "balance", balances.getOrDefault(sessionId, 0L)
        ));
    }

    private void sendMessage(WebSocketSession session, Object data) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        balances.remove(sessionId);
        fortuneGodsGames.remove(sessionId);
        log.info("玩家離線: {}", sessionId);
    }
}
