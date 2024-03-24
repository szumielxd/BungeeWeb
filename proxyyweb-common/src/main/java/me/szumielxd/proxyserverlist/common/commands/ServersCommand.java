package me.szumielxd.proxyserverlist.common.commands;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.legacyminiadventure.LegacyMiniadventure;
import me.szumielxd.legacyminiadventure.VersionableObject;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerDisplay;
import me.szumielxd.proxyserverlist.common.managers.ServerPingManager;
import me.szumielxd.proxyserverlist.common.utils.MiscUtil;
import me.szumielxd.proxyyweb.common.commands.CommonCommand;
import me.szumielxd.proxyyweb.common.objects.SenderWrapper;

public class ServersCommand<T, U extends T, C> extends CommonCommand<T> {
	
	private final @NotNull ProxyServerList<T, U, C> plugin;
	private final @NotNull VersionableObject<C> invalidServerMessage;

	public ServersCommand(@NotNull ProxyServerList<T, U, C> plugin) {
		super(Config.GUI_COMMAND_NAME.getString(), "proxyserverlist.command.servers", Config.GUI_COMMAND_ALIASES.getStringList().toArray(new String[0]));
		this.plugin = plugin;
		this.invalidServerMessage = LegacyMiniadventure.get().deserialize(Config.MESSAGES_INVALID_SERVER.getString())
				.map(this.plugin.getComponentMapper()::kyoriToComponent);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(@NotNull T sender, String[] args) {
		SenderWrapper<T, U, C> swrapper = this.plugin.getSenderWrapper();
		if (swrapper.isPlayer(sender)) {
			if (args.length == 0) {
				UUID uuid = this.plugin.getSenderWrapper().getUniqueId((U) sender);
				this.plugin.getServersGUI().open(uuid);
				return;
			}
			if (args.length > 0) {
				ServerPingManager<T, U, C> mgr = this.plugin.getServerPingManager();
				SerializableServerDisplay srv = this.plugin.getServersConfig().getServerIcons().get(String.join(" ", args).toLowerCase());
				if (srv != null && !(srv.isHidingOffline() && srv.getNames().stream().noneMatch(mgr::isCachedOnline))) {
					List<String> names = srv.getNames().stream()
							.filter(mgr::isCachedOnline)
							.toList();
					this.plugin.getSenderWrapper().connectToServer((U) sender, MiscUtil.random(names.isEmpty() ? srv.getNames() : names));
				} else {
					this.plugin.getSenderWrapper().sendMessage(sender, this.invalidServerMessage);
				}
			}
		}
	}

	@Override
	public @NotNull List<String> onTabComplete(@NotNull T sender, @NotNull String[] args) {
		if (args.length > 0) {
			String arg = String.join(" ", args).toLowerCase();
			ServerPingManager<T, U, C> mgr = this.plugin.getServerPingManager();
			return this.plugin.getServersConfig().getServerIcons().entrySet().stream()
					.filter(e -> e.getKey().startsWith(arg)).map(Map.Entry::getValue)
					.filter(s -> !s.isHidingOffline() || s.getNames().stream().anyMatch(mgr::isCachedOnline))
					.map(SerializableServerDisplay::getFriendlyName).map(s -> this.skipSpaces(s, args.length-1))
					.toList();
		}
		return Collections.emptyList();
	}
	
	private String skipSpaces(String text, int spaces) {
		int skipped = 0;
		int index = 0;
		for (; index < text.length() && skipped < spaces; index++) {
			if (text.charAt(index) == ' ') skipped++;
		}
		return text.substring(index);
	}

}
