package dfh.anagrammar;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

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

	private WorkInProgress(Trie t, Node n, CharCount cc, WorkInProgress previous, boolean overEdge) {
		this.t = t;
		this.n = n;
		this.cc = cc;
		this.previous = previous;
		this.overEdge = overEdge;
	}

	public WorkInProgress(Terminal n, CharCount cc) {
		this(n.trie, n, cc.dup(), null, true);
	}
	
	public void work(Deque<WorkInProgress> queue) {
		if (n instanceof End) {
			((End) n).getOutput().add(this);
		} else {
			for (Node o: n.edges) {
				if (o instanceof End) {
					End e = (End) o;
					e.getOutput().add(new WorkInProgress(t, n , cc, this, false));
				} else {
					Terminal term = (Terminal) o;
					for (int i: term.trie.jumpList) {
						CharCount cc2 = cc.decrement(i);
						if (cc2 != null) {
							Trie t2 = term.trie.children[i];
							WorkInProgress wip = new WorkInProgress(t2, term, cc2, this, false);
							queue.addFirst(wip);
							if (t2.terminal && t2.children.length > 0) {
								wip = new WorkInProgress(t2, term, cc2, this, false);
							}
						}
					}
					if (t.terminal) {
						for (Node o2: term.edges) {
							if (o2 instanceof Terminal) {
								Terminal term2 = (Terminal) o2;
								WorkInProgress wip = new WorkInProgress(term2.trie, o2, cc, this, true);
								queue.addFirst(wip);
							} else { // must be End
								End e = (End) o2;
								e.getOutput().add(new WorkInProgress(t, n , cc, this, true));
							}
						}
					}
				}
			}
		}
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
