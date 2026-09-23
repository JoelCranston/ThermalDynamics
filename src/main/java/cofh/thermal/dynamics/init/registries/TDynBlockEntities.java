package cofh.thermal.dynamics.init.registries;

import cofh.thermal.dynamics.common.block.entity.ItemBufferBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.EnergyDuctBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.FluidDuctBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.FluidDuctWindowedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.BLOCK_ENTITIES;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

public class TDynBlockEntities {

    private TDynBlockEntities() {

    }

    public static void register() {

    }

    public static final Supplier<BlockEntityType<EnergyDuctBlockEntity>> ENERGY_DUCT_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_ENERGY_DUCT, () -> new BlockEntityType<>(EnergyDuctBlockEntity::new, BLOCKS.get(ID_ENERGY_DUCT)));
    public static final Supplier<BlockEntityType<FluidDuctBlockEntity>> FLUID_DUCT_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_FLUID_DUCT, () -> new BlockEntityType<>(FluidDuctBlockEntity::new, BLOCKS.get(ID_FLUID_DUCT)));
    public static final Supplier<BlockEntityType<FluidDuctWindowedBlockEntity>> FLUID_DUCT_WINDOWED_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_FLUID_DUCT_WINDOWED, () -> new BlockEntityType<>(FluidDuctWindowedBlockEntity::new, BLOCKS.get(ID_FLUID_DUCT_WINDOWED)));

    //        TILE_ENTITIES.register(ID_ENERGY_DISTRIBUTOR, () -> TileEntityType.Builder.of(EnergyDistributorTile::new, ENERGY_DISTRIBUTOR_BLOCK).build(null));

    public static final Supplier<BlockEntityType<ItemBufferBlockEntity>> ITEM_BUFFER_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_ITEM_BUFFER, () -> new BlockEntityType<>(ItemBufferBlockEntity::new, BLOCKS.get(ID_ITEM_BUFFER)));

}
