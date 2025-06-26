package de.tomalbrc.texteffects.impl;

import de.tomalbrc.texteffects.Ticker;
import de.tomalbrc.texteffects.gif.GifFrameExtractor;
import de.tomalbrc.texteffects.util.TagWrapperParser;
import de.tomalbrc.texteffects.util.TextAligner;
import de.tomalbrc.texteffects.util.TextUtil;
import eu.pb4.polymer.resourcepack.extras.api.format.font.BitmapProvider;
import eu.pb4.polymer.resourcepack.extras.api.format.font.FontAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.font.FontProviderEntry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Book(String title, int width, int linesPerPage, List<Page> pages) {
    public record Page(List<Line> lines) {
        public String asString() {
            return asString(0);
        }

        public String asString(int frame) {
            return String.join("\n", lines.stream().map(x -> x.frames != null && !x.frames.isEmpty() ? x.frames.get(frame % x.frames.size()) : x.text).filter(Objects::nonNull).toList());
        }

        public record Line(String text, List<String> frames) {
            public boolean animated() {
                return frames != null && !frames.isEmpty();
            }
        }

        public boolean animated() {
            return lines != null && lines.stream().anyMatch(Line::animated);
        }
    }

    public static Book from(String filename, InputStream inputStream, FontAsset.Builder fontAssetBuilder) throws IOException {
        int pageWidth = 200;
        int linesPerPage = 15;
        List<Page.Line> texts = new ObjectArrayList<>();

        var imgDir = FabricLoader.getInstance().getConfigDir().resolve("books/img");

        List<String> fileLines = readLines(inputStream);
        for (String line : fileLines) {
            var wrappedLines = TagWrapperParser.wrapHtmlLine(line, pageWidth);
            for (String current : wrappedLines) {
                if (current.startsWith("<img:")) {
                    var tags = TextAligner.getImageTags(current);
                    List<String> frames = new ObjectArrayList<>();
                    StringBuilder leftTextBuilder = new StringBuilder();
                    for (String filepath : tags) {
                        var path = imgDir.resolve(filepath);
                        var fileInputStream = new FileInputStream(path.toFile());
                        var height = ImageIO.read(fileInputStream).getHeight();

                        if (path.toString().endsWith(".gif")) {
                            var imgs = GifFrameExtractor.get(path);
                            for (BufferedImage img : imgs) {
                                int index = imgs.indexOf(img);
                                var glyphId = (char) (int) TextAligner.FONT_INDEX++;

                                var modifiedFilename = FilenameUtils.getBaseName(filepath) + "_" + index + ".png";

                                // add img as glyph
                                fontAssetBuilder.add(new FontProviderEntry(new BitmapProvider(ResourceLocation.fromNamespaceAndPath("texteffects", "font/generated/" + modifiedFilename), List.of(String.valueOf(glyphId)), 3, height)));

                                // add img
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write(img, "png", baos);
                                var bytes = baos.toByteArray();

                                TextAligner.builder.addData("assets/texteffects/textures/font/generated/" + modifiedFilename, bytes);

                                var str = "<font:texteffects:generated>" + glyphId + "</font>";
                                frames.add(str);
                                if (index == 0)
                                    leftTextBuilder.append(str);
                            }
                        } else {
                            var glyphId = (char) (int) TextAligner.FONT_INDEX++;

                            fileInputStream = new FileInputStream(imgDir.resolve(filepath).toFile());

                            // add img as glyph
                            fontAssetBuilder.add(new FontProviderEntry(new BitmapProvider(ResourceLocation.fromNamespaceAndPath("texteffects", "font/generated/" + filepath), List.of(String.valueOf(glyphId)), 3, height)));

                            // add img
                            TextAligner.builder.addData("assets/texteffects/textures/font/generated/" + filepath, fileInputStream.readAllBytes());

                            leftTextBuilder.append("<font:texteffects:generated>").append(glyphId).append("</font>");
                        }
                    }

                    current = leftTextBuilder.toString();
                    texts.add(new Page.Line(current, frames));
                } else {
                    var finalString = TextAligner.alignSingleLine(current, TextAligner.Align.LEFT, pageWidth);
                    texts.add(new Page.Line(finalString, null));
                }
            }
        }

        List<Book.Page> pages = new ObjectArrayList<>();
        for (int i = 0; i < texts.size(); i += linesPerPage) {
            int end = Math.min(i + linesPerPage, texts.size());
            ObjectArrayList<Page.Line> pageLines = new ObjectArrayList<>();
            pageLines.addAll(texts.subList(i, end));

            // padding
            while (pageLines.size() < linesPerPage) {
                pageLines.add(new Page.Line("", null));
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
        openBookAtPage(player, pageNum, 0);
    }

    public void openBookAtPage(ServerPlayer player, int pageNum, int frame) {
        var page = this.pages().get(Mth.clamp(0, this.pages().size() - 1, pageNum));
        if (page.animated() && !Ticker.contains(player.getUUID())) {
            Ticker.add(player.getUUID(), new Ticker.ViewerData((server, time) -> {
                if (Ticker.contains(player.getUUID()))
                    openBookAtPage(player, pageNum, (int) time);
            }, pageNum));
        } else if (!page.animated()) {
            Ticker.remove(player.getUUID());
        }

        var dialogBodies = new ObjectArrayList<DialogBody>();

        var cc = TextUtil.parse("<font:texteffects:ui>3</font>");
        cc = Component.empty().append(cc).withStyle(Style.EMPTY.withShadowColor(0));

        dialogBodies.add(new PlainMessage(cc, 1));

        var str = page.asString(frame);
        for (String paragraph : str.split("\\R--- {3,}")) {
            var formattedText = TextUtil.parse(paragraph);
            formattedText = Component.empty().append(formattedText).withStyle(Style.EMPTY.withShadowColor(0));

            dialogBodies.add(new PlainMessage(formattedText, this.width() + 256));
        }

        Holder<Dialog> dialogHolder = createDialogHolder(dialogBodies, this.title(), pageNum);

        player.openDialog(dialogHolder);
    }

    public @NotNull Holder<Dialog> createDialogHolder(List<DialogBody> list, String title, int page) {
        Optional<Action> close = Optional.of(new StaticAction(new ClickEvent.RunCommand("texteffects close")));
        var closeButton = new ActionButton(new CommonButtonData(Component.literal("Close"), 150), close);
        var data = new CommonDialogData(createTitle(title, page, this.pages.size()), Optional.empty(), false, false, DialogAction.NONE, list, List.of());
        return Holder.direct(new MultiActionDialog(data, createButtons(title, page, this.pages.size()), Optional.of(closeButton), 4));
    }

    public Component createTitle(String title, int page, int maxPage) {
        var titleString = String.format("%s (Page %d / %d)", title, page + 1, this.pages.size());
        var formattedTitle = TextUtil.parse(titleString);
        formattedTitle = Component.empty().append(formattedTitle);
        return formattedTitle;
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
