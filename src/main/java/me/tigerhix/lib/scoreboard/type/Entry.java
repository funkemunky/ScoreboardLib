package me.tigerhix.lib.scoreboard.type;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Entry {

    private TextComponent name;
    private int position;

    public Entry(String name, int position) {
        this.name = LegacyComponentSerializer.legacySection().deserialize(name);
        this.position = position;
    }

    public TextComponent getName() {
        return name;
    }

    public void setName(TextComponent name) {
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

}
