package dfh.anagrammar;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingDeque;

import dfh.anagrammar.node.End;
import dfh.anagrammar.node.Node;
import dfh.anagrammar.node.Terminal;

/**
 * Holds information pertinent to a partially constructed anagram.
 * 
 * @author houghton
 *
 */
public class WorkInProgress {
	final Trie t;
	final Node n;
	final CharCount cc;
	final WorkInProgress previous;
	final boolean overEdge;

	 WorkInProgress(Trie t, Node n, CharCount cc, WorkInProgress previous, boolean overEdge) {
		this.t = t;
		this.n = n;
		this.cc = cc;
		this.previous = previous;
		this.overEdge = overEdge;
	}

	public WorkInProgress(Terminal n, CharCount cc) {
		this(n.trie, n, cc.dup(), null, true);
	}

	/**
	 * @return all the phrases that can be made with this linked sequence of
	 *         works in progress
	 */
	public List<List<String>> phrases() {
		return phrases(null, true);
	}

	private List<List<String>> phrases(List<List<String>> p, boolean terminal) {
		if (terminal) {
			if (p == null) {
				p = new ArrayList<>(t.values.length);
				for (int i = 0; i < t.values.length; i++) {
					LinkedList<String> phrase = new LinkedList<>();
					phrase.add(t.values[i]);
					p.add(phrase);
				}
			} else {
				if (t.values.length == 1) {
					for (List<String> phrase : p) {
						phrase.add(0, t.values[0]);
					}
				} else {
					List<List<String>> p2 = new ArrayList<>(t.values.length * p.size());
					for (String s : t.values) {
						for (List<String> phrase : p) {
							LinkedList<String> phrase2 = new LinkedList<>(phrase);
							phrase2.addFirst(s);
							p2.add(phrase2);
						}
					}
					p = p2;
				}
			}
		}
		if (previous == null)
			return p;
		return previous.phrases(p, overEdge);
	}
}
