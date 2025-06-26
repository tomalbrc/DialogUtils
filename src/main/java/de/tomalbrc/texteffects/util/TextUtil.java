package de.tomalbrc.texteffects.util;

import eu.pb4.placeholders.api.TextParserUtils;
import net.minecraft.network.chat.Component;

public class TextUtil {
    public static Component parse(String s) {
        return TextParserUtils.formatText(s);
    }
}
