package me.szumielxd.proxyyweb.common;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProxyyWebProvider {
	
	
	private static @Nullable ProxyyWeb<?, ?, ?> instance = null;
	
	
	public static void init(@NotNull ProxyyWeb<?, ?, ?> instance) {
		ProxyyWebProvider.instance = Objects.requireNonNull(instance, "instance cannot be null");
	}
	
	
	public static @NotNull ProxyyWeb<?, ?, ?> get() {
		if (instance == null) {
			throw new IllegalArgumentException("ProxyyWeb is not initialized");
		}
		return instance;
	}
	

}
