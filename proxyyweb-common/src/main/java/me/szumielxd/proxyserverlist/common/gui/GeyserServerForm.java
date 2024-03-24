package me.szumielxd.proxyserverlist.common.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.util.FormImage;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.jetbrains.annotations.NotNull;

import me.szumielxd.proxyserverlist.common.ProxyServerList;
import me.szumielxd.proxyserverlist.common.configuration.Config;
import me.szumielxd.proxyserverlist.common.utils.MiscUtil;
import me.szumielxd.proxyyweb.common.objects.SenderWrapper;

public class GeyserServerForm<T, U extends T> {
	
	
	private final @NotNull ProxyServerList<T, U, ?> plugin;
	private final @NotNull String title;
	private final @NotNull String content;
	
	
	public GeyserServerForm(@NotNull ProxyServerList<T, U, ?> plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
		this.title = Config.GEYSER_FORM_TITLE.getString();
		this.content = Config.GEYSER_FORM_CONTENT.getString();
	}
	
	
	public boolean open(@NotNull UUID playerUniqueId) {
		
		GeyserSession session = GeyserImpl.getInstance().connectionByUuid(playerUniqueId);
		if (session != null) {
			
			SimpleForm.Builder builder = SimpleForm.builder()
					.title(this.title)
					.content(this.content);
			
			List<List<String>> availableServers = new ArrayList<>(); 
			
			SenderWrapper<T, U, ?> swrapper = this.plugin.getSenderWrapper();
			this.plugin.getServerPingManager().getAvailableServerIcons(playerUniqueId,
					swrapper.getProtocolVersion(swrapper.getPlayer(playerUniqueId)
							.orElseThrow(IllegalStateException::new)))
					.forEach(icon -> {
						if (icon.getGeyserImage() != null) builder.button(icon.getFriendlyName(), FormImage.Type.URL, icon.getGeyserImage());
						else builder.button(icon.getFriendlyName());
						availableServers.add(icon.getNames());
						
					});
			builder.responseHandler((form, responseString) -> {
				SimpleFormResponse response = form.parseResponse(responseString);
				if (response.isCorrect()) {
					Optional<U> proxyPlayer = this.plugin.getSenderWrapper().getPlayer(playerUniqueId);
					if (proxyPlayer.isPresent() && response.getClickedButtonId() < availableServers.size()) {
						swrapper.connectToServer(proxyPlayer.get(),
								MiscUtil.random(availableServers.get(response.getClickedButtonId())));
					}
				}
			});
			session.sendForm(builder);
			
			return true;
		}
		return false;
		
	}
	

}
