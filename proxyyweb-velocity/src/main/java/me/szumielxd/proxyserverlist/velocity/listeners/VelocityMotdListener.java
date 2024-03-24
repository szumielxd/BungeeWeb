package me.szumielxd.proxyserverlist.velocity.listeners;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;

import me.szumielxd.legacyminiadventure.VersionableObject;
import me.szumielxd.legacyminiadventure.VersionableObject.ChatVersion;
import me.szumielxd.proxyserverlist.common.listeners.MotdListener;
import me.szumielxd.proxyserverlist.common.objects.PingResult;
import me.szumielxd.proxyserverlist.common.objects.PingResult.PlayersList;
import me.szumielxd.proxyserverlist.common.objects.PingResult.Version;
import me.szumielxd.proxyserverlist.velocity.ProxyServerListVelocity;
import net.kyori.adventure.text.Component;

public class VelocityMotdListener extends MotdListener<Component> {

	public VelocityMotdListener(@NotNull ProxyServerListVelocity plugin) {
		super(plugin);
	}
	
	
	@Subscribe
	public void onPing(ProxyPingEvent event) {
		Optional<Integer> protocolId = Optional.of(event.getConnection().getProtocolVersion().getProtocol())
				.filter(i -> i > -1);
		ServerPing ping = event.getPing();
		PingResult<Component> wrapper = new PingResult<>(
				new Version(ping.getVersion().getProtocol(), ping.getVersion().getName()),
				ping.getPlayers().map(p -> new PlayersList(
						p.getMax(),
						p.getOnline(),
						p.getSample().stream()
								.map(s -> new PlayersList.SamplePlayer(s.getId(), s.getName()))
								.toList())),
				VersionableObject.same(ping.getDescriptionComponent()),
				-1);
		PingResult<Component> result = this.applyMotd(wrapper);
		
		event.setPing(new ServerPing(new ServerPing.Version(result.getVersion().getProtocol(), result.getVersion().getName()),
				result.getPlayers()
						.map(p -> new ServerPing.Players(
								p.getOnline(),
								p.getMax(),
								p.getPlayers().stream()
										.map(s -> new ServerPing.SamplePlayer(s.getName(), s.getUniqueId()))
										.toList()))
						.orElse(null),
				result.getDescription().get(ChatVersion.getCorrect(protocolId)), ping.getFavicon().orElse(null), ping.getModinfo().orElse(null)));
		
	}
	

}
