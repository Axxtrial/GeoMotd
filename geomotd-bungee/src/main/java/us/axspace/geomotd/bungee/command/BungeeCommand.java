package us.axspace.geomotd.bungee.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.chat.ComponentSerializer;
import us.axspace.geomotd.core.GeoMOTDCore;
import us.axspace.geomotd.core.config.MessagesManager;

import java.util.*;

/**
 * Command handler for BungeeCord and Waterfall proxies.
 * Developed for axspace.us/geomotd
 */
public class BungeeCommand extends Command implements TabExecutor {

    private final GeoMOTDCore core;

    public BungeeCommand(GeoMOTDCore core) {
        super("geomotd", "geomotd.admin");
        this.core = core;
    }

    private void send(CommandSender sender, Component comp) {
        try {
            String json = GsonComponentSerializer.gson().serialize(comp);
            BaseComponent[] parsed = ComponentSerializer.parse(json);
            sender.sendMessage(new TextComponent(parsed));
        } catch (Exception e) {
            sender.sendMessage(new TextComponent(comp.toString()));
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        MessagesManager msg = core.getMessages();

        if (!hasPermission(sender)) {
            send(sender, msg.getPrefixed("general.no-permission"));
            return;
        }

        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            sendHelp(sender, msg);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                core.reload();
                send(sender, msg.getPrefixed("commands.reload.success"));
            }
            case "status" -> {
                sendStatus(sender, msg);
            }
            default -> {
                send(sender, msg.getPrefixed("general.unknown-command"));
            }
        }
    }

    private void sendHelp(CommandSender sender, MessagesManager msg) {
        send(sender, msg.getComponent("commands.help.header"));
        send(sender, msg.getComponent("commands.help.reload"));
        send(sender, msg.getComponent("commands.help.status"));
    }

    private void sendStatus(CommandSender sender, MessagesManager msg) {
        send(sender, msg.getComponent("commands.status.header"));

        send(sender, msg.getComponent("commands.status.platform", Collections.singletonMap("platform", "BungeeCord / Waterfall")));

        boolean isLoaded = core.getGeoIPService().isLoaded();
        String statusStr = isLoaded ? msg.getRaw("commands.status.database-loaded-true") : msg.getRaw("commands.status.database-loaded-false");
        send(sender, msg.getComponent("commands.status.database-loaded", Collections.singletonMap("status", statusStr)));

        send(sender, msg.getComponent("commands.status.countries-count", Collections.singletonMap("count", String.valueOf(core.getCountryManager().getCountryCount()))));
        send(sender, msg.getComponent("commands.status.configured-languages", Collections.singletonMap("languages", core.getConfig().getConfiguredLanguages().toString())));
        send(sender, msg.getComponent("commands.status.default-language", Collections.singletonMap("language", core.getConfig().getDefaultLanguage())));
        send(sender, msg.getComponent("commands.status.cache-size", Collections.singletonMap("size", String.valueOf(core.getGeoIPService().getCacheSize()))));
        send(sender, msg.getComponent("commands.status.cache-hit-rate", Collections.singletonMap("rate", String.format(Locale.ROOT, "%.2f%%", core.getGeoIPService().getCacheHitRate() * 100))));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (!hasPermission(sender)) {
            return Collections.emptyList();
        }
        if (args.length <= 1) {
            String current = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> options = Arrays.asList("reload", "status", "help");
            List<String> result = new ArrayList<>();
            for (String opt : options) {
                if (opt.toLowerCase(Locale.ROOT).startsWith(current)) {
                    result.add(opt);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
