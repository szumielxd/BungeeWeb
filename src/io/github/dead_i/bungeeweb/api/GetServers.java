package io.github.dead_i.bungeeweb.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class GetServers extends APICommand {
    
    public GetServers(@NotNull BungeeWeb plugin) {
        super(plugin, "getservers", "players");
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException {
        Map<String, Map<String, String>> out = new HashMap<>();
        for (ServerInfo info : plugin.getProxy().getServers().values()) {
            Map<String, String> players = new HashMap<>();
            int i = 0;
            for (ProxiedPlayer p : info.getPlayers()) {
                players.put(BungeeWeb.getUUID(p), p.getName());
                i++;
                if (i > 50) break;
            }
            out.put(info.getName(), players);
        }
        res.getWriter().print(GSON_PARSER.toJson(out));
    }
}
