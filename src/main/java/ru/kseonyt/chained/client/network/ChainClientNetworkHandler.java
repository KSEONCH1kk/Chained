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
                    
                    // Создаем множество всех связей из пакета для быстрой проверки
                    java.util.Set<String> newLinks = new java.util.HashSet<>();
                    
                    for (ChainSyncPayload.ChainLink link : syncPayload.links()) {
                        // Создаем уникальный ключ для пары (меньший UUID всегда первый)
                        String linkKey = link.player1().compareTo(link.player2()) < 0 
                            ? link.player1().toString() + "_" + link.player2().toString()
                            : link.player2().toString() + "_" + link.player1().toString();
                        newLinks.add(linkKey);
                    }
                    
                    // Получаем всех игроков, у которых есть связи на клиенте
                    java.util.Set<UUID> allPlayersWithChains = clientData.getAllPlayersWithChains();
                    
                    // Собираем все текущие связи для удаления
                    java.util.List<java.util.AbstractMap.SimpleEntry<UUID, UUID>> linksToRemove = new java.util.ArrayList<>();
                    
                    for (UUID player1 : allPlayersWithChains) {
                        java.util.Set<UUID> oldNeighbors = new java.util.HashSet<>(clientData.getChainedPlayers(player1));
                        for (UUID player2 : oldNeighbors) {
                            // Проверяем только один раз для каждой пары
                            if (player1.compareTo(player2) < 0) {
                                String linkKey = player1.toString() + "_" + player2.toString();
                                
                                // Добавляем в список для удаления, если связи нет в новом пакете
                                if (!newLinks.contains(linkKey)) {
                                    linksToRemove.add(new java.util.AbstractMap.SimpleEntry<>(player1, player2));
                                }
                            }
                        }
                    }
                    
                    // Удаляем все старые связи, которых нет в новом пакете
                    for (java.util.AbstractMap.SimpleEntry<UUID, UUID> link : linksToRemove) {
                        clientData.removeChain(link.getKey(), link.getValue());
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

