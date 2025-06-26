package de.tomalbrc.dialogutils;

import de.tomalbrc.dialogutils.impl.Book;
import de.tomalbrc.dialogutils.util.Command;
import de.tomalbrc.dialogutils.util.Globals;
import de.tomalbrc.dialogutils.util.RegistryHack;
import de.tomalbrc.dialogutils.util.TextAligner;
import eu.pb4.polymer.autohost.impl.AutoHost;
import eu.pb4.polymer.common.impl.CommonNetworkHandlerExt;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.font.FontAsset;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.io.FilenameUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DialogUtils implements ModInitializer {
    public static String MODID = "dialogutils";
    public static MinecraftServer SERVER;
    public static final Map<String, Book> BOOKS = new Object2ObjectArrayMap<>();

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
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(TextAligner::init);
        ServerLifecycleEvents.SERVER_STARTED.register(DialogUtils::onStarted);

        bookSupport();
    }

    public static void bookSupport() {
        ServerTickEvents.END_SERVER_TICK.register(Ticker::tick);
        ServerPlayerEvents.LEAVE.register((serverPlayer -> {
            Ticker.remove(serverPlayer.getUUID());
        }));

        PolymerResourcePackUtils.RESOURCE_PACK_FINISHED_EVENT.register(() -> Globals.FONT_INDEX = 0xF000);
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(DialogUtils::loadOrReloadBooks);

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((minecraftServer, closeableResourceManager, b) -> {
            loadWithReload(minecraftServer);
        });

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

    private static void loadOrReloadBooks(ResourcePackBuilder builder) {
        var bookPath = FabricLoader.getInstance().getConfigDir().resolve("books");
        if (!bookPath.toFile().exists())
            return;

        try (var x = Files.walk(bookPath, FileVisitOption.FOLLOW_LINKS)) {
            var fontAssetBuilder = FontAsset.builder();

            for (Object object : x.toArray()) {
                Path path = (Path) object;
                if (path.toString().endsWith(".txt") && !path.toFile().isDirectory()) {
                    try (var f = new FileInputStream(path.toFile())) {
                        String name = FilenameUtils.getBaseName(path.toString());
                        BOOKS.put(name, Book.from(name, f, fontAssetBuilder));
                    } catch (IOException ignored) {}
                }
            }

            var fontAsset = fontAssetBuilder.build();
            builder.addData("assets/" + DialogUtils.MODID + "/font/generated.json", fontAsset.toJson().replace("minecraft:bitmap", "bitmap").getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {

        }

        for (Map.Entry<String, Book> entry : BOOKS.entrySet()) {
            Dialog dialog = entry.getValue().createDialog(0, 0);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(DialogUtils.MODID, entry.getKey());
            DIALOGS.put(id, dialog);
            QUICK_ACTIONS.add(id);
        }
    }

    public static void loadWithReload(MinecraftServer server) {
        PolymerResourcePackUtils.buildMain();

        if (AutoHost.config != null && AutoHost.config.enabled) {
            var provider =  AutoHost.provider;
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
