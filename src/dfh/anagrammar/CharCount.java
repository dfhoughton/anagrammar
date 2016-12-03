package dfh.anagrammar;

import java.util.Arrays;

public class CharCount {
	int n = 0;
	final int[] counts;

	CharCount(int countSize) {
		counts = new int[countSize];
	}

	CharCount dup() {
		return new CharCount(n, counts);
	}

	/**
	 * @param i
	 * @return whether there are any characters with index i
	 */
	boolean has(int i) {
		return n > 0 && i < counts.length && counts[i] > 0;
	}

	/**
	 * @param i
	 *            translated character to decrement
	 * @return appropriately decremented duplicate character count, or null if
	 *         this particular count cannot be decremented
	 */
	CharCount decrement(int i) {
		if (i < counts.length && counts[i] > 0) {
			CharCount d = dup();
			d.counts[i]--;
			d.n--;
			return d;
		} else {
			return null;
		}
	}

	boolean empty() {
		return n == 0;
	}

	private CharCount(int n, int[] counts) {
		this.n = n;
		this.counts = Arrays.copyOf(counts, counts.length);
	}
}
