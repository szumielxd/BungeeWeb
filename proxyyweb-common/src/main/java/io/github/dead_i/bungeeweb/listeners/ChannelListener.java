package io.github.dead_i.bungeeweb.listeners;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@RequiredArgsConstructor
public class ChannelListener implements Listener {

	@NonNull private final @NotNull BungeeWeb plugin;
	
	@EventHandler
	public void onPluginMessage(PluginMessageEvent event) {
		if (event.getSender() instanceof ProxiedPlayer player
				&& (event.getTag().equals("MC|Brand") || event.getTag().equals("minecraft:brand"))) {
				this.plugin.getPlayerInfoManager().getActiveSession(player.getUniqueId())
						.ifPresent(session -> {
							ByteBuf in = Unpooled.copiedBuffer(event.getData());
							session.setClient(DefinedPacket.readString(in));
						});
		}
	}

}
