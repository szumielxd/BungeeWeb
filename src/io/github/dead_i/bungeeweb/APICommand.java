package io.github.dead_i.bungeeweb;

import java.io.IOException;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class APICommand {
	
	protected static final Gson GSON_PARSER = new Gson();
    
	protected final @NotNull BungeeWeb plugin;
	@Getter private final String name;
    private final String permission;
    private final boolean login;

    protected APICommand(@NotNull BungeeWeb plugin, String name) {
        this(plugin, name, "");
    }

    protected APICommand(@NotNull BungeeWeb plugin, String name, String permission) {
    	this(plugin, name, permission, false);
    }

    protected APICommand(@NotNull BungeeWeb plugin, String name, boolean login) {
    	this(plugin, name, "", login);
    }

    public boolean hasPermission(HttpServletRequest req) {
        return login || hasPermission(req, permission);
    }

    protected boolean hasPermission(HttpServletRequest req, String i) {
        Integer group = (Integer) req.getSession().getAttribute("group");
        if (group == null) {
            group = 0;
        }

        return group > 0 && (i == null || i.isEmpty() || this.plugin.getGroupPermissions(group).contains(i));
    }

    public abstract void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException;
}
