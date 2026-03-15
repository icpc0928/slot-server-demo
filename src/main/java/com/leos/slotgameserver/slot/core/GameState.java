package com.leos.slotgameserver.slot.core;

/**
 * 遊戲狀態
 */
public enum GameState {
    BASE_GAME(0, "主遊戲"),
    FREE_GAME(1, "免費遊戲");
    
    private final int id;
    private final String name;
    
    GameState(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
}
