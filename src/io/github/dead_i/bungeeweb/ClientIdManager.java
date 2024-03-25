package io.github.dead_i.bungeeweb;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.hikari.HikariDB;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientIdManager {
	
	private final @NotNull Map<String, Long> clientIds = new ConcurrentHashMap<>();
	private final @NotNull Map<String, CompletableFuture<Long>> clientIdQueries = new ConcurrentHashMap<>();
	private @NotNull HikariDB hikari;
	
	
	public long getClientId(@NotNull String clientName) {
		String lowerName = clientName.toLowerCase();
		Long id = this.clientIds.get(lowerName);
		if (id == null) {
			try {
				return clientIdQueries.computeIfAbsent(lowerName,
						name -> CompletableFuture.supplyAsync(
								() -> {
									long clientId = hikari.getOrCreateClientId(name);
									clientIds.put(lowerName, clientId);
									clientIdQueries.remove(lowerName);
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
	
	public long getDefaultClientId() {
		return this.getClientId(PlayerInfoManager.DEFAULT_CLIENT);
	}

}
