package de.rexlmanu.bewerbung;
/*
* Class created by rexlManu
* Twitter: @rexlManu | Website: rexlManu.de
* Coded with IntelliJ
*/

import com.mongodb.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bewerbung extends JavaPlugin implements Listener {

    private final ExecutorService service = Executors.newCachedThreadPool();

    private DB database;
    private MongoClient client;
    private MongoCredential mongoCredential;
    private DBCollection collectionCoins;

    @Override
    public void onEnable() {
        this.service.execute(this::connect);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void connect() {
        this.mongoCredential = MongoCredential.createCredential("user", "database", "pw".toCharArray());
        this.client = new MongoClient(new ServerAddress("host", 27017), Arrays.asList(this.mongoCredential));
        this.database = this.client.getDB("database");
        this.collectionCoins = this.database.getCollection("coins");
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!playerStored(player)) {
            this.service.execute(() -> this.storePlayer(player));
        } else {
            this.service.execute(() -> this.updateLastName(player));
        }

        this.service.execute(() -> this.sendScoreboard(player));
    }

    private void sendScoreboard(Player player) {
        Scoreboard scoreboard;
        if (player.getScoreboard() == null) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        } else scoreboard = player.getScoreboard();


        Objective objective;

        if (scoreboard.getObjective("coins") == null) {
            objective = scoreboard.registerNewObjective("coins", "dummy");
        } else objective = scoreboard.getObjective("coins");

        Team coins;

        if (scoreboard.getTeam("coins") == null) {
            coins = scoreboard.registerNewTeam("coins");
        } else coins = scoreboard.getTeam("coins");

        coins.setPrefix("Deine Coins:" + getCoins(player));
        player.setScoreboard(scoreboard);
    }

    private void updateLastName(Player player) {
        DBObject data = this.collectionCoins.findOne(new BasicDBObject("uuid", player.getUniqueId().toString()));
        BasicDBObject update = new BasicDBObject("$set", data);
        update.append("$set", new BasicDBObject("lastname", player.getName()));
        collectionCoins.update(data, update);
    }

    private void updateCoins(Player player, int coins) {
        DBObject data = this.collectionCoins.findOne(new BasicDBObject("uuid", player.getUniqueId().toString()));
        BasicDBObject update = new BasicDBObject("$set", data);
        update.append("$set", new BasicDBObject("coins", coins));
        collectionCoins.update(data, update);
    }

    private int getCoins(Player player) {
        DBObject data = this.collectionCoins.findOne(new BasicDBObject("uuid", player.getUniqueId().toString()));
        return Integer.parseInt(String.valueOf(data.get("coins")));
    }

    private void storePlayer(Player player) {
        DBObject object = new BasicDBObject("uuid", player.getUniqueId().toString());
        object.put("lastname", player.getName());
        object.put("coins", 0);
        this.collectionCoins.insert(object);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("coins")) {
            if (!(sender instanceof Player)) {
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                player.sendMessage("§7Deine Coins: §a" + getCoins(player));
            } else {
                if (!player.hasPermission("lookatothercoins")) {
                    player.sendMessage("nix permission");
                    return true;
                }
                String targetname = args[0];

                Player target = Bukkit.getPlayer(targetname);
                if (target != null && target.isOnline()) {

                    player.sendMessage("§7Seine Coins: §7" + getCoins(target));

                } else {
                    player.sendMessage("player nix online");
                }
            }

        }
        return super.onCommand(sender, command, label, args);
    }

    private boolean playerStored(Player player) {
        DBObject data = this.collectionCoins.findOne(new BasicDBObject("uuid", player.getUniqueId().toString()));
        return data != null;
    }
}
