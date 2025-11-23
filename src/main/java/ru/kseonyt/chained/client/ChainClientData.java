package ru.kseonyt.chained.client;

import java.util.*;

/**
 * Хранит данные о связях на клиенте для рендеринга.
 * Поддерживает цепочки из нескольких игроков.
 */
public class ChainClientData {
    private static ChainClientData instance;
    
    // Хранит связи для клиента: UUID игрока -> Set<UUID> связанных игроков
    private final Map<UUID, Set<UUID>> chains = new HashMap<>();
    
    private ChainClientData() {}
    
    public static ChainClientData getInstance() {
        if (instance == null) {
            instance = new ChainClientData();
        }
        return instance;
    }
    
    public void setChain(UUID player1, UUID player2) {
        chains.computeIfAbsent(player1, k -> new HashSet<>()).add(player2);
        chains.computeIfAbsent(player2, k -> new HashSet<>()).add(player1);
    }
    
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
    
    public Set<UUID> getChainedPlayers(UUID player) {
        return chains.getOrDefault(player, Collections.emptySet());
    }
    
    public UUID getChainedPlayer(UUID player) {
        Set<UUID> neighbors = chains.get(player);
        if (neighbors != null && !neighbors.isEmpty()) {
            return neighbors.iterator().next();
        }
        return null;
    }
    
    public boolean isChained(UUID player) {
        Set<UUID> neighbors = chains.get(player);
        return neighbors != null && !neighbors.isEmpty();
    }
    
    public void clear() {
        chains.clear();
    }
}


