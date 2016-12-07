package dfh.anagrammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CharMap {
	private final Map<Character, Integer> map;
	private final Normalizer normalizer;
	private final int countSize;
	private CharCount cc;

	public interface Normalizer {
		/**
		 * @param c
		 * @return null character for characters to ignore, otherwise another
		 *         character
		 */
		char normalize(char c);
	}

	public static final Normalizer BASIC_NORMALIZER = new Normalizer() {
		@Override
		public char normalize(char c) {
			int i = c;
			if (Character.isLetter(i)) {
				if (Character.isUpperCase(i)) {
					return (char) Character.toLowerCase(i);
				} else {
					return c;
				}
			} else {
				return 0;
			}
		}
	};

	public static class Builder {
		Map<Character, AtomicInteger> counter = new HashMap<>();
		private Normalizer normalizer;

		public Builder() {
			this.normalizer = CharMap.BASIC_NORMALIZER;
		}

		public Builder(Normalizer n) {
			this.normalizer = n;
		}

		public void add(List<String> words) {
			add(words.toArray(new String[words.size()]));
		}

		public void add(String[] words) {
			for (String w : words) {
				for (int i = 0; i < w.length(); i++) {
					char c = normalizer.normalize(w.charAt(i));
					if (c == 0)
						continue;
					AtomicInteger a = counter.get(c);
					if (a == null) {
						a = new AtomicInteger();
						counter.put(c, a);
					}
					a.incrementAndGet();
				}
			}
		}

		public CharMap build() {
			int biggest = -1;
			for (char c : counter.keySet()) {
				int i = (int) c;
				if (i > biggest) {
					biggest = i;
				}
			}
			int next = 0;
			List<Character> order = new ArrayList<Character>(counter.keySet());
			order.sort(new Comparator<Character>() {
				@Override
				public int compare(Character o1, Character o2) {
					int i1 = counter.get(o1).get(), i2 = counter.get(o2).get();
					return i2 - i1;
				}
			});
			Map<Character, Integer> m = new HashMap<>();
			for (char c : order) {
				m.put(c, next++);
			}
			return new CharMap(normalizer, m, biggest);
		}
	}

	private CharMap(Normalizer normalizer, Map<Character, Integer> m, int biggest) {
		this.normalizer = normalizer;
		this.map = m;
		this.countSize = biggest + 1;
	}

	public int[] translate(String word) {
		int[] t = new int[word.length()];
		int offset = 0;
		for (int i = 0; i < word.length(); i++) {
			char c = normalizer.normalize(word.charAt(i));
			if (c == 0)
				continue;
			Integer translation = map.get(c);
			if (translation == null)
				return null;
			t[offset++] = translation;
		}
		return Arrays.copyOf(t, offset);
	}

	public CharCount count(String word) {
		cc = new CharCount(countSize);
		for (int i = 0; i < word.length(); i++) {
			char c = normalizer.normalize(word.charAt(i));
			if (c == 0)
				continue;
			Integer idx = map.get(c);
			if (idx == null)
				return null;
			cc.counts[idx]++;
			cc.n++;
		}
		return cc;
	}

	/**
	 * @param words
	 *            a phrase represented as a list of words
	 * @return counts for whole phrase
	 */
	public CharCount count(List<String> words) {
		cc = new CharCount(countSize);
		for (String word : words) {
			for (int i = 0; i < word.length(); i++) {
				char c = normalizer.normalize(word.charAt(i));
				if (c == 0)
					continue;
				Integer idx = map.get(c);
				if (idx == null)
					return null;
				cc.counts[idx]++;
				cc.n++;
			}
		}
		return cc;
	}
}
