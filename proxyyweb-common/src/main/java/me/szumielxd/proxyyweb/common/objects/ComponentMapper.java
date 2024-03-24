package me.szumielxd.proxyyweb.common.objects;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonElement;

import me.szumielxd.legacyminiadventure.LegacyMiniadventure;
import me.szumielxd.legacyminiadventure.VersionableObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public interface ComponentMapper<T> {
	
	
	public @NotNull T jsonToComponent(@NotNull JsonElement json);
	
	public @NotNull T jsonStringToComponent(@NotNull String json);

	public @NotNull JsonElement componentToJson(@NotNull T component);

	public @NotNull String componentToJsonString(@NotNull T component);
	
	public @NotNull T replaceText(@NotNull T component, @NotNull String needle, String replacement);
	
	public @NotNull T plainText(@NotNull String text);
	
	public default @NotNull T empty() {
		return plainText("");
	}
	
	public default @NotNull T parseMiniMessage(@NotNull String message) {
		return this.kyoriToComponent(MiniMessage.miniMessage().deserialize(message));
	}
	
	public default @NotNull T kyoriToComponent(@NotNull Component component) {
		return this.jsonToComponent(GsonComponentSerializer.gson().serializeToTree(component));
	}
	
	public default @NotNull Component componentToKyori(@NotNull T component) {
		return GsonComponentSerializer.gson().deserializeFromTree(this.componentToJson(component));
	}
	
	public @NotNull T parsePlaceholders(@NotNull T comp, Map<String, T> replacements);
	
	public @NotNull T parsePlaceholdersInHover(@NotNull T comp, Map<String, T> replacements);
	
	public default @NotNull VersionableObject<T> parseLegacyMessage(@NotNull String message) {
		return LegacyMiniadventure.get().deserialize(message)
				.map(this::kyoriToComponent);
	}
	
	public default @NotNull T fullyParsePlaceholders(@NotNull T component, Map<String, T> replacements) {
		return parsePlaceholders(parsePlaceholdersInHover(component, replacements), replacements);
	}
	
	public default @NotNull VersionableObject<T> replacePlaceholders(@NotNull VersionableObject<T> message, Map<String, T> replacements) {
		return message.map(c -> fullyParsePlaceholders(c, replacements));
	}
	
	public default @NotNull VersionableObject<T> replacePlainPlaceholders(@NotNull VersionableObject<T> message, Map<String, String> replacements) {
		return replacePlaceholders(message, replacements.entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, e -> plainText(e.getValue()))));
	}
	

}
