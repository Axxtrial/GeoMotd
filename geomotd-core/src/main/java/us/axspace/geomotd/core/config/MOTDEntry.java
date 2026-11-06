package us.axspace.geomotd.core.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Pre-compiled MOTD Entry using MiniMessage for ultra-fast ping responses.
 * Author: axxtrial
 */
public class MOTDEntry {
    private final String rawLine1;
    private final String rawLine2;
    private final Component line1Component;
    private final Component line2Component;
    private final Component fullComponent;

    public MOTDEntry(String rawLine1, String rawLine2) {
        this.rawLine1 = rawLine1 != null ? rawLine1 : "";
        this.rawLine2 = rawLine2 != null ? rawLine2 : "";
        MiniMessage mm = MiniMessage.miniMessage();
        this.line1Component = mm.deserialize(this.rawLine1);
        this.line2Component = mm.deserialize(this.rawLine2);
        this.fullComponent = Component.text()
                .append(this.line1Component)
                .append(Component.newline())
                .append(this.line2Component)
                .build();
    }

    public String getRawLine1() {
        return rawLine1;
    }

    public String getRawLine2() {
        return rawLine2;
    }

    public Component getLine1Component() {
        return line1Component;
    }

    public Component getLine2Component() {
        return line2Component;
    }

    public Component getFullComponent() {
        return fullComponent;
    }
}
