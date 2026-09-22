package cofh.thermal.dynamics.common.attachment;

import cofh.thermal.dynamics.api.grid.IDuct;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

public interface IAttachmentFactory<T extends IAttachment> {

    T createAttachment(HolderLookup.Provider registries, CompoundTag nbt, IDuct<?, ?> duct, Direction side);

}
