package me.szumielxd.proxyserverlist.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

@UtilityClass
public class MiscUtil {
	
	
	private static char[] randomCharset = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	
	/**
	 * The special character which prefixes all chat colour codes. Use this if
	 * you need to dynamically convert colour codes from your custom format.
	 */
	public static final char COLOR_CHAR = '\u00A7';
	public static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";
	public static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
			.strict(false)
			.build();
	
	private static final Random RANDOM = new Random();
	
	
	/*public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
		char[] b = textToTranslate.toCharArray();
		for ( int i = 0; i < b.length - 1; i++ )
		{
			if ( b[i] == altColorChar && ALL_CODES.indexOf( b[i + 1] ) > -1 )
			{
				b[i] = COLOR_CHAR;
				b[i + 1] = Character.toLowerCase( b[i + 1] );
			}
		}
		return new String( b );
	}*/
	
	
	/*public static Component deepReplace(Component comp, final String match, final Object replacement) {
		final String rep = replacement instanceof ComponentLike compLike ?
				LegacyComponentSerializer.legacyAmpersand().serialize((compLike).asComponent())
				: String.valueOf(replacement);
		if (comp.clickEvent() != null) {
			ClickEvent click = comp.clickEvent();
			comp = comp.clickEvent(ClickEvent.clickEvent(click.action(), click.value().replace("{"+match+"}", rep)));
		}
		if (comp.insertion() != null) comp = comp.insertion(comp.insertion().replace("{"+match+"}", rep));
		ArrayList<Component> child = new ArrayList<>(comp.children());
		if (!child.isEmpty()) {
			child.replaceAll(c -> deepReplace(c, match, replacement));
			comp = comp.children(child);
		}
		return comp;
	}*/
	
	
	/*public static Component parseComponent(@Nullable String text, boolean legacy, boolean emptyAsNull) {
		if (text == null || (text.isEmpty() && emptyAsNull)) return null;
		try {
			// JSON
			return (legacy ? GSON_LEGACY_SERIALIZER : GSON_SERIALIZER).deserializeFromTree(new Gson().fromJson(text, JsonObject.class));
		} catch (JsonParseException e) {
			text = text.replace(COLOR_CHAR, '&');
			Component comp = MINI_MESSAGE.deserialize(text);
			LegacyComponentSerializer serializer = legacy ? ALT_LEGACY_SERIALIZER : ALT_SERIALIZER;
			String str = serializer.serialize(comp);
			// MiniMessage
			if (!str.equalsIgnoreCase(text.replace("\\n", "\n"))) return comp;
			// Legacy
			return serializer.deserializeOr(MiscUtil.translateAlternateColorCodes('&', text).replace("\\n", "\n"), Component.text("INVALID").color(NamedTextColor.RED));
		}
	}*/
	
	
	public static String getPlainVisibleText(Component component) {
		Objects.requireNonNull(component, "component cannot be null");
		StringBuilder sb = new StringBuilder();
		if (component instanceof TextComponent text) sb.append(text.content());
		component.children().forEach(c -> sb.append(getPlainVisibleText(c)));
		return sb.toString();
	}
	
	
	public static String randomString(int length) {
		StringBuilder sb = new StringBuilder();
		RANDOM.ints(length, 0, randomCharset.length-1).forEach(cons ->{
			sb.append(randomCharset[cons]);
		});
		return sb.toString();
	}
	
	
	public <E> @Nullable E random(@NotNull List<E> list) {
		if (list.isEmpty()) return null;
		return list.get(RANDOM.nextInt(list.size()));
	}
	
	
	public static @NotNull String formatTimespan(long seconds) {
		long sec = seconds % 60;
		seconds /= 60;
		long min = seconds % 60;
		seconds /= 60;
		StringBuilder time = new StringBuilder(sec + "s");
		if (min > 0) time.insert(0, min + "m ");
		if (seconds > 0) time.insert(0, seconds + "h ");
		return time.toString();
	}
	
	
	public static <K, V> @NotNull Map<K, V> merge(@NotNull Map<K, V> map1, @NotNull Map<K, V> map2) {
		return Stream.concat(map1.entrySet().stream(), map2.entrySet().stream())
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}
	
	
	public static String parseOnlyDate(long timestamp) {
		return new SimpleDateFormat("dd-MM-yyyy").format(new Date(timestamp));
	}
	
	
	public static String parseOnlyTime(long timestamp) {
		return new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));
	}
	

}
