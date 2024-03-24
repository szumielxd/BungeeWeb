package me.szumielxd.proxyserverlist.common.listeners;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;

import me.szumielxd.legacyminiadventure.LegacyMiniadventure;
import me.szumielxd.legacyminiadventure.VersionableObject.ChatVersion;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerDisplay;
import me.szumielxd.proxyserverlist.common.objects.CachedServerInfo;
import me.szumielxd.proxyserverlist.common.objects.PingResult;
import me.szumielxd.proxyserverlist.common.objects.PingResult.PlayersList;
import me.szumielxd.proxyserverlist.common.objects.enums.ServerSortType;
import me.szumielxd.proxyserverlist.common.utils.MiscUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public abstract class MotdListener<C> {
	
	
	private final @NotNull ProxyServerList<?, ?, C> plugin;
	private final int cacheTime;
	private final boolean modifyPlayers;
	private final int modifyMaxPlayers;
	private final @NotNull List<String> playersDisplay;
	private final @NotNull List<String> motd;
	private final @NotNull List<String> versionNames;
	private final @NotNull List<ServerSortType> sortOrder;
	private final @NotNull Comparator<Entry<String, Integer>> serverComparator;
	
	private long lastUpdate;
	private PingResult<C> cachedResult;
	
	
	protected MotdListener(@NotNull ProxyServerList<?, ?, C> plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
		this.cacheTime = Config.MOTD_CACHE.getInt() * 1000;
		this.modifyPlayers = Config.MOTD_PLAYERS_MODIFY.getBoolean();
		this.modifyMaxPlayers = Config.MOTD_PLAYERS_SHOWMOREMAX.getInt();
		this.playersDisplay = Config.MOTD_PLAYERS_DISPLAY.getStringList();
		this.sortOrder = Config.MOTD_SERVERS_ORDER.getStringList().stream()
				.map(ServerSortType::tryParse)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		this.serverComparator = this.sortOrder.stream()
				.collect(() -> new AtomicReference<Comparator<Entry<String, Integer>>>(),
						(ref, c) -> ref.updateAndGet(cmp -> cmp == null ? c : cmp.thenComparing(c)),
						(v1, v2) -> v1.updateAndGet(cmp -> cmp.thenComparing(v2.get()))).get();
		this.motd = Config.MOTD_DISPLAY.getStringList();
		this.versionNames = Config.MOTD_VERSION_DISPLAY.getStringList();
	}
	
	
	protected @NotNull PingResult<C> applyMotd(@NotNull PingResult<C> ping) {
		if (this.hasValidCache()) {
			this.cachedResult = this.buildPingResult(ping);
			this.lastUpdate = System.currentTimeMillis();
		}
		return this.cachedResult;
		
	}
	
	
	protected boolean hasValidCache() {
		return this.lastUpdate + this.cacheTime <= System.currentTimeMillis()
				|| this.cachedResult != null;
	}
	
	
	private PingResult<C> buildPingResult(@NotNull PingResult<C> ping) {
		// set max players
		if (this.modifyPlayers) {
			ping.getPlayers().ifPresent(
					players -> players.setMax(Math.min(0, players.getMax() + this.modifyMaxPlayers)));
		}
		
		if (!this.playersDisplay.isEmpty()) {
			this.buildPlayersList(ping);
		}
		
		int onlinePlayers = ping.getPlayers().map(PlayersList::getOnline).orElse(0);
		int maxOnline = ping.getPlayers().map(PlayersList::getMax).orElse(0);
		String version = this.plugin.getServerPingManager().getGlobalVersion();
		Map<String, String> staticReplacements = Map.of(
				"gonline", String.valueOf(onlinePlayers),
				"gmaxonline", String.valueOf(maxOnline),
				"gversion", version);
		
		if (!this.motd.isEmpty()) {
			ping.setDescription(
					LegacyMiniadventure.get().deserialize(MiscUtil.random(this.motd), staticReplacements)
							.map(this.plugin.getComponentMapper()::kyoriToComponent));
		}
		
		if (!this.versionNames.isEmpty()) {
			String versionName = LegacyComponentSerializer.legacySection().serialize(
					LegacyMiniadventure.get().deserialize(
							ChatVersion.LEGACY,
							MiscUtil.random(this.versionNames),
							staticReplacements));
			ping.setVersion(new PingResult.Version(-1, versionName));
		}
		return ping.unmodifiable();
	}
	
	private void buildPlayersList(@NotNull PingResult<C> ping) {
		PlayersList players = ping.getPlayers().orElseGet(PlayersList::emptyList);
		int onlinePlayers = players.getOnline();
		int maxOnline = players.getMax();
		String version = this.plugin.getServerPingManager().getGlobalVersion();
		Map<String, String> staticReplacements = Map.of(
				"gonline", String.valueOf(onlinePlayers),
				"gmaxonline", String.valueOf(maxOnline),
				"gversion", version);
		
		List<PlayersList.SamplePlayer> samples = new LinkedList<>();
		for (String line : this.playersDisplay) {
			if (line.contains("{servers}")) {
				samples.addAll(buildPlayersListServers(line, staticReplacements));
			} else {
				String playerName = LegacyComponentSerializer.legacySection().serialize(
						LegacyMiniadventure.get().deserialize(ChatVersion.LEGACY, line, staticReplacements));
				samples.add(new PlayersList.SamplePlayer(UUID.randomUUID(), playerName));
			}
		}
		players.setPlayers(samples);
	}
	
	private @NotNull List<PlayersList.SamplePlayer> buildPlayersListServers(@NotNull String format, @NotNull Map<String, String> staticReplacements) {
		return this.plugin.getServerPingManager().getAvailableServerIcons(null, -1).stream()
				.map(this::fetch)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.sorted(Comparator.comparing(s -> Map.entry(s.friendlyName(), s.online()), this.serverComparator))
				.map(s -> LegacyMiniadventure.get().deserialize(
						ChatVersion.LEGACY,
						format,
						MiscUtil.merge(staticReplacements, Map.of(
								"online", String.valueOf(s.online()),
								"maxonline", String.valueOf(s.maxOnline()),
								"servers", s.friendlyName(),
								"version", s.version())),
						Placeholder.styling("accent", s.icon().getAccent())))
				.map(LegacyComponentSerializer.legacySection()::serialize)
				.map(s -> new PlayersList.SamplePlayer(UUID.randomUUID(), s))
				.toList();
	}
	
	public @NotNull Optional<ActiveServerStats> fetch(@NotNull SerializableServerDisplay icon) {
		var visibleSubServers = icon.getNames().stream()
				.map(this.plugin.getServerPingManager()::getCachedServer)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.filter(s -> !icon.isHidingOffline() || s.getPing() > -1)
				.toList();
		if (!visibleSubServers.isEmpty()) {
			int online = this.getCachedServerPlayerCount(icon, visibleSubServers);
			int maxOnline = visibleSubServers.stream()
					.mapToInt(CachedServerInfo::getMaxOnline)
					.sum();
			String version = visibleSubServers.stream()
					.findFirst()
					.map(CachedServerInfo::getVersionFriendlyName)
					.orElse("?");
			return Optional.of(new ActiveServerStats(icon, online, maxOnline, version));
		}
		return Optional.empty();
	}
	
	private int getCachedServerPlayerCount(@NotNull SerializableServerDisplay icon, @NotNull List<CachedServerInfo<C>> cachedInfo) {
		if (icon.isUsePingedPlayers()) {
			return cachedInfo.stream()
					.mapToInt(CachedServerInfo::getOnline)
					.sum();
		}
		return icon.getNames().stream()
				.map(this.plugin.getSenderWrapper()::getPlayers)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.mapToInt(Collection::size)
				.sum();
	}
	
	private record ActiveServerStats(@NotNull SerializableServerDisplay icon, int online, int maxOnline, @NotNull String version) {
		public String friendlyName() {
			return this.icon.getFriendlyName();
		}
	}
	

}
