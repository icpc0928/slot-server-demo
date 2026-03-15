# 🎰 Slot Server Demo

Spring Boot 老虎機遊戲伺服器，整合多款 Slot 遊戲於單一服務中。

## 概述

將老虎機遊戲邏輯封裝為統一的 API 服務，支援多款遊戲同時運行。遊戲參數透過 YAML 設定檔管理，新增遊戲無需修改核心程式碼。

## 支援遊戲

| 遊戲 | 盤面 | 計算方式 | 特色 |
|------|------|---------|------|
| Fortune Gods (贏財神) | 6×5 | Ways | 連消 + 多格符號 |
| Gates of Olympus 1000 | 6×5 | Ways | 連消 + 累進倍率 |
| Super Ace | 5×3 | 20 Lines | 經典線制 |

## API

| Method | Path | 說明 |
|--------|------|------|
| GET | `/api/games` | 遊戲列表 |
| GET | `/api/games/{gameId}` | 遊戲資訊 |
| POST | `/api/games/spin` | 執行旋轉 |
| GET | `/api/games/balance` | 查詢餘額 |

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
  "wins": [{"symbolName": "CROWN", "matchCount": 4, "ways": 12, "payout": 150.0}],
  "totalWin": 150.0,
  "winMultiplier": 7.5,
  "freeSpinTriggered": false,
  "cascadeCount": 2,
  "balance": 10130.0
}
```

## 技術棧

- Java 25
- Spring Boot 3.5
- Gradle
- Lombok
- JUnit 5

## 啟動

```bash
./gradlew bootRun
```

## 測試

```bash
./gradlew test
```

## 專案結構

```
src/main/java/com/leo/slotserver/
├── controller/          # API 端點
├── service/             # 業務邏輯（餘額管理、遊戲調度）
├── engine/              # 遊戲引擎核心
│   ├── fortunegods/     # Fortune Gods 引擎
│   ├── superace/        # Super Ace 引擎
│   └── gatesofolympus/  # Gates of Olympus 引擎
├── model/               # 內部資料模型
├── dto/                 # API 請求/回應物件
├── exception/           # 統一例外處理
└── config/              # 遊戲設定載入

src/main/resources/games/   # 遊戲設定檔（YAML）
```
