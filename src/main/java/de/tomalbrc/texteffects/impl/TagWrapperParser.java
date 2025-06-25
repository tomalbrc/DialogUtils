package de.tomalbrc.texteffects.impl;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagWrapperParser {
    private static final Pattern TOKENIZER = Pattern.compile("(<[^>]+>)|([^<]+)");
    private static final Pattern WORD_SPLIT = Pattern.compile("\\S+\\s*|\\s+");

    public static List<String> wrapHtmlLine(String html, int pageWidth) {
        if (html.isEmpty())
            return List.of(html);

        // (htmlFragment, visibleText) tokens
        List<Token> tokens = new ObjectArrayList<>();
        Matcher m = TOKENIZER.matcher(html);
        while (m.find()) {
            if (m.group(1) != null) {
                tokens.add(new Token(m.group(1), ""));
            } else {
                String textRun = m.group(2);
                Matcher wm = WORD_SPLIT.matcher(textRun);
                while (wm.find()) {
                    String piece = wm.group();
                    tokens.add(new Token(piece, piece));
                }
            }
        }

        // wrap into lines
        List<String> lines = new ObjectArrayList<>();
        List<Token> current = new ObjectArrayList<>();
        int widthSoFar = 0;

        for (Token tk : tokens) {
            int w = tk.visibleText.isBlank()
                    ? 0
                    : TextAligner.getTextWidth(tk.visibleText);

            if (!current.isEmpty() && widthSoFar + w > pageWidth) {
                lines.add(concat(current));
                current.clear();
                widthSoFar = 0;
            }

            current.add(tk);
            widthSoFar += w;
        }
        if (!current.isEmpty()) {
            lines.add(concat(current));
        }

        return lines;
    }

    public static int countBoldVisibleCharacters(String html) {
        List<Token> tokens = new ObjectArrayList<>();
        Matcher m = TOKENIZER.matcher(html);

        while (m.find()) {
            if (m.group(1) != null) {
                tokens.add(new Token(m.group(1), ""));
            } else {
                tokens.add(new Token(m.group(2), m.group(2)));
            }
        }

        boolean inBold = false;
        int total = 0;
        for (Token tok : tokens) {
            String tag = tok.html.toLowerCase();
            if (tag.equals("<b>")  || tag.equals("<bold>")) {
                inBold = true;
            } else if (tag.equals("</b>") || tag.equals("</bold>")) {
                inBold = false;
            } else if (inBold) {
                total += tok.visibleText.length();
            }
        }

        return total;
    }

    private static String concat(List<Token> toks) {
        StringBuilder sb = new StringBuilder();
        for (Token t : toks) sb.append(t.html);
        return sb.toString();
    }

    private static class Token {
        final String html;
        final String visibleText;
        Token(String html, String visibleText) {
            this.html        = html;
            this.visibleText = visibleText;
        }
    }
}