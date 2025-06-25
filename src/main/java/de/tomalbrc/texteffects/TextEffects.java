package de.tomalbrc.texteffects;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import de.tomalbrc.texteffects.impl.Book;
import de.tomalbrc.texteffects.impl.TextAligner;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.util.Mth;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(resourcePackBuilder -> {
            TextAligner.init(resourcePackBuilder);

            var bookPath = FabricLoader.getInstance().getConfigDir().resolve("books");
            bookPath.toFile().mkdir();

            try (var x = Files.walk(bookPath, FileVisitOption.FOLLOW_LINKS)) {
                for (Object object : x.toArray()) {
                    Path path = (Path) object;
                    if (path.toString().endsWith(".txt") && !path.toFile().isDirectory()) {
                        try (var f = new FileInputStream(path.toFile())) {
                            var name = FilenameUtils.getBaseName(path.toString());
                            bookMap.put(name, Book.from(name, f));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
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

                        var bookId = StringArgumentType.getString(commandContext, "book");
                        var book = bookMap.get(bookId);

                        var pageNum = IntegerArgumentType.getInteger(commandContext, "page");
                        pageNum = Mth.clamp(0, book.pages().size() - 1, pageNum);

                        book.openBookAtPage(player, pageNum);

                        return Command.SINGLE_SUCCESS;
                    }))).then(literal("close").executes(commandContext -> {
                                var player = commandContext.getSource().getPlayer();
                                if (player != null)
                                    player.connection.send(ClientboundClearDialogPacket.INSTANCE);

                                return Command.SINGLE_SUCCESS;
                            })
                    ));
        }));
    }
}
