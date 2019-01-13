package com.github.euonmyoji.kickafkplayer.configuration;

import com.github.euonmyoji.kickafkplayer.KickAfkPlayer;
import com.github.euonmyoji.kickafkplayer.util.Util;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tuple;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author yinyangshi
 */
public class PlayerLog {
    private static CommentedConfigurationNode cfg;
    private static ConfigurationLoader<CommentedConfigurationNode> loader;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMddHHmmss");
    private static final DateTimeFormatter TEXT_FORMATTER = DateTimeFormatter.ofPattern("MM月dd号HH时mm分ss秒");


    private PlayerLog() {
        throw new UnsupportedOperationException();
    }

    public static void init() {
        loader = HoconConfigurationLoader.builder()
                .setPath(KickAfkPlayer.plugin.cfgDir.resolve("playerlog.conf")).build();
        reload();
        save();
    }

    public static void kickedPlayer(Player p) {
        LocalDateTime now = LocalDateTime.now();
        CommentedConfigurationNode node = cfg.getNode(p.getName()).getNode(now.format(FORMATTER));
        node.getNode("l").setValue(p.getWorld().getName() + "," + Util.vector3DToString(p.getLocation().getPosition()));
        save();
    }

    public static Iterable<Text> getTop() {
        List<Tuple<String, Integer>> list = new LinkedList<>();
        cfg.getChildrenMap().forEach((s, times) -> list
                .add(Tuple.of(s.toString(), times.getChildrenMap().size())));
        list.sort((o1, o2) -> o2.getSecond() - o1.getSecond());
        List<Text> texts = new ArrayList<>(list.size());
        int[] rank = {1};
        list.forEach(tuple -> texts.add(Text.of(rank[0]++ + ":" + tuple.getFirst() + " " + tuple.getSecond() + "次")));
        return texts;
    }

    public static void reload() {
        loadNode();
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
            cfg = loader.load();
        } catch (IOException e) {
            KickAfkPlayer.logger.warn("load player log config failed", e);
        }
    }

    public static Iterable<Text> getInfo(String name) {
        List<Tuple<String, String>> list = new LinkedList<>();
        cfg.getNode(name).getChildrenMap().forEach((o, o2) ->
                list.add(Tuple.of(o.toString(), o2.getNode("l").getString("null"))));
        list.sort((o1, o2) -> o2.getFirst().compareTo(o1.getFirst()));
        List<Text> texts = new ArrayList<>();
        texts.add(Text.of("总次数:" + list.size()));
        list.forEach(tuple -> texts.add(Text
                .of(TEXT_FORMATTER.format(FORMATTER.parse(tuple.getFirst())) + tuple.getSecond())));
        return texts;
    }
}
