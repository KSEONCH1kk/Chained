package ru.kseonyt.chained.network.payload;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import ru.kseonyt.chained.Chained;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Пакет синхронизации целой цепочки игроков (сервер -> клиент).
 * Содержит список всех связей в цепочке.
 */
public record ChainSyncPayload(List<ChainLink> links) implements CustomPayload {
    public record ChainLink(UUID player1, UUID player2) {}
    
    public static final CustomPayload.Id<ChainSyncPayload> ID = new CustomPayload.Id<>(Identifier.of(Chained.MOD_ID, "chain_sync"));
    public static final PacketCodec<PacketByteBuf, ChainSyncPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeCollection(payload.links, (b, link) -> {
                b.writeUuid(link.player1);
                b.writeUuid(link.player2);
            });
        },
        buf -> {
            List<ChainLink> links = buf.readCollection(ArrayList::new, b -> 
                new ChainLink(b.readUuid(), b.readUuid())
            );
            return new ChainSyncPayload(links);
        }
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

