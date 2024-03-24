package me.szumielxd.proxyserverlist.common.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.simplix.protocolize.api.ClickType;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.chat.ChatElement;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import me.szumielxd.legacyminiadventure.LegacyMiniadventure;
import me.szumielxd.legacyminiadventure.VersionableObject;
import me.szumielxd.legacyminiadventure.VersionableObject.ChatVersion;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.utils.MiscUtil;
import me.szumielxd.proxyyweb.common.objects.ComponentMapper;
import me.szumielxd.proxyyweb.common.objects.SenderWrapper;
import me.szumielxd.proxyyweb.common.objects.CommonScheduler.ExecutedTask;

public class ServersGUI<T, U extends T, C> {
	
	
	private static final Pattern BACKGROUND_PATTERN = Pattern.compile("([a-zA-Z_]+)\\[(\\d+(-\\d+|(,\\d+){0,100}))\\]");
	

	private final @NotNull ProxyServerList<T, U, C> plugin;
	private final @NotNull ComponentMapper<C> component;
	private final @NotNull Optional<GeyserServerForm<T, U>> geyserForm;
	private final @NotNull Map<Integer, ItemStack> background = new HashMap<>();
	private final @NotNull VersionableObject<C> title;
	private final @NotNull List<String> format;
	private final @NotNull ConcurrentMap<UUID, Inventory> openedGUIs = new ConcurrentHashMap<>();
	private @Nullable ExecutedTask updateTask = null;
	
	
	public ServersGUI(@NotNull ProxyServerList<T, U, C> plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
		this.component = plugin.getComponentMapper();
		Optional<GeyserServerForm<T, U>> form;
		try {
			Class.forName("org.geysermc.geyser.GeyserImpl");
			form = Optional.of(new GeyserServerForm<>(plugin));
		} catch (ClassNotFoundException e) {
			form = Optional.empty();
		}
		this.geyserForm = form;
		
		String[] backgroundTexts = Config.GUI_COMMAND_BACKGROUND.getString().split("\\|");
		Stream.of(backgroundTexts).forEach(str -> {
			Matcher match = BACKGROUND_PATTERN.matcher(str);
			if (match.matches()) {
				ItemType type = ItemType.valueOf(match.group(1));
				if (type != null) {
					String slots = match.group(2);
					int index = slots.indexOf('-');
					(index > -1 ? IntStream.rangeClosed(Integer.parseInt(slots.substring(0, index)), Integer.parseInt(slots.substring(index+1, slots.length())))
							: Stream.of(slots.split(",")).mapToInt(Integer::parseInt)
					).filter(i -> Config.GUI_COMMAND_ROWS.getInt()*9 > i).forEach(i -> {
						ItemStack item = new ItemStack(type);
						item.displayName(ChatElement.ofLegacyText(""));
						this.background.put(i, item);
					});
				}
			}
		});
		this.format = Config.GUI_COMMAND_FORMAT.getStringList();
		this.title = LegacyMiniadventure.get().deserialize(Config.GUI_COMMAND_TITLE.getString())
				.map(this.component::kyoriToComponent);
		
	}
	
	
	public ServersGUI<T, U, C> start() {
		if (Config.GUI_REFRESH_TIME.getInt() > 0) {
			this.updateTask = this.plugin.getScheduler().runTaskTimer(this::update, 1L, Config.GUI_REFRESH_TIME.getInt(), TimeUnit.SECONDS);
		}
		return this;
	}
	
	public boolean stop() {
		if (this.updateTask == null) return false;
		this.updateTask.cancel();
		this.openedGUIs.keySet().parallelStream().map(Protocolize.playerProvider()::player)
				.filter(Objects::nonNull).forEach(ProtocolizePlayer::closeInventory);
		this.openedGUIs.clear();
		return true;
	}
	
	
	public void open(@NotNull UUID playerUniqueId) {
		if (this.geyserForm.isPresent() && this.geyserForm.get().open(playerUniqueId)) { // bedrock player
			return;
		}
		ProtocolizePlayer player = Protocolize.playerProvider().player(playerUniqueId);
		if (player != null) {
			Optional<Integer> playerVersion = Optional.of(player.protocolVersion()).filter(i -> i < -1);
			SenderWrapper<T, U, C> swrapper = this.plugin.getSenderWrapper();
			Inventory inv = new Inventory(InventoryType.chestInventoryWithRows(Config.GUI_COMMAND_ROWS.getInt()));
			inv.title(ChatElement.of(swrapper.componentToBase(this.title.get(ChatVersion.getCorrect(playerVersion)))));
			this.setupIcons(inv, player);
			inv.onClose(close -> this.openedGUIs.remove(close.player().uniqueId(), inv));
			inv.onClick(click -> {
				if (click.clickedItem() instanceof ServerItemStack<?> srvItem
						&& (click.clickType() == ClickType.RIGHT_CLICK || click.clickType() == ClickType.LEFT_CLICK)) {
					Optional<U> proxyPlayer = this.plugin.getSenderWrapper().getPlayer(click.player().uniqueId());
					if (proxyPlayer.isPresent()) {
						this.openedGUIs.remove(click.player().uniqueId(), inv);
						click.player().closeInventory();
						swrapper.connectToServer(proxyPlayer.get(), MiscUtil.random(srvItem.getServerNames()));
					}
				}
				click.cancelled(true);
			});
			player.openInventory(inv);
			openedGUIs.put(playerUniqueId, inv);
		}
		
	}
	
	
	private Inventory setupIcons(@NotNull Inventory inv, @NotNull ProtocolizePlayer player) {
		// background
		this.background.forEach(inv::item);
		
		// icons
		this.plugin.getServerPingManager().getAvailableServerIcons(player.uniqueId(), player.protocolVersion())
				.forEach(icon -> inv.item(icon.getSlot(), new ServerItemStack<>(
						Optional.of(player.protocolVersion()),
						this.format,
						icon,
						this.plugin,
						(short) 0)));
		return inv;
	}
	
	
	public void update() {
		this.openedGUIs.entrySet().parallelStream().forEach(entry -> {
			ProtocolizePlayer pl = Protocolize.playerProvider().player(entry.getKey());
			if (pl != null) pl.openInventory(this.setupIcons(entry.getValue(), pl));
		});
	}
	

}
