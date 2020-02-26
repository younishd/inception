package fr.younishd.inception;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class InceptionPlugin extends JavaPlugin implements Listener {

    private DreamDimension dreamDimension;

    @Override
    public void onEnable() {
        this.getLogger().info("Hello.");

        this.getServer().getPluginManager().registerEvents(this, this);

        this.dreamDimension = new DreamDimension(this.getLogger());

        this.getLogger().info("Updating dream levels...");
        for (Player p : this.getServer().getOnlinePlayers()) {
            this.dreamDimension.prepareDream(p);
        }
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Teleport all players to the real world...");
        for (Player p : this.getServer().getOnlinePlayers()) {
            p.teleport(this.getServer().getWorlds().get(0).getSpawnLocation());
        }

        this.getLogger().info("Bye.");
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent e) {
        this.getLogger().info("PlayerBedEnterEvent");
        this.dreamDimension.enterDream(e.getPlayer(), e.getBed().getLocation());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        this.getLogger().info("PlayerRespawnEvent");
        Location exit = this.dreamDimension.leaveDream(e.getPlayer());
        if (exit == null) {
            return;
        }
        e.setRespawnLocation(exit);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {
        this.getLogger().info("PlayerLoginEvent");
        this.dreamDimension.prepareDream(e.getPlayer());
    }

}
