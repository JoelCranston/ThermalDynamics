package cofh.thermal.dynamics.common.network.packet.client;


import cofh.core.util.ProxyUtils;
import cofh.lib.util.Utils;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.attachment.IPacketHandlerAttachment;
import cofh.thermal.dynamics.common.network.data.client.AttachmentControlPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class AttachmentControlPacket {

    public static final AttachmentControlPacket INSTANCE = new AttachmentControlPacket();

    public static AttachmentControlPacket get() {

        return INSTANCE;
    }

    public void handle(final AttachmentControlPayload payload, final IPayloadContext context) {

        context.enqueueWork(() -> {
            Level world = ProxyUtils.getClientWorld();

            BlockPos pos = payload.pos();
            Direction side = payload.side();

            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof IDuct<?, ?> duct && duct.getAttachment(side) instanceof IPacketHandlerAttachment attachment) {
                attachment.handleControlPacket(payload.buf());
            }
        });
    }

    public static void sendToClient(IPacketHandlerAttachment attachment) {

        if (attachment == null || attachment.world() == null || attachment.world().isClientSide() || !attachment.hasControlPacket()) {
            return;
        }
        Utils.sendNear(attachment.world(), attachment.pos(), new AttachmentControlPayload(attachment.pos(), attachment.side(), attachment.getControlPacket(new FriendlyByteBuf(Unpooled.buffer()))));
    }
}
