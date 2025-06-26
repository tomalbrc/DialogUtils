package de.tomalbrc.dialogutils.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.tomalbrc.dialogutils.DialogUtils;
import de.tomalbrc.dialogutils.Ticker;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.util.Mth;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Command {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        commandDispatcher.register(
                literal(DialogUtils.MODID).then(argument("book", StringArgumentType.word()).suggests((commandContext, suggestionsBuilder) -> {
                    DialogUtils.BOOKS.keySet().forEach(suggestionsBuilder::suggest);
                    return suggestionsBuilder.buildFuture();
                }).then(argument("page", IntegerArgumentType.integer()).executes(commandContext -> {
                    var player = commandContext.getSource().getPlayer();
                    if (player == null)
                        return 0;

                    Ticker.remove(player.getUUID());

                    var bookId = StringArgumentType.getString(commandContext, "book");
                    var book = DialogUtils.BOOKS.get(bookId);

                    var pageNum = IntegerArgumentType.getInteger(commandContext, "page");
                    pageNum = Mth.clamp(0, book.pages().size() - 1, pageNum);

                    book.openBookAtPage(player, pageNum);

                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                }))).then(literal("close").executes(commandContext -> {
                            var player = commandContext.getSource().getPlayer();
                            if (player != null) {
                                player.connection.send(ClientboundClearDialogPacket.INSTANCE);
                                Ticker.remove(player.getUUID());
                            }
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })
                ));
    }
}
