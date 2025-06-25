package de.tomalbrc.texteffects.impl;

import eu.pb4.mapcanvas.api.font.CanvasFont;
import eu.pb4.mapcanvas.impl.font.BitmapFont;
import eu.pb4.mapcanvas.impl.font.serialization.UniHexFontReader;
import eu.pb4.mapcanvas.impl.font.serialization.VanillaFontReader;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.font.BitmapProvider;
import eu.pb4.polymer.resourcepack.extras.api.format.font.FontAsset;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextAligner {
    public static ResourcePackBuilder builder;
    private static BitmapFont fontReader;

    private static final String BACKSPACE_CHAR = " ";
    private static final String BACKSPACE_FONT_OPEN = "<font:texteffects:align>";
    private static final String BACKSPACE_FONT_CLOSE = "</font>";

    public enum Align {
        LEFT, CENTER, RIGHT
    }

    public static void init(ResourcePackBuilder resourcePackBuilder) {
        builder = resourcePackBuilder;
        fontReader = VanillaFontReader.build((x) -> new ByteArrayInputStream(Objects.requireNonNull(builder.getDataOrSource(x))), CanvasFont.Metadata.create("Resource Pack Font", List.of("Unknown"), "Generated"), ResourceLocation.withDefaultNamespace("default"));
    }

    public static String stripTags(String input) {
        return Objects.toString(input, "").replaceAll("<[^>]*>","");
    }

    public static String alignLine(String leftText, String centerText, String rightText, int maxPixelWidth) {
        String leftTextStripped   = stripTags(leftText);
        String centerTextStripped = stripTags(centerText);
        String rightTextStripped  = stripTags(rightText);

        int leftWidth   = getTextWidth(leftTextStripped) + TagWrapperParser.countBoldVisibleCharacters(leftText);
        int centerWidth = getTextWidth(centerTextStripped) + TagWrapperParser.countBoldVisibleCharacters(centerText);
        int rightWidth  = getTextWidth(rightTextStripped) + TagWrapperParser.countBoldVisibleCharacters(rightText);
        int spaceWidth  = getTextWidth(" ");

        int rightStart  = maxPixelWidth - rightWidth;
        int centerStart = centerWidth == 0 ? rightStart : (int) Math.round((maxPixelWidth - centerWidth) / 2.0);

        StringBuilder result = new StringBuilder();
        result.append(leftText);

        int shiftToCenter = centerStart - leftWidth;
        if (shiftToCenter > 0) {
            int spacesToAdd      = Mth.ceil((float) shiftToCenter / spaceWidth);
            int backspacesToAdd  = (spacesToAdd*spaceWidth - shiftToCenter) % spaceWidth;
            result.append(" ".repeat(spacesToAdd));
            if (backspacesToAdd > 0) {
                result.append(BACKSPACE_FONT_OPEN)
                        .append(BACKSPACE_CHAR.repeat(backspacesToAdd))
                        .append(BACKSPACE_FONT_CLOSE);
            }
        } else if (shiftToCenter < 0) {
            result.append(BACKSPACE_FONT_OPEN)
                    .append(BACKSPACE_CHAR.repeat(-shiftToCenter))
                    .append(BACKSPACE_FONT_CLOSE);
        }

        result.append(centerText);

        int afterCenter   = centerStart + centerWidth;
        int shiftToRight  = rightStart - afterCenter;
        if (shiftToRight > 0) {
            int spacesToAdd      = Mth.ceil((float) shiftToRight / spaceWidth);
            int backspacesToAdd  = (spacesToAdd*spaceWidth - shiftToRight) % spaceWidth;
            result.append(" ".repeat(spacesToAdd));
            if (backspacesToAdd > 0) {
                result.append(BACKSPACE_FONT_OPEN)
                        .append(BACKSPACE_CHAR.repeat(backspacesToAdd))
                        .append(BACKSPACE_FONT_CLOSE);
            }
        }

        result.append(rightText);

        var finalString = result.toString();
        return finalString;
    }

    public static List<String> alignLines(List<String> lines, Align alignment, int maxPixelWidth) {
        List<String> aligned = new ObjectArrayList<>();
        for (String line : lines) {
            aligned.add(alignSingleLine(line, alignment, maxPixelWidth));
        }
        return aligned;
    }

    public static String alignSingleLine(String text, Align alignment, int maxPixelWidth) {
        return switch (alignment) {
            case LEFT -> alignLine(text, "", "", maxPixelWidth);
            case CENTER -> alignLine("", text, "", maxPixelWidth);
            case RIGHT -> alignLine("", "", text, maxPixelWidth);
        };
    }

    public static int getGlyphWidth(int character, int offset) {
        BitmapFont.Glyph glyph = fontReader.characters.getOrDefault(character, fontReader.defaultGlyph);

        if (glyph.logicalHeight() != 0 && glyph.height() != 0) {
            return glyph.width()+offset;
        } else {
            return (int)((double)glyph.fontWidth());
        }
    }

    public static int getTextWidth(String text) {
        if (text.isEmpty()) {
            return 0;
        } else {
            int posX = 0;
            int[] array = text.codePoints().toArray();
            for (int j : array) {
                posX += getGlyphWidth(j, 1);
            }
            return posX;
        }
    }

    public static Map<String, String> getImageTags(String text) {
        Pattern pattern = Pattern.compile("<img:([^>]+)>");
        Matcher matcher = pattern.matcher(text);

        Map<String, String> list = new Object2ObjectArrayMap<>();
        while (matcher.find()) {
            list.put(matcher.group(0), matcher.group(1));
        }

        return list;
    }
}