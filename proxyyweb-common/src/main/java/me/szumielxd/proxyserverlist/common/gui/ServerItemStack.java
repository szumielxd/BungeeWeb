package me.szumielxd.proxyserverlist.common.gui;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import dev.simplix.protocolize.api.chat.ChatElement;
import dev.simplix.protocolize.api.item.ItemStack;
import lombok.Getter;
import me.szumielxd.legacyminiadventure.LegacyMiniadventure;
import me.szumielxd.legacyminiadventure.VersionableObject.ChatVersion;
import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.configuration.SerializableServerDisplay;
import me.szumielxd.proxyserverlist.common.objects.CachedServerInfo;
import me.szumielxd.proxyyweb.common.objects.ComponentMapper;
import me.szumielxd.proxyyweb.common.objects.SenderWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class ServerItemStack<C> extends ItemStack {

	private final @Getter @NotNull List<String> serverNames;
	
	public ServerItemStack(@NotNull Optional<Integer> protocolVersion, @NotNull List<String> format, @NotNull SerializableServerDisplay icon, @NotNull ProxyServerList<?, ?, C> plugin, short durability) {
		super(Objects.requireNonNull(icon, "icon cannot be null").getType(), 1, durability);
		SenderWrapper<?, ?, C> senderWrapper = plugin.getSenderWrapper();
		ComponentMapper<C> component = plugin.getComponentMapper();
		Objects.requireNonNull(format, "format cannot be null");
		this.serverNames = icon.getNames();
		List<Component> formatComp = new LinkedList<>();
		ChatVersion chatVersion = ChatVersion.getCorrect(protocolVersion);
		TagResolver accentTag = Placeholder.styling("accent", icon.getAccent());
		format.stream()
				.map(s -> LegacyMiniadventure.get().deserialize(chatVersion, s, Map.of("name", icon.getFriendlyName()), accentTag))
				.forEachOrdered(line -> {
					AtomicBoolean matched = new AtomicBoolean(false);
					// very stupid containment check
					line.replaceText(builder -> builder.matchLiteral("{description}").replacement(b -> {
						matched.set(true);
							return Component.empty();
					}));
					if (matched.get()) {
						// append description
						icon.getDescription().forEach(desc -> formatComp.add(line.replaceText(
								builder -> builder.matchLiteral("{description}")
										.replacement(LegacyMiniadventure.get().deserialize(chatVersion, desc)))));
					} else {
						// append format line
						formatComp.add(line);
					}
				});
		
		List<CachedServerInfo<C>> pings = this.serverNames.stream()
				.map(senderWrapper.getPlugin().getServerPingManager()::getCachedServer)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
		int amount = icon.isUsePingedPlayers() ? pings.stream().mapToInt(CachedServerInfo::getOnline).sum()
				: this.serverNames.parallelStream().map(senderWrapper::getPlayers).filter(Optional::isPresent).map(Optional::get).mapToInt(Collection::size).sum();
		Component online = Component.text(amount);
		Component maxOnline = Component.text(pings.stream().mapToInt(CachedServerInfo::getMaxOnline).sum());
		String versions = pings.stream().map(CachedServerInfo::getVersionFriendlyName).distinct().collect(Collectors.joining(", "));
		int ping = pings.stream().mapToInt(CachedServerInfo::getPing).filter(i -> i > -1).max().orElse(-1);
		List<Object> description = formatComp.stream()
				.map(line -> line.decoration(TextDecoration.ITALIC, false))
				.map(line -> parse(line, online, maxOnline, versions, ping))
				.map(GsonComponentSerializer.gson()::serializeToTree)
				.map(component::jsonToComponent)
				.map(senderWrapper::componentToBase)
				.toList();
		if (description.isEmpty()) {
			this.displayName(ChatElement.ofLegacyText(""));
		} else {
			this.displayName(ChatElement.of(description.get(0)));
			description.subList(1, description.size()).stream()
					.map(ChatElement::of)
					.forEach(this::addToLore);
		}
		
		if (Config.GUI_COMMAND_PLAYERSASAMOUNT.getBoolean()) {
			this.amount((byte) Math.max(1, amount));
		}
		
	}
	
	
	private Component parse(Component comp, Component online, Component maxOnline, @NotNull String version, int ping) {
		Component pingComp = ping > -1 ? Component.text(ping + "ms") : Component.text("Offline", NamedTextColor.RED);
		return comp.replaceText(b -> b.matchLiteral("{online}").replacement(online))
				.replaceText(b -> b.matchLiteral("{maxonline}").replacement(maxOnline))
				.replaceText(b -> b.matchLiteral("{ping}").replacement(pingComp))
				.replaceText(b -> b.matchLiteral("{version}").replacement(version));
	}

}
