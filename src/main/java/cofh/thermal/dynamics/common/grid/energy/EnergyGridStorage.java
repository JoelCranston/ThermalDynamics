package cofh.thermal.dynamics.common.grid.energy;

import cofh.lib.common.energy.IRedstoneFluxStorage;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import static cofh.lib.util.constants.NBTTags.*;

public final class EnergyGridStorage implements IRedstoneFluxStorage, EnergyHandler {

    private long baseCapacity;
    private long capacity;
    private long energy;

    private byte sampleTracker = 0;

    //    private final long[] samplesIn = new long[40];
    //    private long rollingIn = 0;
    //    private long averageIn = 0;

    private final long[] samplesOut = new long[40];
    private long rollingOut = 0;
    private long averageOut = 0;

    private final SnapshotJournal<Long> journal = new SnapshotJournal<>() {

        @Override
        protected Long createSnapshot() {

            return energy;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {

            energy = snapshot;
        }
    };

    public EnergyGridStorage(long baseCapacity) {

        this.baseCapacity = baseCapacity;
    }

    public EnergyGridStorage setBaseCapacity(long baseCapacity) {

        this.baseCapacity = Math.max(0, baseCapacity);
        //        if (this.energy > this.capacity) {
        //            this.energy = this.capacity;
        //        }
        return this;
    }

    public EnergyGridStorage setCapacity(long capacity) {

        this.capacity = capacity;
        resetTrackers();
        return this;
    }

    public EnergyGridStorage setEnergy(long energy) {

        this.energy = Math.max(0, energy);
        //        if (this.energy > this.capacity) {
        //            this.energy = this.capacity;
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

    public long getCapacity() {

        return capacity;
    }

    public long getEnergy() {

        return energy;
    }

    public void tick() {

        samplesOut[sampleTracker] = energy;
    }

    public void postTick() {

        //        rollingIn += samplesIn[sampleTracker];
        //        averageIn = rollingIn / samplesIn.length;

        samplesOut[sampleTracker] -= energy;
        rollingOut += samplesOut[sampleTracker];
        averageOut = rollingOut / samplesOut.length;

        ++sampleTracker;
        if (sampleTracker >= samplesOut.length) {
            sampleTracker = 0;
            updateCapacity();

            //            System.out.println("Average attempted input (2 seconds): " + averageIn);
            //            System.out.println("Average realized output (2 seconds): " + averageOut);
            //            System.out.println("Dynamic capacity:" + capacity);
            //            System.out.println("Energy stored:" + energy);
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
    public EnergyGridStorage read(CompoundTag nbt) {

        this.energy = nbt.getLongOr(TAG_ENERGY, 0);
        this.baseCapacity = nbt.getLongOr(TAG_ENERGY_MAX, 0);

        //        this.averageIn = nbt.getLong(TAG_TRACK_IN);
        this.averageOut = nbt.getLongOr(TAG_TRACK_OUT, 0);

        updateCapacity();
        return this;
    }

    public CompoundTag write(CompoundTag nbt) {

        nbt.putLong(TAG_ENERGY, energy);
        nbt.putLong(TAG_ENERGY_MAX, baseCapacity);

        //        nbt.putLong(TAG_TRACK_IN, averageIn);
        nbt.putLong(TAG_TRACK_OUT, averageOut);

        return nbt;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider registries) {

        return write(new CompoundTag());
    }

    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {

        read(nbt);
    }
    // endregion

    // region IEnergyStorage
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {

        //        if (!simulate) {
        //            samplesIn[sampleTracker] += maxReceive;
        //        }
        long energyReceived = Math.max(0, Math.min(capacity - energy, maxReceive));
        if (!simulate) {
            energy += energyReceived;
        }
        return (int) energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {

        long energyExtracted = Math.min(energy, maxExtract);
        if (!simulate) {
            energy -= energyExtracted;
        }
        return (int) energyExtracted;
    }

    @Override
    public int getEnergyStored() {

        return energy > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) energy;
    }

    @Override
    public int getMaxEnergyStored() {

        return capacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
    }

    @Override
    public boolean canExtract() {

        return false;
    }

    @Override
    public boolean canReceive() {

        return true;
    }
    // endregion

    // region EnergyHandler
    @Override
    public long getAmountAsLong() {

        return energy;
    }

    @Override
    public long getCapacityAsLong() {

        return capacity;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {

        TransferPreconditions.checkNonNegative(amount);
        journal.updateSnapshots(transaction);
        return receiveEnergy(amount, false);
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {

        TransferPreconditions.checkNonNegative(amount);
        journal.updateSnapshots(transaction);
        return extractEnergy(amount, false);
    }
    // endregion
}
