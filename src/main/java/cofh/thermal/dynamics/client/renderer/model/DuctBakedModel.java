package cofh.thermal.dynamics.client.renderer.model;

import cofh.core.client.model.SimpleItemModel;
import cofh.core.client.renderer.model.ModelUtils;
import cofh.core.util.helpers.RenderHelper;
import cofh.thermal.dynamics.client.model.DuctModel;
import cofh.thermal.dynamics.client.model.data.DuctModelData;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import org.joml.Matrix4fc;

import javax.annotation.Nullable;
import java.util.*;

import static cofh.lib.util.Constants.DIRECTIONS;
import static cofh.thermal.core.client.ThermalTextures.BLANK_TEXTURE;
import static cofh.thermal.dynamics.client.model.data.DuctModelData.DUCT_MODEL_DATA;

public class DuctBakedModel implements DynamicBlockStateModel {

    private static final boolean DEBUG = Boolean.getBoolean("DuctModel.debug");

    private static final DuctModelData INV_DATA = Util.make(new DuctModelData(), data -> {
        data.setInternalConnection(Direction.UP, true);
        data.setInternalConnection(Direction.DOWN, true);
    });

    public void clearCache() {

        modelCache.clear();
        centerFillCache.clear();
        fillCache.clear();
        attachmentCache.clear();
    }

    private final Material.Baked particle;
    private final boolean ambientOcclusion;
    private final int materialFlags;
    private final Map<Direction, List<BakedQuad>> centerModel;
    private final Map<Direction, List<BakedQuad>> centerFill;
    private final Map<Direction, List<BakedQuad>> sides;
    private final Map<Direction, List<BakedQuad>> fill;
    private final Map<Direction, List<BakedQuad>> connections;
    private final Set<BakedQuad> backfaces;
    private final boolean isInventory;
    private final Map<DuctModelData, BlockStateModelPart> modelCache = new HashMap<>();
    private final Map<TexColorWrapper, Map<Direction, List<BakedQuad>>> centerFillCache = new Object2ObjectOpenHashMap<>();
    private final Map<TexColorWrapper, Map<Direction, List<BakedQuad>>> fillCache = new Object2ObjectOpenHashMap<>();
    private final Map<Identifier, Map<Direction, List<BakedQuad>>> attachmentCache = new Object2ObjectOpenHashMap<>();

    public DuctBakedModel(Material.Baked particle, boolean ambientOcclusion, EnumMap<Direction, List<BakedQuad>> centerModel, EnumMap<Direction, List<BakedQuad>> centerFill, EnumMap<Direction, List<BakedQuad>> sides, EnumMap<Direction, List<BakedQuad>> fill, EnumMap<Direction, List<BakedQuad>> connections, Set<BakedQuad> backfaces, boolean isInventory) {

        this.particle = particle;
        this.ambientOcclusion = ambientOcclusion;
        this.centerModel = ImmutableMap.copyOf(centerModel);
        this.centerFill = ImmutableMap.copyOf(centerFill);
        this.sides = ImmutableMap.copyOf(sides);
        this.fill = ImmutableMap.copyOf(fill);
        this.connections = ImmutableMap.copyOf(connections);
        this.backfaces = backfaces;
        this.isInventory = isInventory;

        int flags = 0;
        for (Map<Direction, List<BakedQuad>> map : List.of(this.centerModel, this.centerFill, this.sides, this.fill, this.connections)) {
            for (List<BakedQuad> quads : map.values()) {
                for (BakedQuad quad : quads) {
                    flags |= quad.materialInfo().flags();
                }
            }
        }
        this.materialFlags = flags;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {

        ModelData extraData = level.getModelData(pos);
        if (isInventory) {
            extraData = ModelData.builder()
                    .with(DUCT_MODEL_DATA, INV_DATA)
                    .build();
        }
        if (!(extraData.has(DUCT_MODEL_DATA))) {
            return;
        }
        parts.add(getModelFor(extraData.get(DUCT_MODEL_DATA)));
    }

    public BlockStateModelPart getInventoryModel() {

        if (!isInventory) {
            return new SimpleModelWrapper(QuadCollection.EMPTY, ambientOcclusion, particle);
        }
        return getModelFor(INV_DATA);
    }

    private BlockStateModelPart getModelFor(DuctModelData modelData) {

        BlockStateModelPart model = modelCache.get(modelData);
        if (!DEBUG && model != null) {
            return model;
        }
        synchronized (modelCache) {
            model = modelCache.get(modelData); // Another thread could have computed whilst we were locked.
            if (!DEBUG && model != null) return model;
            QuadCollection.Builder quads = new QuadCollection.Builder();
            for (Direction dir : DIRECTIONS) {
                boolean internal = modelData.hasInternalConnection(dir);
                boolean external = modelData.hasExternalConnection(dir);
                Identifier attachment = modelData.getAttachment(dir);

                if (!internal && !external) {
                    List<BakedQuad> fillQuads = rebakeFill(centerFillCache, centerFill, modelData.getFill(), modelData.getFillColor(), dir);
                    filterBlank(centerModel.get(dir), false).forEach(quads::addUnculledFace);
                    filterBlank(fillQuads, false).forEach(quads::addUnculledFace);
                } else {
                    List<BakedQuad> fillQuads = rebakeFill(fillCache, fill, modelData.getFill(), modelData.getFillColor(), dir);
                    filterBlank(sides.get(dir), !fillQuads.isEmpty()).forEach(quads::addUnculledFace);
                    filterBlank(fillQuads, false).forEach(quads::addUnculledFace);
                    if (external) {
                        filterBlank(rebakeAttachment(attachmentCache, connections, attachment, dir), true).forEach(quads::addUnculledFace);
                    }
                }
            }
            model = new SimpleModelWrapper(quads.build(), ambientOcclusion, particle);
            modelCache.put(new DuctModelData(modelData), model);
            return model;
        }
    }

    private List<BakedQuad> filterBlank(List<BakedQuad> quads, boolean cullBack) {

        List<BakedQuad> newQuads = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (cullBack && backfaces.contains(quad) || quad.materialInfo().sprite().contents().name().equals(BLANK_TEXTURE)) {
                // do nothing
            } else {
                newQuads.add(quad);
            }
        }
        return newQuads;
    }

    private List<BakedQuad> rebakeFill(Map<TexColorWrapper, Map<Direction, List<BakedQuad>>> cache, Map<Direction, List<BakedQuad>> raw, @Nullable Identifier texture, int color, Direction dir) {

        // Easy bail if there are no quads.
        List<BakedQuad> fillQuads = raw.get(dir);
        if (fillQuads.isEmpty()) {
            return ImmutableList.of();
        }
        // Again if there is no texture.
        if (texture == null) {
            return fillQuads;
        }
        // Is it cached already?
        Map<Direction, List<BakedQuad>> retextured = cache.get(new TexColorWrapper(texture, color));
        if (retextured != null) {
            List<BakedQuad> quads = retextured.get(dir);
            // Sure is!
            if (quads != null) {
                return quads;
            }
        }
        // Whatever intellij, I know what im doing.
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (cache) {
            retextured = cache.get(new TexColorWrapper(texture, color)); // Another thread could have computed whilst we were locked.
            if (retextured != null) {
                List<BakedQuad> quads = retextured.get(dir);
                // \o/ memory saved++
                if (quads != null) {
                    return quads;
                }
            } else {
                retextured = new HashMap<>();
                cache.put(new TexColorWrapper(texture, color), retextured);
            }

            // Grab the sprite
            TextureAtlasSprite sprite = RenderHelper.getTexture(texture);

            // Retexture
            List<BakedQuad> newQuads = new ArrayList<>(fillQuads.size());
            for (BakedQuad quad : fillQuads) {
                newQuads.add(ModelUtils.retexture(RenderHelper.mulColor(quad, color), sprite));
            }
            // slap in cache.
            retextured.put(dir, newQuads);
            return newQuads;
        }
    }

    private List<BakedQuad> rebakeAttachment(Map<Identifier, Map<Direction, List<BakedQuad>>> cache, Map<Direction, List<BakedQuad>> raw, @Nullable Identifier texture, Direction dir) {

        // Easy bail if there are no quads.
        List<BakedQuad> connQuads = raw.get(dir);
        if (connQuads.isEmpty()) {
            return ImmutableList.of();
        }
        // Again if there is no texture.
        if (texture == null) {
            return connQuads;
        }
        // Is it cached already?
        Map<Direction, List<BakedQuad>> retextured = cache.get(texture);
        if (retextured != null) {
            List<BakedQuad> quads = retextured.get(dir);
            // Sure is!
            if (quads != null) {
                return quads;
            }
        }
        // Whatever intelliJ, I know what im doing.
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (cache) {
            retextured = cache.get(texture); // Another thread could have computed whilst we were locked.
            if (retextured != null) {
                List<BakedQuad> quads = retextured.get(dir);
                // \o/ memory saved++
                if (quads != null) {
                    return quads;
                }
            } else {
                retextured = new HashMap<>();
                cache.put(texture, retextured);
            }
            // Grab the sprite
            TextureAtlasSprite sprite = RenderHelper.getTexture(texture);

            // Retexture
            List<BakedQuad> newQuads = new ArrayList<>(connQuads.size());
            for (BakedQuad quad : connQuads) {
                newQuads.add(ModelUtils.retexture(quad, sprite));
            }
            // slap in cache.
            retextured.put(dir, newQuads);
            return newQuads;
        }
    }

    private static class TexColorWrapper {

        Identifier texture;
        int color;

        public TexColorWrapper(Identifier texture, int color) {

            this.texture = texture;
            this.color = color;
        }

        @Override
        public int hashCode() {

            return texture.hashCode() + color * 31;
        }

    }

    //@formatter:off
    @Override public Material.Baked particleMaterial() { return particle; }
    @Override public int materialFlags() { return materialFlags; }
    //@formatter:on

    // region LOADER
    public record Unbaked(Variant variant) implements CustomUnbakedBlockStateModel {

        public static final MapCodec<Unbaked> CODEC = Variant.MAP_CODEC.xmap(Unbaked::new, Unbaked::variant);

        @Override
        public BlockStateModel bake(ModelBaker baker) {

            ResolvedModel model = baker.getModel(variant.modelLocation());
            DuctModel ductModel = DuctModel.find(model);
            if (ductModel == null) {
                return new SingleVariant(baker.missingBlockModelPart());
            }
            return ductModel.bake(baker, model, variant.modelState().asModelState(), DuctModel.isInventory(model));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {

            variant.resolveDependencies(resolver);
        }

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {

            return CODEC;
        }

    }

    public record ItemUnbaked(CuboidItemModelWrapper.Unbaked model) implements ItemModel.Unbaked {

        public static final MapCodec<ItemUnbaked> CODEC = CuboidItemModelWrapper.Unbaked.MAP_CODEC.xmap(ItemUnbaked::new, ItemUnbaked::model);

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {

            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolvedModel = baker.getModel(model.model());
            TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolvedModel, textureSlots);
            DuctModel ductModel = DuctModel.find(resolvedModel);
            BlockStateModelPart part = ductModel == null ? baker.missingBlockModelPart() : ductModel.bake(baker, resolvedModel, BlockModelRotation.IDENTITY, DuctModel.isInventory(resolvedModel)).getInventoryModel();
            return new SimpleItemModel((stack, original) -> original, part, model.tints(), properties, Transformation.compose(transformation, model.transformation()));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {

            model.resolveDependencies(resolver);
        }

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {

            return CODEC;
        }

    }
    // endregion
}
