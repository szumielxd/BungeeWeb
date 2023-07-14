package io.github.dead_i.bungeeweb;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.jetbrains.annotations.NotNull;

import io.github.dead_i.bungeeweb.hikari.HikariDB;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerIdManager {
	
	private final @NotNull Map<String, Long> serverIds = new ConcurrentHashMap<>();
	private final @NotNull Map<String, CompletableFuture<Long>> serverIdQueries = new ConcurrentHashMap<>();
	private @NotNull HikariDB hikari;
	
	
	public long getServerId(@NotNull String serverName) {
		String lowerName = serverName.toLowerCase();
		Long id = this.serverIds.get(lowerName);
		if (id == null) {
			try {
				return serverIdQueries.computeIfAbsent(lowerName,
						name -> CompletableFuture.supplyAsync(
								() -> {
									long serverId = hikari.getOrCreateServerId(name);
									serverIds.put(name, serverId);
									serverIdQueries.remove(name);
									return serverId;
								}))
						.get();
			} catch (ExecutionException e) {
				serverIdQueries.remove(lowerName);
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				serverIdQueries.remove(lowerName);
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}
		return id;
	}

}
