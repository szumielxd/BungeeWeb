package me.szumielxd.proxyserverlist.common.managers;

import java.util.Objects;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.web.WebHandler;

public class WebManager {
	
	
	private final @NotNull ProxyServerList<?, ?, ?> plugin;
	private @Nullable Server server;
	
	
	public WebManager(@NotNull ProxyServerList<?, ?, ?> plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
	}
	
	
	public WebManager start() {
		
		int port = Config.WEB_PORT.getInt();
		
		if (port <= 0) return this;
		
		QueuedThreadPool pool = new QueuedThreadPool();
		pool.setName("proxyserverlist-web");

		// Setup the server
		this.server = new Server(pool);
		// Setup the context
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		ServerConnector connector = new ServerConnector(this.server, new HttpConnectionFactory(httpConfig));
		ContextHandler context = new ContextHandler("/");
		SessionHandler sessions = new SessionHandler();
		connector.setPort(port);
		sessions.setHandler(new WebHandler(plugin));
		context.setHandler(sessions);
		this.server.addConnector(connector);
		this.server.setHandler(sessions);
		this.server.setStopAtShutdown(true);

		// Start listening
		try {
			this.server.start();
		} catch(Exception e) {
			this.plugin.getLogger().warning("Unable to bind web server to port.");
			e.printStackTrace();
		}
		
		return this;
		
	}
	
	
	public void stop() {
		if (this.server != null) {
			try {
				this.server.stop();
				this.server.destroy();
				this.server = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
