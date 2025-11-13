package org.secverse.SecVerseDupeUtils.Helper;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LanguageGUICloseEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final boolean returnToMain;

    public LanguageGUICloseEvent(Player player, boolean returnToMain) {
        this.player = player;
        this.returnToMain = returnToMain;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean shouldReturnToMain() {
        return returnToMain;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
