package cofh.thermal.dynamics.client;

import net.minecraft.resources.Identifier;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;

public class TDynTextures {

    private TDynTextures() {

    }

    public static Identifier ENERGY_LIMITER_ATTACHMENT_ACTIVE_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/energy_limiter_attachment_active");
    public static Identifier ENERGY_LIMITER_ATTACHMENT_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/energy_limiter_attachment");

    public static Identifier FILTER_ATTACHMENT_ACTIVE_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/filter_attachment_active");
    public static Identifier FILTER_ATTACHMENT_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/filter_attachment");

    public static Identifier FILTER_ATTACHMENT_TO_GRID_ACTIVE_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/filter_attachment_to_grid_active");
    public static Identifier FILTER_ATTACHMENT_TO_GRID_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/filter_attachment_to_grid");

    public static Identifier FILTER_ATTACHMENT_TO_EXTERNAL_ACTIVE_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/filter_attachment_to_external_active");
    public static Identifier FILTER_ATTACHMENT_TO_EXTERNAL_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/filter_attachment_to_external");

    public static Identifier SERVO_ATTACHMENT_ACTIVE_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/servo_attachment_active");
    public static Identifier SERVO_ATTACHMENT_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/servo_attachment");

    public static Identifier TURBO_SERVO_ATTACHMENT_ACTIVE_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/turbo_servo_attachment_active");
    public static Identifier TURBO_SERVO_ATTACHMENT_LOC = Identifier.parse(ID_THERMAL + ":block/ducts/turbo_servo_attachment");

}
