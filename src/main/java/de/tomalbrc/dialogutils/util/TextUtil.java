package de.tomalbrc.dialogutils.util;

import eu.pb4.mapcanvas.impl.font.BitmapFont;
import eu.pb4.placeholders.api.TextParserUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TextUtil {
    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    public static int getGlyphWidth(int character, int offset, ResourceLocation font) {
        var fr = FontUtil.fontReader(font);
        if (fr == null)
            return 5;

        BitmapFont.Glyph glyph = fr.characters.getOrDefault(character, fr.defaultGlyph);

        if (glyph.logicalHeight() != 0 && glyph.height() != 0) {
            return glyph.width() + offset;
        } else {
            return (int) ((double) glyph.fontWidth());
        }
    }

    public static int getTextWidth(String text, ResourceLocation font) {
        if (text.isEmpty()) {
            return 0;
        } else {
            int posX = 0;
            int[] array = text.codePoints().toArray();
            for (int j : array) {
                posX += getGlyphWidth(j, 1, font);
            }
            return posX;
        }
    }

    public static Component parse(String s) {
        return TextParserUtils.formatText(s);
    }

}
