package dfh.anagrammar;

import java.util.Arrays;

public class Trie {
	public String[] values = new String[0];
	Trie[] children = new Trie[0];
	int[] jumpList;
	boolean terminal = false;

	/**
	 * Prepare this and all the {@link Trie} children for use.
	 */
	public void done() {
		int count = 0;
		for (Trie t : children) {
			if (t != null) {
				count++;
				t.done();
			}
		}
		jumpList = new int[count];
		for (int i = 0, j = 0; i < children.length; i++) {
			Trie t = children[i];
			if (t != null) {
				jumpList[j++] = i;
			}
		}
	}
	
	/**
	 * Store a word in the trie. Multiple words may be stored in the same node
	 * if they all normalize down to the same translation.
	 * 
	 * @param word
	 *            word being translated
	 * @param translation
	 *            word translated into integers
	 * @param offset
	 *            particular integer within translation being considered
	 */
	public void add(String word, int[] translation, int offset) {
		if (offset == translation.length) {
			for (int i = 0; i < values.length; i++) {
				if (values[i] == word)
					return;
			}
			this.values = Arrays.copyOf(values, values.length + 1);
			this.values[values.length - 1] = word;
			this.terminal = true;
		} else {
			int i = translation[offset++];
			if (i >= children.length) {
				this.children = Arrays.copyOf(children, i + 1);
			}
			Trie t = children[i];
			if (t == null) {
				t = new Trie();
				children[i] = t;
			}
			t.add(word, translation, offset);
		}
	}
}
