package fr.younishd.inception;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

public class DreamDimension {

    private final Logger logger;
    private Map<Location, DreamStack> bedDreams;
    private Map<String, DreamStack> playerDreams;

    public DreamDimension(Logger logger) {
        this.logger = logger;
        this.bedDreams = new HashMap<>();
        this.playerDreams = new HashMap<>();
    }

    public void prepareDream(Player player) {
        if (!this.playerDreams.containsKey(player.getName())) {
            this.logger.info("Prepare dream for player " + player.getName() + "...");
            this.playerDreams.put(player.getName(), new DreamStack(player, this.logger));
        }
    }

    public boolean enterDream(Player player, Location bed) {
        this.logger.info("Player " + player.getName() + " enters dream.");

        DreamStack dream;

        if (this.bedDreams.containsKey(bed)) {
            this.logger.info("Bed has dream attached to it.");
            dream = this.bedDreams.get(bed);
            this.playerDreams.put(player.getName(), dream);
            return dream.sleep(player, bed);
        }

        if (this.playerDreams.containsKey(player.getName())) {
            this.logger.info("Player " + player.getName() + " has dream attached to it.");
            dream = this.playerDreams.get(player.getName());
            this.bedDreams.put(bed, dream);
            return dream.sleep(player, bed);
        }

        this.logger.info("Enter new dream.");
        dream = new DreamStack(player, this.logger);
        this.playerDreams.put(player.getName(), dream);
        this.bedDreams.put(bed, dream);
        return dream.sleep(player, bed);
    }

    public Location leaveDream(Player player) {
        if (!this.playerDreams.containsKey(player.getName())) {
            this.logger.info("Player " + player.getName() + " is not in a dream.");
            return null;
        }

        this.logger.info("Player " + player.getName() + " leaves dream.");

        DreamStack dream = this.playerDreams.get(player.getName());

        if (dream.getDreamer(player) == null) {
            this.logger.info("Player " + player.getName() + " was still attached to a dream that he already left.");
            this.playerDreams.remove(player.getName());
            return null;
        }

        if (dream.getDreamer(player).getLevel() == 1) {
            this.logger.info("Detach player " + player.getName() + " from dream as they wake up.");
            this.playerDreams.remove(player.getName());

            if (player.getName().equals(dream.getOwner().getPlayer().getName())) {
                this.logger.info("Player " + player.getName() + " is dream owner.");
                this.logger.info("Detach all other players from dream...");
                for (Dreamer d : dream.getDreamers()) {
                    this.playerDreams.remove(d.getPlayer().getName());
                }
                this.logger.info("Detach the bed from dream.");
                this.bedDreams.remove(dream.getOwner().peekBed());
            }
        }

        return dream.wakeup(player);
    }

    class DreamStack {

        private final Logger logger;
        private Dreamer owner;
        private Map<String, Dreamer> dreamers;
        private Map<Integer, World> worlds;
        private int levels;

        public DreamStack(Player owner, Logger logger) {
            this.owner = new Dreamer(owner, logger);
            this.logger = logger;
            this.dreamers = new HashMap<>();
            this.worlds = new HashMap<>();
            this.levels = 0;

            this.logger.info("Create new DreamStack for player " + owner.getName() + ".");

            this.prepareLevel(1);
        }

        private void prepareLevel(int level) {
            if (this.worlds.containsKey(level)) {
                this.logger.info("Level " + level + " has already been prepared.");
                return;
            }

            this.logger.info("Prepare level " + level + " of " + this.owner.getPlayer().getName() + "'s dream.");

            WorldCreator c = new WorldCreator("world_dream_" + this.owner.getPlayer().getName() + "_level_" + level);
            c.generateStructures(false);
            this.worlds.put(level, this.owner.getPlayer().getServer().createWorld(c));
        }

        public Dreamer getOwner() {
            return this.owner;
        }

        public boolean sleep(Player player, Location bed) {
            this.logger.info("Sleep player " + player.getName() + ".");

            if (!this.dreamers.containsKey(player.getName())) {
                this.logger.info("Player " + player.getName() + " was not dreaming yet.");
                if (this.owner.getPlayer().getName().equals(player.getName())) {
                    this.logger.info("Player " + player.getName() + " is entering their own dream.");
                    this.dreamers.put(player.getName(), this.owner);
                } else {
                    this.logger.info("Player " + player.getName() + " is entering " + this.owner.getPlayer().getName() + "'s dream.");
                    this.dreamers.put(player.getName(), new Dreamer(player, this.logger));
                }
            }

            Dreamer dreamer = this.dreamers.get(player.getName());

            if (!player.getName().equals(this.owner.getPlayer().getName()) && dreamer.getLevel() == this.levels) {
                this.logger.info("Player " + player.getName() + " is not dream owner and has reached last dream level.");
                return false;
            }

            dreamer.pushBed(bed);
            this.logger.info("Teleport " + player.getName() + " to " + this.owner.getPlayer().getName() + "'s dream world level " + dreamer.getLevel() + ".");
            player.teleport(this.worlds.get(dreamer.getLevel()).getSpawnLocation());

            this.prepareLevel(this.owner.getLevel() + 1);

            return true;
        }

        public Location wakeup(Player player) {
            this.logger.info("Wake up player " + player.getName() + ".");

            if (!this.dreamers.containsKey(player.getName())) {
                this.logger.info("Cannot wake up an already awake player!");
                return null;
            }

            Dreamer dreamer = this.dreamers.get(player.getName());

            int level = dreamer.getLevel();

            if (level == 0) {
                this.logger.info("Cannot wake up an already awake player!");
                return null;
            }

            this.logger.info("Teleport " + player.getName() + " back to level " + (dreamer.getLevel() - 1) + ".");
            Location exit = dreamer.popBed();
            player.teleport(exit);

            if (player.getName().equals(this.owner.getPlayer().getName())) {
                this.logger.info("Player " + player.getName() + " is the dream owner.");
                this.logger.info("Killing everyone at level " + level + "...");
                for (Player p : this.worlds.get(level).getPlayers()) {
                    p.setHealth(0);
                }
            }

            if (dreamer.getLevel() == 0) {
                this.logger.info("Remove awake player " + player.getName() + " from dream.");
                this.dreamers.remove(player.getName());
            }

            return exit;
        }

        public Dreamer getDreamer(Player player) {
            if (!this.dreamers.containsKey(player.getName())) {
                return null;
            }

            return this.dreamers.get(player.getName());
        }

        public List<Dreamer> getDreamers() {
            return new ArrayList<>(this.dreamers.values());
        }

    }

    class Dreamer {

        private final Logger logger;
        private Player player;
        private Stack<Location> beds;

        public Dreamer(Player player, Logger logger) {
            this.player = player;
            this.logger = logger;
            this.beds = new Stack<>();
        }

        public void pushBed(Location bed) {
            this.beds.push(bed);
            this.logger.info("push bed " + bed.getWorld().getName() + " (" + bed.getX() + ", " + bed.getY() + ", " + bed.getZ() + ")");
        }

        public Location popBed() {
            Location bed = this.beds.pop();
            this.logger.info("pop bed " + bed.getWorld().getName() + " (" + bed.getX() + ", " + bed.getY() + ", " + bed.getZ() + ")");
            return bed;
        }

        public int getLevel() {
            return this.beds.size();
        }

        public Player getPlayer() {
            return this.player;
        }

        public Location peekBed() {
            if (this.beds.size() > 0) {
                return this.beds.peek();
            }
            return null;
        }

    }

}
