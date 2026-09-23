package cofh.thermal.dynamics.init.registries;

import cofh.thermal.dynamics.common.item.AttachmentItem;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.init.registries.ThermalCreativeTabs.toolsTab;
import static cofh.thermal.core.util.RegistrationHelper.itemProperties;
import static cofh.thermal.core.util.RegistrationHelper.registerItem;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

public class TDynItems {

    private TDynItems() {

    }

    public static void register() {

        toolsTab(40, registerItem(ID_ENERGY_LIMITER_ATTACHMENT, id -> new AttachmentItem(itemProperties(id), ENERGY_LIMITER).setModId(ID_THERMAL_DYNAMICS)));

        toolsTab(40, registerItem(ID_FILTER_ATTACHMENT, id -> new AttachmentItem(itemProperties(id), FILTER).setModId(ID_THERMAL_DYNAMICS)));

        toolsTab(40, registerItem(ID_SERVO_ATTACHMENT, id -> new AttachmentItem(itemProperties(id), SERVO).setModId(ID_THERMAL_DYNAMICS)));
        toolsTab(40, registerItem(ID_TURBO_SERVO_ATTACHMENT, id -> new AttachmentItem(itemProperties(id), TURBO_SERVO).setModId(ID_THERMAL_DYNAMICS)));

        registerTools();
    }

    // region HELPERS
    private static void registerTools() {

        // CreativeModeTab group = THERMAL_TOOLS;

        // registerItem("ender_tuner", () -> new EnderTunerItem(itemProperties().stacksTo(1)).setModId(ID_THERMAL_DYNAMICS));
    }
    // endregion
}
