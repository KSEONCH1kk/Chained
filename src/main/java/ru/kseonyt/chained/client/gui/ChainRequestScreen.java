package ru.kseonyt.chained.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.kseonyt.chained.client.network.ChainClientNetworkHandler;

import java.util.UUID;

/**
 * Экран для принятия/отклонения запроса на связывание.
 */
public class ChainRequestScreen extends Screen {
    private final UUID senderId;
    private final String senderName;
    private int ticksOpen = 0;
    private static final int TIMEOUT_TICKS = 600; // 30 секунд
    
    public ChainRequestScreen(UUID senderId, String senderName) {
        super(Text.translatable("chained.request.title"));
        this.senderId = senderId;
        this.senderName = senderName;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Кнопка "Принять"
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("chained.request.accept"),
            button -> {
                ChainClientNetworkHandler.sendResponse(true, senderId);
                this.close();
            })
            .dimensions(centerX - 105, centerY + 10, 100, 20)
            .build());
        
        // Кнопка "Отклонить"
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("chained.request.decline"),
            button -> {
                ChainClientNetworkHandler.sendResponse(false, senderId);
                this.close();
            })
            .dimensions(centerX + 5, centerY + 10, 100, 20)
            .build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, 
            Text.translatable("chained.request.title"), centerX, centerY - 40, 0xFFFFFF);
        
        // Текст запроса
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.translatable("chained.request.message", senderName), centerX, centerY - 20, 0xCCCCCC);
        
        // Таймер
        int remaining = (TIMEOUT_TICKS - ticksOpen) / 20;
        if (remaining > 0) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("chained.request.timeout", remaining), centerX, centerY + 35, 0xAAAAAA);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void tick() {
        super.tick();
        ticksOpen++;
        
        // Автоматическое закрытие при таймауте
        if (ticksOpen >= TIMEOUT_TICKS) {
            ChainClientNetworkHandler.sendResponse(false, senderId);
            this.close();
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}

