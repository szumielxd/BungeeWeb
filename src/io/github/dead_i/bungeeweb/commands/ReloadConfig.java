package io.github.dead_i.bungeeweb.commands;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class ReloadConfig extends Command {
    
	private @NotNull BungeeWeb plugin;

    public ReloadConfig(@NotNull BungeeWeb plugin) {
        super("bwreload", "bungeeweb.reload");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] strings) {
        this.plugin.reloadConfig();
        sender.sendMessage(new ComponentBuilder("The BungeeWeb configuration has been reloaded. Please note that certain changes may require a proxy restart to take effect.").color(ChatColor.RED).create());
    }
}
