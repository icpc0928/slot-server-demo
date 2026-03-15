# 🎰 Slot Game Server

Spring Boot 老虎機遊戲伺服器 

## Architecture

```
Controller (thin) → Service (business logic) → Engine (game math)
```

### Design Patterns

| Pattern | 用途 | 位置 |
|---------|------|------|
| **Strategy** | 每個遊戲一個引擎，共用介面 | `SlotEngine` interface |
| **Template Method** | 共用遊戲流程，子類覆寫差異 | `AbstractSlotEngine` |
| **Factory** | 根據 gameId 建立引擎 | `SlotEngineFactory` |

### SOLID Principles

- **SRP**: Controller 只處理 HTTP、Service 管業務邏輯、Engine 管數學
- **OCP**: 新增遊戲只需實作 `AbstractSlotEngine` + 加設定檔
- **LSP**: 所有引擎都可以替換使用，行為一致
- **ISP**: `WaysEvaluator` 和 `LinesEvaluator` 分開，引擎只用自己需要的
- **DIP**: Service 依賴 `SlotEngine` 介面，不依賴具體實作

## Games

| Game | Type | Features |
|------|------|----------|
| Fortune Gods | 6x5 Ways | Cascade + Multi-size Symbols |
| Gates of Olympus 1000 | 6x5 Ways | Tumble + Progressive Multiplier |
| Super Ace | 5x3 Lines | 20 Paylines |

## API

```
GET  /api/games              — 遊戲列表
GET  /api/games/{gameId}     — 遊戲資訊
POST /api/games/spin         — 執行旋轉
GET  /api/games/balance      — 查詢餘額
```

### Spin Request

```json
{
  "gameId": "gates-of-olympus-1000",
  "betAmount": 20.0
}
```

### Spin Response

```json
{
  "gameId": "gates-of-olympus-1000",
  "grid": [[0,1,2,3,4], ...],
  "wins": [{ "symbolName": "CROWN", "matchCount": 4, "ways": 12, "payout": 150.0 }],
  "totalWin": 150.0,
  "winMultiplier": 7.5,
  "freeSpinTriggered": false,
  "cascadeCount": 2,
  "balance": 10130.0
}
```

## Tech Stack

- Java 25 + Spring Boot 3.4
- Lombok
- Jackson (YAML config)
- JUnit 5

## Run

```bash
./gradlew bootRun
```

## Test

```bash
./gradlew test
```
