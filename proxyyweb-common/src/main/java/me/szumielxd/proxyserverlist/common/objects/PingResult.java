package me.szumielxd.proxyserverlist.common.objects;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import me.szumielxd.legacyminiadventure.VersionableObject;

@ToString
@AllArgsConstructor
public class PingResult<C> {
	
	private @Getter @Setter @NonNull Version version;
	private @Getter @Setter @NonNull Optional<PlayersList> players;
	private @Getter @Setter @NonNull VersionableObject<C> description;
	private @Getter @Setter int ping;
	
	public PingResult<C> copy() {
		return new PingResult<>(this.version.copy(), this.players.map(PlayersList::copy), this.description, this.ping);
	}
	
	public PingResult<C> unmodifiable() {
		return new UnmodifiablePingResult<>(this.version.unmodifiable(), this.players.map(PlayersList::unmodifiable), this.description, this.ping);
	}
	
	public static class UnmodifiablePingResult<C> extends PingResult<C> {

		public UnmodifiablePingResult(@NonNull Version version, @NonNull Optional<PlayersList> players, @NonNull VersionableObject<C> description, int ping) {
			super(version, players, description, ping);
		}
		@Override
		public void setVersion(@NonNull Version version) {
			throw new UnsupportedOperationException("cannot modify this object");
		}
		@Override
		public void setPlayers(@NonNull Optional<PlayersList> players) {
			throw new UnsupportedOperationException("cannot modify this object");
		}
		@Override
		public void setDescription(@NonNull VersionableObject<C> description) {
			throw new UnsupportedOperationException("cannot modify this object");
		}
		@Override
		public void setPing(int ping) {
			throw new UnsupportedOperationException("cannot modify this object");
		}
	}
	
	
	@ToString
	@AllArgsConstructor
	public static class Version {
		
		private @Getter @Setter int protocol;
		private @Getter @Setter @Nullable String name;
		
		public Version copy() {
			return new Version(this.protocol, this.name);
		}
		
		public Version unmodifiable() {
			return new UnmodifiableVersion(this.protocol, this.name);
		}
		
		private static class UnmodifiableVersion extends Version {

			public UnmodifiableVersion(int protocol, @Nullable String name) {
				super(protocol, name);
			}
			@Override
			public void setProtocol(int protocol) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
			@Override
			public void setName(@Nullable String name ) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
		}
		
	}
	
	
	@ToString
	@AllArgsConstructor
	public static class PlayersList {
		
		private @Getter @Setter int max;
		private @Getter @Setter int online;
		private @Getter @Setter List<SamplePlayer> players;
		
		public PlayersList copy() {
			return new PlayersList(this.max, this.online, this.players.stream()
					.map(SamplePlayer::copy)
					.toList());
		}
		
		public PlayersList unmodifiable() {
			return new UnmodifiablePlayersList(this.max, this.online, this.players.stream()
					.map(SamplePlayer::unmodifiable)
					.toList());
		}
		
		public static PlayersList emptyList() {
			return new PlayersList(0, 0, List.of());
		}
		
		private static class UnmodifiablePlayersList extends PlayersList {

			public UnmodifiablePlayersList(int max, int online, List<SamplePlayer> players) {
				super(max, online, players);
			}
			@Override
			public void setMax(int max) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
			@Override
			public void setOnline(int online) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
			@Override
			public void setPlayers(List<SamplePlayer> players) {
				throw new UnsupportedOperationException("cannot modify this object");
			}
		}
		
		
		@ToString
		@AllArgsConstructor
		public static class SamplePlayer {
			
			private @Getter @Setter @Nullable UUID uniqueId;
			private @Getter @Setter @Nullable String name;
			
			public SamplePlayer copy() {
				return new SamplePlayer(this.uniqueId, this.name);
			}
			
			public SamplePlayer unmodifiable() {
				return new UnmodifiableSamplePlayer(this.uniqueId, this.name);
			}
			
			private static class UnmodifiableSamplePlayer extends SamplePlayer {

				public UnmodifiableSamplePlayer(@Nullable UUID uniqueId, @Nullable String name) {
					super(uniqueId, name);
				}
				@Override
				public void setUniqueId(@Nullable UUID uniqueId) {
					throw new UnsupportedOperationException("cannot modify this object");
				}
				@Override
				public void setName(@Nullable String name) {
					throw new UnsupportedOperationException("cannot modify this object");
				}
			}
			
		}
		
	}
	

}
