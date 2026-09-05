package pl.landmc.lobby.sidebar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pl.landmc.lobby.config.LobbyConfig;

/**
 * Turns a written layout into the characters that draw it.
 *
 * <p>Minecraft cannot draw a box; it draws text, and the resource pack decides what a character
 * looks like. A panel is therefore a character whose picture is a rounded rectangle, and moving
 * something two pixels left is a character whose only property is that it advances the cursor
 * by minus two. Everything a custom interface does is one of those two things.
 *
 * <p>Which means a layout could be written directly as private use codepoints in a config file,
 * and must not be: {@code } is invisible in every editor, survives no copy-paste, and
 * says nothing about what it is. So the config says {@code {PANEL:sidebar_head}} and
 * {@code {SPACE:-176}}, and this turns them into the real thing.
 *
 * <p>An offset is written as a sum of powers of two because that is what the pack provides -
 * nine characters covering every distance up to 511 in either direction, rather than one glyph
 * per pixel. Twenty-three pixels left is the characters for sixteen, four, two and one.
 */
public final class UiText {

    /** {@code {SPACE:-176}} and {@code {PANEL:sidebar_head}}. */
    private static final Pattern TOKEN = Pattern.compile("\\{(SPACE|PANEL):([^}]+)}");

    /** The steps the pack's space font defines, largest first so the greedy split is shortest. */
    private static final int[] STEPS = {256, 128, 64, 32, 16, 8, 4, 2, 1};

    /** Where those characters start, matching scripts/build-ui-font.py in landmc-deploy. */
    private static final int SPACE_BASE = 0xE900;
    private static final int SPACE_NEGATIVE_OFFSET = 0x80;

    private final LobbyConfig.UiSection config;

    /** Panel name to the character that draws it, read once from the configuration. */
    private final Map<String, String> panels = new LinkedHashMap<>();

    public UiText(LobbyConfig.UiSection config) {
        this.config = Objects.requireNonNull(config, "config");

        for (Map.Entry<String, String> panel : config.panels.entrySet()) {
            this.panels.put(panel.getKey(), fromCodepoint(panel.getValue()));
        }
    }

    public boolean isEnabled() {
        return this.config.enabled;
    }

    /**
     * Replaces every layout token in a line.
     *
     * <p>With the interface switched off the tokens are removed rather than drawn. A server
     * running without the pack would otherwise show a column of missing-glyph boxes, which is
     * worse than a plain sidebar.
     */
    public String expand(String line) {
        Matcher matcher = TOKEN.matcher(line);
        StringBuilder result = new StringBuilder(line.length());

        while (matcher.find()) {
            String replacement = this.config.enabled
                    ? this.resolve(matcher.group(1), matcher.group(2))
                    : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String resolve(String kind, String argument) {
        if ("SPACE".equals(kind)) {
            try {
                return this.space(Integer.parseInt(argument.trim()));
            }
            catch (NumberFormatException notANumber) {
                // A layout that does not parse draws nothing rather than a stray glyph.
                return "";
            }
        }

        String panel = this.panels.get(argument.trim());
        return panel == null ? "" : "<font:" + this.config.font + ">" + panel + "</font>";
    }

    /** The characters that move the cursor by this many pixels. */
    private String space(int pixels) {
        if (pixels == 0) {
            return "";
        }

        int remaining = Math.abs(pixels);
        boolean negative = pixels < 0;

        StringBuilder characters = new StringBuilder();
        for (int step : STEPS) {
            while (remaining >= step) {
                characters.append(spaceCharacter(step, negative));
                remaining -= step;
            }
        }

        return "<font:" + this.config.spaceFont + ">" + characters + "</font>";
    }

    private static String spaceCharacter(int step, boolean negative) {
        int index = Integer.numberOfTrailingZeros(step);
        return String.valueOf(
                (char) (SPACE_BASE + index + (negative ? SPACE_NEGATIVE_OFFSET : 0)));
    }

    /** {@code U+E000} as written in the configuration, or the text itself if it is a character. */
    private static String fromCodepoint(String value) {
        String text = value.trim();
        if (!text.regionMatches(true, 0, "U+", 0, 2)) {
            return text;
        }

        try {
            return String.valueOf((char) Integer.parseInt(text.substring(2), 16));
        }
        catch (NumberFormatException notHex) {
            return "";
        }
    }
}
