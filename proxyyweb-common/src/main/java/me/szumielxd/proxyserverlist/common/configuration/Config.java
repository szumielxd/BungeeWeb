package me.szumielxd.proxyserverlist.common.configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.Configuration;
import org.simpleyaml.configuration.MemorySection;
import org.simpleyaml.configuration.file.YamlConfiguration;

import me.szumielxd.proxyserverlist.common.ProxyServerList;

public enum Config {
	
	PREFIX("common.prefix", "<b><dark_purple>P<light_purple>S<aqua>L</b> <dark_gray><b>»</b> <gray>"),
	DEBUG("common.debug", false),
	MESSAGES_PERM_ERROR("message.perm-error", "<red>No, you can't"),
	MESSAGES_COMMAND_ERROR("message.command-error", "<dark_red>An error occured while attempting to perform this command. Please report this to admin."),
	MESSAGES_CONSOLE_ERROR("message.console-error", "<red>Not for console ;c"),
	MESSAGES_INVALID_SERVER("message.invalid-server", "<red>Invalid server"),
	COMMAND_NAME("command.name", "proxyserverlist"),
	COMMAND_ALIASES("command.aliases", List.of("proxysrvlist", "psl")),
	COMMAND_SUB_RELOAD_EXECUTE("command.sub.reload.execute", "Reloading..."),
	COMMAND_SUB_RELOAD_ERROR("command.sub.reload.error", "<red>An error occured while reloading plugin. See console for more info."),
	COMMAND_SUB_RELOAD_SUCCESS("command.sub.reload.success", "<green>Successfully reloaded {plugin} v{version}"),
	GUI_REFRESH_TIME("gui.refresh-time", 5),
	GUI_COMMAND_NAME("gui.command.name", "servers"),
	GUI_COMMAND_ALIASES("gui.command.aliases", List.of("servergui", "hub")),
	GUI_COMMAND_ROWS("gui.rows", 6),
	GUI_COMMAND_PLAYERSASAMOUNT("gui.players-as-amount", true),
	GUI_COMMAND_TITLE("gui.title", "<dark_purple><b>Available Servers"),
	GUI_COMMAND_BACKGROUND("gui.background", "BLACK_STAINED_GLASS_PANE[0-54]|RED_STAINED_GLASS_PANE[4,13,22,31,40,49]"),
	GUI_COMMAND_FORMAT("gui.format", List.of(
			"<gold><st>---<obf>[]</obf>-------------------------<obf>[]</obf>---</st></gold>",
			"<dark_gray>» <gray>Server: <accent>{name}",
			"<dark_gray>» <gray>Base version: <accent>{version}",
			"<dark_gray>» <gray>Online: <accent>{online}",
			"<dark_gray>» <gray>Ping: <accent>{ping}",
			"",
			"<dark_gray>» <gray>Description:",
			"  <accent>{description}",
			"<gold><st>---<obf>[]</obf>-------------------------<obf>[]</obf>---</st></gold>")),
	GEYSER_FORM_TITLE("geyser.form.title", "Game Browser"),
	GEYSER_FORM_CONTENT("geyser.form.content", "Chose your server"),
	SERVERSTATUS_REFRESH_TIME("server-status.refresh-time", 5),
	SERVERSTATUS_NOTIFY_AFTER("server-status.notify-after", 90),
	SERVERSTATUS_NOTIFY_INTERVAL("server-status.notify-interval", 180),
	SERVERSTATUS_NOTIFY_MESSAGE("server-status.notify-message", "<dark_red><b>[<red><obf>$</obf></red>]</b> <dark_red>Server <aqua>{name}</aqua> is offline since <aqua>{time}</aqua>."),
	STATS_INTERVAL("stats.save-interval", 10),
	MOTD_CACHE("motd.cache", 30),
	MOTD_PLAYERS_MODIFY("motd.players.modify", true),
	MOTD_PLAYERS_SHOWMOREMAX("motd.players.show-more-max", 1),
	MOTD_PLAYERS_DISPLAY("motd.players.display", List.of(
			"",
			"  <gray>Version: <green>{gversion}",
			"  <gray>Facebook: <blue>fb.example.com",
			"  <gray>Forum: <aqua>forum.example.com",
			"  <gray>TeamSpeak: <red>ts.example.com",
			"  <gray>Discord: <dark_red>dc.example.com",
			"  <gray>Shop: <light_purple>shop.example.com",
			"",
			"  <gray>Available servers:",
			"    <dark_gray>- <accent>{servers}</accent> [<gold>{online}/{maxonline}</gold>] (<gray>{version}</gray>)",
			"",
			"  <gray>Catch 'em all!",
			"")),
	MOTD_SERVERS_ORDER("motd.servers-order", List.of("NATURAL", "ALPHABETICALLY", "ALPHABETICALLY_IGNORECASE", "ALPHABETICALLY_REVERSE", "ALPHABETICALLY_IGNORECASE_REVERSE", "ONLINE", "ONLINE_REVERSE")),
	MOTD_VERSION_DISPLAY("motd.versions.display", List.of("<aqua>{gversion} <dark_gray>»<reset>                                                             <gray>Players: <gold>{gonline}")),
	MOTD_DISPLAY("motd.display", Stream.of(
			List.of("<dark_gray>[ <dark_red>*</dark_red> ] <b>«</b><gray><st>----</st> </gray>[ <gradient:#AA00AA:#55FFFF><b>Example.com</gradient> <gradient:#FF55FF:#FF0000>{gversion}</gradient> ] <gray><st>----</st></gray><b>»</b> <dark_gray>[ <dark_red>*</dark_red> ]",
					"<dark_gray>» <gold><b>PvP</b></gold> - <yellow>Fight for The Queen!"),
			List.of("<dark_gray>[ <dark_red>*</dark_red> ] <b>«</b><gray><st>----</st> </gray>[ <gradient:#AA00AA:#55FFFF><b>Example.com</gradient> <gradient:#FF55FF:#FF0000>{gversion}</gradient> ] <gray><st>----</st></gray><b>»</b> <dark_gray>[ <dark_red>*</dark_red> ]",
					"<dark_gray>» <gradient:#FF69B4:#FFB6C1>Supports</gradient> <b><dark_red>R<green>G<dark_blue>B</b>"))
			.map(l -> String.join("<br>", l))
			.toList()),
	WEB_PORT("web.port", 4420),
	;
	
	
	
	//////////////////////////////////////////////////////////////////////
	
	private final String path;
	private List<String> texts;
	private String text;
	private int number;
	private boolean bool;
	private Map<String, Object> map;
	private Class<?> type;
	
	
	private Config(String path, String text) {
		this.path = path;
		setValue(text);
	}
	private Config(String path, List<String> texts) {
		this.path = path;
		setValue(texts);
	}
	private Config(String path, int number) {
		this.path = path;
		setValue(number);
	}
	private Config(String path, boolean bool) {
		this.path = path;
		setValue(bool);
	}
	private Config(String path, Map<String, Object> valueMap) {
		this.path = path;
		setValue(valueMap);
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	
	private void setValue(String text) {
		this.type = String.class;
		this.text = text;
		this.texts = List.of(this.text);
		this.number = text.length();
		this.bool = !text.isEmpty();
		this.map = Map.of();
	}
	private void setValue(List<String> texts) {
		this.type = String[].class;
		this.text = String.join(", ", texts);
		this.texts = texts;
		this.number = texts.size();
		this.bool = !texts.isEmpty();
		AtomicInteger index = new AtomicInteger(0);
		this.map = texts.stream().collect(Collectors.toMap(v -> String.valueOf(index.getAndAdd(1)), v -> v));
	}
	private void setValue(int number) {
		this.type = Integer.class;
		this.text = Integer.toString(number);
		this.texts = List.of(this.text);
		this.number = number;
		this.bool = number > 0;
		this.map = Map.of();
	}
	private void setValue(boolean bool) {
		this.type = Boolean.class;
		this.text = Boolean.toString(bool);
		this.texts = List.of(this.text);
		this.number = bool? 1 : 0;
		this.bool = bool;
		this.map = Map.of();
	}
	private void setValue(Map<String, Object> valueMap) {
		this.type = Map.class;
		this.text = valueMap.toString();
		this.texts = valueMap.values().stream().map(Object::toString).toList();
		this.number = valueMap.size();
		this.bool = !valueMap.isEmpty();
		this.map = valueMap;
	}
	
	
	public String getString() {
		return this.text;
	}
	@Override
	public String toString() {
		return this.text;
	}
	public List<String> getStringList() {
		return new ArrayList<>(this.texts);
	}
	public int getInt() {
		return this.number;
	}
	public boolean getBoolean() {
		return this.bool;
	}
	public Map<String, Object> getValueMap() {
		return this.map;
	}
	public Class<?> getType() {
		return this.type;
	}
	public String getPath() {
		return this.path;
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	
	public static void load(@NotNull File file, @NotNull ProxyServerList<?, ?, ?> plugin) {
		Objects.requireNonNull(plugin, "plugin cannot be null").getLogger().info("Loading configuration from '" + Objects.requireNonNull(file, "file cannot be null").getName() + "'");
		if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
		try {
			if(!file.exists()) file.createNewFile();
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			if(loadConfig(config) > 0) config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int loadConfig(Configuration config) {
		int modify = 0;
		for (Config val : Config.values()) {
			if(!config.contains(val.getPath())) modify++;
			if (val.getType().equals(String.class)) val.setValue(getStringOrSetDefault(config, val.getPath(), val.getString()));
			else if (val.getType().equals(String[].class)) val.setValue(getStringListOrSetDefault(config, val.getPath(), val.getStringList()));
			else if (val.getType().equals(Integer.class)) val.setValue(getIntOrSetDefault(config, val.getPath(), val.getInt()));
			else if (val.getType().equals(Boolean.class)) val.setValue(getBooleanOrSetDefault(config, val.getPath(), val.getBoolean()));
			else if (val.getType().equals(Map.class)) val.setValue(getMapOrSetDefault(config, val.getPath(), val.getValueMap()));
		}
		return modify;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> Map<String,T> getMapOrSetDefault(Configuration config, String path, Map<String,T> def) {
		if (config.contains(path)) {
			return (Map<String, T>) ((MemorySection) config.getConfigurationSection(path)).getMapValues(false);
		}
		config.set(path, def);
		return def;
	}
	
	private static int getIntOrSetDefault(Configuration config, String path, int def) {
		if (config.contains(path)) return config.getInt(path);
		config.set(path, def);
		return def;
	}
	
	private static boolean getBooleanOrSetDefault(Configuration config, String path, boolean def) {
		if (config.contains(path)) return config.getBoolean(path);
		config.set(path, def);
		return def;
	}
	
	private static String getStringOrSetDefault(Configuration config, String path, String def) {
		if (config.contains(path)) return config.getString(path);
		config.set(path, def);
		return def;
	}
	
	private static List<String> getStringListOrSetDefault(Configuration config, String path, List<String> def) {
		if(config.contains(path)) return new ArrayList<>(config.getStringList(path));
		config.set(path, def);
		return List.copyOf(def);
	}

}
