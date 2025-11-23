package ru.kseonyt.chained.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.kseonyt.chained.Chained;

import java.util.UUID;

/**
 * Пакет ответа на запрос (клиент -> сервер).
 */
public record ChainResponsePayload(boolean accepted, UUID senderId) implements CustomPayload {
    public static final CustomPayload.Id<ChainResponsePayload> ID = new CustomPayload.Id<>(Identifier.of(Chained.MOD_ID, "chain_response"));
    public static final PacketCodec<PacketByteBuf, ChainResponsePayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeBoolean(payload.accepted);
            buf.writeUuid(payload.senderId);
        },
        buf -> new ChainResponsePayload(buf.readBoolean(), buf.readUuid())
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

