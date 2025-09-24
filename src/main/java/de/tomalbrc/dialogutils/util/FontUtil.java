package de.tomalbrc.dialogutils.util;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.tomalbrc.dialogutils.DialogUtils;
import eu.pb4.mapcanvas.api.font.CanvasFont;
import eu.pb4.mapcanvas.impl.font.BitmapFont;
import eu.pb4.mapcanvas.impl.font.serialization.VanillaFontReader;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FontUtil {
    public final static ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(DialogUtils.MODID, "default");
    public static final ResourceLocation ALIGN_FONT = ResourceLocation.fromNamespaceAndPath(DialogUtils.MODID, "align");
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
        if (FONTS.containsKey(Style.DEFAULT_FONT)) {
            try {
                if (builder == null) builder = PolymerResourcePackUtils.createBuilder(FabricLoader.getInstance().getGameDir().resolve("polymer/dialog"));
                var fr = loadFont(builder, Style.DEFAULT_FONT);
                FONTS.put(Style.DEFAULT_FONT, fr);
                FONTS.put(FontUtil.FONT, fr);
            } catch (Exception ignored) {}
        }

        return FONTS.get(Style.DEFAULT_FONT);
    }

    @Nullable
    public static BitmapFont fontReader(ResourceLocation font) {
        return FONTS.get(font);
    }

    public static void copyVanillaFont(ResourcePackBuilder resourcePackBuilder) {
        copyVanillaFont(resourcePackBuilder, DialogUtils.MODID);
    }

    public static void registerDefaultFonts(ResourcePackBuilder resourcePackBuilder) {
        var def = loadFont(resourcePackBuilder, Style.DEFAULT_FONT);
        FONTS.put(FontUtil.FONT, def);
        loadFont(resourcePackBuilder, FontUtil.ALIGN_FONT);
    }

    public static void copyVanillaFont(ResourcePackBuilder resourcePackBuilder, String namespace) {
        List<String> fontTextures = ImmutableList.of(
                "assets/minecraft/textures/font/nonlatin_european.png",
                "assets/minecraft/textures/font/accented.png",
                "assets/minecraft/textures/font/ascii.png"
        );
        for (String path : fontTextures) {
            resourcePackBuilder.addData(path.replace("minecraft", namespace), resourcePackBuilder.getDataOrSource(path));
        }

        var defaultFont = resourcePackBuilder.getDataOrSource("assets/minecraft/font/include/default.json");
        var spaceFont = resourcePackBuilder.getDataOrSource("assets/minecraft/font/include/space.json");

        assert defaultFont != null;
        JsonElement def = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(defaultFont)));

        assert spaceFont != null;
        JsonElement space = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(spaceFont)));

        space.getAsJsonObject().getAsJsonArray("providers").addAll(def.getAsJsonObject().getAsJsonArray("providers"));
        var combined = space.toString().replace("minecraft:", namespace + ":");

        resourcePackBuilder.addData("assets/" + namespace + "/font/default.json", combined.getBytes(StandardCharsets.UTF_8));
    }
}
