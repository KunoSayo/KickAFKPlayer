package com.github.euonmyoji.kickafkplayer.command;

import com.github.euonmyoji.kickafkplayer.configuration.PlayerLog;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;

import static com.github.euonmyoji.kickafkplayer.KickAfkPlayer.*;
import static org.spongepowered.api.text.Text.of;

/**
 * @author yinyangshi
 */
public class KickAfkPlayerCommand {
    public static CommandSpec afkKickTop = CommandSpec.builder().permission("kickafkplayer.command.admin.afkkicktop")
            .executor((src, args) -> {
                src.sendMessage(of("开始查询被请出服务器玩家次数"));
                Task.builder().async().execute(() -> {
                    PaginationList.Builder builder = PaginationList.builder().padding(of("-"))
                            .title(of("被请出服务器玩家排行"))
                            .linesPerPage(10);
                    builder.contents(PlayerLog.getTop());
                    builder.build().sendTo(src);
                }).submit(plugin);
                return CommandResult.success();
            }).build();

    public static CommandSpec afkKickinfo = CommandSpec.builder().permission("kickafkplayer.command.admin.afkkickinfo")
            .arguments(GenericArguments.onlyOne(GenericArguments.userOrSource(of("user"))))
            .executor((src, args) -> {
                src.sendMessage(of("开始查询用户被t出情况"));
                User user = args.<User>getOne(Text.of("user")).orElseThrow(IllegalArgumentException::new);
                Task.builder().async().execute(() -> {
                    PaginationList.Builder builder = PaginationList.builder().padding(of("-"))
                            .title(of("玩家" + user.getName() + "被请出服务器情况"))
                            .linesPerPage(10);
                    builder.contents(PlayerLog.getInfo(user.getName()));
                    builder.build().sendTo(src);
                }).submit(plugin);
                return CommandResult.success();
            }).build();

    public static CommandSpec afkVerify = CommandSpec.builder().permission("kickafkplayer.command.afkverify")
            .arguments(GenericArguments.onlyOne(GenericArguments.integer(of("v"))))
            .executor((src, args) -> {
                if (src instanceof Player) {
                    Player p = ((Player) src);
                    if (codeCache.containsKey(p.getUniqueId())) {
                        Integer i = args.<Integer>getOne(of("v")).orElseThrow(IllegalArgumentException::new);
                        if (codeCache.get(p.getUniqueId()).equals(i)) {
                            kickCache.remove(p.getUniqueId());
                            src.sendMessage(of("检测成功!"));
                            vCache.remove(p.getUniqueId());
                            codeCache.remove(p.getUniqueId());
                        }
                    }
                }
                return CommandResult.success();
            }).build();
}
