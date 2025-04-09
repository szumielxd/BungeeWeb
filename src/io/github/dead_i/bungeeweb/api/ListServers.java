package io.github.dead_i.bungeeweb.api;

import java.io.IOException;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ListServers extends APICommand {

	public ListServers(@NotNull BungeeWeb plugin) {
		super(plugin, "listservers", "dashboard");
	}

	@Override
	public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException {
		res.getWriter().print(GSON_PARSER.toJson(
				this.plugin.getProxy().getAllServers().stream()
						.collect(
								Collectors.toMap(s -> s.getServerInfo().getName(),
								s -> s.getPlayersConnected().size()))));
	}
}
