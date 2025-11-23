package ru.kseonyt.chained.chain;

import java.util.*;

/**
 * Управляет запросами на связывание между игроками.
 * Реализует паттерн Singleton.
 */
public class ChainRequestManager {
    private static ChainRequestManager instance;
    
    // Хранит активные запросы: UUID получателя -> UUID отправителя
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    // Таймаут запросов (в тиках, 20 тиков = 1 секунда)
    private static final int REQUEST_TIMEOUT = 600; // 30 секунд
    
    private ChainRequestManager() {}
    
    public static ChainRequestManager getInstance() {
        if (instance == null) {
            instance = new ChainRequestManager();
        }
        return instance;
    }
    
    /**
     * Создает запрос на связывание
     * @param chainManager менеджер цепей для проверки связей
     */
    public boolean createRequest(UUID sender, UUID receiver, ChainManager chainManager) {
        if (pendingRequests.containsKey(receiver)) {
            return false; // Уже есть активный запрос
        }
        // Проверяем, что игроки не связаны друг с другом напрямую
        if (chainManager.areChained(sender, receiver)) {
            return false; // Уже связаны
        }
        pendingRequests.put(receiver, sender);
        return true;
    }
    
    /**
     * Принимает запрос
     */
    public boolean acceptRequest(UUID receiver) {
        UUID sender = pendingRequests.remove(receiver);
        return sender != null;
    }
    
    /**
     * Отклоняет запрос
     */
    public boolean declineRequest(UUID receiver) {
        return pendingRequests.remove(receiver) != null;
    }
    
    /**
     * Получает отправителя запроса для получателя
     */
    public UUID getRequestSender(UUID receiver) {
        return pendingRequests.get(receiver);
    }
    
    /**
     * Проверяет, есть ли активный запрос для игрока
     */
    public boolean hasPendingRequest(UUID receiver) {
        return pendingRequests.containsKey(receiver);
    }
    
    /**
     * Удаляет запрос (используется при таймауте или других случаях)
     */
    public void removeRequest(UUID receiver) {
        pendingRequests.remove(receiver);
    }
    
    /**
     * Очищает все запросы (например, при отключении игрока)
     */
    public void clearRequestsForPlayer(UUID player) {
        pendingRequests.entrySet().removeIf(entry -> 
            entry.getKey().equals(player) || entry.getValue().equals(player)
        );
    }
}

