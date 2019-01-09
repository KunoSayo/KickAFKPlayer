package com.github.euonmyoji.kickafkplayer;

import com.flowpowered.math.vector.Vector3d;
import com.github.euonmyoji.kickafkplayer.configuration.PluginConfig;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tuple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import static com.github.euonmyoji.kickafkplayer.configuration.PluginConfig.*;

/**
 * @author yinyangshi
 */
@Plugin(id = "kickafkplayer", name = "Kick A F K Player", version = "0.1.0", authors = "yinyangshi", description = "Kick the afk player")
public class KickAfkPlayer {
    private static HashMap<UUID, Tuple<LocalDateTime, Vector3d>> kickCache = new HashMap<>(18);
    private static HashMap<UUID, Tuple<LocalDateTime, Vector3d>> vCache = new HashMap<>(18);
    private static HashMap<UUID, Integer> codeCache = new HashMap<>();
    public static Logger logger;
    public static KickAfkPlayer plugin;
    @Inject
    @ConfigDir(sharedRoot = true)
    public Path cfgDir;

    @Inject
    public void setLogger(Logger l) {
        logger = l;
    }

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        plugin = this;
        try {
            Files.createDirectories(cfgDir);
            PluginConfig.init();
        } catch (IOException e) {
            logger.warn("init plugin IOE!", e);
        }
    }

    private volatile boolean running = true;

    @Listener
    public void onStarted(GameStartedServerEvent event) {
        Sponge.getCommandManager().register(this, CommandSpec.builder().permission("kickafkplayer.command.afkreload")
                .executor((src, args) -> {
                    PluginConfig.reload();
                    src.sendMessage(Text.of("reload kickafkplayer config successful"));
                    return CommandResult.success();
                }).build(), "afkreload");
        Sponge.getCommandManager().register(this, CommandSpec.builder().permission("kickafkplayer.command.afkverify")
                .arguments(GenericArguments.onlyOne(GenericArguments.integer(Text.of("v"))))
                .executor((src, args) -> {
                    if (src instanceof Player) {
                        Player p = ((Player) src);
                        if (codeCache.containsKey(p.getUniqueId())) {
                            Integer i = args.<Integer>getOne(Text.of("v")).orElseThrow(IllegalArgumentException::new);
                            if (codeCache.get(p.getUniqueId()).equals(i)) {
                                kickCache.remove(p.getUniqueId());
                                src.sendMessage(Text.of("检测成功!"));
                                vCache.remove(p.getUniqueId());
                                codeCache.remove(p.getUniqueId());
                            }
                        }
                    }
                    return CommandResult.success();
                }).build(), "afkverify");

        Task.builder().async().execute(() -> {
            while (running) {
                LocalDateTime now = LocalDateTime.now();
                Sponge.getServer().getOnlinePlayers().stream().filter(p -> !p.hasPermission("kickafkplayer.bypass.admin")
                        && (!vCache.containsKey(p.getUniqueId())
                        || vCache.get(p.getUniqueId()).getFirst().plusSeconds(checkSeconds).isBefore(now))).forEach(p -> {
                    Vector3d v = p.getLocation().getPosition();
                    if (vCache.containsKey(p.getUniqueId())) {
                        Vector3d last = vCache.get(p.getUniqueId()).getSecond();
                        if (last.distanceSquared(v) < radiusSquared) {
                            kickCache.put(p.getUniqueId(), Tuple.of(LocalDateTime.now(), v));
                            int i = Math.abs(p.getRandom().nextInt(100000));
                            codeCache.put(p.getUniqueId(), i);
                            p.sendMessage(Text.of(String.format(format, i)));
                        } else {
                            kickCache.remove(p.getUniqueId());
                            codeCache.remove(p.getUniqueId());
                        }
                    }
                    vCache.put(p.getUniqueId(), Tuple.of(LocalDateTime.now(), v));
                });
                try {
                    Thread.sleep(250 * checkSeconds);
                } catch (InterruptedException e) {
                    logger.warn("有人唤醒线程", e);
                }
            }
        }).submit(this);

        Task.builder().async().execute(() -> {
            while (running) {
                Task.builder().execute(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    new HashMap<>(kickCache).forEach((uuid, tuple) -> {
                        if (tuple.getFirst().plusSeconds(kickSeconds).isBefore(now)) {
                            Sponge.getServer().getPlayer(uuid)
                                    .filter(p -> !p.hasPermission("kickafkplayer.bypass.admin")).ifPresent(p -> {
                                if (p.getLocation().getPosition().distanceSquared(tuple.getSecond()) < radiusSquared) {
                                    p.kick(Text.of("你因为长时间挂机被请出游戏"));
                                }
                            });

                            kickCache.remove(uuid);
                        }
                    });
                }).submit(this);
                try {
                    Thread.sleep(250 * kickSeconds);
                } catch (InterruptedException e) {
                    logger.warn("有人唤醒线程", e);
                }
            }
        }).submit(this);
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event) {
        UUID uuid = event.getTargetEntity().getUniqueId();
        kickCache.remove(uuid);
        vCache.remove(uuid);
        codeCache.remove(uuid);
    }

    @Listener
    public void onStop(GameStoppingServerEvent event) {
        running = false;
    }
}
