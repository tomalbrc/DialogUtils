package de.tomalbrc.dialogutils;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.tomalbrc.dialogutils.mixin.DefaultRPBuilderAccessor;
import de.tomalbrc.dialogutils.util.Command;
import de.tomalbrc.dialogutils.util.RegistryHack;
import de.tomalbrc.dialogutils.util.TextAligner;
import eu.pb4.polymer.autohost.impl.AutoHost;
import eu.pb4.polymer.common.impl.CommonNetworkHandlerExt;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DialogUtils implements ModInitializer {
    public static String MODID = "dialogutils";
    public static String FONT = MODID + ":default";
    public static MinecraftServer SERVER;

    private static final Map<ResourceLocation, Dialog> DIALOGS = new Object2ObjectArrayMap<>();
    private static final List<ResourceLocation> QUICK_ACTIONS = new ObjectArrayList<>();

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

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets(DialogUtils.MODID);
        PolymerResourcePackUtils.markAsRequired();

        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> SERVER = minecraftServer);
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(resourcePackBuilder -> {
            copyVanillaFont(resourcePackBuilder);
            TextAligner.init(resourcePackBuilder);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(DialogUtils::onStarted);
    }

    public static void addCloseCommand() {
        CommandRegistrationCallback.EVENT.register(Command::register);
    }

    private static void onStarted(MinecraftServer server) {
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

    private static void copyVanillaFont(ResourcePackBuilder resourcePackBuilder, String namespace) {
        if (resourcePackBuilder instanceof DefaultRPBuilder defaultRPBuilder) {
            List<String> fontTextures = ImmutableList.of(
                    "assets/minecraft/textures/font/nonlatin_european.png",
                    "assets/minecraft/textures/font/accented.png",
                    "assets/minecraft/textures/font/ascii.png"
            );
            for (String path : fontTextures) {
                try {
                    resourcePackBuilder.addData(path.replace("minecraft", namespace), ((DefaultRPBuilderAccessor) defaultRPBuilder).inkvokeGetSourceStream(path).readAllBytes());
                } catch (IOException ignored) {}
            }

            var defaultFont = ((DefaultRPBuilderAccessor) defaultRPBuilder).inkvokeGetSourceStream("assets/minecraft/font/include/default.json");
            var spaceFont = ((DefaultRPBuilderAccessor) defaultRPBuilder).inkvokeGetSourceStream("assets/minecraft/font/include/space.json");

            JsonElement def = JsonParser.parseReader(new InputStreamReader(defaultFont));
            JsonElement space = JsonParser.parseReader(new InputStreamReader(spaceFont));

            space.getAsJsonObject().getAsJsonArray("providers").addAll(def.getAsJsonObject().getAsJsonArray("providers"));
            var combined = space.toString().replace("minecraft:", namespace + ":");

            resourcePackBuilder.addData("assets/" + namespace + "/font/default.json", combined.getBytes(StandardCharsets.UTF_8));
        }
    }
}
