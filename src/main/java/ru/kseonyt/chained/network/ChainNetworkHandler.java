package ru.kseonyt.chained.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import ru.kseonyt.chained.chain.ChainManager;
import ru.kseonyt.chained.chain.ChainPhysics;
import ru.kseonyt.chained.item.ChainItemHandler;
import ru.kseonyt.chained.network.payload.ChainClimbPayload;
import ru.kseonyt.chained.network.payload.ChainResponsePayload;
import ru.kseonyt.chained.network.payload.ChainSyncPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Управляет сетевыми пакетами для синхронизации между клиентом и сервером.
 */
public class ChainNetworkHandler {
    private static ChainItemHandler itemHandler;
    private static ChainPhysics chainPhysics;
    
    public static void registerServerHandlers(ChainItemHandler handler, ChainPhysics physics) {
        itemHandler = handler;
        chainPhysics = physics;
        
        ServerPlayNetworking.registerGlobalReceiver(
            ChainResponsePayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    if (context.player() instanceof ServerPlayerEntity) {
                        itemHandler.handleResponse(
                            (ServerPlayerEntity) context.player(),
                            payload.accepted(),
                            payload.senderId()
                        );
                    }
                });
            }
        );
        
        ServerPlayNetworking.registerGlobalReceiver(
            ChainClimbPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    if (context.player() instanceof ServerPlayerEntity) {
                        chainPhysics.setClimbing(
                            context.player().getUuid(),
                            payload.climbing()
                        );
                    }
                });
            }
        );
    }
    
    /**
     * Отправляет обновление связи всем игрокам на сервере.
     * Отправляет информацию о всей цепочке, в которую входят эти игроки.
     */
    public static void broadcastChainUpdate(ServerPlayerEntity player1, ServerPlayerEntity player2, boolean create) {
        ChainManager chainManager = ChainManager.getInstance();
        
        // Собираем все текущие связи на сервере (после изменения)
        Map<UUID, Set<UUID>> allChains = chainManager.getAllChains();
        List<ChainSyncPayload.ChainLink> links = new ArrayList<>();
        
        for (Map.Entry<UUID, Set<UUID>> entry : allChains.entrySet()) {
            UUID playerId = entry.getKey();
            for (UUID neighborId : entry.getValue()) {
                // Добавляем только один раз для каждой пары (playerId < neighborId)
                if (playerId.compareTo(neighborId) < 0) {
                    links.add(new ChainSyncPayload.ChainLink(playerId, neighborId));
                }
            }
        }
        
        // Отправляем синхронизацию всех связей всем игрокам на сервере
        ChainSyncPayload syncPayload = new ChainSyncPayload(links);
        var server = player1.getServer();
        if (server != null) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, syncPayload);
            }
        }
    }
    
    /**
     * Синхронизирует все активные связи с конкретным игроком (при подключении)
     */
    public static void syncAllChainsToPlayer(ServerPlayerEntity player) {
        ChainManager chainManager = ChainManager.getInstance();
        Map<UUID, Set<UUID>> allChains = chainManager.getAllChains();
        
        // Собираем все связи
        List<ChainSyncPayload.ChainLink> links = new ArrayList<>();
        for (Map.Entry<UUID, Set<UUID>> entry : allChains.entrySet()) {
            UUID player1 = entry.getKey();
            for (UUID player2 : entry.getValue()) {
                // Отправляем только один раз для каждой пары (player1 < player2)
                if (player1.compareTo(player2) < 0) {
                    links.add(new ChainSyncPayload.ChainLink(player1, player2));
                }
            }
        }
        
        // Отправляем все связи одним пакетом
        ChainSyncPayload syncPayload = new ChainSyncPayload(links);
        ServerPlayNetworking.send(player, syncPayload);
    }
}

