package cofh.thermal.dynamics.common.attachment;

import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.IFilter;
import cofh.lib.api.IConveyableData;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.inventory.attachment.FluidFilterAttachmentMenu;
import cofh.thermal.dynamics.common.network.packet.server.AttachmentConfigPacket;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static cofh.lib.util.constants.NBTTags.TAG_MODE;
import static cofh.lib.util.constants.NBTTags.TAG_TYPE;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.client.TDynTextures.*;
import static cofh.thermal.dynamics.init.registries.TDynIDs.FILTER;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ID_FILTER_ATTACHMENT;
import static net.covers1624.quack.util.SneakyUtils.unsafeCast;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class FluidFilterAttachment implements IFilterableAttachment, IRedstoneControllableAttachment, IConveyableData, MenuProvider {

    public enum FilterMode {
        BIDIRECTIONAL, TO_EXTERNAL_ONLY, TO_GRID_ONLY;

        public static final FilterMode[] VALUES = values();
    }

    public static final Component DISPLAY_NAME = Component.translatable("attachment.thermal.filter");

    protected final IDuct<?, ?> duct;
    protected final Direction side;

    protected FilterMode mode = FilterMode.BIDIRECTIONAL;

    protected BaseFluidFilter filter = new BaseFluidFilter(15);
    protected RedstoneControlLogic rsControl = new RedstoneControlLogic(this);

    protected IFluidHandler gridCap = null;
    protected IFluidHandler extCap = null;

    public FluidFilterAttachment(IDuct<?, ?> duct, Direction side) {

        this.duct = duct;
        this.side = side;
    }

    public FilterMode getFilterMode() {

        return mode;
    }

    public void setFilterMode(FilterMode mode) {

        this.mode = mode;
        AttachmentConfigPacket.sendToServer(this);
    }

    @Override
    public IDuct<?, ?> duct() {

        return duct;
    }

    @Override
    public Direction side() {

        return side;
    }

    @Override
    public void invalidate() {

        gridCap = null;
        extCap = null;
    }

    @Override
    public IAttachment read(HolderLookup.Provider registries, CompoundTag nbt) {

        if (nbt.isEmpty()) {
            return this;
        }
        mode = FilterMode.VALUES[nbt.getByteOr(TAG_MODE, (byte) 0)];

        filter.read(registries, nbt);
        rsControl.read(nbt);

        return this;
    }

    @Override
    public CompoundTag write(HolderLookup.Provider registries, CompoundTag nbt) {

        nbt.putString(TAG_TYPE, FILTER);
        nbt.putByte(TAG_MODE, (byte) mode.ordinal());

        filter.write(registries, nbt);
        rsControl.write(nbt);

        return nbt;
    }

    @Override
    public ItemStack getItem() {

        return new ItemStack(ITEMS.get(ID_FILTER_ATTACHMENT));
    }

    @Override
    public Identifier getTexture() {

        switch (mode) {
            case TO_EXTERNAL_ONLY -> {
                return rsControl.getState() ? FILTER_ATTACHMENT_TO_EXTERNAL_ACTIVE_LOC : FILTER_ATTACHMENT_TO_EXTERNAL_LOC;
            }
            case TO_GRID_ONLY -> {
                return rsControl.getState() ? FILTER_ATTACHMENT_TO_GRID_ACTIVE_LOC : FILTER_ATTACHMENT_TO_GRID_LOC;
            }
            default -> {
                return rsControl.getState() ? FILTER_ATTACHMENT_ACTIVE_LOC : FILTER_ATTACHMENT_LOC;
            }
        }
    }

    @Override
    public Component getDisplayName() {

        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new FluidFilterAttachmentMenu(i, player.level(), pos(), side, inventory, player);
    }

    @Nullable
    @Override
    @SuppressWarnings ("unchecked")
    public <T, C> T wrapGridCapability(BlockCapability<T, C> capability, T gridCapIn) {

        if (capability == Capabilities.Fluid.BLOCK) {
            if (gridCap != null) {
                return (T) gridCap;
            }
            if (gridCapIn instanceof ResourceHandler<?> handler) {
                gridCap = new WrappedGridFluidHandler(unsafeCast(handler), e -> rsControl.getState() && filter.valid(e) || !rsControl.getState());
                return (T) gridCap;
            }
        }
        return gridCapIn;
    }

    @Nullable
    @Override
    @SuppressWarnings ("unchecked")
    public <T, C> T wrapExternalCapability(BlockCapability<T, C> capability, T extCapIn) {

        if (capability == Capabilities.Fluid.BLOCK) {
            if (extCap != null) {
                return (T) extCap;
            }
            if (extCapIn instanceof ResourceHandler<?> handler) {
                extCap = new WrappedExternalFluidHandler(unsafeCast(handler), e -> rsControl.getState() && filter.valid(e) || !rsControl.getState());
                return (T) extCap;
            }
        }
        return extCapIn;
    }

    // region IFilterableAttachment
    @Override
    public IFilter getFilter() {

        return filter;
    }
    // endregion

    // region IPacketHandlerAttachment
    @Override
    public FriendlyByteBuf getConfigPacket(FriendlyByteBuf buffer) {

        buffer.writeByte(mode.ordinal());

        buffer.writeBoolean(filter.getAllowList());
        buffer.writeBoolean(filter.getCheckNBT());

        return buffer;
    }

    @Override
    public void handleConfigPacket(FriendlyByteBuf buffer) {

        FilterMode prevMode = mode;
        mode = FilterMode.VALUES[buffer.readByte()];

        filter.setAllowList(buffer.readBoolean());
        filter.setCheckNBT(buffer.readBoolean());

        if (mode != prevMode) {
            onControlUpdate();
        }
    }

    @Override
    public FriendlyByteBuf getControlPacket(FriendlyByteBuf buffer) {

        buffer.writeByte(mode.ordinal());
        rsControl.writeToBuffer(buffer);

        buffer.writeBoolean(filter.getAllowList());
        buffer.writeBoolean(filter.getCheckNBT());

        return buffer;
    }

    @Override
    public void handleControlPacket(FriendlyByteBuf buffer) {

        mode = FilterMode.VALUES[buffer.readByte()];
        rsControl.readFromBuffer(buffer);

        filter.setAllowList(buffer.readBoolean());
        filter.setCheckNBT(buffer.readBoolean());
    }
    // endregion

    // region IRedstoneControllableAttachment
    @Override
    public RedstoneControlLogic redstoneControl() {

        return rsControl;
    }
    // endregion

    // region IConveyableData
    @Override
    public void readConveyableData(Player player, CompoundTag tag) {

        mode = FilterMode.VALUES[tag.getByteOr("FilterAttachmentMode", (byte) 0)];
        rsControl.readSettings(tag);
        filter.read(player.registryAccess(), tag);

        onControlUpdate();
    }

    @Override
    public void writeConveyableData(Player player, CompoundTag tag) {

        tag.putByte("FilterAttachmentMode", (byte) mode.ordinal());
        rsControl.writeSettings(tag);
        filter.write(player.registryAccess(), tag);
    }
    // endregion

    // region GRID WRAPPER CLASS
    private class WrappedGridFluidHandler implements IFluidHandler, ResourceHandler<FluidResource> {

        protected IFluidHandler wrappedHandler;
        protected ResourceHandler<FluidResource> wrappedResourceHandler;

        protected Predicate<FluidStack> validator;

        public WrappedGridFluidHandler(ResourceHandler<FluidResource> wrappedHandler, Predicate<FluidStack> validator) {

            this.wrappedHandler = IFluidHandler.of(wrappedHandler);
            this.wrappedResourceHandler = wrappedHandler;
            this.validator = validator;
        }

        @Override
        public int getTanks() {

            return wrappedHandler.getTanks();
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tank) {

            return wrappedHandler.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {

            return wrappedHandler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return false;
            }
            return validator.test(stack) && wrappedHandler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return 0;
            }
            return validator.test(resource) ? wrappedHandler.fill(resource, action) : 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return FluidStack.EMPTY;
            }
            return validator.test(resource) ? wrappedHandler.drain(resource, action) : FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return FluidStack.EMPTY;
            }
            return validator.test(wrappedHandler.drain(maxDrain, SIMULATE)) ? wrappedHandler.drain(maxDrain, action) : FluidStack.EMPTY;
        }

        // region ResourceHandler
        @Override
        public int size() {

            return wrappedResourceHandler.size();
        }

        @Override
        public FluidResource getResource(int index) {

            return wrappedResourceHandler.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {

            return wrappedResourceHandler.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {

            return wrappedResourceHandler.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return false;
            }
            return validator.test(resource.toStack(1)) && wrappedResourceHandler.isValid(index, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return 0;
            }
            return validator.test(resource.toStack(amount)) ? wrappedResourceHandler.insert(index, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return 0;
            }
            return validator.test(resource.toStack(amount)) ? wrappedResourceHandler.extract(index, resource, amount, transaction) : 0;
        }
        // endregion

    }
    // endregion

    // region EXTERNAL WRAPPER CLASS
    private class WrappedExternalFluidHandler implements IFluidHandler, ResourceHandler<FluidResource> {

        protected IFluidHandler wrappedHandler;
        protected ResourceHandler<FluidResource> wrappedResourceHandler;

        protected Predicate<FluidStack> validator;

        public WrappedExternalFluidHandler(ResourceHandler<FluidResource> wrappedHandler, Predicate<FluidStack> validator) {

            this.wrappedHandler = IFluidHandler.of(wrappedHandler);
            this.wrappedResourceHandler = wrappedHandler;
            this.validator = validator;
        }

        @Override
        public int getTanks() {

            return wrappedHandler.getTanks();
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tank) {

            return wrappedHandler.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {

            return wrappedHandler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return false;
            }
            return validator.test(stack) && wrappedHandler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return 0;
            }
            return validator.test(resource) ? wrappedHandler.fill(resource, action) : 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return FluidStack.EMPTY;
            }
            return validator.test(resource) ? wrappedHandler.drain(resource, action) : FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return FluidStack.EMPTY;
            }
            return validator.test(wrappedHandler.drain(maxDrain, SIMULATE)) ? wrappedHandler.drain(maxDrain, action) : FluidStack.EMPTY;
        }

        // region ResourceHandler
        @Override
        public int size() {

            return wrappedResourceHandler.size();
        }

        @Override
        public FluidResource getResource(int index) {

            return wrappedResourceHandler.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {

            return wrappedResourceHandler.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {

            return wrappedResourceHandler.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return false;
            }
            return validator.test(resource.toStack(1)) && wrappedResourceHandler.isValid(index, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return 0;
            }
            return validator.test(resource.toStack(amount)) ? wrappedResourceHandler.insert(index, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return 0;
            }
            return validator.test(resource.toStack(amount)) ? wrappedResourceHandler.extract(index, resource, amount, transaction) : 0;
        }
        // endregion

    }
    // endregion
}
