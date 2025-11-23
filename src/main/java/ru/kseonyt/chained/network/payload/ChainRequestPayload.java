package ru.kseonyt.chained.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.kseonyt.chained.Chained;

import java.util.UUID;

/**
 * Пакет запроса на связывание (сервер -> клиент).
 */
public record ChainRequestPayload(UUID senderId, String senderName) implements CustomPayload {
    public static final CustomPayload.Id<ChainRequestPayload> ID = new CustomPayload.Id<>(Identifier.of(Chained.MOD_ID, "chain_request"));
    public static final PacketCodec<PacketByteBuf, ChainRequestPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeUuid(payload.senderId);
            buf.writeString(payload.senderName);
        },
        buf -> new ChainRequestPayload(buf.readUuid(), buf.readString())
    );

    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

