package ru.kseonyt.chained.item;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import ru.kseonyt.chained.chain.ChainManager;
import ru.kseonyt.chained.chain.ChainRequestManager;
import ru.kseonyt.chained.network.payload.ChainRequestPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import java.util.UUID;

/**
 * Обрабатывает взаимодействие с цепью (Shift + ПКМ по игроку).
 */
public class ChainItemHandler {
    private final ChainManager chainManager;
    private final ChainRequestManager requestManager;
    
    public ChainItemHandler(ChainManager chainManager, ChainRequestManager requestManager) {
        this.chainManager = chainManager;
        this.requestManager = requestManager;
    }
    
    public void register() {
        UseEntityCallback.EVENT.register((player, world, hand, target, hitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }
            
            if (!(player instanceof ServerPlayerEntity)) {
                return ActionResult.PASS;
            }
            
            if (!(target instanceof PlayerEntity)) {
                return ActionResult.PASS;
            }
            
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            PlayerEntity targetPlayer = (PlayerEntity) target;
            
            ItemStack heldItem = serverPlayer.getStackInHand(hand);
            
            // Обработка бутылированного драконьего дыхания на связанного игрока
            if (heldItem.isOf(Items.DRAGON_BREATH)) {
                // Проверяем, связаны ли игроки
                if (chainManager.areChained(serverPlayer.getUuid(), targetPlayer.getUuid())) {
                    // Разрываем цепь
                    chainManager.removeChain(serverPlayer.getUuid(), targetPlayer.getUuid());
                    serverPlayer.sendMessage(
                        Text.translatable("chained.chain.broken.dragon_breath"), false);
                    
                    if (targetPlayer instanceof ServerPlayerEntity) {
                        ServerPlayerEntity targetServer = (ServerPlayerEntity) targetPlayer;
                        targetServer.sendMessage(
                            Text.translatable("chained.chain.broken.dragon_breath"), false);
                        
                        // Отправляем обновление связи клиентам
                        ru.kseonyt.chained.network.ChainNetworkHandler.broadcastChainUpdate(
                            serverPlayer, targetServer, false);
                    }
                    return ActionResult.SUCCESS;
                }
                return ActionResult.PASS;
            }
            
            // Проверяем, держит ли игрок цепь
            if (!heldItem.isOf(Items.CHAIN)) {
                return ActionResult.PASS;
            }
            
            // Проверяем Shift + ПКМ
            if (!serverPlayer.isSneaking() || hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            
            // Нельзя связаться с самим собой
            if (serverPlayer.getUuid().equals(targetPlayer.getUuid())) {
                return ActionResult.PASS;
            }
            
            // Если уже связаны, разрываем связь
            if (chainManager.areChained(serverPlayer.getUuid(), targetPlayer.getUuid())) {
                chainManager.removeChain(serverPlayer.getUuid(), targetPlayer.getUuid());
                serverPlayer.sendMessage(Text.translatable("chained.chain.broken", targetPlayer.getName()), false);
                if (targetPlayer instanceof ServerPlayerEntity) {
                    ServerPlayerEntity targetServer = (ServerPlayerEntity) targetPlayer;
                    targetServer.sendMessage(
                        Text.translatable("chained.chain.broken", serverPlayer.getName()), false);
                    
                    // Отправляем обновление связи клиентам
                    ru.kseonyt.chained.network.ChainNetworkHandler.broadcastChainUpdate(
                        serverPlayer, targetServer, false);
                }
                return ActionResult.SUCCESS;
            }
            
            // Проверяем, что игроки не связаны друг с другом напрямую
            if (chainManager.areChained(serverPlayer.getUuid(), targetPlayer.getUuid())) {
                // Уже связаны - это обрабатывается выше
                return ActionResult.PASS;
            }
            
            // Очищаем старые запросы между этими игроками (если есть)
            // Это позволяет отправить новый запрос после отклонения или смерти
            if (requestManager.hasPendingRequest(targetPlayer.getUuid())) {
                UUID existingSender = requestManager.getRequestSender(targetPlayer.getUuid());
                if (existingSender != null && existingSender.equals(serverPlayer.getUuid())) {
                    // Удаляем старый запрос от этого отправителя
                    requestManager.declineRequest(targetPlayer.getUuid());
                }
            }
            // Также очищаем запросы, где текущий игрок - получатель, а целевой - отправитель
            if (requestManager.hasPendingRequest(serverPlayer.getUuid())) {
                UUID existingSender = requestManager.getRequestSender(serverPlayer.getUuid());
                if (existingSender != null && existingSender.equals(targetPlayer.getUuid())) {
                    requestManager.declineRequest(serverPlayer.getUuid());
                }
            }
            
            // Отправляем запрос на связывание
            if (requestManager.createRequest(serverPlayer.getUuid(), targetPlayer.getUuid(), chainManager)) {
                // Отправляем пакет получателю
                if (targetPlayer instanceof ServerPlayerEntity) {
                    ChainRequestPayload payload = new ChainRequestPayload(
                        serverPlayer.getUuid(),
                        serverPlayer.getName().getString()
                    );
                    ServerPlayNetworking.send((ServerPlayerEntity) targetPlayer, payload);
                }
                
                serverPlayer.sendMessage(
                    Text.translatable("chained.request.sent", targetPlayer.getName()), false);
            } else {
                serverPlayer.sendMessage(
                    Text.translatable("chained.request.pending"), false);
            }
            
            return ActionResult.SUCCESS;
        });
    }
    
    /**
     * Обрабатывает ответ на запрос (принятие/отклонение)
     */
    public void handleResponse(ServerPlayerEntity player, boolean accepted, UUID senderId) {
        if (!requestManager.hasPendingRequest(player.getUuid())) {
            return;
        }
        
        UUID requestSender = requestManager.getRequestSender(player.getUuid());
        if (requestSender == null || !requestSender.equals(senderId)) {
            return;
        }
        
            if (accepted) {
            // Проверяем, что игроки не связаны друг с другом напрямую
            if (chainManager.areChained(player.getUuid(), senderId)) {
                player.sendMessage(Text.translatable("chained.request.already_chained.receiver"), false);
                requestManager.declineRequest(player.getUuid());
                return;
            }
            
            requestManager.acceptRequest(player.getUuid());
            
            // Создаем связь
            chainManager.createChain(senderId, player.getUuid());
            
            // Получаем отправителя запроса
            PlayerEntity sender = player.getServerWorld().getPlayerByUuid(senderId);
            
            // Уведомляем обоих игроков
            if (sender instanceof ServerPlayerEntity) {
                ServerPlayerEntity senderServer = (ServerPlayerEntity) sender;
                senderServer.sendMessage(
                    Text.translatable("chained.chain.created", player.getName()), false);
                
                // Отправляем обновление связи клиентам
                ru.kseonyt.chained.network.ChainNetworkHandler.broadcastChainUpdate(
                    senderServer, player, true);
            }
            player.sendMessage(
                Text.translatable("chained.chain.created", 
                    sender != null ? sender.getName() : Text.literal("Unknown")), false);
        } else {
            requestManager.declineRequest(player.getUuid());
            
            PlayerEntity sender = player.getServerWorld().getPlayerByUuid(senderId);
            if (sender instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity) sender).sendMessage(
                    Text.translatable("chained.request.declined", player.getName()), false);
            }
        }
    }
}

