package me.szumielxd.proxyserverlist.bungee;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import me.szumielxd.proxyserverlist.bungee.commands.BungeeCommandWrapper;
import me.szumielxd.proxyserverlist.bungee.listeners.BungeeChannelListener;
import me.szumielxd.proxyserverlist.bungee.listeners.BungeeMotdListener;
import me.szumielxd.proxyserverlist.bungee.objects.BungeeComponentMapper;
import me.szumielxd.proxyserverlist.bungee.objects.BungeeScheduler;
import me.szumielxd.proxyserverlist.bungee.objects.BungeeSenderWrapper;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.commands.MainCommand;
import me.szumielxd.proxyserverlist.common.commands.ServersCommand;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.ServersConfig;
import me.szumielxd.proxyserverlist.common.gui.ServersGUI;
import me.szumielxd.proxyserverlist.common.managers.ServerPingManager;
import me.szumielxd.proxyserverlist.common.managers.StatsManager;
import me.szumielxd.proxyserverlist.common.managers.WebManager;
import me.szumielxd.proxyyweb.common.ProxyServerListProvider;
import me.szumielxd.proxyyweb.common.commands.CommonCommand;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class ProxyServerListBungee extends Plugin implements ProxyServerList<CommandSender, ProxiedPlayer, Component> {
	
	
	@Getter private final @NotNull BungeeScheduler scheduler = new BungeeScheduler(this);
	@Getter private final @NotNull BungeeSenderWrapper senderWrapper = new BungeeSenderWrapper(this);
	@Getter private final @NotNull StatsManager<CommandSender, ProxiedPlayer> statsManager = new StatsManager<>(this, this.getDataFolder().toPath().resolve("server-stats.csv"));
	@Getter private final @NotNull WebManager webManager = new WebManager(this);
	@Getter private final @NotNull BungeeComponentMapper componentMapper = new BungeeComponentMapper();
	private @Nullable ServerPingManager<CommandSender, ProxiedPlayer, Component> pingManager;
	private @Nullable ServersConfig serversConfig;
	private @Nullable ServersGUI<CommandSender, ProxiedPlayer, Component> serversGUI;
	
	
	@Override
	public void onEnable() {
		ProxyServerListProvider.init(this);
		this.registerCommand(new MainCommand<>(this));
		this.registerCommand(new ServersCommand<>(this));
		Config.load(new File(this.getDataFolder(), "config.yml"), this);
		this.getLogger().info("Loading server icons...");
		this.serversConfig = new ServersConfig(new File(this.getDataFolder(), "servers.yml")).load(this);
		this.getLogger().info(() -> "Successfully loaded %d server icons!".formatted(this.serversConfig.getServerIcons().size()));
		this.pingManager = new ServerPingManager<>(this).start();
		this.statsManager.start();
		this.serversGUI = new ServersGUI<>(this).start();
		this.webManager.start();
		this.getProxy().registerChannel(SERVERLIST_CHANNEL);
		this.getProxy().getPluginManager().registerListener(this, new BungeeMotdListener(this));
		this.getProxy().getPluginManager().registerListener(this, new BungeeChannelListener(this));
	}
	
	
	private void registerCommand(@NotNull CommonCommand<CommandSender> command) {
		this.getProxy().getPluginManager().registerCommand(this, new BungeeCommandWrapper(this, command));
	}
	
	
	@Override
	public void onDisable() {
		this.getLogger().info("Disabling all modules...");
		this.getProxy().getScheduler().cancel(this);
		this.pingManager.stop();
		this.statsManager.stop();
		this.serversGUI.stop();
		this.webManager.stop();
		this.getProxy().getPluginManager().unregisterListeners(this);
		this.getProxy().getPluginManager().unregisterCommands(this);
		this.getLogger().info("Well done. Time to sleep!");
	}


	@Override
	public @NotNull ServerPingManager<CommandSender, ProxiedPlayer, Component> getServerPingManager() {
		if (this.pingManager == null) throw new IllegalStateException("Plugin is not initialized");
		return this.pingManager;
	}


	@Override
	public @NotNull ServersConfig getServersConfig() {
		if (this.serversConfig == null) throw new IllegalStateException("Plugin is not initialized");
		return this.serversConfig;
	}


	@Override
	public @NotNull ServersGUI<CommandSender, ProxiedPlayer, Component> getServersGUI() {
		if (this.serversGUI == null) throw new IllegalStateException("Plugin is not initialized");
		return this.serversGUI;
	}


	@Override
	public @NotNull String getName() {
		return this.getDescription().getName();
	}


	@Override
	public @NotNull String getVersion() {
		return this.getDescription().getVersion();
	}
	

}
