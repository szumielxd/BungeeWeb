package me.szumielxd.proxyserverlist.common.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import me.szumielxd.proxyserverlist.common.ProxyServerList;


@AllArgsConstructor
public class WebHandler extends AbstractHandler {

	
	private @NotNull ProxyServerList<?, ?, ?> plugin;
	
	private static final @NotNull Gson GSON = new GsonBuilder().disableHtmlEscaping().disableInnerClassSerialization().create();
	private static final @NotNull String URL_INDEX_PATTERN = "(\\/(index\\.(html|php))?)?";
	private static final @NotNull Pattern API_STATUS_PATTERN = Pattern.compile(Pattern.quote("/api/status") + URL_INDEX_PATTERN, Pattern.CASE_INSENSITIVE);
	
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		baseRequest.setHandled(true);
		response.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
		if (API_STATUS_PATTERN.matcher(target).matches() && "GET".equals(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/json");
			JsonObject json = new JsonObject();
			JsonObject players = new JsonObject();
			players.addProperty("online", this.plugin.getSenderWrapper().getPlayerCount());
			players.addProperty("max", this.plugin.getSenderWrapper().getPlayerCount());
			json.add("players", players);
			json.addProperty("versions", this.plugin.getServerPingManager().getGlobalVersion());
			if ("true".equals(request.getParameter("favicon"))) json.addProperty("favicon", this.plugin.getSenderWrapper().getFavicon().orElse(null));
			json.add("servers", this.plugin.getServerPingManager().serializeServersToJson());
			GSON.toJson(json, response.getWriter());
			return;
		}
		

		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		
	}

}
