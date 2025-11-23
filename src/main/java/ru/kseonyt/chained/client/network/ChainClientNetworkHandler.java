package ru.kseonyt.chained.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import ru.kseonyt.chained.client.ChainClientData;
import ru.kseonyt.chained.client.gui.ChainRequestScreen;
import ru.kseonyt.chained.network.payload.ChainClimbPayload;
import ru.kseonyt.chained.network.payload.ChainRequestPayload;
import ru.kseonyt.chained.network.payload.ChainResponsePayload;
import ru.kseonyt.chained.network.payload.ChainSyncPayload;
import ru.kseonyt.chained.network.payload.ChainUpdatePayload;

import java.util.UUID;

/**
 * Обрабатывает сетевые пакеты на клиенте.
 */
public class ChainClientNetworkHandler {
    
    public static void register() {
        // Регистрируем обработчик запроса на связывание
        ClientPlayNetworking.registerGlobalReceiver(
            ChainRequestPayload.ID,
            (payload, context) -> {
                ChainRequestPayload requestPayload = payload;
                context.client().execute(() -> {
                    MinecraftClient.getInstance().setScreen(
                        new ChainRequestScreen(requestPayload.senderId(), requestPayload.senderName())
                    );
                });
            }
        );
        
        // Регистрируем обработчик обновления связей
        ClientPlayNetworking.registerGlobalReceiver(
            ChainUpdatePayload.ID,
            (payload, context) -> {
                ChainUpdatePayload updatePayload = payload;
                context.client().execute(() -> {
                    ChainClientData clientData = ChainClientData.getInstance();
                    if (updatePayload.create()) {
                        clientData.setChain(updatePayload.player1(), updatePayload.player2());
                    } else {
                        clientData.removeChain(updatePayload.player1(), updatePayload.player2());
                    }
                });
            }
        );
        
        // Регистрируем обработчик синхронизации цепочки
        ClientPlayNetworking.registerGlobalReceiver(
            ChainSyncPayload.ID,
            (payload, context) -> {
                ChainSyncPayload syncPayload = payload;
                context.client().execute(() -> {
                    ChainClientData clientData = ChainClientData.getInstance();
                    // Получаем всех игроков из цепочки
                    java.util.Set<UUID> playersInChain = new java.util.HashSet<>();
                    for (ChainSyncPayload.ChainLink link : syncPayload.links()) {
                        playersInChain.add(link.player1());
                        playersInChain.add(link.player2());
                    }
                    
                    // Удаляем все старые связи для игроков в цепочке
                    for (UUID playerId : playersInChain) {
                        java.util.Set<UUID> oldNeighbors = new java.util.HashSet<>(clientData.getChainedPlayers(playerId));
                        for (UUID neighborId : oldNeighbors) {
                            if (playersInChain.contains(neighborId)) {
                                clientData.removeChain(playerId, neighborId);
                            }
                        }
                    }
                    
                    // Добавляем все новые связи из пакета
                    for (ChainSyncPayload.ChainLink link : syncPayload.links()) {
                        clientData.setChain(link.player1(), link.player2());
                    }
                });
            }
        );
    }
    
    /**
     * Отправляет ответ на запрос (принятие/отклонение)
     */
    public static void sendResponse(boolean accepted, UUID senderId) {
        ChainResponsePayload payload = new ChainResponsePayload(accepted, senderId);
        ClientPlayNetworking.send(payload);
    }
    
    /**
     * Отправляет запрос на взбирание по цепи
     */
    public static void sendClimbRequest(boolean climbing) {
        ChainClimbPayload payload = new ChainClimbPayload(climbing);
        ClientPlayNetworking.send(payload);
    }
}

