package io.github.dead_i.bungeeweb.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import io.github.dead_i.bungeeweb.hikari.HikariDB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetTypes extends APICommand {
    
	public GetTypes(@NotNull BungeeWeb plugin) {
        super(plugin, "gettypes", true);
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        res.getWriter().print(GSON_PARSER.toJson(Stream.of(HikariDB.LogType.values())
    			.map(Enum::name)
    			.map(String::toLowerCase)
    			.filter(t -> this.plugin.getConfig().getBoolean("log." + t))
    			.toArray()));
    }
}
