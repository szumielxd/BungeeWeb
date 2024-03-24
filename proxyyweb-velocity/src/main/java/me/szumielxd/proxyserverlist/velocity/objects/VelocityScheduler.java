package me.szumielxd.proxyserverlist.velocity.objects;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.scheduler.ScheduledTask;

import me.szumielxd.proxyserverlist.velocity.ProxyServerListVelocity;
import me.szumielxd.proxyyweb.common.objects.CommonScheduler;

public class VelocityScheduler implements CommonScheduler {
	
	
	private final @NotNull ProxyServerListVelocity plugin;
	
	
	private final Map<Integer, VelocityExecutedTask> activeTasks = new ConcurrentHashMap<>();
	private final AtomicInteger lastId = new AtomicInteger(0);
	
	
	public VelocityScheduler(@NotNull ProxyServerListVelocity plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
	}
	

	@Override
	public @NotNull VelocityExecutedTask runTask(@NotNull Runnable task) {
		final int id = this.lastId.addAndGet(1);
		VelocityExecutedTask ex = new VelocityExecutedTask(id, this.plugin.getProxy().getScheduler().buildTask(this.plugin, () -> {
			// remove task from list after finish
			task.run();
			this.activeTasks.remove(id);
		}).schedule());
		this.activeTasks.put(id, ex);
		return ex;
	}

	@Override
	public @NotNull VelocityExecutedTask runTaskLater(@NotNull Runnable task, long delay, @NotNull TimeUnit unit) {
		final int id = this.lastId.addAndGet(1);
		VelocityExecutedTask ex = new VelocityExecutedTask(id, this.plugin.getProxy().getScheduler().buildTask(this.plugin, () -> {
			// remove task from list after finish
			task.run();
			this.activeTasks.remove(id);
		}).delay(delay, unit).schedule());
		this.activeTasks.put(id, ex);
		return ex;
	}

	@Override
	public @NotNull VelocityExecutedTask runTaskTimer(@NotNull Runnable task, long delay, long period, @NotNull TimeUnit unit) {
		
		final int id = this.lastId.addAndGet(1);
		VelocityExecutedTask ex = new VelocityExecutedTask(id, this.plugin.getProxy().getScheduler().buildTask(this.plugin, () -> {
			// this is periodical task, so it's never ending fun! No need to be removed from list until cancelled
			task.run();
		}).delay(delay, unit).repeat(period, unit).schedule());
		this.activeTasks.put(id, ex);
		return ex;
	}

	@Override
	public void cancel(int id) {
		Optional.ofNullable(this.activeTasks.get(id)).ifPresent(ExecutedTask::cancel);
	}

	@Override
	public void cancelAll() {
		this.activeTasks.values().forEach(ExecutedTask::cancel);
	}
	
	
	public final class VelocityExecutedTask implements ExecutedTask {

		
		private final int id;
		private final @NotNull ScheduledTask task;
		
		
		public VelocityExecutedTask(int id, @NotNull ScheduledTask task) {
			this.id = id;
			this.task = Objects.requireNonNull(task, "task cannot be null");
		}
		
		
		@Override
		public void cancel() {
			activeTasks.remove(this.id);
			this.task.cancel();
		}

		@Override
		public boolean isSync() {
			return false;
		}

		@Override
		public int getId() {
			return this.id;
		}
		
	}
	

}
