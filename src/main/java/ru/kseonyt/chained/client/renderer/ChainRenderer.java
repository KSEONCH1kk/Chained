package ru.kseonyt.chained.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import ru.kseonyt.chained.client.ChainClientData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Рендерит визуальную цепь между связанными игроками.
 */
public class ChainRenderer {
    private static final Identifier CHAIN_TEXTURE = Identifier.ofVanilla("block/chain");
    private static final int CHAIN_SEGMENTS = 24; // Количество сегментов цепи
    private static final float CHAIN_WIDTH = 0.15f; // Ширина текстуры цепи
    private static final float CHAIN_SAG = 0.1f; // Провисание цепи

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ChainRenderer::renderChains);
    }

    private static void renderChains(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || client.world == null) {
            return;
        }

        ChainClientData clientData = ChainClientData.getInstance();
        
        // Рендерим все видимые связи между игроками в мире
        // Используем Set для отслеживания уже отрендеренных пар, чтобы избежать дублирования
        Set<String> renderedPairs = new HashSet<>();
        
        for (PlayerEntity player1 : client.world.getPlayers()) {
            UUID player1Id = player1.getUuid();
            
            // Проверяем, есть ли у этого игрока связи
            if (!clientData.isChained(player1Id)) {
                continue;
            }
            
            // Рендерим все связи этого игрока
            Set<UUID> chainedPlayers = clientData.getChainedPlayers(player1Id);
            for (UUID player2Id : chainedPlayers) {
                // Создаем уникальный ключ для пары (меньший UUID всегда первый)
                String pairKey = player1Id.compareTo(player2Id) < 0 
                    ? player1Id.toString() + "_" + player2Id.toString()
                    : player2Id.toString() + "_" + player1Id.toString();
                
                // Рендерим цепь только один раз для каждой пары
                if (!renderedPairs.contains(pairKey)) {
                    PlayerEntity player2 = client.world.getPlayerByUuid(player2Id);
                    if (player2 != null) {
                        renderChainBetweenPlayers(context, player1, player2);
                        renderedPairs.add(pairKey);
                    }
                }
            }
        }
    }

    private static void renderChainBetweenPlayers(WorldRenderContext context,
                                                  PlayerEntity player1,
                                                  PlayerEntity player2) {
        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();

        // Получаем позиции игроков с интерполяцией
        Vec3d pos1 = getInterpolatedPosition(player1, context.tickCounter().getTickDelta(true));
        Vec3d pos2 = getInterpolatedPosition(player2, context.tickCounter().getTickDelta(true));

        // Получаем позицию камеры
        Vec3d cameraPos = camera.getPos();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Получаем текстуру цепи
        MinecraftClient client = MinecraftClient.getInstance();
        Sprite chainSprite = client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .apply(CHAIN_TEXTURE);

        float minU = chainSprite.getMinU();
        float maxU = chainSprite.getMaxU();
        float minV = chainSprite.getMinV();
        float maxV = chainSprite.getMaxV();

        // Настройка рендеринга
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR_TEX_LIGHTMAP);
        RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        double totalDistance = pos1.distanceTo(pos2);

        // Рисуем цепь сегментами
        for (int i = 0; i < CHAIN_SEGMENTS; i++) {
            float t1 = (float) i / CHAIN_SEGMENTS;
            float t2 = (float) (i + 1) / CHAIN_SEGMENTS;

            // Получаем точки с провисанием
            Vec3d point1 = getChainPoint(pos1, pos2, t1, totalDistance);
            Vec3d point2 = getChainPoint(pos1, pos2, t2, totalDistance);

            // Вектор от камеры к середине сегмента
            Vec3d segmentCenter = point1.add(point2).multiply(0.5);
            Vec3d toCamera = cameraPos.subtract(segmentCenter).normalize();

            // Вектор направления сегмента
            Vec3d direction = point2.subtract(point1).normalize();

            // Вычисляем перпендикуляр (billboard) - вектор вправо относительно камеры
            Vec3d right = direction.crossProduct(toCamera).normalize();

            // Проверка на вырожденный случай
            if (right.lengthSquared() < 0.001) {
                right = new Vec3d(1, 0, 0);
            }

            Vec3d offset = right.multiply(CHAIN_WIDTH / 2);

            // Координаты текстуры
            float vOffset = t1 * 4; // Повторяем текстуру
            float v1 = minV + (maxV - minV) * (vOffset % 1.0f);
            float v2 = minV + (maxV - minV) * ((vOffset + 1.0f / CHAIN_SEGMENTS * 4) % 1.0f);

            // Создаем четыре вершины квада
            Vec3d p1 = point1.subtract(offset);
            Vec3d p2 = point1.add(offset);
            Vec3d p3 = point2.add(offset);
            Vec3d p4 = point2.subtract(offset);

            // Цвет и освещение
            int light = 15728880; // Полное освещение

            // Добавляем вершины (против часовой стрелки для правильного отображения)
            buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z)
                    .color(255, 255, 255, 255)
                    .texture(minU, v1)
                    .light(light);

            buffer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z)
                    .color(255, 255, 255, 255)
                    .texture(maxU, v1)
                    .light(light);

            buffer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z)
                    .color(255, 255, 255, 255)
                    .texture(maxU, v2)
                    .light(light);

            buffer.vertex(matrix, (float) p4.x, (float) p4.y, (float) p4.z)
                    .color(255, 255, 255, 255)
                    .texture(minU, v2)
                    .light(light);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Восстановление состояния рендеринга
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);

        matrices.pop();
    }

    /**
     * Получает точку на цепи с провисанием (катенарная кривая)
     */
    private static Vec3d getChainPoint(Vec3d start, Vec3d end, float t, double distance) {
        // Линейная интерполяция между точками
        Vec3d point = start.lerp(end, t);

        // Добавляем провисание (синусоида для упрощенной катенарной кривой)
        double sag = CHAIN_SAG * distance * Math.sin(t * Math.PI);

        return point.add(0, -sag, 0);
    }

    /**
     * Получает интерполированную позицию игрока для плавной анимации
     */
    private static Vec3d getInterpolatedPosition(PlayerEntity player, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
        double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()) + player.getStandingEyeHeight() * 0.5;
        double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());
        return new Vec3d(x, y, z);
    }
}