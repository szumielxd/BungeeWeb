package me.szumielxd.proxyserverlist.common.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.szumielxd.legacyminiadventure.LegacyMiniadventure;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyyweb.common.commands.CommonCommand;
import me.szumielxd.proxyyweb.common.objects.ComponentMapper;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MainCommand<T, C> extends CommonCommand<T> {

	private static final @NotNull String PERMISSIION_BASE = "proxyserverlist.command.main";
	private static final @NotNull Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-z]+)\\}", Pattern.CASE_INSENSITIVE);
	private static final @NotNull MiniMessage MINI = MiniMessage.miniMessage();
	
	private final @NotNull ProxyServerList<T, ?, C> plugin;
	private final ComponentMapper<C> component;

	private final Map<String, DummyCommand<T>> subCmds = new LinkedHashMap<>();

	public MainCommand(@NotNull ProxyServerList<T, ?, C> plugin) {
		super(Config.COMMAND_NAME.getString(), "proxyserverlist.command.main", Config.COMMAND_ALIASES.getStringList().toArray(new String[0]));
		this.plugin = plugin;
		this.component = this.plugin.getComponentMapper();
		this.registerSubCommands();
	}
	
	private void registerSubCommands() {
		var list = List.of(
				new DummyCommand<T>(List.of("reload", "rl"), "reload", (s, a) -> this.processReloadCommand(s), (s, a) -> List.of()),
				new DummyCommand<T>(List.of("stats"), "stats", (s, a) -> this.processStatsCommand(s), (s, a) -> List.of()));
		list.forEach(c -> c.names().forEach(n -> this.subCmds.put(n.toLowerCase(), c)));
	}

	@Override
	public void execute(@NotNull T sender, String[] args) {
		if (args.length > 0) {
			String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
			var subcmd = this.subCmds.get(args[0].toLowerCase());
			if (subcmd != null) {
				if (subcmd.hasPermission(this.plugin, sender)) {
					subcmd.commandExecutor.accept(sender, subArgs);
				} else {
					sendSystemMessage(sender, Config.MESSAGES_PERM_ERROR.getString());
				}
				return;
			}
		}
		List<String> labels = new LinkedList<>();
		labels.add("help");
		this.subCmds.entrySet().stream()
				.filter(c -> c.getValue().hasPermission(this.plugin, sender))
				.map(Entry::getKey)
				.forEach(labels::add);
		sendCommandUsage(sender, String.join("|", labels));
	}

	@Override
	public @NotNull List<String> onTabComplete(@NotNull T sender, @NotNull String[] args) {
		List<String> list = new LinkedList<>();
		if (this.plugin.getSenderWrapper().hasPermission(sender, PERMISSIION_BASE)) {
			if (args.length > 1) {
				String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
				var subcmd = this.subCmds.get(args[0].toLowerCase());
				if (subcmd != null) {
					list.addAll(subcmd.tabExecutor.apply(sender, subArgs));
				}
			} else {
				String arg = args.length > 0 ? args[0].toLowerCase() : "";
				this.subCmds.entrySet().stream()
						.filter(c -> c.getKey().toLowerCase().startsWith(arg))
						.filter(c -> c.getValue().hasPermission(this.plugin, sender))
						.map(Entry::getKey)
						.forEach(list::add);
			}
		}
		Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
		return list;
	}
	
	private void processReloadCommand(@NotNull T sender) {
		boolean failed = false;
		Map<String, String> replacements = Map.of("plugin", this.plugin.getName(), "version", this.plugin.getVersion());
		sendSystemMessage(sender, Config.COMMAND_SUB_RELOAD_EXECUTE.getString(), replacements);
		try {
			this.plugin.onDisable();
		} catch (Exception e) {
			e.printStackTrace();
			failed = true;
		}
		try {
			this.plugin.onEnable();
		} catch (Exception e) {
			e.printStackTrace();
			failed = true;
		}
		if (failed) {
			sendSystemMessage(sender, Config.COMMAND_SUB_RELOAD_ERROR.getString(), replacements);
		} else {
			sendSystemMessage(sender, Config.COMMAND_SUB_RELOAD_SUCCESS.getString(), replacements);
		}
	}
	
	private void processStatsCommand(@NotNull T sender) {
		sendSystemMessage(sender, "Not yet...");
	}
	
	private void sendCommandUsage(@NotNull T sender, @NotNull String usage) {
		this.sendSystemMessage(sender, "/" + Config.COMMAND_NAME.getString()+" " + usage);
	}
	
	private void sendSystemMessage(@NotNull T sender, @NotNull String message) {
		this.sendSystemMessage(sender, message, UnaryOperator.identity());
	}
	
	private void sendSystemMessage(@NotNull T sender, @NotNull String message, @NotNull Map<String, String> replacements) {
		this.sendSystemMessage(sender, message, s -> this.replacePlaceholders(s, replacements));
	}
	
	private void sendSystemMessage(@NotNull T sender, @NotNull String message, @NotNull UnaryOperator<String> replacer) {
		this.plugin.getSenderWrapper().sendMessage(sender,
				LegacyMiniadventure.get().deserialize(Config.PREFIX.getString() + replacer.apply(message))
						.map(this.component::kyoriToComponent));
	}
	
	private @NotNull String replacePlaceholders(@NotNull String text, @NotNull Map<String, String> replacements) {
		return PLACEHOLDER_PATTERN.matcher(text).replaceAll(match -> {
			String replacement = replacements.get(match.group(1));
			if (replacement != null) {
				return MINI.escapeTags(replacement);
			}
			return match.group();
		});
	}
	
	
	private record DummyCommand<T>(@NotNull List<String> names, @Nullable String subpermission, @NotNull BiConsumer<T, String[]> commandExecutor, @NotNull BiFunction<T, String[], List<String>> tabExecutor) {
		
		public boolean hasPermission(ProxyServerList<T, ?, ?> plugin, T sender) {
			return subpermission() != null && plugin.getSenderWrapper().hasPermission(sender, PERMISSIION_BASE + "." + subpermission());
		}
	}

}
