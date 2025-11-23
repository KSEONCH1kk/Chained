package ru.kseonyt.chained.server;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import ru.kseonyt.chained.chain.ChainManager;
import ru.kseonyt.chained.chain.ChainRequestManager;
import ru.kseonyt.chained.network.ChainNetworkHandler;
import ru.kseonyt.chained.network.payload.ChainSyncPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Обрабатывает события, связанные с цепью: смерть, переходы между измерениями, драконье дыхание.
 */
public class ChainEventHandler {
    private final ChainManager chainManager;
    private final ChainRequestManager requestManager;
    
    public ChainEventHandler(ChainManager chainManager) {
        this.chainManager = chainManager;
        this.requestManager = ChainRequestManager.getInstance();
    }
    
    public void register() {
        // Обработка смерти игрока - только если умер связанный игрок
        // Обработка смерти от любого источника
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            handlePlayerDeath(oldPlayer);
        });
        
        // Обработка перехода между измерениями через COPY_FROM
        ServerPlayerEvents.COPY_FROM.register((newPlayer, oldPlayer, alive) -> {
            // Проверяем, изменилось ли измерение (при смерти или телепортации)
            ServerWorld oldWorld = oldPlayer.getServerWorld();
            ServerWorld newWorld = newPlayer.getServerWorld();
            if (oldWorld != newWorld) {
                handleDimensionChange(newPlayer, oldWorld, newWorld);
            }
        });
        
        // Синхронизация всех цепей при подключении игрока
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            // Синхронизируем все существующие цепи с новым игроком
            ChainNetworkHandler.syncAllChainsToPlayer(player);
        });
        
        // Обработка бутылированного драконьего дыхания будет в ChainItemHandler
    }
    
    /**
     * Обрабатывает смерть игрока - разрывает все связи
     */
    private void handlePlayerDeath(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        
        // Очищаем все запросы, связанные с умершим игроком
        requestManager.clearRequestsForPlayer(playerId);
        
        if (!chainManager.isChained(playerId)) {
            return;
        }
        
        // Получаем всех связанных игроков
        Set<UUID> chainedPlayers = chainManager.getChainedPlayers(playerId);
        if (chainedPlayers.isEmpty()) {
            return;
        }
        
        ServerWorld world = player.getServerWorld();
        
        // Удаляем все связи умершего игрока
        for (UUID chainedId : chainedPlayers) {
            chainManager.removeChain(playerId, chainedId);
            
            // Уведомляем связанного игрока
            PlayerEntity chainedPlayerEntity = world.getPlayerByUuid(chainedId);
            if (chainedPlayerEntity instanceof ServerPlayerEntity chainedPlayer) {
                chainedPlayer.sendMessage(Text.translatable("chained.chain.broken.death"), false);
            }
        }
        
        // Уведомляем умершего игрока
        player.sendMessage(Text.translatable("chained.chain.broken.death"), false);
        
        // Синхронизируем обновления со всеми клиентами
        // Отправляем синхронизацию всем игрокам на сервере
        var server = player.getServer();
        if (server != null) {
            // Собираем все оставшиеся связи после удаления умершего игрока
            List<ChainSyncPayload.ChainLink> links = new ArrayList<>();
            Map<UUID, Set<UUID>> allChains = chainManager.getAllChains();
            for (Map.Entry<UUID, Set<UUID>> entry : allChains.entrySet()) {
                UUID player1 = entry.getKey();
                for (UUID player2 : entry.getValue()) {
                    if (player1.compareTo(player2) < 0) {
                        links.add(new ChainSyncPayload.ChainLink(player1, player2));
                    }
                }
            }
            
            // Отправляем синхронизацию всем игрокам
            ChainSyncPayload syncPayload = new ChainSyncPayload(links);
            for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(serverPlayer, syncPayload);
            }
        }
    }
    
    /**
     * Обрабатывает переход между измерениями - телепортирует связанного игрока
     */
    private void handleDimensionChange(ServerPlayerEntity player, ServerWorld originWorld, ServerWorld destinationWorld) {
        UUID playerId = player.getUuid();
        
        if (!chainManager.isChained(playerId)) {
            return;
        }
        
        UUID chainedId = chainManager.getChainedPlayer(playerId);
        if (chainedId == null) {
            return;
        }
        
        // Находим связанного игрока в исходном мире
        PlayerEntity chainedPlayerEntity = originWorld.getPlayerByUuid(chainedId);
        
        if (chainedPlayerEntity == null || !(chainedPlayerEntity instanceof ServerPlayerEntity)) {
            return;
        }
        
        ServerPlayerEntity chainedPlayer = (ServerPlayerEntity) chainedPlayerEntity;
        
        // Проверяем, что связанный игрок ещё в старом измерении
        if (chainedPlayer.getServerWorld() == originWorld) {
            // Телепортируем связанного игрока в новое измерение
            Vec3d targetPos = player.getPos();
            
            // Используем метод teleport для перехода между измерениями
            chainedPlayer.teleport(
                destinationWorld,
                targetPos.x,
                targetPos.y,
                targetPos.z,
                java.util.Set.of(),
                player.getYaw(),
                player.getPitch(),
                false
            );
            
            // Уведомляем игроков
            player.sendMessage(Text.translatable("chained.dimension.teleported"), false);
            chainedPlayer.sendMessage(Text.translatable("chained.dimension.teleported"), false);
        }
    }
    
}

