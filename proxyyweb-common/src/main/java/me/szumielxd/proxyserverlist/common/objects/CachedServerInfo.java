package me.szumielxd.proxyserverlist.common.objects;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class CachedServerInfo<C> {
	
	
	private final @Getter @NonNull String friendlyName;
	private @Getter @Setter int versionId = -1;
	private @Getter @Setter @Nullable String versionName = null;
	private @Getter @Setter @Nullable String versionFriendlyName = null;
	private @Getter @Nullable C description = null;
	private @Getter @Nullable String plainDescription = null;
	private @Getter @Setter int online = 0;
	private @Getter @Setter int maxOnline = 0;
	private @Getter @Setter int ping = -1;
	private @Getter @Setter long lastStateToggle = System.currentTimeMillis();
	
	
	public CachedServerInfo(@NotNull String friendlyName) {
		// filled with default values
		this.friendlyName = Objects.requireNonNull(friendlyName, "friendlyName cannot be null");
	}
	
	public void setDescription(@Nullable C description, @Nullable String plainDescription) {
		this.description = description;
		this.plainDescription = plainDescription;
	}
	
	public @NotNull JsonElement serializeToJson() {
		JsonObject json = new JsonObject();
		JsonObject version = new JsonObject();
		version.addProperty("protocol", this.versionId);
		version.addProperty("human_protocol", this.versionFriendlyName);
		version.addProperty("name", this.versionName);
		json.add("version", version);
		JsonObject players = new JsonObject();
		players.addProperty("online", this.online);
		players.addProperty("max", this.maxOnline);
		json.add("players", players);
		JsonObject health = new JsonObject();
		health.addProperty("ping", this.ping);
		health.addProperty("enabled", this.ping >= 0);
		health.addProperty("last_state_toggle", this.lastStateToggle);
		json.add("health", health);
		return json;
	}
	

}
