package dfh.anagrammar.node;

import dfh.anagrammar.Trie;

/**
 * Represents a list of words.
 * 
 * @author houghton
 *
 */
public class Terminal extends Node {
	public Trie trie;
	public String listName;
	
	public Terminal() {
	}
	
	@Override
	protected Node dupSelf() {
		return new Terminal(listName);
	}

	public Terminal(String listName) {
		this.listName = listName;
	}

	String graphvizSpec() {
		return graphvizID() + " [label=\"" + listName.replaceAll("\"", "\\\"") + "\"];";
	}

	@Override
	protected Pipe reduce() {
		Node n = dup();
		return new Pipe(n, n);
	}
}
