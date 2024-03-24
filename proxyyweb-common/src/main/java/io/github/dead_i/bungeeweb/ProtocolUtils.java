package io.github.dead_i.bungeeweb;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

public class ProtocolUtils {
	
	private static final @NotNull Pattern PROTOCOL_PATTERN = Pattern.compile("MINECRAFT_(\\d+)(_(\\d+)(_(\\d+))?)?");
	public static final @NotNull Map<Integer, String> PROTOCOL_MAPPING;
	
	static {
		Map<Integer, String> mappings = Collections.unmodifiableMap(Map.of());
		try {
			Class<?> clazz = Class.forName("net.md_5.bungee.protocol.ProtocolConstants");
			mappings = Stream.of(clazz.getFields())
					.filter(f -> int.class.isAssignableFrom(f.getType()))
					.map(f -> {
						try {
							return new Pair<>(f.getInt(null), PROTOCOL_PATTERN.matcher(f.getName()));
						} catch (IllegalAccessException e) {
							throw new RuntimeException(e);
						}
					})
					.filter(p -> p.right().matches())
					.collect(Collectors.toUnmodifiableMap(p -> p.left(), p -> {
						StringBuilder version = new StringBuilder(p.right().group(1));
						for (int i = 0; i < 2; i++) {
							String part = p.right().group(3 + i * 2);
							if (part != null) {
								version.append('.').append(part);
							}
						}
						return version.toString();
					}));
		} catch (Exception e) {
			// something is wrong... No protocol mapping today :c
		}
		PROTOCOL_MAPPING = mappings;
	}
	
	public static @NotNull Optional<String> getProtocolName(int protocolId) {
		return Optional.ofNullable(PROTOCOL_MAPPING.get(protocolId));
	}
	
	private record Pair<T, S>(T left, S right) {}

}
