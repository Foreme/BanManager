package me.confuser.banmanager.commands.external;

import me.confuser.banmanager.BanManager;
import me.confuser.banmanager.data.PlayerData;
import me.confuser.banmanager.data.external.ExternalPlayerMuteData;
import me.confuser.banmanager.util.CommandParser;
import me.confuser.banmanager.util.CommandUtils;
import me.confuser.banmanager.util.UUIDUtils;
import me.confuser.bukkitutil.Message;
import me.confuser.bukkitutil.commands.BukkitCommand;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class MuteAllCommand extends BukkitCommand<BanManager> {

  public MuteAllCommand() {
    super("muteall");
  }

  @Override
  public boolean onCommand(final CommandSender sender, Command command, String commandName, String[] args) {
    CommandParser parser = new CommandParser(args);
    args = parser.getArgs();

    final boolean isSoft = parser.isSoft();

    if (isSoft && !sender.hasPermission(command.getPermission() + ".soft")) {
      sender.sendMessage(Message.getString("sender.error.noPermission"));
      return true;
    }

    if (args.length < 2) {
      return false;
    }

    if (args[0].equalsIgnoreCase(sender.getName())) {
      sender.sendMessage(Message.getString("sender.error.noSelf"));
      return true;
    }

    // Check if UUID vs name
    final String playerName = args[0];
    final boolean isUUID = playerName.length() > 16;

    final String reason = StringUtils.join(args, " ", 1, args.length);

    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

      @Override
      public void run() {
        final PlayerData player = CommandUtils.getPlayer(sender, playerName);

        if (player == null) {
          sender.sendMessage(Message.get("sender.error.notFound").set("player", playerName).toString());
          return;
        }

        final PlayerData actor = CommandUtils.getActor(sender);

        if (actor == null) return;

        final ExternalPlayerMuteData ban = new ExternalPlayerMuteData(player, actor, reason, isSoft);
        int created;

        try {
          created = plugin.getExternalPlayerMuteStorage().create(ban);
        } catch (SQLException e) {
          sender.sendMessage(Message.get("sender.error.exception").toString());
          e.printStackTrace();
          return;
        }

        if (created != 1) {
          return;
        }

        Message.get("muteall.notify")
               .set("actor", ban.getActorName())
               .set("reason", ban.getReason())
               .set("player", player.getName())
               .set("playerId", player.getUUID().toString())
               .sendTo(sender);
      }

    });

    return true;
  }

}
