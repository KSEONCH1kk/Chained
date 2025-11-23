package ru.kseonyt.chained.chain;

import java.util.*;

/**
 * Управляет связями между игроками.
 * Поддерживает цепочки из нескольких игроков.
 * Реализует паттерн Singleton для единой точки доступа.
 */
public class ChainManager {
    private static ChainManager instance;
    
    // Хранит связи как граф: UUID игрока -> Set<UUID> связанных игроков
    private final Map<UUID, Set<UUID>> chains = new HashMap<>();
    
    private static final double MAX_CHAIN_LENGTH = 5.0; // Максимальная длина цепи в блоках
    
    private ChainManager() {}
    
    public static ChainManager getInstance() {
        if (instance == null) {
            instance = new ChainManager();
        }
        return instance;
    }
    
    /**
     * Создает связь между двумя игроками
     */
    public void createChain(UUID player1, UUID player2) {
        chains.computeIfAbsent(player1, k -> new HashSet<>()).add(player2);
        chains.computeIfAbsent(player2, k -> new HashSet<>()).add(player1);
    }
    
    /**
     * Удаляет связь между игроками
     */
    public void removeChain(UUID player1, UUID player2) {
        Set<UUID> neighbors1 = chains.get(player1);
        if (neighbors1 != null) {
            neighbors1.remove(player2);
            if (neighbors1.isEmpty()) {
                chains.remove(player1);
            }
        }
        
        Set<UUID> neighbors2 = chains.get(player2);
        if (neighbors2 != null) {
            neighbors2.remove(player1);
            if (neighbors2.isEmpty()) {
                chains.remove(player2);
            }
        }
    }
    
    /**
     * Проверяет, связаны ли два игрока напрямую
     */
    public boolean areChained(UUID player1, UUID player2) {
        Set<UUID> neighbors = chains.get(player1);
        return neighbors != null && neighbors.contains(player2);
    }
    
    /**
     * Получает всех связанных игроков (соседей)
     */
    public Set<UUID> getChainedPlayers(UUID player) {
        return chains.getOrDefault(player, Collections.emptySet());
    }
    
    /**
     * Получает первого связанного игрока (для обратной совместимости)
     */
    public UUID getChainedPlayer(UUID player) {
        Set<UUID> neighbors = chains.get(player);
        if (neighbors != null && !neighbors.isEmpty()) {
            return neighbors.iterator().next();
        }
        return null;
    }
    
    /**
     * Проверяет, связан ли игрок с кем-либо
     */
    public boolean isChained(UUID player) {
        Set<UUID> neighbors = chains.get(player);
        return neighbors != null && !neighbors.isEmpty();
    }
    
    /**
     * Удаляет все связи игрока
     */
    public void removeAllChains(UUID player) {
        Set<UUID> neighbors = chains.remove(player);
        if (neighbors != null) {
            // Удаляем обратные ссылки
            for (UUID neighbor : neighbors) {
                Set<UUID> neighborNeighbors = chains.get(neighbor);
                if (neighborNeighbors != null) {
                    neighborNeighbors.remove(player);
                    if (neighborNeighbors.isEmpty()) {
                        chains.remove(neighbor);
                    }
                }
            }
        }
    }
    
    public double getMaxChainLength() {
        return MAX_CHAIN_LENGTH;
    }
    
    /**
     * Получает все активные связи (для отладки)
     */
    public Map<UUID, Set<UUID>> getAllChains() {
        Map<UUID, Set<UUID>> result = new HashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : chains.entrySet()) {
            result.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Находит всех игроков в связанной компоненте (цепочке), используя BFS
     */
    public Set<UUID> getConnectedComponent(UUID player) {
        Set<UUID> component = new HashSet<>();
        if (!chains.containsKey(player)) {
            return component;
        }
        
        Queue<UUID> queue = new LinkedList<>();
        queue.add(player);
        component.add(player);
        
        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            Set<UUID> neighbors = chains.get(current);
            if (neighbors != null) {
                for (UUID neighbor : neighbors) {
                    if (!component.contains(neighbor)) {
                        component.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        
        return component;
    }
}

