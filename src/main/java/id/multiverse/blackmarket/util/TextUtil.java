package id.multiverse.blackmarket.util;

import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN =
            Pattern.compile("<gradient:([#A-Fa-f0-9]+):([#A-Fa-f0-9]+)>(.*?)</gradient>", Pattern.DOTALL);

    /**
     * Parse text with gradient tags and hex colors into legacy Minecraft color codes.
     * Supports: <gradient:#from:#to>text</gradient>, #RRGGBB, &codes
     */
    public static String parse(String text) {
        if (text == null) return "";

        // Handle \n literal
        text = text.replace("\\n", "\n");

        // Process gradients
        Matcher gm = GRADIENT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (gm.find()) {
            String fromHex = gm.group(1);
            String toHex = gm.group(2);
            String content = gm.group(3);
            gm.appendReplacement(sb, Matcher.quoteReplacement(applyGradient(content, fromHex, toHex)));
        }
        gm.appendTail(sb);
        text = sb.toString();

        // Process hex #RRGGBB
        text = parseHex(text);

        // Process MiniMessage-like basic tags (simplified)
        text = parseMiniTags(text);

        // Process &codes
        text = ChatColor.translateAlternateColorCodes('&', text);

        return text;
    }

    public static String parseLegacy(String text) {
        return parse(text);
    }

    private static String applyGradient(String text, String fromHex, String toHex) {
        // Strip existing color codes from text for character counting
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
        int len = stripped.length();
        if (len == 0) return text;

        Color from = hexToColor(fromHex);
        Color to = hexToColor(toHex);

        StringBuilder result = new StringBuilder();
        int coloredIndex = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Skip color codes in source
            if (c == '\u00A7' || c == '&') {
                if (i + 1 < text.length()) {
                    result.append(c).append(text.charAt(i + 1));
                    i++;
                }
                continue;
            }
            float ratio = len == 1 ? 0f : (float) coloredIndex / (len - 1);
            result.append(interpolateColor(from, to, ratio));
            result.append(c);
            coloredIndex++;
        }
        return result.toString();
    }

    private static String interpolateColor(Color from, Color to, float ratio) {
        int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * ratio);
        int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * ratio);
        int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * ratio);
        return String.format("\u00A7x\u00A7%02x\u00A7%02x\u00A7%02x\u00A7%02x\u00A7%02x\u00A7%02x",
                hexDigit(r >> 4), hexDigit(r & 0xF),
                hexDigit(g >> 4), hexDigit(g & 0xF),
                hexDigit(b >> 4), hexDigit(b & 0xF))
                .replace("%02x", "") // fix - rebuild manually
                + toLegacyHex(r, g, b);
    }

    private static String toLegacyHex(int r, int g, int b) {
        String hex = String.format("%06X", (r << 16) | (g << 8) | b);
        return "\u00A7x" +
                "\u00A7" + hex.charAt(0) +
                "\u00A7" + hex.charAt(1) +
                "\u00A7" + hex.charAt(2) +
                "\u00A7" + hex.charAt(3) +
                "\u00A7" + hex.charAt(4) +
                "\u00A7" + hex.charAt(5);
    }

    private static int hexDigit(int v) { return v; }

    private static Color hexToColor(String hex) {
        hex = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            return new Color(Integer.parseInt(hex, 16));
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private static String parseHex(String text) {
        Matcher m = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1).toUpperCase();
            String replacement = "\u00A7x" +
                    "\u00A7" + hex.charAt(0) +
                    "\u00A7" + hex.charAt(1) +
                    "\u00A7" + hex.charAt(2) +
                    "\u00A7" + hex.charAt(3) +
                    "\u00A7" + hex.charAt(4) +
                    "\u00A7" + hex.charAt(5);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Very simplified MiniMessage tag support */
    private static String parseMiniTags(String text) {
        text = text.replaceAll("<white>", "\u00A7f").replaceAll("</white>", "");
        text = text.replaceAll("<gray>", "\u00A77").replaceAll("</gray>", "");
        text = text.replaceAll("<dark_gray>", "\u00A78").replaceAll("</dark_gray>", "");
        text = text.replaceAll("<black>", "\u00A70").replaceAll("</black>", "");
        text = text.replaceAll("<green>", "\u00A7a").replaceAll("</green>", "");
        text = text.replaceAll("<dark_green>", "\u00A72").replaceAll("</dark_green>", "");
        text = text.replaceAll("<aqua>", "\u00A7b").replaceAll("</aqua>", "");
        text = text.replaceAll("<dark_aqua>", "\u00A73").replaceAll("</dark_aqua>", "");
        text = text.replaceAll("<blue>", "\u00A79").replaceAll("</blue>", "");
        text = text.replaceAll("<dark_blue>", "\u00A71").replaceAll("</dark_blue>", "");
        text = text.replaceAll("<red>", "\u00A7c").replaceAll("</red>", "");
        text = text.replaceAll("<dark_red>", "\u00A74").replaceAll("</dark_red>", "");
        text = text.replaceAll("<yellow>", "\u00A7e").replaceAll("</yellow>", "");
        text = text.replaceAll("<gold>", "\u00A76").replaceAll("</gold>", "");
        text = text.replaceAll("<light_purple>", "\u00A7d").replaceAll("</light_purple>", "");
        text = text.replaceAll("<dark_purple>", "\u00A75").replaceAll("</dark_purple>", "");
        text = text.replaceAll("<bold>", "\u00A7l").replaceAll("</bold>", "");
        text = text.replaceAll("<italic>", "\u00A7o").replaceAll("</italic>", "");
        text = text.replaceAll("<underline>", "\u00A7n").replaceAll("</underline>", "");
        text = text.replaceAll("<strikethrough>", "\u00A7m").replaceAll("</strikethrough>", "");
        text = text.replaceAll("<reset>", "\u00A7r");
        return text;
    }
}
