package com.leos.slotgameserver.slot.games.knockoutriches;

import java.util.HashMap;
import java.util.Map;

/**
 * Knockout Riches 9 Icon 遊戲配置
 */
public class KnockoutRichesConfig {
    
    // === 基本配置 ===
    public static final int GAME_ID = 1001;
    public static final int DEFAULT_PROB = 96;
    public static final int MAX_REEL = 5;
    public static final int[] GRID_SIZE = {3, 4, 4, 4, 3};
    public static final int BET_TIMES = 20;
    public static final int MAX_WIN_LIMIT = 10000;
    public static final int BUY_FREE_MULTIPLIER = 100;  // 買免費 100 倍下注
    
    // === 符號 ===
    public static final int WILD = 9;
    public static final int SCATTER = 10;
    
    // === 免費遊戲 ===
    public static final int FREE_SPIN_NEED = 2;
    public static final int FREE_SPIN_COUNT = 5;
    
    // === 記分板 ===
    public static final int[] MAX_REEL_STACK = {0, 20, 20, 20, 0};
    public static final int STACK_TRIGGER = 3;  // 第四消開始計算
    
    // === 符號對照表 ===
    public static final Map<Integer, String> SYMBOL_MAP = new HashMap<>() {{
        put(0, "L5");   // 10
        put(1, "L4");   // J
        put(2, "L3");   // Q
        put(3, "L2");   // K
        put(4, "L1");   // A
        put(5, "S4");   // 手套
        put(6, "S3");   // 頭盔
        put(7, "S2");   // 沙包
        put(8, "S1");   // 辣妹
        put(9, "WILD");
        put(10, "SC");
    }};
    
    // === 賠付表 ===
    public static final int[][] PAY_TABLE = {
        {0, 0, 0, 2, 3, 5},    // 10
        {0, 0, 0, 2, 3, 5},    // J
        {0, 0, 0, 2, 3, 5},    // Q
        {0, 0, 0, 2, 3, 5},    // K
        {0, 0, 0, 2, 3, 5},    // A
        {0, 0, 0, 5, 8, 10},   // 手套
        {0, 0, 0, 10, 15, 20}, // 頭盔
        {0, 0, 0, 10, 15, 20}, // 沙包
        {0, 0, 0, 20, 35, 50}, // 辣妹
        {0, 0, 0, 0, 0, 0},    // WILD
        {0, 0, 0, 0, 0, 0},    // SC
    };
    
    // === 記分板 Ways 等級 ===
    public static final int[] SCOREBOARD_WAYS_LEVEL = {1, 4, 7, 15, 20, 30, 60, 120, 200, 500, 800, 2000, 4000, 8000};
    
    // === 記分板圖標權重（一般模式）===
    public static final int[][] SCOREBOARD_ICON_WEIGHT = {
        {50, 50, 50, 50, 50, 500, 120, 120, 110},   // 0-1
        {50, 50, 50, 50, 50, 50, 100, 100, 50},     // 2-4
        {80, 90, 90, 90, 90, 400, 250, 300, 100},   // 5-7
        {70, 75, 75, 75, 75, 500, 200, 250, 100},   // 8-15
        {80, 80, 80, 90, 90, 300, 200, 350, 70},    // 16-20
        {100, 100, 100, 100, 100, 450, 200, 250, 100}, // 21-30
        {100, 100, 120, 120, 120, 800, 400, 500, 60},  // 31-60
        {100, 100, 120, 120, 150, 500, 100, 100, 50},  // 61-120
        {400, 400, 400, 400, 400, 500, 80, 80, 50},    // 121-200
        {460, 560, 600, 700, 900, 500, 250, 300, 40},  // 201-500
        {520, 570, 750, 800, 920, 350, 150, 160, 30},  // 501-800
        {520, 570, 750, 800, 920, 350, 150, 160, 30},  // 801-2000
        {600, 800, 999, 999, 999, 320, 30, 70, 20},    // 2001-4000
        {999, 999, 999, 999, 999, 190, 30, 50, 0},     // 4001~8000
    };
    
    // === 記分板圖標權重（買免費模式）===
    public static final int[][] SCOREBOARD_ICON_WEIGHT_BUY_FREE = {
        {50, 50, 50, 50, 50, 450, 120, 120, 200},
        {50, 50, 50, 50, 50, 50, 50, 50, 100},
        {50, 50, 50, 50, 50, 240, 180, 200, 90},
        {50, 50, 50, 50, 50, 350, 200, 220, 90},
        {50, 50, 50, 50, 50, 400, 160, 160, 70},
        {100, 100, 100, 100, 100, 500, 300, 350, 70},
        {100, 100, 100, 100, 150, 400, 300, 350, 70},
        {100, 100, 100, 100, 150, 400, 250, 300, 75},
        {200, 200, 200, 200, 200, 500, 200, 250, 50},
        {500, 600, 700, 800, 800, 750, 350, 400, 30},
        {800, 800, 900, 999, 999, 400, 60, 80, 20},
        {800, 800, 900, 999, 999, 150, 60, 80, 20},
        {800, 900, 999, 999, 999, 150, 30, 60, 20},
        {999, 999, 999, 999, 999, 100, 20, 25, 0},
    };
    
    // === 強制中獎機率（主遊戲連消）===
    public static final float[] BASE_CASCADE_WIN_PROB = {0f, 0.22f, 0.10f, 0.37f, 0.75f, 0.48f, 0.2f, 0.08f, 0.03f, 0.0f};
    
    // === 買免費連消中獎機率 ===
    public static final float[] BUY_FREE_WIN_PROB = {0.4f, 0.4f, 0.4f, 0.5f, 0.5f, 0.2f};
    public static final int BUY_FREE_SB_WAYS_CONDITION = 21;
    public static final int BUY_FREE_SB_WAYS_CHANGE_WEIGHT = 500;
    public static final int[] BUY_FREE_SC_PROB = {0, 0, 0, 95, 5};
    
    // === 消除圖標掉落機率（每消用的權重表）===
    public static final int[][] CASCADE_PROB_BASE = {
        {},                              // 第0消不需要
        {0, 5, 5, 5, 50, 50, 0, 0},      // 第1消
        {0, 20, 20, 0, 50, 50, 0, 0},    // 第2消
        {0, 0, 30, 0, 50, 50, 0, 0},     // 第3消
        {0, 0, 10, 0, 100, 100, 0, 0},   // 第4消以上
    };
    
    public static final int[][] CASCADE_PROB_FREE = {
        {},
        {0, 0, 0, 0, 80, 80, 100, 10},
        {0, 0, 0, 0, 150, 150, 100, 10},
        {0, 0, 0, 0, 50, 50, 100, 0},
        {0, 0, 0, 0, 50, 50, 50, 0},
    };
    
    public static final int[][] CASCADE_PROB_BUY_FREE = {
        {},
        {0, 0, 0, 10, 80, 80, 100, 10},
        {0, 0, 0, 0, 150, 150, 100, 10},
        {0, 0, 0, 0, 70, 70, 100, 0},
        {0, 0, 0, 0, 150, 150, 100, 0},
    };
}
