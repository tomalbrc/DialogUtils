package de.tomalbrc.texteffects.impl;

import de.tomalbrc.texteffects.TextEffects;
import eu.pb4.placeholders.api.TextParserUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

public record Book(String title, int width, int linesPerPage, List<Page> pages) {
    public record Page(List<String> lines) {
        public String asString() {
            return String.join("\n", lines);
        }
    }

    public static Book from(String filename, InputStream inputStream) throws IOException {
        int pageWidth = 200;
        int linesPerPage = 15;
        List<String> texts = new ObjectArrayList<>();

        List<String> fileLines = readLines(inputStream);
        for (String line : fileLines) {
            var r = TagWrapperParser.wrapHtmlLine(line, pageWidth);
            for (String current : r) {
                var finalString = TextAligner.alignSingleLine(current, TextAligner.Align.LEFT, pageWidth);
                texts.add(finalString);
            }
        }

        List<Book.Page> pages = new ObjectArrayList<>();

        for (int i = 0; i < texts.size(); i += linesPerPage) {
            int end = Math.min(i + linesPerPage, texts.size());
            ObjectArrayList<String> pageLines = new ObjectArrayList<>();
            pageLines.addAll(texts.subList(i, end));

            // padding
            while (pageLines.size() < linesPerPage) {
                pageLines.add("");
            }

            pages.add(new Book.Page(pageLines));
        }

        return new Book(filename, pageWidth, linesPerPage, pages);
    }

    private static List<String> readLines(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<String> fileLines = new ObjectArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            fileLines.add(line);
        }
        return fileLines;
    }

    public void openBookAtPage(ServerPlayer player, int pageNum) {
        var page = this.pages().get(Mth.clamp(0, this.pages().size() - 1, pageNum));

        var dialogBodies = new ObjectArrayList<DialogBody>();

        var cc = TextParserUtils.formatText("<font:texteffects:ui>3</font>");
        cc = Component.empty().append(cc).withStyle(Style.EMPTY.withShadowColor(0));

        dialogBodies.add(new PlainMessage(cc, 1));

        var str = page.asString();
        for (String paragraph : str.split("\\R--- {3,}")) {
            var formattedText = TextParserUtils.formatText(paragraph);
            formattedText = Component.empty().append(formattedText).withStyle(Style.EMPTY.withShadowColor(0));

            dialogBodies.add(new PlainMessage(formattedText, this.width() + 256));
        }

        Holder<Dialog> dialogHolder = createDialogHolder(dialogBodies, this.title(), pageNum);
        player.openDialog(dialogHolder);
    }

    public @NotNull Holder<Dialog> createDialogHolder(List<DialogBody> list, String title, int page) {
        Optional<Action> close = Optional.of(new StaticAction(new ClickEvent.RunCommand("texteffects close")));
        var closeButton = new ActionButton(new CommonButtonData(Component.literal("Close"), 150), close);
        var titleString = String.format("%s (Page %d / %d)", title, page+1, this.pages.size());
        var formattedTitle = TextParserUtils.formatText(titleString);
        formattedTitle = Component.empty().append(formattedTitle);
        var data = new CommonDialogData(formattedTitle, Optional.empty(), true, false, DialogAction.NONE, list, List.of());
        return Holder.direct(new MultiActionDialog(data, createButtons(title, page, this.pages.size()), Optional.of(closeButton), 4));
    }

    public List<ActionButton> createButtons(String title, int page, int maxPage) {
        var prevCmd = String.format("texteffects %s %d", title, Math.max(0, page - 1));
        var nextCmd = String.format("texteffects %s %d", title, Math.min(maxPage, page + 1));
        Optional<Action> actionPrev = Optional.of(new StaticAction(new ClickEvent.RunCommand(prevCmd)));
        Optional<Action> actionNext = Optional.of(new StaticAction(new ClickEvent.RunCommand(nextCmd)));
        var prev = new ActionButton(new CommonButtonData(Component.literal("<"), 20), actionPrev);
        var next = new ActionButton(new CommonButtonData(Component.literal(">"), 20), actionNext);
        return List.of(prev, next);
    }
}
