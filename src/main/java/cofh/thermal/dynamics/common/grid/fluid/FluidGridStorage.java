package cofh.thermal.dynamics.common.grid.fluid;

import cofh.core.util.helpers.FluidHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nonnull;

import static cofh.lib.util.constants.NBTTags.TAG_CAPACITY;
import static cofh.lib.util.constants.NBTTags.TAG_TRACK_OUT;

public final class FluidGridStorage implements IFluidHandler, ResourceHandler<FluidResource> {

    private int baseCapacity;
    private int capacity;

    private FluidStack fluid = FluidStack.EMPTY;

    private byte sampleTracker = 0;

    //    private final int[] samplesIn = new int[40];
    //    private int rollingIn = 0;
    //    private int averageIn = 0;

    private final int[] samplesOut = new int[40];
    private int rollingOut = 0;
    private int averageOut = 0;

    private final SnapshotJournal<FluidStack> journal = new SnapshotJournal<>() {

        @Override
        protected FluidStack createSnapshot() {

            return fluid.copy();
        }

        @Override
        protected void revertToSnapshot(FluidStack snapshot) {

            fluid = snapshot;
        }
    };

    public FluidGridStorage(int baseCapacity) {

        this.baseCapacity = baseCapacity;
    }

    public FluidGridStorage setBaseCapacity(int baseCapacity) {

        this.baseCapacity = Math.max(0, baseCapacity);
        //        if (!this.fluid.isEmpty()) {
        //            this.fluid.setAmount(MathHelper.clamp(this.fluid.getAmount(), 0, baseCapacity));
        //        }
        return this;
    }

    public FluidGridStorage setCapacity(int capacity) {

        this.capacity = capacity;
        resetTrackers();
        return this;
    }

    public FluidGridStorage setFluid(FluidStack fluid) {

        this.fluid = fluid.copy();
        //        if (!this.fluid.isEmpty()) {
        //            this.fluid.setAmount(MathHelper.clamp(this.fluid.getAmount(), 0, baseCapacity));
        //        }
        return this;
    }

    public void resetTrackers() {

        sampleTracker = 0;

        //        rollingIn = 0;
        //        averageIn = 0;

        rollingOut = 0;
        averageOut = 0;
    }

    public int getCapacity() {

        return capacity;
    }

    public FluidStack getFluid() {

        return fluid;
    }

    public void tick() {

        samplesOut[sampleTracker] = fluid.getAmount();
    }

    public void postTick() {

        //        rollingIn += samplesIn[sampleTracker];
        //        averageIn = rollingIn / samplesIn.length;

        samplesOut[sampleTracker] -= fluid.getAmount();
        rollingOut += samplesOut[sampleTracker];
        averageOut = rollingOut / samplesOut.length;

        ++sampleTracker;
        if (sampleTracker >= samplesOut.length) {
            sampleTracker = 0;
            updateCapacity();

            //            System.out.println("Average attempted input (2 seconds): " + averageIn);
            //            System.out.println("Average realized output (2 seconds): " + averageOut);
            //            System.out.println("Dynamic capacity:" + capacity);
            //            System.out.println("Fluid stored:" + fluid.getAmount());
        }
        //        rollingIn -= samplesIn[sampleTracker];
        //        samplesIn[sampleTracker] = 0;
        rollingOut -= samplesOut[sampleTracker];
        samplesOut[sampleTracker] = 0;
    }

    private void updateCapacity() {

        this.capacity = Math.max(baseCapacity, 4 * averageOut);
    }

    // region NBT
    public FluidGridStorage read(HolderLookup.Provider registries, CompoundTag nbt) {

        setFluid(FluidHelper.parseOptional(registries, nbt));
        this.baseCapacity = nbt.getIntOr(TAG_CAPACITY, 0);

        //        this.averageIn = nbt.getInt(TAG_TRACK_IN);
        this.averageOut = nbt.getIntOr(TAG_TRACK_OUT, 0);

        updateCapacity();
        return this;
    }

    public CompoundTag write(HolderLookup.Provider registries, CompoundTag nbt) {

        // save() throws on an empty stack.
        if (FluidHelper.saveOptional(registries, fluid) instanceof CompoundTag savedTag) {
            nbt.merge(savedTag);
        }
        nbt.putInt(TAG_CAPACITY, baseCapacity);

        //        nbt.putInt(TAG_TRACK_IN, averageIn);
        nbt.putInt(TAG_TRACK_OUT, averageOut);

        return nbt;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider registries) {

        return write(registries, new CompoundTag());
    }

    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {

        read(registries, nbt);
    }
    // endregion

    @Override
    public int getTanks() {

        return 1;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {

        return fluid;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {

        if (resource.isEmpty() || !isFluidValid(0, resource)) {
            return 0;
        }
        if (action.simulate()) {
            if (fluid.isEmpty()) {
                return Math.min(capacity, resource.getAmount());
            }
            if (!FluidStack.isSameFluidSameComponents(fluid, resource)) {
                return 0;
            }
            return Math.min(capacity - fluid.getAmount(), resource.getAmount());
        }
        if (fluid.isEmpty()) {
            setFluid(resource.copyWithAmount(Math.min(capacity, resource.getAmount())));
            return fluid.getAmount();
        }
        if (!FluidStack.isSameFluidSameComponents(fluid, resource)) {
            return 0;
        }
        if (fluid.getAmount() >= capacity) {
            return 0;
        }
        int filled = capacity - fluid.getAmount();

        if (resource.getAmount() < filled) {
            fluid.grow(resource.getAmount());
            filled = resource.getAmount();
        } else {
            fluid.setAmount(capacity);
        }
        return filled;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {

        if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource, fluid)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {

        if (maxDrain <= 0 || fluid.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int drained = maxDrain;
        if (fluid.getAmount() < drained) {
            drained = fluid.getAmount();
        }
        FluidStack stack = fluid.copyWithAmount(drained);
        if (action.execute()) {
            fluid.shrink(drained);
            if (fluid.isEmpty()) {
                setFluid(FluidStack.EMPTY);
            }
        }
        return stack;
    }

    @Override
    public int getTankCapacity(int tank) {

        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {

        return true;
    }
    // endregion

    // region ResourceHandler
    @Override
    public int size() {

        return 1;
    }

    @Override
    public FluidResource getResource(int index) {

        return FluidResource.of(fluid);
    }

    @Override
    public long getAmountAsLong(int index) {

        return fluid.getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {

        return capacity;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {

        return !resource.isEmpty() && isFluidValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {

        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (index != 0 || amount == 0) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        return fill(resource.toStack(amount), FluidAction.EXECUTE);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {

        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (index != 0 || amount == 0 || !resource.matches(fluid)) {
            return 0;
        }
        journal.updateSnapshots(transaction);
        return drain(amount, FluidAction.EXECUTE).getAmount();
    }
    // endregion
}
