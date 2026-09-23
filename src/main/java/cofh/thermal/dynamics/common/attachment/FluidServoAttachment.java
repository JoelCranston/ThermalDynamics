package cofh.thermal.dynamics.common.attachment;

import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.IFilter;
import cofh.lib.api.IConveyableData;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.inventory.attachment.FluidServoAttachmentMenu;
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

import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.lib.util.constants.NBTTags.TAG_AMOUNT;
import static cofh.lib.util.constants.NBTTags.TAG_TYPE;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.client.TDynTextures.SERVO_ATTACHMENT_ACTIVE_LOC;
import static cofh.thermal.dynamics.client.TDynTextures.SERVO_ATTACHMENT_LOC;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ID_SERVO_ATTACHMENT;
import static cofh.thermal.dynamics.init.registries.TDynIDs.SERVO;
import static net.covers1624.quack.util.SneakyUtils.unsafeCast;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class FluidServoAttachment implements IFilterableAttachment, IRedstoneControllableAttachment, IConveyableData, MenuProvider {

    public static final Component DISPLAY_NAME = Component.translatable("attachment.thermal.servo");

    public static final int TRANSFER = 50;
    public static final int MAX_TRANSFER = BUCKET_VOLUME;

    protected final IDuct<?, ?> duct;
    protected final Direction side;

    public int amountTransfer = TRANSFER;

    protected BaseFluidFilter filter = new BaseFluidFilter(15);
    protected RedstoneControlLogic rsControl = new RedstoneControlLogic(this);

    protected IFluidHandler internalGridCap = null;
    protected IFluidHandler gridCap = null;
    protected IFluidHandler extCap = null;

    public FluidServoAttachment(IDuct<?, ?> duct, Direction side) {

        this.duct = duct;
        this.side = side;
    }

    public final int getTransfer() {

        return TRANSFER;
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

        internalGridCap = null;
        gridCap = null;
        extCap = null;
    }

    @Override
    public IAttachment read(HolderLookup.Provider registries, CompoundTag nbt) {

        if (nbt.isEmpty()) {
            return this;
        }
        amountTransfer = nbt.getIntOr(TAG_AMOUNT, 0);

        filter.read(registries, nbt);
        rsControl.read(nbt);

        return this;
    }

    @Override
    public CompoundTag write(HolderLookup.Provider registries, CompoundTag nbt) {

        nbt.putString(TAG_TYPE, SERVO);
        nbt.putInt(TAG_AMOUNT, amountTransfer);

        filter.write(registries, nbt);
        rsControl.write(nbt);

        return nbt;
    }

    @Override
    public void tick() {

        if (!rsControl.getState()) {
            return;
        }
        if (internalGridCap == null) {
            ResourceHandler<FluidResource> gridHandler = duct.getGrid().getCapability(Capabilities.Fluid.BLOCK);
            internalGridCap = gridHandler == null ? null : IFluidHandler.of(gridHandler);
        }
        if (extCap != null && internalGridCap != null) {
            amountTransfer += TRANSFER;
            amountTransfer = Math.min(amountTransfer, MAX_TRANSFER);
            int toFill = internalGridCap.fill(extCap.drain(amountTransfer, SIMULATE), SIMULATE);
            internalGridCap.fill(extCap.drain(toFill, EXECUTE), EXECUTE);
            amountTransfer -= toFill;
        }
    }

    @Override
    public ItemStack getItem() {

        return new ItemStack(ITEMS.get(ID_SERVO_ATTACHMENT));
    }

    @Override
    public Identifier getTexture() {

        return rsControl.getState() ? SERVO_ATTACHMENT_ACTIVE_LOC : SERVO_ATTACHMENT_LOC;
    }

    @Override
    public Component getDisplayName() {

        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new FluidServoAttachmentMenu(i, player.level(), pos(), side, inventory, player);
    }

    @Nullable
    @Override
    public <T, C> T wrapGridCapability(BlockCapability<T, C> capability, T gridCapIn) {

        if (capability == Capabilities.Fluid.BLOCK) {
            if (gridCap != null) {
                return (T) gridCap;
            }
            if (gridCapIn instanceof ResourceHandler<?> handler) {
                gridCap = new WrappedGridFluidHandler(unsafeCast(handler));
                return (T) gridCap;
            }
        }
        return gridCapIn;
    }

    @Nullable
    @Override
    public <T, C> T wrapExternalCapability(BlockCapability<T, C> capability, T extCapIn) {

        if (capability == Capabilities.Fluid.BLOCK) {
            if (extCap != null) {
                return (T) extCap;
            }
            if (extCapIn instanceof ResourceHandler<?> handler) {
                extCap = new WrappedExternalFluidHandler(unsafeCast(handler), e -> rsControl.getState() && filter.valid(e));
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

        buffer.writeBoolean(filter.getAllowList());
        buffer.writeBoolean(filter.getCheckNBT());

        return buffer;
    }

    @Override
    public void handleConfigPacket(FriendlyByteBuf buffer) {

        filter.setAllowList(buffer.readBoolean());
        filter.setCheckNBT(buffer.readBoolean());
    }

    @Override
    public FriendlyByteBuf getControlPacket(FriendlyByteBuf buffer) {

        rsControl.writeToBuffer(buffer);

        buffer.writeBoolean(filter.getAllowList());
        buffer.writeBoolean(filter.getCheckNBT());

        return buffer;
    }

    @Override
    public void handleControlPacket(FriendlyByteBuf buffer) {

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

        rsControl.readSettings(tag);
        filter.read(player.registryAccess(), tag);

        onControlUpdate();
    }

    @Override
    public void writeConveyableData(Player player, CompoundTag tag) {

        rsControl.writeSettings(tag);
        filter.write(player.registryAccess(), tag);
    }
    // endregion

    // region GRID WRAPPER CLASS
    private static class WrappedGridFluidHandler implements IFluidHandler, ResourceHandler<FluidResource> {

        protected IFluidHandler wrappedHandler;
        protected ResourceHandler<FluidResource> wrappedResourceHandler;

        public WrappedGridFluidHandler(ResourceHandler<FluidResource> wrappedHandler) {

            this.wrappedHandler = IFluidHandler.of(wrappedHandler);
            this.wrappedResourceHandler = wrappedHandler;
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

            return wrappedHandler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {

            return 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {

            return FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

            return FluidStack.EMPTY;
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

            return wrappedResourceHandler.isValid(index, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {

            return 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {

            return 0;
        }
        // endregion

    }
    // endregion

    // region EXTERNAL WRAPPER CLASS
    private static class WrappedExternalFluidHandler implements IFluidHandler, ResourceHandler<FluidResource> {

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

            return validator.test(stack) && wrappedHandler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {

            return 0;
            // return validator.test(resource) ? wrappedHandler.fill(resource, action) : 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {

            return validator.test(resource) ? wrappedHandler.drain(resource, action) : FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

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

            return validator.test(resource.toStack(1)) && wrappedResourceHandler.isValid(index, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {

            return 0;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {

            return validator.test(resource.toStack(amount)) ? wrappedResourceHandler.extract(index, resource, amount, transaction) : 0;
        }
        // endregion

    }
    // endregion
}
