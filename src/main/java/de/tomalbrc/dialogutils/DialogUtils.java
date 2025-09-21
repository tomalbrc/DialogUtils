package de.tomalbrc.dialogutils;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.tomalbrc.dialogutils.util.CloseCommand;
import de.tomalbrc.dialogutils.util.RegistryHack;
import eu.pb4.mapcanvas.api.font.CanvasFont;
import eu.pb4.mapcanvas.impl.font.BitmapFont;
import eu.pb4.mapcanvas.impl.font.serialization.VanillaFontReader;
import eu.pb4.polymer.autohost.impl.AutoHost;
import eu.pb4.polymer.common.api.events.SimpleEvent;
import eu.pb4.polymer.common.impl.CommonNetworkHandlerExt;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class DialogUtils implements ModInitializer {
    public final static String MODID = "dialogutils";
    public final static String FONT = MODID + ":default";

    private static final Map<ResourceLocation, Dialog> DIALOGS = new Object2ObjectArrayMap<>();
    private static final List<ResourceLocation> QUICK_ACTIONS = new ObjectArrayList<>();
    private static BitmapFont FONT_READER;

    public static MinecraftServer SERVER;
    public static final SimpleEvent<Consumer<BitmapFont>> FONT_AVAILABLE = new SimpleEvent<>();

    public static void registerQuickDialog(ResourceLocation id) {
        DialogUtils.QUICK_ACTIONS.add(id);
    }

    public static void registerDialog(ResourceLocation id, Dialog dialog) {
        DialogUtils.DIALOGS.put(id, dialog);
    }

    public static Map<ResourceLocation, Dialog> getDialogs() {
        return DIALOGS;
    }

    public static List<ResourceLocation> getQuickActions() {
        return QUICK_ACTIONS;
    }

    @Nullable
    public static BitmapFont fontReader() {
        if (FONT_READER == null) {
            try {
                var builder = PolymerResourcePackUtils.createBuilder(FabricLoader.getInstance().getGameDir().resolve("polymer/dialog"));
                FONT_READER = VanillaFontReader.build((x) -> new ByteArrayInputStream(Objects.requireNonNull(builder.getDataOrSource(x))), CanvasFont.Metadata.create("Resource Pack Font", List.of("Unknown"), "Generated"), ResourceLocation.fromNamespaceAndPath("minecraft", "default"));
            } catch (Exception ignored) {}
        }

        return FONT_READER;
    }

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets(DialogUtils.MODID);
        PolymerResourcePackUtils.markAsRequired();

        if (fontReader() != null)
            FONT_AVAILABLE.invoke(x -> x.accept(fontReader()));

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(DialogUtils::copyVanillaFont);

        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> {
            SERVER = minecraftServer;
        });

        ServerLifecycleEvents.SERVER_STARTED.register(DialogUtils::onStarted);
    }

    public static void addCloseCommand() {
        CommandRegistrationCallback.EVENT.register(CloseCommand::register);
    }

    private static void onStarted(MinecraftServer server) {
        if (DIALOGS.isEmpty())
            return;

        var dialogRegistry = SERVER.registryAccess().lookup(Registries.DIALOG).orElseThrow();
        ((RegistryHack) dialogRegistry).du$unfreeze();
        for (Map.Entry<ResourceLocation, Dialog> entry : DIALOGS.entrySet()) {
            Dialog dialog = entry.getValue();
            dialogRegistry.createIntrusiveHolder(dialog);
            ((RegistryHack) dialogRegistry).du$remove(entry.getKey());
            Registry.register(dialogRegistry, entry.getKey(), dialog);
        }
        SERVER.registryAccess().freeze();
    }

    @SuppressWarnings("all")
    public static void reloadRebuildResourcePack(MinecraftServer server) {
        PolymerResourcePackUtils.buildMain();

        if (AutoHost.config != null && AutoHost.config.enabled) {
            var provider = AutoHost.provider;
            if (provider.isReady()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    for (var x : provider.getProperties(((CommonNetworkHandlerExt) player.connection).polymerCommon$getConnection())) {
                        player.connection.send(new ClientboundResourcePackPushPacket(x.id(), x.url(), x.hash(), AutoHost.config.require || PolymerResourcePackUtils.isRequired(), Optional.ofNullable(AutoHost.message)));
                    }
                }
            }
        }
    }

    public static void copyVanillaFont(ResourcePackBuilder resourcePackBuilder) {
        copyVanillaFont(resourcePackBuilder, MODID);
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
