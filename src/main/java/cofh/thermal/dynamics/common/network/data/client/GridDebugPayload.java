package cofh.thermal.dynamics.common.network.data.client;

import cofh.core.common.network.data.PayloadCodecs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public record GridDebugPayload(FriendlyByteBuf buf) implements CustomPacketPayload {

    public static final Type<GridDebugPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ID_THERMAL_DYNAMICS, "grid_debug_packet"));

    public static final StreamCodec<FriendlyByteBuf, GridDebugPayload> STREAM_CODEC = StreamCodec.composite(
            PayloadCodecs.REMAINING_BYTES, GridDebugPayload::buf,
            GridDebugPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {

        return TYPE;
    }

}
