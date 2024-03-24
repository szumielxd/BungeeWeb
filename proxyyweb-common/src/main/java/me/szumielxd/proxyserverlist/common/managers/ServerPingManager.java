package me.szumielxd.proxyserverlist.common.managers;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.simplix.protocolize.api.util.ProtocolVersions;
import io.netty.channel.unix.DomainSocketAddress;
import lombok.Getter;
import me.szumielxd.legacyminiadventure.VersionableObject;
import me.szumielxd.legacyminiadventure.VersionableObject.ChatVersion;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerDisplay;
import me.szumielxd.proxyserverlist.common.configuration.ServersConfig;
import me.szumielxd.proxyserverlist.common.objects.CachedServerInfo;
import me.szumielxd.proxyserverlist.common.objects.PingResult;
import me.szumielxd.proxyserverlist.common.objects.PingResult.PlayersList;
import me.szumielxd.proxyserverlist.common.utils.MiscUtil;
import me.szumielxd.proxyyweb.common.objects.ComponentMapper;
import me.szumielxd.proxyyweb.common.objects.SenderWrapper;
import me.szumielxd.proxyyweb.common.objects.CommonScheduler.ExecutedTask;

public class ServerPingManager<T, U extends T, C> {
	
	
	private static final Pattern fieldPattern = Pattern.compile("MINECRAFT_(1_\\d+(_\\d+)?)");
	private static final Map<Integer, String> VERSIONS_MAP = Stream.of(ProtocolVersions.class.getDeclaredFields()).map(f -> {
			try {
				Matcher match = fieldPattern.matcher(f.getName());
				if (match.matches()) {
					f.setAccessible(true);
					int id = f.getInt(null);
					return new AbstractMap.SimpleEntry<>(id, match.group(1).replace('_', '.'));
				}
				return null;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
	
	
	private final @NotNull ProxyServerList<T, U, C> plugin;
	private final @NotNull ComponentMapper<C> component;
	private final @NotNull Map<String, CachedServerInfo<C>> cache;
	private final @NotNull VersionableObject<C> notification;
	private final int notifyAfter = Config.SERVERSTATUS_NOTIFY_AFTER.getInt() * 1000;
	private final int notifyInterval = Config.SERVERSTATUS_NOTIFY_INTERVAL.getInt() * 1000;
	private @Nullable ExecutedTask updateTask;
	private long lastNotification = 0;
	private final @Getter @NotNull String globalVersion;
	
	
	public ServerPingManager(@NotNull ProxyServerList<T, U, C> plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
		this.component = this.plugin.getComponentMapper();
		this.cache = this.plugin.getServersConfig().getServerIcons().values().stream()
				.map(SerializableServerDisplay::getNames)
				.flatMap(List::stream)
				.filter(srv -> this.plugin.getSenderWrapper().getPlayers(srv).isPresent())
				.collect(Collectors.toMap(String::toLowerCase, CachedServerInfo::new, (a, b) -> a));
		this.notification = this.plugin.getComponentMapper().parseLegacyMessage(Config.SERVERSTATUS_NOTIFY_MESSAGE.getString());
		List<Entry<Integer, String>> versions = ServerPingManager.VERSIONS_MAP.entrySet().stream()
				.filter(e -> e.getKey() > 0)
				.sorted(Entry.comparingByKey())
				.toList();
		this.globalVersion = versions.size() == 1 ?
				versions.get(0).getValue()
				: versions.get(0).getValue() + "-" + versions.get(versions.size() -1).getValue();
	}
	
	
	public @NotNull JsonElement serializeServersToJson() {
		JsonObject json = new JsonObject();
		this.cache.forEach((name, server) -> json.add(name, server.serializeToJson()));
		return json;
	}
	
	
	public ServerPingManager<T, U, C> start() {
		if (Config.SERVERSTATUS_REFRESH_TIME.getInt() > 0) {
			this.plugin.getScheduler().runTaskTimer(this::update, 1L, Config.SERVERSTATUS_REFRESH_TIME.getInt(), TimeUnit.SECONDS);
		}
		return this;
	}
	
	
	public boolean stop() {
		if (this.updateTask == null) return false;
		this.updateTask.cancel();
		return true;
	}
	
	
	public void update() {
		final Map<String, CachedServerInfo<C>> dead = this.pingServers();
		if (!dead.isEmpty() && System.currentTimeMillis() >= this.lastNotification + this.notifyInterval && this.notifyDown(dead)) {
			this.lastNotification = System.currentTimeMillis();
		}
	}
	
	
	public Optional<CachedServerInfo<C>> getCachedServer(@NotNull String server) {
		Objects.requireNonNull(server, "server cannot be null");
		return Optional.ofNullable(this.cache.get(server.toLowerCase()));
	}
	
	
	public boolean isCachedOnline(@NotNull String server) {
		Objects.requireNonNull(server, "server cannot be null");
		return this.getCachedServer(server).filter(srv -> srv.getPing() >= 0).isPresent();
	}
	
	
	public int readVarInt(DataInputStream in) throws IOException {
		int i = 0;
		int j = 0;
		while (true) {
			int k = in.readByte();
			i |= (k & 0x7F) << j++ * 7;
			if (j > 5) throw new RuntimeException("VarInt too big");
			if ((k & 0x80) != 128) break;
		}
		return i;
	}
 
	public void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
		while (true) {
			if ((paramInt & 0xFFFFFF80) == 0) {
			  out.writeByte(paramInt);
			  return;
			}

			out.writeByte(paramInt & 0x7F | 0x80);
			paramInt >>>= 7;
		}
	}
	
	public @NotNull Collection<SerializableServerDisplay> getAvailableServerIcons(@Nullable UUID playerId, int protocol) {
		// icons
		return this.plugin.getServersConfig().getServerIcons().values().stream()
				.filter(i -> i.isVisible(plugin, playerId, protocol))
				.toList();
	}
	
	
	private boolean notifyDown(@NotNull Map<String, CachedServerInfo<C>> dead) {
		List<VersionableObject<C>> notifications = dead.entrySet().stream()
				.map(e -> this.buildNotification(e.getKey(), e.getValue()))
				.toList();
		SenderWrapper<T, U, C> swrapper = this.plugin.getSenderWrapper();
		List<U> targets = swrapper.getPlayers().parallelStream()
				.filter(p -> swrapper.hasPermission(p, "proxyserverlist.notify.serverdown"))
				.toList();
		if (!targets.isEmpty()) {
			targets.parallelStream()
					.forEach(p -> notifications.forEach(c -> swrapper.sendMessage(p, c)));
			return true;
		}
		return false;
	}
	
	
	private @NotNull VersionableObject<C> buildNotification(@NotNull String name, @NotNull CachedServerInfo<C> serverInfo) {
		String time = MiscUtil.formatTimespan((System.currentTimeMillis() - serverInfo.getLastStateToggle()) / 1000);
		return this.plugin.getComponentMapper().replacePlainPlaceholders(this.notification, Map.of("time", time, "name", name));
	}
	
	
	/**
	 * Try to ping servers
	 * 
	 * @return return dead servers
	 */
	private Map<String, CachedServerInfo<C>> pingServers() {
		ServersConfig cfg = this.plugin.getServersConfig();
		return this.cache.entrySet().parallelStream()
				.map(entry -> {
					CompletableFuture<Entry<String, CachedServerInfo<C>>> future = new CompletableFuture<>();
					Optional<SocketAddress> address = this.plugin.getSenderWrapper().getServerAddress(entry.getKey());
					if (!address.isPresent()) {
						future.complete(null);
					} else {
						ping(address.get(), result -> this.processPing(result, future, entry));
					}
					return future;
				}).map(CompletableFuture::join)
				.filter(Objects::nonNull)
				.filter(e -> Optional.ofNullable(cfg.getServerIcons().get(e.getValue().getFriendlyName().toLowerCase()))
						.filter(s -> !s.isHidingOffline()) // ignore servers marked for hide when offline
						.isPresent())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}
	
	
	private void ping(SocketAddress address, Consumer<PingResult<C>> resultConsumer) {
		new Thread(() -> {
			PingResult<C> result = new PingResult<>(new PingResult.Version(-1, null), Optional.empty(), VersionableObject.same(component.empty()), -1);
			
			String hostname = address instanceof InetSocketAddress inet ? inet.getHostString() : ((DomainSocketAddress) address).path();
			
			try (Socket socket = new Socket()) {
				socket.setSoTimeout(5000);
				long time = System.currentTimeMillis();
				socket.connect(address, 5000);
				try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
					
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutputStream handshake = new DataOutputStream(baos);
					handshake.writeByte(0x00); // handshake packet id
					this.writeVarInt(handshake, -1); // protocol version
					this.writeVarInt(handshake, hostname.length()); // hostname length
					handshake.writeBytes(hostname); // hostname
					handshake.writeShort(address instanceof InetSocketAddress inet ? inet.getPort() : 1);
					this.writeVarInt(handshake, 1); // state (1 for handshake)
					
					this.writeVarInt(out, baos.size()); // handshake size
					out.write(baos.toByteArray()); // handshake
					
					out.writeByte(0x01);
					out.writeByte(0x00);
					
					try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
						this.readVarInt(in); // packet size
						int id = this.readVarInt(in); // packet id
						if (id == 0x00) { // handshake packet id
							int length = this.readVarInt(in); // string length
							if (length > 0) {
								byte[] bytes = new byte[length];
								in.readFully(bytes);
								time = System.currentTimeMillis() - time;
								JsonObject json = new Gson().fromJson(new String(bytes), JsonObject.class);
								
								JsonObject version = json.get("version").getAsJsonObject();
								String versionName = version.get("name").getAsString();
								int versionId = version.get("protocol").getAsInt();
								
								JsonObject players = json.get("players").getAsJsonObject();
								int online = players.get("online").getAsInt();
								int max = players.get("max").getAsInt();
								C description = this.plugin.getComponentMapper().jsonToComponent(json.get("description"));
								result = new PingResult<>(new PingResult.Version(versionId, versionName), Optional.of(new PlayersList(online, max, Collections.emptyList())), VersionableObject.same(description), (int) time);
								
							}
						}
					}
					
				}
			} catch (IOException e) {
				// empty catch
			} finally {
				resultConsumer.accept(result);
			}
		},"proxyserverlist-pinger").start();
	}
	
	private void processPing(@NotNull PingResult<C> result, CompletableFuture<Entry<String, CachedServerInfo<C>>> future, Entry<String, CachedServerInfo<C>> entry) {
		Entry<String, CachedServerInfo<C>> down = null;
		try {
			CachedServerInfo<C> info = entry.getValue();
			if (result.getPing() > -1) {
				if (info.getPing() <= -1) {
					info.setLastStateToggle(System.currentTimeMillis());
				}
				C desc = result.getDescription().get(ChatVersion.NORMAL);
				info.setDescription(desc, MiscUtil.getPlainVisibleText(this.component.componentToKyori(desc)));
				info.setVersionId(result.getVersion().getProtocol());
				info.setVersionFriendlyName(VERSIONS_MAP.getOrDefault(result.getVersion().getProtocol(), "?"));
				info.setVersionName(result.getVersion().getName());
			} else {
				if (info.getPing() > -1) {
					info.setLastStateToggle(System.currentTimeMillis());
				} else if (System.currentTimeMillis() >= info.getLastStateToggle() + this.notifyAfter) {
					down = entry;
				}
			}
			Optional<PlayersList> players = result.getPlayers();
			info.setOnline(players.map(PlayersList::getOnline).orElse(0));
			info.setMaxOnline(players.map(PlayersList::getMax).orElse(0));
			info.setPing(result.getPing());
		} finally {
			future.complete(down);
		}
	}
	
	public static @NotNull String getHumanReadableVersion(int protocol) {
		return getHumanReadableVersion(protocol, "UNKNOWN");
	}
	
	public static @NotNull String getHumanReadableVersion(int protocol, @NotNull String defaultVersionName) {
		Objects.requireNonNull(defaultVersionName, "defaultVersionName cannot be null");
		return VERSIONS_MAP.getOrDefault(protocol, defaultVersionName);
	}
	

}
