package me.szumielxd.proxyyweb.common;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;
import me.szumielxd.proxyyweb.common.hikari.HikariDB;

@RequiredArgsConstructor
public class ClientIdManager<T> {
	
	private final @NotNull Map<String, Long> clientIds = new ConcurrentHashMap<>();
	private final @NotNull Map<String, CompletableFuture<Long>> clientIdQueries = new ConcurrentHashMap<>();
	private @NotNull HikariDB<T> hikari;
	
	public long getDefaultClientId() {
		return this.getClientId("UNKNOWN");
	}
	
	public Map<String, Long> getClientIds() {
		this.hikari.getAllClientIds()
				.forEach((name, id) -> this.clientIds.put(name.toLowerCase(), id));
		return Collections.unmodifiableMap(this.clientIds);
	}
	
	public long getClientId(@NotNull String clientName) {
		String lowerName = clientName.toLowerCase();
		Long id = this.clientIds.get(lowerName);
		if (id == null) {
			try {
				return clientIdQueries.computeIfAbsent(lowerName,
						name -> CompletableFuture.supplyAsync(
								() -> {
									long clientId = hikari.getOrCreateClientId(name);
									clientIds.put(name, clientId);
									clientIdQueries.remove(name);
									return clientId;
								}))
						.get();
			} catch (ExecutionException e) {
				clientIdQueries.remove(lowerName);
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				clientIdQueries.remove(lowerName);
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}
		return id;
	}

}
