package ru.kseonyt.chained.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import ru.kseonyt.chained.chain.ChainManager;
import ru.kseonyt.chained.chain.ChainPhysics;

/**
 * Обрабатывает серверные тики для применения физики цепи.
 */
public class ChainServerTicker {
    private final ChainPhysics chainPhysics;
    private final ChainManager chainManager;
    
    public ChainServerTicker(ChainPhysics chainPhysics, ChainManager chainManager) {
        this.chainPhysics = chainPhysics;
        this.chainManager = chainManager;
    }
    
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Применяем физику ко всем связанным игрокам
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (chainManager.isChained(player.getUuid())) {
                    chainPhysics.applyChainPhysics(player);
                }
            }
        });
    }
}

