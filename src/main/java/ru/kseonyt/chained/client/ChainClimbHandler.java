package ru.kseonyt.chained.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import ru.kseonyt.chained.client.network.ChainClientNetworkHandler;

/**
 * Обрабатывает взбирание по цепи при нажатии Shift.
 */
public class ChainClimbHandler {
    private static boolean wasPressed = false;
    
    public static void register() {
        // Регистрируем обработчик тиков клиента
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            
            // Проверяем, связан ли игрок
            ChainClientData clientData = ChainClientData.getInstance();
            if (!clientData.isChained(client.player.getUuid())) {
                if (wasPressed) {
                    // Отправляем отключение взбирания, если игрок больше не связан
                    ChainClientNetworkHandler.sendClimbRequest(false);
                    wasPressed = false;
                }
                return;
            }
            
            // Проверяем нажатие Shift (sneak key) и что игрок смотрит вверх
            boolean isPressed = client.options.sneakKey.isPressed();
            boolean isLookingUp = client.player.getPitch() < -45.0f; // Смотрит вверх (более мягкий угол)
            
            // Взбирание работает только при нажатии Shift И смотря вверх
            boolean shouldClimb = isPressed && isLookingUp;
            
            // Отправляем пакет только при изменении состояния
            if (shouldClimb != wasPressed) {
                ChainClientNetworkHandler.sendClimbRequest(shouldClimb);
                wasPressed = shouldClimb;
            }
        });
    }
}

