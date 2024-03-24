package me.szumielxd.proxyserverlist.common.managers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerDisplay;
import me.szumielxd.proxyserverlist.common.objects.CachedServerInfo;
import me.szumielxd.proxyyweb.common.objects.SenderWrapper;
import me.szumielxd.proxyyweb.common.objects.CommonScheduler.ExecutedTask;

public class StatsManager<T, U extends T> {
	
	private static final @NotNull Gson GSON = new GsonBuilder()
			.disableHtmlEscaping()
			.create();
	
	private final @NotNull ProxyServerList<T, U, ?> plugin;
	private final @NotNull Path statsFile;
	private ExecutedTask saveTask;
	
	public StatsManager(@NotNull ProxyServerList<T, U, ?> plugin, @NotNull Path statsFile) {
		this.plugin = plugin;
		this.statsFile = statsFile;
	}
	
	public @NotNull StatsManager<T, U> start() {
		this.stop();
		if (Config.STATS_INTERVAL.getBoolean()) {
			this.saveTask = this.plugin.getScheduler().runTaskTimer(this::appendServerStats,
					Config.STATS_INTERVAL.getInt(), 
					Config.STATS_INTERVAL.getInt(),
					TimeUnit.MINUTES);
		}
		return this;
	}
	
	public void stop() {
		if (this.saveTask != null) {
			this.saveTask.cancel();
			this.saveTask = null;
		}
	}
	
	private void appendServerStats() {
		JsonObject json = new JsonObject();
		this.getVersionsIcon().forEach((name, stats) -> json.add(name, stats.asJsonObject()));
		try {
			Files.writeString(this.statsFile,
					"%d;%s%n".formatted(System.currentTimeMillis(), GSON.toJson(json)),
					StandardCharsets.UTF_8,
					StandardOpenOption.APPEND,
					StandardOpenOption.CREATE,
					StandardOpenOption.SYNC);
		} catch (IOException e) {
			this.plugin.getLogger().warning("StatsManager: Cannot access file `%s`".formatted(this.statsFile.getFileName()));
			e.printStackTrace();
		}
	}
	
	public Optional<Map<String, Integer>> getVersions(String serverName) {
		SenderWrapper<T, U, ?> swrapper = this.plugin.getSenderWrapper();
		return swrapper.getPlayers(serverName).map(players -> players.stream()
				.map(swrapper::getProtocolVersion)
				.map(ServerPingManager::getHumanReadableVersion)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(i -> 1))));
	}
	
	public Map<String, Integer> getVersions() {
		SenderWrapper<T, U, ?> swrapper = this.plugin.getSenderWrapper();
		return swrapper.getPlayers().stream()
				.map(swrapper::getProtocolVersion)
				.map(ServerPingManager::getHumanReadableVersion)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(i -> 1)));
	}
	
	public Map<String, Integer> getVersionsByIcon(SerializableServerDisplay serverIcon) {
		SenderWrapper<T, U, ?> swrapper = this.plugin.getSenderWrapper();
		return serverIcon.getNames().stream()
				.map(swrapper::getPlayers)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.flatMap(Collection::stream)
				.map(swrapper::getProtocolVersion)
				.map(ServerPingManager::getHumanReadableVersion)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(i -> 1)));
	}
	
	public Map<String, ServerStat> getVersionsIcon() {
		return this.plugin.getServersConfig().getServerIcons().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey,
						e -> iconToServerStat(e.getValue())));
	}
	
	private ServerStat iconToServerStat(SerializableServerDisplay icon) {
		return new ServerStat(
				icon.getFriendlyName(),
				icon.getNames(),
				icon.getNames().stream()
						.map(plugin.getServerPingManager()::getCachedServer)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.map(CachedServerInfo::getVersionId)
						.map(ServerPingManager::getHumanReadableVersion)
						.findAny().orElse("null"),
				getVersionsByIcon(icon));
	}
	
	public record ServerStat(String friendlyName, List<String> serverNames, String version, Map<String, Integer> clientVersions) {
		public JsonObject asJsonObject() {
			JsonObject json = new JsonObject();
			json.addProperty("friendlyName", friendlyName());
			json.add("serverNames", serverNames().stream()
					.collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
			json.addProperty("version", version());
			json.add("clientVersions", clientVersions().entrySet().stream()
					.collect(JsonObject::new,
							(obj, e) -> obj.addProperty(e.getKey(), e.getValue()),
							(o1, o2) -> o2.entrySet().forEach(e -> o1.add(e.getKey(), e.getValue()))));
			return json;
		}
		
	}

}
