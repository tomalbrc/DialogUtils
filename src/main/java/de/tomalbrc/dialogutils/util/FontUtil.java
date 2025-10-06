package de.tomalbrc.dialogutils.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import de.tomalbrc.dialogutils.DialogUtils;
import de.tomalbrc.dialogutils.mixin.DefaultRPBuilderAccessor;
import eu.pb4.mapcanvas.api.font.CanvasFont;
import eu.pb4.mapcanvas.impl.font.BitmapFont;
import eu.pb4.mapcanvas.impl.font.serialization.VanillaFontReader;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class FontUtil {
    public final static FontDescription.Resource FONT = new FontDescription.Resource(ResourceLocation.fromNamespaceAndPath(DialogUtils.MODID, "default"));
    public static final FontDescription.Resource ALIGN_FONT = new FontDescription.Resource(ResourceLocation.fromNamespaceAndPath(DialogUtils.MODID, "align"));
    public static Map<ResourceLocation, BitmapFont> FONTS = new Object2ObjectOpenHashMap<>();

    public static BitmapFont loadFont(ResourcePackBuilder resourcePackBuilder, ResourceLocation id) {
        try {
            var fr = VanillaFontReader.build((x) -> new ByteArrayInputStream(Objects.requireNonNull(resourcePackBuilder.getDataOrSource(x))), CanvasFont.Metadata.create("Resource Pack Font", List.of("Unknown"), "Generated"),id);
            FONTS.put(id, fr);

            return fr;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    public static BitmapFont fontReader(ResourcePackBuilder builder) {
        if (!FONTS.containsKey(FontDescription.DEFAULT.id())) {
            try {
                if (builder == null) builder = PolymerResourcePackUtils.createBuilder(FabricLoader.getInstance().getGameDir().resolve("polymer/dialog"));
                var fr = loadFont(builder, FontDescription.DEFAULT.id());
                FONTS.put(FontDescription.DEFAULT.id(), fr);
                FONTS.put(FontUtil.FONT.id(), fr);
            } catch (Exception ignored) {}
        }

        return FONTS.get(FontDescription.DEFAULT.id());
    }

    @Nullable
    public static BitmapFont fontReader(ResourceLocation font) {
        return FONTS.get(font);
    }

    public static void copyVanillaFont(ResourcePackBuilder resourcePackBuilder) {
        copyVanillaFont(resourcePackBuilder, DialogUtils.MODID);
    }

    public static void registerDefaultFonts(ResourcePackBuilder resourcePackBuilder) {
        var def = loadFont(resourcePackBuilder, FontDescription.DEFAULT.id());
        FONTS.put(FontUtil.FONT.id(), def);
        loadFont(resourcePackBuilder, FontUtil.ALIGN_FONT.id());
    }

    public static void copyVanillaFont(ResourcePackBuilder resourcePackBuilder, String namespace) {
        List<String> fontTextures = ImmutableList.of(
                "assets/minecraft/textures/font/nonlatin_european.png",
                "assets/minecraft/textures/font/accented.png",
                "assets/minecraft/textures/font/ascii.png"
        );

        Function<String, byte[]> getter = null;
        try {
            if (resourcePackBuilder instanceof DefaultRPBuilder x) {
                getter = path -> ((DefaultRPBuilderAccessor)x).invokeGetSourceData(path);
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Error copying vanilla font: ", e);
        }

        if (getter == null) {
            getter = resourcePackBuilder::getDataOrSource;
        }

        for (String path : fontTextures) {
            resourcePackBuilder.addData(path.replace("minecraft", namespace), getter.apply(path));
        }

        var defaultFont = getter.apply("assets/minecraft/font/include/default.json");
        var spaceFont = getter.apply("assets/minecraft/font/include/space.json");

        assert defaultFont != null;
        JsonElement def = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(defaultFont)));

        assert spaceFont != null;
        JsonElement space = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(spaceFont)));

        space.getAsJsonObject().getAsJsonArray("providers").addAll(def.getAsJsonObject().getAsJsonArray("providers"));
        var combined = space.toString().replace("minecraft:", namespace + ":");

        resourcePackBuilder.addData("assets/" + namespace + "/font/default.json", combined.getBytes(StandardCharsets.UTF_8));
    }
}
