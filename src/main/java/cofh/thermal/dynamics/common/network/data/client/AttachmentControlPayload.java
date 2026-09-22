package cofh.thermal.dynamics.common.network.data.client;

import cofh.core.common.network.data.PayloadCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public record AttachmentControlPayload(BlockPos pos, Direction side, FriendlyByteBuf buf) implements CustomPacketPayload {

    public static final Type<AttachmentControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ID_THERMAL_DYNAMICS, "attachment_control_packet"));

    public static final StreamCodec<FriendlyByteBuf, AttachmentControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AttachmentControlPayload::pos,
            Direction.STREAM_CODEC, AttachmentControlPayload::side,
            PayloadCodecs.REMAINING_BYTES, AttachmentControlPayload::buf,
            AttachmentControlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {

        return TYPE;
    }

}
