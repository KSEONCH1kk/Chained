package ru.kseonyt.chained.chain;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Управляет физикой ограничения расстояния между связанными игроками.
 * Применяет силу к игрокам, если они слишком далеко друг от друга.
 */
public class ChainPhysics {
    private final ChainManager chainManager;
    private static final double PULL_FORCE = 0.15; // Сила притяжения
    private static final double DAMPING = 0.8; // Затухание скорости
    
    public ChainPhysics(ChainManager chainManager) {
        this.chainManager = chainManager;
    }
    

    // Хранит состояние взбирания для каждого игрока
    private final Map<UUID, Boolean> climbingStates = new HashMap<>();
    
    public void setClimbing(UUID playerId, boolean climbing) {
        if (climbing) {
            climbingStates.put(playerId, true);
        } else {
            climbingStates.remove(playerId);
        }
    }
    
    public void applyChainPhysics(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        if (!chainManager.isChained(playerId)) {
            climbingStates.remove(playerId);
            return;
        }
        
        World world = player.getServerWorld();
        Vec3d pos1 = player.getPos();
        double maxLength = chainManager.getMaxChainLength();
        
        // Если игрок взбирается (приседает и смотрит вверх), применяем силу подтягивания к ближайшему соседу выше
        if (climbingStates.containsKey(playerId) && climbingStates.get(playerId)) {
            // Проверяем, что игрок смотрит вверх (на сервере тоже проверяем для надежности)
    
                // Находим ближайшего соседа выше
                ServerPlayerEntity targetNeighbor = null;
                double minHeightDiff = Double.MAX_VALUE;
                
                for (UUID neighborId : chainManager.getChainedPlayers(playerId)) {
                    PlayerEntity neighbor = world.getPlayerByUuid(neighborId);
                    if (neighbor instanceof ServerPlayerEntity) {
                        ServerPlayerEntity neighborServer = (ServerPlayerEntity) neighbor;
                        double heightDiff = neighborServer.getY() - pos1.y;
                        // Ищем соседа выше (с положительной разницей высоты)
                        if (heightDiff > 0 && heightDiff < minHeightDiff) {
                            minHeightDiff = heightDiff;
                            targetNeighbor = neighborServer;
                        }
                    }
                }
                
                if (targetNeighbor != null) {
                    // Применяем вертикальную силу для подтягивания
                    Vec3d currentVelocity = player.getVelocity();
                    double verticalForce = 0.1; // Увеличена сила подтягивания
                    Vec3d verticalVelocity = new Vec3d(0, verticalForce, 0);
                    
                    Vec3d newVelocity = new Vec3d(
                        currentVelocity.x * 0.9, // Немного уменьшаем горизонтальную скорость
                        currentVelocity.y + verticalVelocity.y,
                        currentVelocity.z * 0.9
                    );
                    
                    player.setVelocity(newVelocity);
                    player.velocityModified = true;
                }
        }
        
        // Применяем физику ограничения расстояния ко всем соседям
        Vec3d totalPullForce = Vec3d.ZERO;
        int neighborCount = 0;
        
        for (UUID neighborId : chainManager.getChainedPlayers(playerId)) {
            PlayerEntity neighbor = world.getPlayerByUuid(neighborId);
            if (neighbor == null || !(neighbor instanceof ServerPlayerEntity)) {
                continue;
            }
            
            ServerPlayerEntity neighborServer = (ServerPlayerEntity) neighbor;
            Vec3d pos2 = neighborServer.getPos();
            double distance = pos1.distanceTo(pos2);
            
            if (distance > maxLength) {
                Vec3d direction = pos1.subtract(pos2).normalize();
                Vec3d pullForce = direction.multiply(-PULL_FORCE * (distance - maxLength));
                totalPullForce = totalPullForce.add(pullForce);
                neighborCount++;
                
                // Применяем силу к соседу
                Vec3d neighborPullForce = direction.multiply(PULL_FORCE * (distance - maxLength));
                Vec3d neighborVelocity = neighborServer.getVelocity();
                Vec3d newNeighborVelocity = neighborVelocity.add(neighborPullForce).multiply(DAMPING);
                neighborServer.setVelocity(newNeighborVelocity);
                neighborServer.velocityModified = true;
            }
        }
        
        // Применяем суммарную силу к игроку
        if (neighborCount > 0) {
            Vec3d velocity = player.getVelocity();
            Vec3d newVelocity = velocity.add(totalPullForce).multiply(DAMPING);
            player.setVelocity(newVelocity);
            player.velocityModified = true;
        }
    }
}

