package ru.kseonyt.chained.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.kseonyt.chained.Chained;

/**
 * Пакет для взбирания по цепи (клиент -> сервер).
 */
public record ChainClimbPayload(boolean climbing) implements CustomPayload {
    public static final CustomPayload.Id<ChainClimbPayload> ID = new CustomPayload.Id<>(Identifier.of(Chained.MOD_ID, "chain_climb"));
    public static final PacketCodec<PacketByteBuf, ChainClimbPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeBoolean(payload.climbing);
        },
        buf -> new ChainClimbPayload(buf.readBoolean())
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

