package me.szumielxd.proxyserverlist.common.listeners;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import me.szumielxd.proxyserverlist.common.ProxyServerList;

public abstract class ChannelListener<T, U extends T> {
	
	
	private final @NotNull ProxyServerList<T, U, ?> plugin;
	
	
	protected ChannelListener(@NotNull ProxyServerList<T, U, ?> plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
	}
	
	
	public boolean onChannelMessage(@NotNull String channel, @NotNull U player, byte[] message) {
		if (ProxyServerList.SERVERLIST_CHANNEL.equals(channel)) {
			ByteArrayDataInput in = ByteStreams.newDataInput(message);
			String subchannel = in.readUTF();
			if ("gui".equals(subchannel)) {
				String guiName = in.readUTF();
				if ("servers".equals(guiName)) {
					UUID uuid = this.plugin.getSenderWrapper().getUniqueId(player);
					this.plugin.getScheduler().runTask(() -> this.plugin.getServersGUI().open(uuid));
				}
			}
			return true;
		}
		return false;
	}
	

}
