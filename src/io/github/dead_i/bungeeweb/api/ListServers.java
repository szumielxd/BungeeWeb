package io.github.dead_i.bungeeweb.api;

import java.io.IOException;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.md_5.bungee.api.config.ServerInfo;

public class ListServers extends APICommand {
    
	private Gson gson = new Gson();

    public ListServers(@NotNull BungeeWeb plugin) {
        super(plugin, "listservers", "dashboard");
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException {
        HashMap<String, Integer> out = new HashMap<>();
        for (ServerInfo info : plugin.getProxy().getServers().values()) out.put(info.getName(), info.getPlayers().size());
        res.getWriter().print(gson.toJson(out));
    }
}
