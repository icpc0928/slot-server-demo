package com.leos.slotgameserver.slot.core;

/**
 * 遊戲模式
 * 
 * Buy Free 下注倍率由各遊戲 Config 自己定義
 */
public enum PlayMode {
    NORMAL("一般模式"),
    BUY_FREE("購買免費");
    
    private final String name;
    
    PlayMode(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
