package io.github.dead_i.bungeeweb.api;

import com.google.gson.Gson;
import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import net.md_5.bungee.api.plugin.Plugin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class GetLogs extends APICommand {
    private Gson gson = new Gson();

    public GetLogs() {
        super("getlogs", "logs");
    }

    @Override
    public void execute(Plugin plugin, HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException {
        ArrayList<String> conditions = new ArrayList<>();
        ArrayList<Object> params = new ArrayList<>();

        String player = req.getParameter("uuid");
        if (player != null) {
            conditions.add("`uuid`=?");
            params.add(player);
        }

        String from = req.getParameter("time");
        if (from != null && BungeeWeb.isNumber(from)) {
            conditions.add("`time`>?");
            params.add(Integer.parseInt(from));
        }

        String filter = req.getParameter("filter");
        if (filter != null) {
            StringBuilder filters = new StringBuilder();
            for (String f : filter.split(",")) {
                if (BungeeWeb.isNumber(f)) {
                    filters.append("`type`=? OR ");
                    params.add(f);
                }
            }
            if (filters.length() > 0) conditions.add("(" + filters.substring(0, filters.length() - 4) + ")");
        }

        String query = req.getParameter("query");
        if (query != null) {
            conditions.add("`content` LIKE ?");
            params.add("%" + query + "%");
        }

        String qry = "SELECT * FROM `" + BungeeWeb.getConfig().getString("database.prefix") + "log` ";

        if (!conditions.isEmpty()) {
            StringBuilder cond = new StringBuilder("WHERE ");
            for (String s : conditions) {
                cond.append(s + " AND ");
            }
            qry += cond.substring(0, cond.length() - 4);
        }

        qry += "ORDER BY `id` DESC ";

        String offset = req.getParameter("offset");
        if (offset == null || !BungeeWeb.isNumber(offset)) {
            offset = "0";
        }

        String limit = req.getParameter("limit");
        if (limit == null || !BungeeWeb.isNumber(limit) || Integer.parseInt(limit) > 100) {
            limit = "100";
        }

        qry += "LIMIT " + offset + ", " + limit;

        ArrayList<Object> out = new ArrayList<>();
        try (Connection conn = BungeeWeb.getDatabase()) {
        	try (PreparedStatement st = conn.prepareStatement(qry)) {
                int i = 0;
                for (Object o : params) {
                    i++;
                    st.setObject(i, o);
                }
                ResultSet rs = st.executeQuery();
                List<String> blackout = BungeeWeb.getConfig().getStringList("blackoutwebcommands").parallelStream().map(String::toLowerCase).map(String::trim).collect(Collectors.toList());
                while (rs.next()) {
                    HashMap<String, Object> dbRecord = new HashMap<>();
                    String content = rs.getString("content");
                    final int type = rs.getInt("type");
                    if (type == 2) {
                    	String[] contentArgs = content.split(" ", -1);
                    	if (blackout.contains(contentArgs[0].substring(1).toLowerCase())) {
                    		StringBuilder sb = new StringBuilder(contentArgs[0]);
                    		for (int j = 1; j < contentArgs.length; j++) {
                    			sb.append(" ******");
                    		}
                    		content = sb.toString();
                    	}
                    }
                    dbRecord.put("time", rs.getLong("time"));
                    dbRecord.put("type", type);
                    dbRecord.put("uuid", rs.getString("uuid"));
                    dbRecord.put("username", rs.getString("username"));
                    dbRecord.put("server", rs.getString("server"));
                    dbRecord.put("content", content);
                    out.add(dbRecord);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        res.getWriter().print(gson.toJson(out));
    }
}
