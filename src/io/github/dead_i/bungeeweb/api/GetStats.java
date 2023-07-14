package io.github.dead_i.bungeeweb.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import io.github.dead_i.bungeeweb.hikari.HikariDB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetStats extends APICommand {
    
	private final String[] types = { "playercount", "maxplayers", "activity" };

    public GetStats(@NotNull BungeeWeb plugin) {
        super(plugin, "getstats", "stats");
    }


    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        String since = req.getParameter("since");
        long current = System.currentTimeMillis() / 1000;
        long month = current - 2628000;
        long time = month;

        if (since != null && BungeeWeb.isNumber(since)) time = Integer.parseInt(since);

        if ((current - time) > month) {
            res.getWriter().print("{ \"error\": \"Attempted to fetch too many records. The number of records you request is capped at 1 month for security reasons.\" }");
            return;
        }

        try (Connection conn = this.plugin.getDatabaseManager().connect()) {
        	try (PreparedStatement stm = conn.prepareStatement("SELECT * FROM `%1$s` WHERE `time` > ?".formatted(HikariDB.TABLE_STATS))) {
        		stm.setLong(1, time);
        		ResultSet rs = stm.executeQuery();
                Map<String, List<Object>> records = Stream.of(types)
                		.collect(Collectors.toMap(Function.identity(), e -> new ArrayList<>()));
                while (rs.next()) {
                	for (String t : types) {
                		records.get(t).add(Arrays.asList(rs.getLong("time")*1000, rs.getInt(t)));
                	}
                }
                HashMap<String, Object> out = new HashMap<>();
                out.put("increment", this.plugin.getConfig().getInt("server.statscheck"));
                out.put("data", records);
                res.getWriter().print(GSON_PARSER.toJson(out));
        	}
        }
    }
}
