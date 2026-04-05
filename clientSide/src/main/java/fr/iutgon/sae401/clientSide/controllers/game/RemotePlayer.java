package fr.iutgon.sae401.clientSide.controller.game;

import fr.iutgon.sae401.clientSide.view.PlayerView;

public final class RemotePlayer {
    public final PlayerView view;
    public int x;
    public int y;
    public int lastX;
    public int lastY;
    public String dir = "S";
    public boolean alive = true;
    public int health;
    public int currentBombs;
    public float speed = 1f;
    public final int skinId;

    public RemotePlayer(PlayerView view, int x, int y, int skinId) {
        this.view = view;
        this.x = x;
        this.y = y;
        this.lastX = x;
        this.lastY = y;
        this.alive = true;
        this.health = 0;
        this.currentBombs = 0;
        this.skinId = skinId;
    }
}
