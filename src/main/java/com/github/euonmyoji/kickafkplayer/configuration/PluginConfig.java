package com.github.euonmyoji.kickafkplayer.configuration;

import com.github.euonmyoji.kickafkplayer.KickAfkPlayer;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.IOException;


/**
 * @author yinyangshi
 */
public final class PluginConfig {
    private static CommentedConfigurationNode cfg;
    private static CommentedConfigurationNode generalNode;
    private static ConfigurationLoader<CommentedConfigurationNode> loader;
    public static double radiusSquared = 6;
    public static int checkSeconds = 60 * 15;
    public static int kickSeconds = 60 * 5;
    public static int playerLimit = 2;
    public static String format = "你是挂机机器人吗？复读这段命令证明你是人类:/afkverify %d";

    private PluginConfig() {
        throw new UnsupportedOperationException();
    }

    public static void init() {
        loader = HoconConfigurationLoader.builder()
                .setPath(KickAfkPlayer.plugin.cfgDir.resolve("kickafkplayer.conf")).build();
        reload();
        save();
    }

    public static void reload() {
        loadNode();
        radiusSquared = generalNode.getNode("radius").getDouble(6);
        radiusSquared *= radiusSquared;
        checkSeconds = generalNode.getNode("checkSeconds").getInt(60 * 15);
        kickSeconds = generalNode.getNode("kickSeconds").getInt(60 * 5);
        playerLimit = generalNode.getNode("player-limit").getInt(2);
        format = generalNode.getNode("afkMsg").getString("你是挂机机器人吗？复读这段命令证明你是人类:/afkverify %d");
    }

    private static void save() {
        try {
            loader.save(cfg);
        } catch (IOException e) {
            KickAfkPlayer.logger.warn("error when saving plugin config", e);
        }
    }

    private static void loadNode() {
        try {
            cfg = loader.load(ConfigurationOptions.defaults().setShouldCopyDefaults(true));
        } catch (IOException e) {
            KickAfkPlayer.logger.warn("load plugin config failed, creating new one(if null)", e);
            if (cfg == null) {
                cfg = loader.createEmptyNode(ConfigurationOptions.defaults());
            }
        }
        generalNode = cfg.getNode("general");
    }
}