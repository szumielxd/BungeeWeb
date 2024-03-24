package io.github.dead_i.bungeeweb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

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
    
    protected void log(HttpServletRequest req, String message) throws IOException {
    	this.log(req, () -> message);
    }
    
    protected void log(HttpServletRequest req, Supplier<String> message) throws IOException {
    	if (this.plugin.getConfig().getBoolean("server.log-requests")) {
    		String user = Optional.ofNullable(req.getSession().getAttribute("user"))
        			.map(Object::toString)
        			.flatMap(username -> Optional.ofNullable(req.getSession().getAttribute("user"))
        					.filter(Integer.class::isInstance)
        					.map(Integer.class::cast)
        					.map(id -> "%s#%d".formatted(username, id)))
        			.orElse("UNKNOWN");
    		String time = DateFormat.getDateTimeInstance().format(new Date());
    		String msg = message.get();
    		String logLine = msg.isEmpty() ? "%s [%s] (%s) %s%n".formatted(time, this.name.toUpperCase(), req.getRemoteAddr(), user)
    				: "%s [%s] (%s) %s: %s%n".formatted(time, this.name.toUpperCase(), req.getRemoteAddr(), user, msg);
    		Files.writeString(this.plugin.getDataFolder().toPath().resolve("web-actions.log"),
    				logLine,
    				StandardCharsets.UTF_8,
    				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    	}
    }

    public abstract void execute(HttpServletRequest req, HttpServletResponse res, String[] args) throws IOException, SQLException;
}
