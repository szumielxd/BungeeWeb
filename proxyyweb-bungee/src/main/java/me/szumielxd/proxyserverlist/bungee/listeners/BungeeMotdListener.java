package me.szumielxd.proxyserverlist.bungee.listeners;

import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.legacyminiadventure.VersionableObject;
import me.szumielxd.legacyminiadventure.VersionableObject.ChatVersion;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.listeners.MotdListener;
import me.szumielxd.proxyserverlist.common.objects.PingResult;
import me.szumielxd.proxyserverlist.common.objects.PingResult.PlayersList;
import me.szumielxd.proxyserverlist.common.objects.PingResult.Version;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeMotdListener extends MotdListener<Component> implements Listener {

	public BungeeMotdListener(@NotNull ProxyServerList<?, ?, Component> plugin) {
		super(plugin);
	}
	
	
	@EventHandler
	public void onPing(ProxyPingEvent event) {
		Optional<Integer> protocolId = Optional.of(event.getConnection().getVersion())
				.filter(i -> i < -1);
		ServerPing ping = event.getResponse();
		PingResult<Component> wrapper = new PingResult<>(
				new Version(ping.getVersion().getProtocol(), ping.getVersion().getName()),
				Optional.ofNullable(ping.getPlayers()).map(p -> new PlayersList(
						p.getMax(),
						p.getOnline(),
						Optional.ofNullable(p.getSample())
								.stream()
								.flatMap(Stream::of)
								.map(s -> new PlayersList.SamplePlayer(s.getUniqueId(), s.getName()))
								.toList())),
				VersionableObject.same(BungeeComponentSerializer.get().deserialize(new BaseComponent[] { ping.getDescriptionComponent() })),
				-1);
		PingResult<Component> result = this.applyMotd(wrapper);
		
		event.setResponse(new ServerPing(new ServerPing.Protocol(result.getVersion().getName(), result.getVersion().getProtocol()),
				result.getPlayers()
						.map(p -> new ServerPing.Players(
								p.getMax(),
								p.getOnline(),
								p.getPlayers().stream()
										.map(s -> new ServerPing.PlayerInfo(s.getName(), s.getUniqueId()))
										.toArray(ServerPing.PlayerInfo[]::new)))
						.orElse(null),
				new TextComponent(BungeeComponentSerializer.get().serialize(result.getDescription().get(ChatVersion.getCorrect(protocolId)))), ping.getFaviconObject()));
	}
	

}
