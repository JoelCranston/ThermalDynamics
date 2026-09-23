package cofh.thermal.dynamics.init.data;

import cofh.thermal.dynamics.init.data.providers.TDynLootTableProvider;
import cofh.thermal.dynamics.init.data.providers.TDynRecipeProvider;
import cofh.thermal.dynamics.init.data.providers.TDynTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

@EventBusSubscriber (modid = ID_THERMAL_DYNAMICS)
public class TDynDataGen {

    @SubscribeEvent
    public static void gatherData(final GatherDataEvent.Client event) {

        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        TDynTagsProvider.Block blockTags = event.addProvider(new TDynTagsProvider.Block(output, lookup));
        event.addProvider(new TDynTagsProvider.Item(output, lookup, blockTags.contentsGetter()));

        event.addProvider(new TDynLootTableProvider(output, lookup));
        event.addProvider(new TDynRecipeProvider.Runner(output, lookup));
    }

}
