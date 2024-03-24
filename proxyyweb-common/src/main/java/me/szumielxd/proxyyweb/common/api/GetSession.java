package me.szumielxd.proxyyweb.common.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.APICommand;
import io.github.dead_i.bungeeweb.BungeeWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GetSession extends APICommand {
    
	public GetSession(@NotNull BungeeWeb plugin) {
        super(plugin, "getsession", true);
    }

    @Override
    public void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException {
        HashMap<String, Object> out = new HashMap<>();

        Integer group = (Integer) req.getSession().getAttribute("group");
        if (group == null) {
            out.put("group", 0);
        } else {
            out.put("id", req.getSession().getAttribute("id"));
            out.put("user", req.getSession().getAttribute("user"));
            out.put("group", group);
            out.put("updatetime", this.plugin.getConfig().getInt("server.updatetime", 10));
            out.put("permissions", this.plugin.getGroupPermissions(group));
        }
        out.put("autosearch", this.plugin.getConfig().getBoolean("server.autosearch"));
        out.put("transitions", !this.plugin.getConfig().getBoolean("server.disabletransitions"));

        this.log(req, String::new);
        res.getWriter().print(GSON_PARSER.toJson(out));
    }
}
