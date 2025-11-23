package ru.kseonyt.chained;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import ru.kseonyt.chained.chain.ChainManager;
import ru.kseonyt.chained.chain.ChainPhysics;
import ru.kseonyt.chained.chain.ChainRequestManager;
import ru.kseonyt.chained.init.ModItems;
import ru.kseonyt.chained.item.ChainItemHandler;
import ru.kseonyt.chained.network.ChainNetworkHandler;
import ru.kseonyt.chained.network.payload.ChainClimbPayload;
import ru.kseonyt.chained.network.payload.ChainRequestPayload;
import ru.kseonyt.chained.network.payload.ChainResponsePayload;
import ru.kseonyt.chained.network.payload.ChainSyncPayload;
import ru.kseonyt.chained.network.payload.ChainUpdatePayload;
import ru.kseonyt.chained.command.ChainCommands;
import ru.kseonyt.chained.server.ChainEventHandler;
import ru.kseonyt.chained.server.ChainServerTicker;

/**
 * Основной класс мода Chained.
 */
public class Chained implements ModInitializer {
    public static final String MOD_ID = "chained";
    
    private ChainManager chainManager;
    private ChainRequestManager requestManager;
    private ChainPhysics chainPhysics;
    private ChainItemHandler itemHandler;
    private ChainServerTicker serverTicker;
    private ChainEventHandler eventHandler;
    
    @Override
    public void onInitialize() {

        // Регистрация типов пакетов на сервере
        PayloadTypeRegistry.playS2C().register(ChainRequestPayload.ID, ChainRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChainUpdatePayload.ID, ChainUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ChainSyncPayload.ID, ChainSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ChainResponsePayload.ID, ChainResponsePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ChainClimbPayload.ID, ChainClimbPayload.CODEC);
        
        // Инициализация менеджеров
        chainManager = ChainManager.getInstance();
        requestManager = ChainRequestManager.getInstance();
        chainPhysics = new ChainPhysics(chainManager);
        
        // Регистрация обработчика взаимодействия с цепью
        itemHandler = new ChainItemHandler(chainManager, requestManager);
        itemHandler.register();
        
        // Регистрация сетевых пакетов
        ChainNetworkHandler.registerServerHandlers(itemHandler, chainPhysics);
        
        // Регистрация серверного тикера для физики
        serverTicker = new ChainServerTicker(chainPhysics, chainManager);
        serverTicker.register();
        
        // Регистрация обработчиков событий (смерть, переходы между измерениями, драконье дыхание)
        eventHandler = new ChainEventHandler(chainManager);
        eventHandler.register();
        
        // Регистрация команд
        ChainCommands.register();
    }
}
