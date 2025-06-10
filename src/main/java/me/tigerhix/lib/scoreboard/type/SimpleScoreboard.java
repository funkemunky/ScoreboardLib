package me.tigerhix.lib.scoreboard.type;

import me.tigerhix.lib.scoreboard.ScoreboardLib;
import me.tigerhix.lib.scoreboard.common.Strings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleScoreboard implements Scoreboard {

    private final org.bukkit.scoreboard.Scoreboard scoreboard;
    private final Objective objective;

    protected Player holder;
    protected long updateInterval = 10L;

    private boolean activated;
    private ScoreboardHandler handler;
    // We use a map to track teams by line number for easy updating.
    private final Map<Integer, Team> teams = new HashMap<>();
    private BukkitRunnable updateTask;

    public SimpleScoreboard(Player holder) {
        this.holder = holder;
        // Initiate the Bukkit scoreboard
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard.registerNewObjective("board", "dummy").setDisplaySlot(DisplaySlot.SIDEBAR);
        objective = scoreboard.getObjective(DisplaySlot.SIDEBAR);
    }

    @Override
    public void activate() {
        if (activated) return;
        if (handler == null) throw new IllegalArgumentException("Scoreboard handler not set");
        activated = true;
        // Set to the custom scoreboard
        holder.setScoreboard(scoreboard);
        // And start updating on a desired interval
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        updateTask.runTaskTimer(ScoreboardLib.getPluginInstance(), 0, updateInterval);
    }

    @Override
    public void deactivate() {
        if (!activated) return;
        activated = false;
        // Set to the main scoreboard
        if (holder.isOnline()) {
            synchronized (this) {
                holder.setScoreboard((Bukkit.getScoreboardManager().getMainScoreboard()));
            }
        }
        // Unregister teams that are created for this scoreboard
        for (Team team : teams.values()) {
            team.unregister();
        }
        // Stop updating
        updateTask.cancel();
    }

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public ScoreboardHandler getHandler() {
        return handler;
    }

    @Override
    public Scoreboard setHandler(ScoreboardHandler handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public long getUpdateInterval() {
        return updateInterval;
    }

    @Override
    public SimpleScoreboard setUpdateInterval(long updateInterval) {
        if (activated) throw new IllegalStateException("Scoreboard is already activated");
        this.updateInterval = updateInterval;
        return this;
    }

    @Override
    public Player getHolder() {
        return holder;
    }

    @SuppressWarnings("deprecation")
    private void update() {
        if (!holder.isOnline()) {
            deactivate();
            return;
        }
        // Title
        String handlerTitle = handler.getTitle(holder);
        String finalTitle = Strings.format(handlerTitle != null ? handlerTitle : ChatColor.BOLD.toString());
        if (!objective.getDisplayName().equals(finalTitle)) objective.setDisplayName(Strings.format(finalTitle));
        // Entries
        List<Entry> passed = handler.getEntries(holder);
        if (passed == null) return;
        for (Entry entry : passed) {
            // Handle the entry
            TextComponent key = entry.getName();
            int score = entry.getPosition();

            setLine(score, key);
            // Update references
        }
    }

    public Objective getObjective() {
        return objective;
    }

    public org.bukkit.scoreboard.Scoreboard getScoreboard() {
        return scoreboard;
    }


    /**
     * Sets the title of the scoreboard.
     * @param title The new title (supports ChatColor).
     */
    public void setTitle(String title) {
        this.objective.setDisplayName(title);
    }

    /**
     * Sets a specific line on the scoreboard to a rich TextComponent.
     * @param line The line number (from top to bottom, 15 is highest).
     * @param component The BaseComponent (e.g., TextComponent) to display.
     */
    public void setLine(int line, Component component) {
        Team team = getTeamForLine(line);

        // In 1.13+ we can set the prefix/suffix directly with components.
        team.prefix(component);

        // Set the score to show this line.
        objective.getScore(getEntryForLine(line)).setScore(line);
    }


    /**
     * Retrieves or creates a team for a given line number.
     * Teams are used to hold the prefix, which is our component content.
     */
    private Team getTeamForLine(int line) {
        if (teams.containsKey(line)) {
            return teams.get(line);
        }

        // Team name must be unique. Line number is a good candidate.
        Team team = scoreboard.registerNewTeam("line" + line);

        // Each team needs a unique entry to display. We use ChatColor codes.
        // This entry is what gets the score, but it will be invisible.
        String entry = getEntryForLine(line);
        team.addEntry(entry);

        teams.put(line, team);
        return team;
    }

    /**
     * Gets the unique, invisible entry name for a given line.
     * We use ChatColor codes to make them unique and not display in chat.
     */
    private String getEntryForLine(int line) {
        return ChatColor.values()[line].toString() + ChatColor.RESET;
    }

}