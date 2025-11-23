package ru.kseonyt.chained.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.kseonyt.chained.Chained;

import java.util.UUID;

/**
 * Пакет обновления связи (сервер -> клиент).
 */
public record ChainUpdatePayload(boolean create, UUID player1, UUID player2) implements CustomPayload {
    public static final CustomPayload.Id<ChainUpdatePayload> ID = new CustomPayload.Id<>(Identifier.of(Chained.MOD_ID, "chain_update"));
    public static final PacketCodec<PacketByteBuf, ChainUpdatePayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeBoolean(payload.create);
            buf.writeUuid(payload.player1);
            buf.writeUuid(payload.player2);
        },
        buf -> new ChainUpdatePayload(buf.readBoolean(), buf.readUuid(), buf.readUuid())
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

