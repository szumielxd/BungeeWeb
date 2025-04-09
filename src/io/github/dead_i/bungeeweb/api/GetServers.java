package io.github.dead_i.bungeeweb.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.proxy.Player;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetServers extends APICommand {
	
	public GetServers(@NotNull BungeeWeb plugin) {
		super(plugin, "getservers", "players");
	}

	@Override
	public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException {
		res.getWriter().print(GSON_PARSER.toJson(
				this.plugin.getProxy().getAllServers().stream()
						.collect(Collectors.toMap(
								s -> s.getServerInfo().getName(),
								s -> playersByUniqueId(s.getPlayersConnected())))));
	}
	
	private @NotNull Map<UUID, String> playersByUniqueId(@NotNull Collection<Player> players) {
		return players.stream().collect(Collectors.toMap(Player::getUniqueId, Player::getUsername));
	}
}
