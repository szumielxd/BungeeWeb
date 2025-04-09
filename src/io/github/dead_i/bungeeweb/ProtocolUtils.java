package io.github.dead_i.bungeeweb;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.network.ProtocolVersion;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProtocolUtils {
	
	public static @NotNull Optional<String> getProtocolName(int protocolId) {
		return Optional.of(ProtocolVersion.getProtocolVersion(protocolId))
				.filter(v -> !v.isUnknown())
				.map(ProtocolVersion::getVersionIntroducedIn);
	}

}
