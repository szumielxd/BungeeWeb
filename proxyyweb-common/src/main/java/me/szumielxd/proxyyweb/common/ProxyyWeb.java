package me.szumielxd.proxyyweb.common;

import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import jakarta.servlet.http.HttpServletRequest;
import me.szumielxd.proxyyweb.common.hikari.HikariDB;
import me.szumielxd.proxyyweb.common.objects.CommonScheduler;
import me.szumielxd.proxyyweb.common.objects.ComponentMapper;
import me.szumielxd.proxyyweb.common.objects.SenderWrapper;

public interface ProxyyWeb<T, U extends T, C> {
	
	public @NotNull Logger getLogger();
	
	public void reloadConfig();
	
	public @NotNull PlayerInfoManager getPlayerInfoManager();
	
	public @NotNull ServerIdManager getServerIdManager();
	
	public @NotNull ClientIdManager getClientIdManager();
	
	public @NotNull HikariDB<T> getDatabaseManager();
	
	public @NotNull CommonScheduler getScheduler();
	
	public @NotNull SenderWrapper<T, U, C> getSenderWrapper();
	
	public @NotNull ComponentMapper<C> getComponentMapper();
	

	public static int getGroupPower(HttpServletRequest req) {
		int group = (Integer) req.getSession().getAttribute("group");
		if (group >= 3) group++;
		return group;
	}

	public static boolean isNumber(String number) {
		try {
			return Long.parseLong(number) >= 0;
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

}
