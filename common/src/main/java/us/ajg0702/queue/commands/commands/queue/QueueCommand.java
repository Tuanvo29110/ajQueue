package us.ajg0702.queue.commands.commands.queue;

import com.google.common.collect.ImmutableList;
import us.ajg0702.queue.api.commands.ICommandSender;
import us.ajg0702.queue.api.commands.ISubCommand;
import us.ajg0702.queue.api.players.AdaptedPlayer;
import us.ajg0702.queue.commands.BaseCommand;
import us.ajg0702.queue.common.QueueMain;
import us.ajg0702.utils.common.Messages;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueCommand extends BaseCommand {

    public static Map<AdaptedPlayer, Long> cooldowns = new ConcurrentHashMap<>();

    private final QueueMain main;

    public QueueCommand(QueueMain main) {
        this.main = main;
    }

    @Override
    public String getName() {
        return "joinqueue";
    }

    @Override
    public ImmutableList<String> getAliases() {
        List<String> aliases = new ArrayList<>(Arrays.asList("move", "joinq"));
        if(main.getConfig().getBoolean("enable-server-command")) {
            aliases.add("server");
        }
        return ImmutableList.copyOf(aliases);
    }

    @Override
    public ImmutableList<ISubCommand> getSubCommands() {
        return ImmutableList.<ISubCommand>builder().build();
    }

    @Override
    public String getPermission() {
        return null;
    }

    @Override
    public Messages getMessages() {
        return main.getMessages();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        if(!checkPermission(sender)) return;
        if(!sender.isPlayer()) {
            sender.sendMessage(getMessages().getComponent("errors.player-only"));
            return;
        }
        AdaptedPlayer player = main.getPlatformMethods().senderToPlayer(sender);

        double cooldownTime = main.getConfig().getDouble("queue-command-cooldown");

        if(cooldownTime > 0) {
            long lastUse = cooldowns.getOrDefault(player, 0L);
            if(System.currentTimeMillis() - lastUse < cooldownTime * 1000L) {
                sender.sendMessage(main.getMessages().getComponent("errors.too-fast-queue"));
                return;
            }

            cooldowns.put(player, System.currentTimeMillis());
        }

        if(args.length > 0) {
            if(main.getConfig().getBoolean("require-permission") && !player.hasPermission("ajqueue.queue."+args[0])) {
                sender.sendMessage(
                        main.getConfig().getBoolean("act-like-no-permission-servers-dont-exist") ?
                                getMessages().getComponent("errors.server-not-exist", "SERVER:"+args[0]) :
                                getMessages().getComponent("noperm")
                );
                return;
            }
            if(main.getConfig().getBoolean("joinfrom-server-permission") && !player.hasPermission("ajqueue.joinfrom."+player.getServerName())) {
                player.sendMessage(getMessages().getComponent("errors.deny-joining-from-server"));
                return;
            }
            main.getQueueManager().addToQueue(player, args[0]);
        } else {
            sender.sendMessage(getMessages().getComponent("commands.joinqueue.usage"));
        }
    }

    @Override
    public List<String> autoComplete(ICommandSender sender, String[] args) {
        if(!main.getConfig().getBoolean("tab-complete-queues")) {
            return Collections.emptyList();
        }
        if(args.length == 1) {
            List<String> servers = filterCompletion(main.getQueueManager().getServerNames(), args[0]);
            if(main.getConfig().getBoolean("require-permission")) {
                servers.removeIf(s -> !sender.hasPermission("ajqueue.queue." + s));
            }
            return servers;
        }
        return Collections.emptyList();
    }
}
