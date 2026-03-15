package com.leos.slotgameserver.slot.games.knockoutriches;

import com.leos.slotgameserver.slot.core.result.Round;

/**
 * Knockout Riches 專用的 Round 資料存取 Helper
 * 提供類型安全的方式存取遊戲專用欄位
 */
public class KnockoutRichesRoundHelper {
    
    private static final String ACC_STACK_COUNT = "accStackCount";
    
    /**
     * 設置累積記分板計數
     */
    public static void setAccStackCount(Round round, int[] counts) {
        round.getExtra().put(ACC_STACK_COUNT, counts);
    }
    
    /**
     * 取得累積記分板計數
     */
    public static int[] getAccStackCount(Round round) {
        Object value = round.getExtra().get(ACC_STACK_COUNT);
        return value != null ? (int[]) value : null;
    }
    
    /**
     * 取得累積記分板計數，如果不存在則返回預設值
     */
    public static int[] getAccStackCountOrDefault(Round round, int[] defaultValue) {
        int[] result = getAccStackCount(round);
        return result != null ? result : defaultValue;
    }
}
