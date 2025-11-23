package ru.kseonyt.chained.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.kseonyt.chained.client.network.ChainClientNetworkHandler;
import ru.kseonyt.chained.client.renderer.ChainRenderer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.kseonyt.chained.network.payload.ChainClimbPayload;
import ru.kseonyt.chained.network.payload.ChainRequestPayload;
import ru.kseonyt.chained.network.payload.ChainResponsePayload;
import ru.kseonyt.chained.network.payload.ChainSyncPayload;
import ru.kseonyt.chained.network.payload.ChainUpdatePayload;

/**
 * Клиентский класс мода Chained.
 */
public class ChainedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        

        
        // Регистрация сетевых пакетов на клиенте
        ChainClientNetworkHandler.register();
        
        // Регистрация обработчика взбирания
        ChainClimbHandler.register();
        
        // Регистрация рендерера цепи
        ChainRenderer.register();
    }
}
