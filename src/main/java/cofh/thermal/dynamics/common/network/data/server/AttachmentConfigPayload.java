package cofh.thermal.dynamics.common.network.data.server;

import cofh.core.common.network.data.PayloadCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public record AttachmentConfigPayload(BlockPos pos, Direction side, FriendlyByteBuf buf) implements CustomPacketPayload {

    public static final Type<AttachmentConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ID_THERMAL_DYNAMICS, "attachment_config_packet"));

    public static final StreamCodec<FriendlyByteBuf, AttachmentConfigPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AttachmentConfigPayload::pos,
            Direction.STREAM_CODEC, AttachmentConfigPayload::side,
            PayloadCodecs.REMAINING_BYTES, AttachmentConfigPayload::buf,
            AttachmentConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {

        return TYPE;
    }

}
