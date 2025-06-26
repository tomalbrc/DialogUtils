package de.tomalbrc.texteffects;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import de.tomalbrc.texteffects.impl.Book;
import de.tomalbrc.texteffects.util.TextAligner;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.format.font.FontAsset;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.util.Mth;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TextEffects implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();

    static Map<String, Book> bookMap = new Object2ObjectArrayMap<>();

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets("texteffects");
        PolymerResourcePackUtils.markAsRequired();
        ServerTickEvents.END_SERVER_TICK.register(Ticker::tick);

        ServerPlayerEvents.LEAVE.register((serverPlayer -> {
            Ticker.remove(serverPlayer.getUUID());
        }));

        PolymerResourcePackUtils.RESOURCE_PACK_FINISHED_EVENT.register(() -> TextAligner.FONT_INDEX = 0xF000);
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(resourcePackBuilder -> {
            TextAligner.init(resourcePackBuilder);

            var bookPath = FabricLoader.getInstance().getConfigDir().resolve("books");
            bookPath.toFile().mkdir();

            try (var x = Files.walk(bookPath, FileVisitOption.FOLLOW_LINKS)) {
                var fontAssetBuilder = FontAsset.builder();

                for (Object object : x.toArray()) {
                    Path path = (Path) object;
                    if (path.toString().endsWith(".txt") && !path.toFile().isDirectory()) {
                        try (var f = new FileInputStream(path.toFile())) {
                            String name = FilenameUtils.getBaseName(path.toString());
                            bookMap.put(name, Book.from(name, f, fontAssetBuilder));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                var fontAsset = fontAssetBuilder.build();
                TextAligner.builder.addData("assets/texteffects/font/generated.json", fontAsset.toJson().replace("minecraft:bitmap", "bitmap").getBytes(StandardCharsets.UTF_8));
            }
            catch (Exception e) {

            }
        });

        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> {
            commandDispatcher.register(
                    literal("texteffects").then(argument("book", StringArgumentType.word()).suggests((commandContext, suggestionsBuilder) -> {
                        bookMap.keySet().forEach(suggestionsBuilder::suggest);
                        return suggestionsBuilder.buildFuture();
                    }).then(argument("page", IntegerArgumentType.integer()).executes(commandContext -> {
                        var player = commandContext.getSource().getPlayer();
                        if (player == null)
                            return 0;

                        Ticker.remove(player.getUUID());

                        var bookId = StringArgumentType.getString(commandContext, "book");
                        var book = bookMap.get(bookId);

                        var pageNum = IntegerArgumentType.getInteger(commandContext, "page");
                        pageNum = Mth.clamp(0, book.pages().size() - 1, pageNum);

                        book.openBookAtPage(player, pageNum);

                        return Command.SINGLE_SUCCESS;
                    }))).then(literal("close").executes(commandContext -> {
                                var player = commandContext.getSource().getPlayer();
                                if (player != null) {
                                    player.connection.send(ClientboundClearDialogPacket.INSTANCE);
                                    Ticker.remove(player.getUUID());
                                }
                                return Command.SINGLE_SUCCESS;
                            })
                    ));
        }));
    }

}
