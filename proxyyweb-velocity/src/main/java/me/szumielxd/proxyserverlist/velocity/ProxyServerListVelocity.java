package me.szumielxd.proxyserverlist.velocity;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import lombok.Getter;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.commands.MainCommand;
import me.szumielxd.proxyserverlist.common.commands.ServersCommand;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.ServersConfig;
import me.szumielxd.proxyserverlist.common.gui.ServersGUI;
import me.szumielxd.proxyserverlist.common.managers.ServerPingManager;
import me.szumielxd.proxyserverlist.common.managers.StatsManager;
import me.szumielxd.proxyserverlist.common.managers.WebManager;
import me.szumielxd.proxyserverlist.velocity.commands.VelocityCommandWrapper;
import me.szumielxd.proxyserverlist.velocity.listeners.VelocityChannelListener;
import me.szumielxd.proxyserverlist.velocity.listeners.VelocityMotdListener;
import me.szumielxd.proxyserverlist.velocity.objects.VelocityComponentMapper;
import me.szumielxd.proxyserverlist.velocity.objects.VelocityScheduler;
import me.szumielxd.proxyserverlist.velocity.objects.VelocitySenderWrapper;
import me.szumielxd.proxyyweb.common.ProxyServerListProvider;
import me.szumielxd.proxyyweb.common.commands.CommonCommand;
import net.kyori.adventure.text.Component;

@Plugin(
		id = "id----",
		name = "@pluginName@",
		version = "@version@",
		authors = { "@author@" },
		description = "@description@",
		url = "https://github.com/szumielxd/ProxyServerList/",
		dependencies = { 
				@Dependency( id="protocolize", optional=false )
		}
)
public class ProxyServerListVelocity implements ProxyServerList<CommandSource, Player, Component> {
	
	
	@Getter private final @NotNull ProxyServer proxy;
	@Getter private final @NotNull Logger logger;
	private final File dataFolder;
	
	
	@Getter private final @NotNull VelocityScheduler scheduler = new VelocityScheduler(this);
	@Getter private final @NotNull VelocitySenderWrapper senderWrapper = new VelocitySenderWrapper(this);
	@Getter private final @NotNull WebManager webManager = new WebManager(this);
	@Getter private final @NotNull VelocityComponentMapper componentMapper = new VelocityComponentMapper();
	private @Nullable ServerPingManager<CommandSource, Player, Component> pingManager;
	private @Nullable StatsManager<CommandSource, Player> statsManager;
	private @Nullable ServersConfig serversConfig;
	private @Nullable ServersGUI<CommandSource, Player, Component> serversGUI;
	
	
	@Inject
	public ProxyServerListVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
		this.proxy = server;
		this.logger = logger;
		this.dataFolder = dataDirectory.toFile();
	}
	
	
	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent event) {
	    this.onEnable();
	}
	
	
	@Override
	public void onEnable() {
		ProxyServerListProvider.init(this);
		this.registerCommand(new MainCommand<>(this));
		this.registerCommand(new ServersCommand<>(this));
		Config.load(new File(this.dataFolder, "config.yml"), this);
		this.getLogger().info("Loading server icons...");
		this.serversConfig = new ServersConfig(new File(this.dataFolder, "servers.yml")).load(this);
		this.getLogger().info(() -> "Successfully loaded %d server icons!".formatted(this.serversConfig.getServerIcons().size()));
		this.pingManager = new ServerPingManager<>(this).start();
		this.statsManager = new StatsManager<>(this, this.dataFolder.toPath().resolve("server-stats.csv")).start();
		this.serversGUI = new ServersGUI<>(this).start();
		this.webManager.start();
		this.getProxy().getChannelRegistrar().register(MinecraftChannelIdentifier.from(SERVERLIST_CHANNEL));
		this.getProxy().getEventManager().register(this, new VelocityMotdListener(this));
		this.getProxy().getEventManager().register(this, new VelocityChannelListener(this));
	}
	
	
	private void registerCommand(@NotNull CommonCommand<CommandSource> command) {
		CommandManager mgr = this.getProxy().getCommandManager();
		CommandMeta meta = mgr.metaBuilder(command.getName()).aliases(command.getAliases()).build();
		mgr.register(meta, new VelocityCommandWrapper(this, command));
	}
	
	
	@Override
	public void onDisable() {
		this.getLogger().info("Disabling all modules...");
		this.pingManager.stop();
		this.statsManager.stop();
		this.serversGUI.stop();
		this.webManager.stop();
		this.getProxy().getEventManager().unregisterListeners(this);
		this.getScheduler().cancelAll();
		this.getLogger().info("Well done. Time to sleep!");
	}


	@Override
	public @NotNull ServerPingManager<CommandSource, Player, Component> getServerPingManager() {
		if (this.pingManager == null) throw new IllegalStateException("Plugin is not initialized");
		return this.pingManager;
	}


	@Override
	public @NotNull StatsManager<CommandSource, Player> getStatsManager() {
		if (this.statsManager == null) throw new IllegalStateException("Plugin is not initialized");
		return this.statsManager;
	}


	@Override
	public @NotNull ServersConfig getServersConfig() {
		if (this.serversConfig == null) throw new IllegalStateException("Plugin is not initialized");
		return this.serversConfig;
	}


	@Override
	public @NotNull ServersGUI<CommandSource, Player, Component> getServersGUI() {
		if (this.serversGUI == null) throw new IllegalStateException("Plugin is not initialized");
		return this.serversGUI;
	}


	@Override
	public @NotNull String getName() {
		return this.getProxy().getPluginManager().ensurePluginContainer(this).getDescription().getName().orElse("");
	}


	@Override
	public @NotNull String getVersion() {
		return this.getProxy().getPluginManager().ensurePluginContainer(this).getDescription().getVersion().orElse("");
	}
	

}
