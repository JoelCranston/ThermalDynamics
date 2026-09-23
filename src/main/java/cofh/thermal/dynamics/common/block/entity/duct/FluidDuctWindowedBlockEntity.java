package cofh.thermal.dynamics.common.block.entity.duct;

import cofh.core.common.network.packet.client.TileStatePacket;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.RenderHelper;
import cofh.lib.api.block.entity.IPacketHandlerTile;
import cofh.thermal.dynamics.api.grid.IGridHostLuminous;
import cofh.thermal.dynamics.api.grid.IGridHostUpdateable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static cofh.lib.util.constants.NBTTags.TAG_RENDER_FLUID;
import static cofh.thermal.core.client.ThermalTextures.BLANK_TEXTURE;
import static cofh.thermal.dynamics.init.registries.TDynBlockEntities.FLUID_DUCT_WINDOWED_BLOCK_ENTITY;

public class FluidDuctWindowedBlockEntity extends FluidDuctBlockEntity implements IGridHostUpdateable, IGridHostLuminous, IPacketHandlerTile {

    FluidStack renderFluid = FluidStack.EMPTY;

    public FluidDuctWindowedBlockEntity(BlockPos pos, BlockState state) {

        super(FLUID_DUCT_WINDOWED_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void update() {

        TileStatePacket.sendToClient(this);
    }

    @Override
    public int getLightValue() {

        return FluidHelper.luminosity(renderFluid);
    }

    @Nonnull
    @Override
    public ModelData getModelData() {

        modelData.setFill(renderFluid.isEmpty() ? BLANK_TEXTURE : RenderHelper.getFluidTexture(renderFluid).contents().name());
        modelData.setFillColor(FluidHelper.color(renderFluid));
        return super.getModelData();
    }

    // region NBT
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {

        if (!renderFluid.isEmpty()) {
            tag.put(TAG_RENDER_FLUID, renderFluid.save(registries));
        }
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);

        renderFluid = FluidStack.parseOptional(registries, tag.getCompound(TAG_RENDER_FLUID));
    }
    // endregion

    // region NETWORK
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {

        return saveWithoutMetadata(registries);
    }

    // STATE
    @Override
    public FriendlyByteBuf getStatePacket(FriendlyByteBuf buffer) {

        renderFluid = getGrid().getRenderFluid();
        // Not a RegistryFriendlyByteBuf, so FluidStack.STREAM_CODEC can't be used here.
        buffer.writeIdentifier(BuiltInRegistries.FLUID.getKey(renderFluid.getFluid()));
        buffer.writeVarInt(renderFluid.getAmount());

        super.getStatePacket(buffer);

        return buffer;
    }

    @Override
    public void handleStatePacket(FriendlyByteBuf buffer) {

        int prevLight = getLightValue();
        renderFluid = new FluidStack(BuiltInRegistries.FLUID.get(buffer.readIdentifier()), buffer.readVarInt());

        if (prevLight != getLightValue()) {
            level.getChunkSource().getLightEngine().checkBlock(worldPosition);
        }
        super.handleStatePacket(buffer);
    }
    // endregion
}
