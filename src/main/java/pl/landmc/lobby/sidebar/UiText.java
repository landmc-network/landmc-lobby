package pl.landmc.lobby.sidebar;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import pl.landmc.lobby.config.LobbyConfig;
import pl.landmc.platform.component.ComponentFormatter;

/**
 * Turns a written layout into the characters that draw it.
 *
 * <p>Minecraft cannot draw a box; it draws text, and the resource pack decides what a character
 * looks like. A panel is therefore a character whose picture is a rounded rectangle, and moving
 * something two pixels left is a character whose only property is that it advances the cursor
 * by minus two. Everything a drawn interface does is one of those two things.
 *
 * <p>Which means a layout could be written directly as private use codepoints in a config file,
 * and must not be: such a character is invisible in every editor, survives no copy-paste and
 * says nothing about what it is. So the config says {@code {PANEL:sidebar}} and
 * {@code {SPACE:-170}}, and this turns them into the real thing.
 *
 * <p>The other half of its job is making every line the same width. The sidebar right-aligns
 * each line on its own, so without this a line of "Monety: 0" and a line of "Diamenty: 1400"
 * start at two different places and the whole board looks torn. Measuring what a line will
 * occupy and padding it to a fixed width is what holds the layout still - and measuring means
 * the font's own glyph widths, because an {@code i} is one pixel and an {@code m} is five.
 */
public final class UiText {

    /** {@code {SPACE:-170}} and {@code {PANEL:sidebar}}. */
    private static final Pattern TOKEN = Pattern.compile("\\{(SPACE|PANEL):([^}]+)}");

    /** The steps the pack's space font defines, largest first so the split is shortest. */
    private static final int[] STEPS = {256, 128, 64, 32, 16, 8, 4, 2, 1};

    /** Where those characters start, matching scripts/build-ui-font.py in landmc-deploy. */
    private static final int SPACE_BASE = 0xE900;
    private static final int SPACE_NEGATIVE_OFFSET = 0x80;

    /** Every glyph in the default font not named in {@link #NARROW}, plus one of spacing. */
    private static final int DEFAULT_GLYPH = 5;

    /** The glyphs that are not five pixels wide, as {@code <char><width>} pairs. */
    private static final String NARROW =
            "!1'1,1.1:1;1i1|1`2l2 3\"3(4)4*3I3[3]3t3f4k4<4>4{4}4@6~6";

    private final LobbyConfig.UiSection config;
    private final ComponentFormatter formatter;

    /** Panel name to the character that draws it. */
    private final Map<String, String> panels = new LinkedHashMap<>();

    /** Every character this pack gives an advance to, and what that advance is. */
    private final Map<Character, Integer> advances = new HashMap<>();

    public UiText(LobbyConfig.UiSection config, ComponentFormatter formatter) {
        this.config = Objects.requireNonNull(config, "config");
        this.formatter = Objects.requireNonNull(formatter, "formatter");

        for (Map.Entry<String, String> panel : config.panels.entrySet()) {
            String character = fromCodepoint(panel.getValue());
            if (character.isEmpty()) {
                continue;
            }
            this.panels.put(panel.getKey(), character);
            this.advances.put(character.charAt(0), widthOf(panel.getKey(), config));
        }

        for (int index = 0; index < STEPS.length; index++) {
            int step = 1 << index;
            this.advances.put((char) (SPACE_BASE + index), step);
            this.advances.put((char) (SPACE_BASE + SPACE_NEGATIVE_OFFSET + index), -step);
        }
    }

    public boolean isEnabled() {
        return this.config.enabled;
    }

    /**
     * A configured line, ready to be parsed.
     *
     * <p>With the interface switched off the tokens are removed and nothing is padded: a server
     * without the pack gets a plain sidebar rather than a column of missing-glyph boxes.
     */
    public String render(String line) {
        return this.render(line, this.config.lineWidth);
    }

    /**
     * The same, padded to a width the caller chooses.
     *
     * <p>Each surface has its own: the sidebar is as wide as the board, the bar across the top
     * is as wide as its panel. What matters is only that every line on one surface agrees, since
     * that is what stops the client aligning them against each other.
     */
    public String render(String line, int width) {
        String expanded = this.expand(line);
        if (!this.config.enabled || width <= 0) {
            return expanded;
        }

        int missing = width - this.width(expanded);
        return missing == 0 ? expanded : expanded + this.space(missing);
    }

    private String expand(String line) {
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

    /**
     * How wide a line will be once it is drawn.
     *
     * <p>Parsed rather than scanned, because bold costs a pixel per character and only the
     * parsed form knows where bold starts. The panel and space characters are already in the
     * string by this point, and carry the advances the pack gives them.
     */
    private int width(String text) {
        return this.width(this.formatter.format(text), false);
    }

    private int width(Component component, boolean inheritedBold) {
        boolean bold = switch (component.style().decoration(TextDecoration.BOLD)) {
            case TRUE -> true;
            case FALSE -> false;
            case NOT_SET -> inheritedBold;
        };

        int width = 0;
        if (component instanceof TextComponent text) {
            for (int index = 0; index < text.content().length(); index++) {
                char character = text.content().charAt(index);

                Integer advance = this.advances.get(character);
                if (advance != null) {
                    // A panel or a space: the pack decides what it advances, and bold does not
                    // apply - these are not letters.
                    width += advance;
                    continue;
                }
                width += glyph(character) + 1 + (bold ? 1 : 0);
            }
        }

        for (Component child : component.children()) {
            width += this.width(child, bold);
        }
        return width;
    }

    private static int glyph(char character) {
        int found = NARROW.indexOf(character);
        // Only an even position is a character; an odd one is the width of the glyph before it.
        if (found < 0 || found % 2 != 0) {
            return DEFAULT_GLYPH;
        }
        return NARROW.charAt(found + 1) - '0';
    }

    /** A panel's drawn width, which is also what it advances the cursor by. */
    private static int widthOf(String name, LobbyConfig.UiSection config) {
        Integer width = config.panelWidths.get(name);
        return width == null ? 0 : width;
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
