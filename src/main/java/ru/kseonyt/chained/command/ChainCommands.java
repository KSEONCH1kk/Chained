package ru.kseonyt.chained.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import ru.kseonyt.chained.chain.ChainManager;
import ru.kseonyt.chained.network.ChainNetworkHandler;

/**
 * Команды для управления цепями.
 */
public class ChainCommands {
    
    public static void register() {
        CommandRegistrationCallback.EVENT.register(ChainCommands::registerCommands);
    }
    
    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
                                         CommandRegistryAccess registryAccess,
                                         CommandManager.RegistrationEnvironment environment) {
        
        // Команда /forcelink <player1> <player2>
        dispatcher.register(CommandManager.literal("forcelink")
            .requires(source -> source.hasPermissionLevel(2)) // Требует уровень оператора 2
            .then(CommandManager.argument("player1", StringArgumentType.string())
                .then(CommandManager.argument("player2", StringArgumentType.string())
                    .executes(context -> forceLink(context))
                )
            )
        );
        
        // Команда /forceunlink <player1> <player2>
        dispatcher.register(CommandManager.literal("forceunlink")
            .requires(source -> source.hasPermissionLevel(2)) // Требует уровень оператора 2
            .then(CommandManager.argument("player1", StringArgumentType.string())
                .then(CommandManager.argument("player2", StringArgumentType.string())
                    .executes(context -> forceUnlink(context))
                )
            )
        );
    }
    
    private static int forceLink(CommandContext<ServerCommandSource> context) {
        String player1Name = StringArgumentType.getString(context, "player1");
        String player2Name = StringArgumentType.getString(context, "player2");
        
        ServerCommandSource source = context.getSource();
        var server = source.getServer();
        
        // Находим игроков по имени
        ServerPlayerEntity player1 = server.getPlayerManager().getPlayer(player1Name);
        ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(player2Name);
        
        if (player1 == null) {
            source.sendError(Text.literal("Игрок '" + player1Name + "' не найден или не в сети"));
            return 0;
        }
        
        if (player2 == null) {
            source.sendError(Text.literal("Игрок '" + player2Name + "' не найден или не в сети"));
            return 0;
        }
        
        if (player1.getUuid().equals(player2.getUuid())) {
            source.sendError(Text.literal("Нельзя связать игрока с самим собой"));
            return 0;
        }
        
        ChainManager chainManager = ChainManager.getInstance();
        
        // Проверяем, не связаны ли уже
        if (chainManager.areChained(player1.getUuid(), player2.getUuid())) {
            source.sendError(Text.literal("Игроки уже связаны"));
            return 0;
        }
        
        // Создаем связь
        chainManager.createChain(player1.getUuid(), player2.getUuid());
        
        // Синхронизируем со всеми клиентами
        ChainNetworkHandler.broadcastChainUpdate(player1, player2, true);
        
        // Уведомляем игроков
        player1.sendMessage(Text.literal("Вы были принудительно связаны с " + player2Name), false);
        player2.sendMessage(Text.literal("Вы были принудительно связаны с " + player1Name), false);
        source.sendFeedback(() -> Text.literal("Игроки " + player1Name + " и " + player2Name + " были принудительно связаны"), true);
        
        return 1;
    }
    
    private static int forceUnlink(CommandContext<ServerCommandSource> context) {
        String player1Name = StringArgumentType.getString(context, "player1");
        String player2Name = StringArgumentType.getString(context, "player2");
        
        ServerCommandSource source = context.getSource();
        var server = source.getServer();
        
        // Находим игроков по имени
        ServerPlayerEntity player1 = server.getPlayerManager().getPlayer(player1Name);
        ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(player2Name);
        
        if (player1 == null) {
            source.sendError(Text.literal("Игрок '" + player1Name + "' не найден или не в сети"));
            return 0;
        }
        
        if (player2 == null) {
            source.sendError(Text.literal("Игрок '" + player2Name + "' не найден или не в сети"));
            return 0;
        }
        
        ChainManager chainManager = ChainManager.getInstance();
        
        // Проверяем, связаны ли
        if (!chainManager.areChained(player1.getUuid(), player2.getUuid())) {
            source.sendError(Text.literal("Игроки не связаны"));
            return 0;
        }
        
        // Разрываем связь
        chainManager.removeChain(player1.getUuid(), player2.getUuid());
        
        // Синхронизируем со всеми клиентами
        ChainNetworkHandler.broadcastChainUpdate(player1, player2, false);
        
        // Уведомляем игроков
        player1.sendMessage(Text.literal("Связь с " + player2Name + " была принудительно разорвана"), false);
        player2.sendMessage(Text.literal("Связь с " + player1Name + " была принудительно разорвана"), false);
        source.sendFeedback(() -> Text.literal("Связь между " + player1Name + " и " + player2Name + " была принудительно разорвана"), true);
        
        return 1;
    }
}

