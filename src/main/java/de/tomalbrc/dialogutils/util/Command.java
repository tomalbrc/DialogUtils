package de.tomalbrc.dialogutils.util;

import com.mojang.brigadier.CommandDispatcher;
import de.tomalbrc.dialogutils.DialogUtils;
import de.tomalbrc.dialogutils.Ticker;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;

import static net.minecraft.commands.Commands.literal;

public class Command {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        commandDispatcher.register(
                literal(DialogUtils.MODID).then(literal("close").executes(commandContext -> {
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
