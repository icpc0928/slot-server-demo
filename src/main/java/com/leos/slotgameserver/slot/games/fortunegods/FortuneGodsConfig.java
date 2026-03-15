package com.leos.slotgameserver.slot.games.fortunegods;

import java.util.HashMap;
import java.util.Map;

/**
 * Fortune Gods 遊戲配置
 */
public class FortuneGodsConfig {
    
    // === 基本配置 ===
    public static final int GAME_ID = 1002;
    public static final int DEFAULT_PROB = 96;
    public static final int MAX_REEL = 6;
    public static final int[] MAIN_GRID_SIZE = {5, 5, 5, 5, 5, 5};
    public static final int[] TOP_REEL_SIZE = {0, 1, 1, 1, 1, 0};  // 附加軸：只有第2-5軸有效
    public static final int BET_TIMES = 20;
    public static final int MAX_WIN_LIMIT = 100000;
    public static final int BUY_FREE_MULTIPLIER = 80;
    
    // === 符號 ===
    public static final int WILD = 11;
    public static final int SCATTER = 12;
    public static final int HORIZONTAL_FORTUNE = 13;  // 橫財神
    
    // === 免費遊戲 ===
    public static final int FREE_SPIN_NEED = 4;
    public static final int BASE_FREE_SPIN_COUNT = 8;
    public static final int EXTRA_FREE_PER_SCATTER = 2;
    public static final int FREE_SPIN_MULTIPLIER = 8;
    
    // === 符號對照表 ===
    public static final Map<Integer, String> SYMBOL_MAP = new HashMap<>() {{
        put(0, "10");
        put(1, "J");
        put(2, "Q");
        put(3, "K");
        put(4, "A");
        put(5, "FIRECRACKER");
        put(6, "INGOT");
        put(7, "RED_PACKET");
        put(8, "KOI");
        put(9, "TOAD");
        put(10, "GOD_HEAD");
        put(11, "WILD");
        put(12, "SCATTER");
        put(13, "HORIZONTAL_FORTUNE");
    }};
    
    // === 賠付表 [符號][連線軸數] ===
    public static final int[][] PAY_TABLE = {
        {0, 0, 0, 1, 2, 3, 4},     // 10
        {0, 0, 0, 1, 2, 3, 4},     // J
        {0, 0, 0, 1, 2, 3, 4},     // Q
        {0, 0, 0, 4, 6, 8, 10},    // K
        {0, 0, 0, 4, 6, 8, 10},    // A
        {0, 0, 0, 6, 10, 12, 15},  // 爆竹
        {0, 0, 0, 6, 10, 12, 15},  // 銅鑼
        {0, 0, 0, 8, 15, 20, 30},  // 紅包
        {0, 0, 0, 10, 25, 30, 40}, // 錦鯉
        {0, 0, 0, 20, 25, 30, 50}, // 金蟾
        {0, 0, 0, 30, 40, 50, 80}, // 獅頭
        {0, 0, 0, 0, 0, 0, 0},     // WILD
        {0, 0, 0, 0, 0, 0, 0},     // SCATTER
        {0, 0, 0, 0, 0, 0, 0},     // HORIZONTAL_FORTUNE
    };
    
    // === 多格符號權重 [符號][1格, 2格, 3格, 4格] ===
    public static final int[][] MULTI_SIZE_WEIGHT = {
        {30, 45, 30, 29},   // 10
        {30, 45, 30, 29},   // J
        {30, 45, 28, 29},   // Q
        {30, 40, 25, 29},   // K
        {30, 40, 25, 29},   // A
        {31, 39, 30, 35},   // 爆竹
        {31, 39, 30, 35},   // 銅鑼
        {35, 35, 25, 25},   // 紅包
        {35, 35, 20, 25},   // 錦鯉
        {36, 30, 15, 20},   // 金蟾
        {36, 25, 15, 20},   // 獅頭
        {100, 0, 0, 0},     // WILD
        {100, 25, 0, 0},    // SCATTER
        {100, 0, 0, 0},     // HORIZONTAL_FORTUNE
    };
    
    // === 銀框機率（萬分比）===
    public static final int[] SILVER_FRAME_PROB = {
        1800, 1800, 1800, 2200, 2200,  // 10, J, Q, K, A
        2400, 2400, 1400, 1500, 1600,  // 爆竹, 銅鑼, 紅包, 錦鯉, 金蟾
        1800, 0, 0, 0                   // 獅頭, WILD, SCATTER, HORIZONTAL_FORTUNE
    };
    
    // === 輪帶權重 ===
    public static final Map<Integer, int[]> N_BASE_WEIGHT = new HashMap<>() {{
        put(96, new int[]{18, 18, 130, 200});
    }};
    
    public static final Map<Integer, int[]> N_FREE_WEIGHT = new HashMap<>() {{
        put(96, new int[]{20, 20, 90, 65});
    }};
    
    public static final Map<Integer, int[]> BF_BASE_WEIGHT = new HashMap<>() {{
        put(96, new int[]{10, 10, 0, 0});
    }};
    
    public static final Map<Integer, int[]> BF_FREE_WEIGHT = new HashMap<>() {{
        put(96, new int[]{20, 20, 90, 65});
    }};
    
    // === Scatter 機率權重 ===
    public static final Map<Integer, int[]> SC_WEIGHT_N_BASE = new HashMap<>() {{
        put(96, new int[]{30000, 15000, 14000, 8500, 285, 90, 10});
    }};
    
    public static final Map<Integer, int[]> SC_WEIGHT_N_FREE = new HashMap<>() {{
        put(96, new int[]{40000, 30000, 16000, 6000, 250, 100, 10});
    }};
    
    public static final Map<Integer, int[]> SC_WEIGHT_BF_BASE = new HashMap<>() {{
        put(96, new int[]{0, 0, 0, 0, 10, 0, 0});
    }};
    
    public static final Map<Integer, int[]> SC_WEIGHT_BF_FREE = new HashMap<>() {{
        put(96, new int[]{40000, 30000, 16000, 6000, 350, 100, 10});
    }};
    
    // === 連消輪帶選擇概率 ===
    public static final int[][] CASCADE_WHEEL_PROB_BASE = {
        {},
        {10, 10, 30, 30},
        {10, 20, 25, 20},
        {10, 30, 25, 15},
        {5, 50, 2, 0},
    };
    
    public static final int[][] CASCADE_WHEEL_PROB_FREE = {
        {},
        {10, 10, 35, 25},
        {15, 15, 30, 20},
        {25, 20, 20, 10},
        {35, 5, 5, 5},
    };
    
    public static final int[][] CASCADE_WHEEL_PROB_BF_BASE = {
        {},
        {10, 10, 0, 0},
        {10, 10, 0, 0},
        {10, 10, 0, 0},
        {10, 10, 0, 0},
    };
    
    public static final int[][] CASCADE_WHEEL_PROB_BF_FREE = {
        {},
        {10, 10, 35, 30},
        {10, 15, 30, 20},
        {10, 30, 25, 15},
        {5, 50, 5, 5},
    };
    
    // === 橫財神觸發機率（萬分比）===
    public static final Map<Integer, int[]> HORIZONTAL_FORTUNE_PROB_BASE = new HashMap<>() {{
        put(96, new int[]{0, 0, 200, 202});
    }};
    
    public static final Map<Integer, int[]> HORIZONTAL_FORTUNE_PROB_FREE = new HashMap<>() {{
        put(96, new int[]{0, 0, 40, 40});
    }};
    
    public static final Map<Integer, int[]> HORIZONTAL_FORTUNE_PROB_BF_BASE = new HashMap<>() {{
        put(96, new int[]{0, 0, 0, 0});
    }};
    
    public static final Map<Integer, int[]> HORIZONTAL_FORTUNE_PROB_BF_FREE = new HashMap<>() {{
        put(96, new int[]{0, 0, 75, 75});
    }};
    
    // === 漸進橫財神配置 ===
    public static final Map<Integer, int[]> GRADUAL_HORIZONTAL_FORTUNE_COUNT_WEIGHT = new HashMap<>() {{
        put(96, new int[]{25, 10, 10, 10, 45});
    }};
    
    public static final int[] GRADUAL_NORMAL_SYMBOL_WEIGHT = {
        35, 35, 35, 30, 30, 15, 15, 7, 3, 2, 1
    };
    
    // === 免費遊戲強制中獎配置 ===
    public static final float FREE_FORCE_WIN_MULTIPLIER_THRESHOLD = 1.0f;
    public static final float FREE_FORCE_WIN_MIN_MULTIPLIER = 1.0f;
    public static final float[] FREE_FORCE_WIN_PROB_N = {1f, 0.8f, 0.5f, 0.35f, 0.4f};
    public static final float[] FREE_FORCE_WIN_PROB_BF = {1f, 0.8f, 0.5f, 0.55f, 0.4f};
    
    // === 賭一把配置 ===
    public static final int[] FREE_SPIN_LEVELS = {8, 10, 12, 14, 16, 18, 20};
    public static final int[] MULTIPLIER_LEVELS = {8, 10, 12, 14, 16, 18, 20};
    
    // === 賭一把 RTP 表 ===
    public static final double[][] GAMBLE_RTP_TABLE_N = {
        {4440.1458, 5506.4088, 6601.4639, 7693.5052, 8799.5272, 9902.9343, 10859.2468},
        {5280.3127, 6546.1910, 7864.3101, 9177.4392, 10484.8673, 11811.2978, 13024.2153},
        {6190.1944, 7717.1862, 9245.8831, 10795.3517, 12341.7625, 13890.3299, 15360.7408},
        {7149.1863, 8927.5077, 10697.9902, 12498.4093, 14279.1768, 16067.5738, 17804.0775},
        {8134.3873, 10165.1208, 12184.8694, 14219.4326, 16266.1075, 18285.0525, 20307.5751},
        {9132.9912, 11419.5844, 13691.3039, 15971.8191, 18244.9261, 20526.7110, 22809.0781},
        {10140.9804, 12659.5571, 15200.0612, 17730.2729, 20260.1175, 22781.8773, 25337.2840},
    };
    
    public static final double[][] GAMBLE_RTP_TABLE_BF = {
        {97.1877, 120.4176, 144.3533, 168.4924, 192.5415, 216.7798, 237.8271},
        {115.2397, 143.2470, 172.0338, 200.1576, 229.2149, 257.7463, 283.9260},
        {135.1061, 168.2984, 202.1336, 235.7293, 269.4971, 303.2044, 336.1711},
        {156.1195, 194.8047, 233.9536, 272.5059, 312.0127, 351.1444, 389.0644},
        {177.6451, 221.5733, 265.9825, 310.5848, 354.7464, 399.2655, 443.4994},
        {199.3765, 249.2917, 299.3676, 348.4948, 398.6402, 448.1780, 498.3214},
        {221.5025, 276.8279, 332.4941, 386.9946, 442.9684, 497.7844, 553.0627},
    };
}
