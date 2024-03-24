package me.szumielxd.proxyyweb.common.commands;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import me.szumielxd.proxyyweb.common.ProxyyWeb;

public class ReloadConfig<T, C> extends CommonCommand<T> {
    
	private @NotNull ProxyyWeb<T, ?, C> plugin;

    public ReloadConfig(@NotNull ProxyyWeb<T, ?, C> plugin) {
        super("bwreload", "bungeeweb.reload");
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull T sender, String[] strings) {
        this.plugin.reloadConfig();
        sender.sendMessage(new ComponentBuilder("The BungeeWeb configuration has been reloaded. Please note that certain changes may require a proxy restart to take effect.").color(ChatColor.RED).create());
    }
}
