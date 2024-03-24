package me.szumielxd.proxyserverlist.velocity.objects;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;

import me.szumielxd.proxyyweb.common.objects.ComponentMapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class VelocityComponentMapper implements ComponentMapper<Component> {
	
	private static final @NotNull Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-z\\d]+)\\}", Pattern.CASE_INSENSITIVE);

	@Override
	public @NotNull Component jsonToComponent(@NotNull JsonElement json) {
		return GsonComponentSerializer.gson().deserializeFromTree(json);
	}

	@Override
	public @NotNull Component jsonStringToComponent(@NotNull String json) {
		return GsonComponentSerializer.gson().deserialize(json);
	}

	@Override
	public @NotNull JsonElement componentToJson(@NotNull Component component) {
		return GsonComponentSerializer.gson().serializeToTree(component);
	}

	@Override
	public @NotNull String componentToJsonString(@NotNull Component component) {
		return GsonComponentSerializer.gson().serialize(component);
	}

	@Override
	public @NotNull Component replaceText(@NotNull Component component, @NotNull String needle, String replacement) {
		return component.replaceText(b -> b.matchLiteral(needle).replacement(replacement));
	}

	@Override
	public @NotNull Component plainText(@NotNull String text) {
		return Component.text(text);
	}

	@Override
	public @NotNull Component parsePlaceholders(@NotNull Component comp, Map<String, Component> replacements) {
		comp = comp.replaceText(b -> b.match(PLACEHOLDER_PATTERN)
				.replacement((match, cb) -> Optional.ofNullable(replacements.get(match.group(1))).orElseGet(() -> Component.text(match.group(0)))));
		return comp.children(comp.children().stream().map(c -> parsePlaceholders(c, replacements)).toList());
	}

	@Override
	public @NotNull Component parsePlaceholdersInHover(@NotNull Component comp, Map<String, Component> replacements) {
		if (comp.hoverEvent() != null && comp.hoverEvent().value() instanceof Component hover) {
			comp = comp.hoverEvent(parsePlaceholders(hover, replacements));
		}
		return comp;
	}

}
