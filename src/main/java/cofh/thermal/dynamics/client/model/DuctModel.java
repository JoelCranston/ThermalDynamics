package cofh.thermal.dynamics.client.model;

import cofh.thermal.dynamics.client.renderer.model.DuctBakedModel;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.DelegateUnbakedModel;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

import static cofh.lib.util.Constants.DIRECTIONS;

public class DuctModel extends DelegateUnbakedModel {

    private final Map<String, Map<String, CuboidModelElement>> parts;

    protected static List<DuctBakedModel> bakedModels = new ArrayList<>();

    public static void clearCaches() {

        for (DuctBakedModel model : bakedModels) {
            model.clearCache();
        }
    }

    public DuctModel(CuboidModel model, Map<String, Map<String, CuboidModelElement>> parts) {

        super(model);
        this.parts = parts;
    }

    @Nullable
    public static DuctModel find(ResolvedModel model) {

        for (ResolvedModel current = model; current != null; current = current.parent()) {
            if (current.wrapped() instanceof DuctModel ductModel) {
                return ductModel;
            }
        }
        return null;
    }

    public static boolean isInventory(ResolvedModel model) {

        Map<String, Boolean> visibility = model.getTopAdditionalProperties().getOrDefault(NeoForgeModelProperties.PART_VISIBILITY, Map.of());
        return visibility.getOrDefault("inv", false);
    }

    public DuctBakedModel bake(ModelBaker baker, ResolvedModel model, ModelState modelState, boolean isInventory) {

        TextureSlots textures = model.getTopTextureSlots();
        Set<BakedQuad> backfaces = Collections.newSetFromMap(new IdentityHashMap<>());
        // Map<Face, List(FrontFace, BackFace)>
        EnumMap<Direction, List<BakedQuad>> center = buildCenter(baker, textures, modelState, model, backfaces);
        // Map<Face, List(FrontFace, BackFace)>
        EnumMap<Direction, List<BakedQuad>> centerFill = buildCenterFill(baker, textures, modelState, model);
        // Map<Connection Side, List(FrontFaces & BackFaces)>
        EnumMap<Direction, List<BakedQuad>> ductSides = buildGroupParts("duct", baker, textures, modelState, model);
        // Map<Connection Side, List(FrontFaces & BackFaces)>
        EnumMap<Direction, List<BakedQuad>> ductFill = buildGroupParts("fill", baker, textures, modelState, model);
        // Map<Connection Side, List(FrontFaces & BackFaces)>
        EnumMap<Direction, List<BakedQuad>> connections = buildGroupParts("attach", baker, textures, modelState, model);

        Material.Baked particle = model.resolveParticleMaterial(textures, baker);
        DuctBakedModel bakedModel = new DuctBakedModel(particle, model.getTopAmbientOcclusion(), center, centerFill, ductSides, ductFill, connections, backfaces, isInventory);
        bakedModels.add(bakedModel);
        return bakedModel;
    }

    // region HELPERS
    private EnumMap<Direction, List<BakedQuad>> buildCenter(ModelBaker baker, TextureSlots textures, ModelState modelState, ResolvedModel model, Set<BakedQuad> backfaces) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);

        CuboidModelElement front = getPart("center/duct", "frontface");
        if (front != null) {
            EnumMap<Direction, List<BakedQuad>> baked = bake(front, baker, textures, modelState, model);
            merge(quads, baked);
        }
        CuboidModelElement back = getPart("center/duct", "backface");
        if (back != null) {
            EnumMap<Direction, List<BakedQuad>> baked = bake(back, baker, textures, modelState, model);
            for (List<BakedQuad> list : baked.values()) {
                backfaces.addAll(list);
            }
            // These are inverse in the json.
            flip(baked);
            merge(quads, baked);
        }
        return quads;
    }

    private EnumMap<Direction, List<BakedQuad>> buildCenterFill(ModelBaker baker, TextureSlots textures, ModelState modelState, ResolvedModel model) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        CuboidModelElement frontFill = getPart("center/fill", "frontface");
        if (frontFill != null) {
            EnumMap<Direction, List<BakedQuad>> baked = bake(frontFill, baker, textures, modelState, model);
            merge(quads, baked);
        }
        return quads;
    }

    private EnumMap<Direction, List<BakedQuad>> buildGroupParts(String groupPart, ModelBaker baker, TextureSlots textures, ModelState modelState, ResolvedModel model) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        fill(quads, Arrays.asList(DIRECTIONS), LinkedList::new);

        for (Direction dir : DIRECTIONS) {
            String group = dir.getName() + "/" + groupPart;
            Map<String, CuboidModelElement> groupParts = parts.get(group);
            if (groupParts == null) continue;

            List<BakedQuad> list = quads.get(dir);
            for (CuboidModelElement part : groupParts.values()) {
                Map<Direction, List<BakedQuad>> baked = bake(part, baker, textures, modelState, model);
                flatMerge(list, baked);
            }
        }

        return quads;
    }

    private EnumMap<Direction, List<BakedQuad>> bake(CuboidModelElement part, ModelBaker baker, TextureSlots textures, ModelState modelState, ResolvedModel model) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        fill(quads, part.faces().keySet(), LinkedList::new);

        for (Map.Entry<Direction, CuboidFace> entry : part.faces().entrySet()) {
            Direction dir = entry.getKey();
            CuboidFace face = entry.getValue();
            Material.Baked material = baker.materials().resolveSlot(textures, face.texture(), model);
            quads.get(dir).add(FaceBakery.bakeQuad(baker, part.from(), part.to(), face, material, dir, modelState, part.rotation(), part.shade(), part.lightEmission()));
        }
        return quads;
    }

    @Nullable
    private CuboidModelElement getPart(String group, String name) {

        Map<String, CuboidModelElement> namedParts = parts.get(group);
        if (namedParts == null) return null;

        return namedParts.get(name);
    }

    private static <K, V> void fill(Map<K, V> map, Iterable<K> keys, Supplier<V> sup) {

        for (K key : keys) {
            map.put(key, sup.get());
        }
    }

    private static <K, T> void merge(Map<K, List<T>> dest, Map<K, List<T>> src) {

        for (K k : src.keySet()) {
            List<T> destList = dest.get(k);
            List<T> srcList = src.get(k);
            if (destList == null) {
                dest.put(k, srcList);
            } else {
                destList.addAll(srcList);
            }
        }
    }

    private static <T> void flatMerge(List<T> dest, Map<?, List<T>> src) {

        for (List<T> value : src.values()) {
            dest.addAll(value);
        }
    }

    private static <T> void flip(EnumMap<Direction, T> map) {

        map.put(Direction.UP, map.put(Direction.DOWN, map.get(Direction.UP)));
        map.put(Direction.SOUTH, map.put(Direction.NORTH, map.get(Direction.SOUTH)));
        map.put(Direction.EAST, map.put(Direction.WEST, map.get(Direction.EAST)));
    }
    // endregion

    // region LOADER
    public static class Loader implements UnbakedModelLoader<DuctModel> {

        private Map<String, Map<String, CuboidModelElement>> parseElements(JsonDeserializationContext ctx, JsonObject model) {

            Map<String, Map<String, CuboidModelElement>> parts = new HashMap<>();
            if (model.has("elements")) {
                for (JsonElement element : GsonHelper.getAsJsonArray(model, "elements")) {
                    JsonObject obj = element.getAsJsonObject();
                    CuboidModelElement part = ctx.deserialize(obj, CuboidModelElement.class);
                    String group = GsonHelper.getAsString(obj, "group", null);
                    String name = GsonHelper.getAsString(obj, "name");
                    Map<String, CuboidModelElement> groupParts = parts.computeIfAbsent(group, e -> new HashMap<>());
                    groupParts.put(name, part);
                }
            }
            return parts;
        }

        @Override
        public DuctModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {

            return new DuctModel(deserializationContext.deserialize(jsonObject, CuboidModel.class), parseElements(deserializationContext, jsonObject));
        }

    }
    // endregion
}
