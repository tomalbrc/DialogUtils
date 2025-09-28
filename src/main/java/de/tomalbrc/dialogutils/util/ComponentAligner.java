package de.tomalbrc.dialogutils.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ComponentAligner {
    public static MutableComponent align(Component component, TextUtil.Alignment alignment, int width) {
        int w = getWidth(component);
        int remainingSpace = width - w;

        if (alignment == TextUtil.Alignment.LEFT) {
            var space = spacer(remainingSpace);
            return Component.empty().append(component).append(space);
        } else if (alignment == TextUtil.Alignment.RIGHT) {
            var space = spacer(remainingSpace);
            return Component.empty().append(space).append(component);
        } else {
            int half = remainingSpace / 2;
            var space = spacer(half);
            if (remainingSpace % 2 == 1) {
                var space2 = spacer(half+1);
                return Component.empty().append(space2).append(component).append(space);
            }

            return Component.empty().append(space).append(component).append(space);
        }
    }

    public static int getWidth(Component component) {
        AtomicInteger w = new AtomicInteger(0);
        for (Component part : component.toFlatList()) {
            int val = part.visit((style, content) -> {
                int textWidth = TextUtil.getTextWidth(content, style.getFont());

                if (style.isBold()) {
                    textWidth += content.length();
                }
                return Optional.of(textWidth);
            }, Style.EMPTY).orElse(0);
            w.addAndGet(val);
        }

        return w.get();
    }

    public static Component defaultFont(Component component) {
        return Component.empty().append(component).withStyle(Style.EMPTY.withFont(FontUtil.FONT));
    }

    public static Component spacer(int width) {
        return Component.literal((width < 0 ? " " : ".").repeat(Mth.abs(width))).withStyle(Style.EMPTY.withFont(FontUtil.ALIGN_FONT));
    }
}
