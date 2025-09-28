package de.tomalbrc.dialogutils;

import de.tomalbrc.dialogutils.util.CloseCommand;
import de.tomalbrc.dialogutils.util.FontUtil;
import de.tomalbrc.dialogutils.util.RegistryHack;
import eu.pb4.polymer.autohost.impl.AutoHost;
import eu.pb4.polymer.common.impl.CommonNetworkHandlerExt;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DialogUtils implements ModInitializer {
    public final static String MODID = "dialogutils";

    private static final Map<ResourceLocation, Dialog> DIALOGS = new Object2ObjectArrayMap<>();
    private static final List<ResourceLocation> QUICK_ACTIONS = new ObjectArrayList<>();

    public static MinecraftServer SERVER;

    public static void registerQuickDialog(ResourceLocation id) {
        DialogUtils.QUICK_ACTIONS.add(id);
    }

    public static void registerDialog(ResourceLocation id, Dialog dialog) {
        DialogUtils.DIALOGS.put(id, dialog);

        var dialogRegistry = SERVER.registryAccess().lookup(Registries.DIALOG).orElseThrow();
        ((RegistryHack) dialogRegistry).du$unfreeze();
        dialogRegistry.createIntrusiveHolder(dialog);
        ((RegistryHack) dialogRegistry).du$remove(id);
        Registry.register(dialogRegistry, id, dialog);
        SERVER.registryAccess().freeze();
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

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(FontUtil::copyVanillaFont);

        ServerLifecycleEvents.SERVER_STARTING.register(minecraftServer -> {
            SERVER = minecraftServer;
        });
    }

    public static void addCloseCommand() {
        CommandRegistrationCallback.EVENT.register(CloseCommand::register);
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
}
