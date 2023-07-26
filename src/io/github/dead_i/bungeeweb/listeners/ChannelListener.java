package io.github.dead_i.bungeeweb.listeners;

import java.io.ByteArrayInputStream;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.BungeeWeb;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@RequiredArgsConstructor
public class ChannelListener implements Listener {

	@NonNull private final @NotNull BungeeWeb plugin;
	
	@EventHandler
	public void onPluginMessage(PluginMessageEvent event) {
		if (event.getSender() instanceof ProxiedPlayer player
				&& (event.getTag().equals("MC|Brand") || event.getTag().equals("minecraft:brand"))) {
				ByteArrayInputStream in = new ByteArrayInputStream(event.getData());
				int size = readVarInt(in);
				byte[] bytes = new byte[size * 4];
				in.readNBytes(bytes, 0, bytes.length);
				this.plugin.getPlayerInfoManager().getActiveSession(player.getUniqueId())
						.ifPresent(session -> session.setClient(new String(bytes)));
		}
	}
	
	private static final int SEGMENT_BITS = 0x7F;
	private static final int CONTINUE_BIT = 0x80;
	
	private int readVarInt(ByteArrayInputStream in) {
		int value = 0;
		int position = 0;
		byte currentByte;

		while (true) {
			currentByte = (byte) in.read();
			value |= (currentByte & SEGMENT_BITS) << position;

			if ((currentByte & CONTINUE_BIT) == 0) break;

			position += 7;

			if (position >= 32) throw new RuntimeException("VarInt is too big");
		}

		return value;
	}

}
