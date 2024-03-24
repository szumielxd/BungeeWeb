package me.szumielxd.proxyserverlist.common;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConditionManagerr {
	
	
	private static final Map<String, BiFunction<String, String, AbstractCondition>> CONDITIONS = Map.ofEntries(
			Map.entry(BiggerCondition.ID, BiggerCondition::new),
			Map.entry(BiggerEqualCondition.ID, BiggerEqualCondition::new),
			Map.entry(SmallerCondition.ID, SmallerCondition::new),
			Map.entry(SmallerEqualCondition.ID, SmallerEqualCondition::new),
			Map.entry(EqualCondition.ID, EqualCondition::new),
			Map.entry(NotEqualCondition.ID, NotEqualCondition::new),
			Map.entry(EqualIgnoreCaseCondition.ID, EqualIgnoreCaseCondition::new),
			Map.entry(NotEqualIgnoreCaseCondition.ID, NotEqualIgnoreCaseCondition::new),
			Map.entry(ContainsCondition.ID, ContainsCondition::new),
			Map.entry(NotContainsCondition.ID, NotContainsCondition::new));
	
	private static final Pattern CONDITION_PATTERN = Pattern.compile("(\\w+?|\"[\\w ]+?\") ?("
			+ CONDITIONS.keySet().stream()
					.map(Pattern::quote)
					.collect(Collectors.joining("|"))
			+ ") ?(.+|\".+\")");
	
	
	public static Optional<AbstractCondition> tryParse(String condition) {
		Matcher match = CONDITION_PATTERN.matcher(condition);
		if (match.matches()) {
			String prefix = match.group(1);
			if (prefix.startsWith("\"") && prefix.endsWith("\"")) prefix = prefix.substring(1, prefix.length()-1);
			String suffix = match.group(3);
			if (suffix.startsWith("\"") && suffix.endsWith("\"")) suffix = suffix.substring(1, suffix.length()-1);
			return Optional.of(CONDITIONS.get(match.group(2)).apply(prefix, suffix));
		} return Optional.empty();
	}
	
	
	
	@AllArgsConstructor
	public abstract static class AbstractCondition implements Predicate<UnaryOperator<String>> {
		
		protected @Getter @NotNull String leftSide;
		protected @Getter @NotNull String rightSide;
		
		public abstract @NotNull String getId();
		
		public @NotNull String serialize() {
			return "\"" + this.leftSide + "\" " + this.getId() + " \"" + this.rightSide + "\"";
		}

	}
	
	private static class BiggerCondition extends AbstractCondition {
		
		static final @NotNull String ID = ">";

		public BiggerCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			try {
				return Integer.parseInt(replacer.apply(this.leftSide)) > Integer.parseInt(replacer.apply(this.rightSide));
			} catch (NumberFormatException e) {
				return false;
			}
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	
	/*
	 * [<>!][=~^]
	 * 
	 * <
	 * <=
	 * >
	 * >=
	 * =
	 * !=
	 * ~
	 * !~
	 * ^
	 * ~^
	 * 
	 */
	
	private static class BiggerEqualCondition extends AbstractCondition {
		
		static final @NotNull String ID = ">=";

		public BiggerEqualCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			try {
				return Integer.parseInt(replacer.apply(this.leftSide)) >= Integer.parseInt(replacer.apply(this.rightSide));
			} catch (NumberFormatException e) {
				return false;
			}
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	
	private static class SmallerCondition extends AbstractCondition {
		
		static final @NotNull String ID = "<";

		public SmallerCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			try {
				return Integer.parseInt(replacer.apply(this.leftSide)) < Integer.parseInt(replacer.apply(this.rightSide));
			} catch (NumberFormatException e) {
				return false;
			}
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	
	private static class SmallerEqualCondition extends AbstractCondition {
		
		static final @NotNull String ID = "<=";

		public SmallerEqualCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			try {
				return Integer.parseInt(replacer.apply(this.leftSide)) < Integer.parseInt(replacer.apply(this.rightSide));
			} catch (NumberFormatException e) {
				return false;
			}
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	
	private static class EqualCondition extends AbstractCondition {
		
		static final @NotNull String ID = "=";

		public EqualCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return replacer.apply(this.leftSide).equals(replacer.apply(this.rightSide));
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	
	private static class NotEqualCondition extends AbstractCondition {
		
		static final @NotNull String ID = "!=";

		public NotEqualCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return !replacer.apply(this.leftSide).equals(replacer.apply(this.rightSide));
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	
	private static class EqualIgnoreCaseCondition extends AbstractCondition {
		
		static final @NotNull String ID = "~";

		public EqualIgnoreCaseCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return replacer.apply(this.leftSide).equalsIgnoreCase(replacer.apply(this.rightSide));
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	
	private static class NotEqualIgnoreCaseCondition extends AbstractCondition {
		
		static final @NotNull String ID = "!~";

		public NotEqualIgnoreCaseCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return !replacer.apply(this.leftSide).equalsIgnoreCase(replacer.apply(this.rightSide));
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	
	private static class ContainsCondition extends AbstractCondition {
		
		static final @NotNull String ID = "^";

		public ContainsCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return replacer.apply(this.leftSide).contains(replacer.apply(this.rightSide));
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	
	private static class NotContainsCondition extends AbstractCondition {
		
		static final @NotNull String ID = "!^";

		public NotContainsCondition(String leftSide, String rightSide) {
			super(leftSide, rightSide);
		}

		@Override
		public boolean test(UnaryOperator<String> replacer) {
			return !replacer.apply(this.leftSide).contains(replacer.apply(this.rightSide));
		}
		
		@Override
		public @NotNull String getId() {
			return ID;
		}
	}
	

}
