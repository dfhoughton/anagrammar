package dfh.anagrammar.node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dfh.anagrammar.Trie;

/**
 * A tuple of an input node and an output node.
 * 
 * @author houghton
 *
 */
public class Pipe {
	public Node in;
	public Node out;

	public Pipe(Node in, Node out) {
		this.in = in;
		this.out = out;
	}

	public String graphvizDOT() {
		return graphvizDOT("DFA");
	}

	/**
	 * @return all the list names for tries required to implement this network
	 */
	public Collection<String> requiredTries() {
		Set<String> names = new HashSet<>();
		for (Node n : in.connectedNodes()) {
			if (n instanceof Terminal) {
				Terminal t = (Terminal) n;
				names.add(t.listName);
			}
		}
		return names;
	}

	public void attachTries(Map<String, Trie> catalog) throws MissingWordlistException {
		for (Node n : in.connectedNodes()) {
			if (n instanceof Terminal) {
				Terminal t = (Terminal) n;
				Trie trie = catalog.get(t.listName);
				if (trie == null)
					throw new MissingWordlistException("cannot find wordlist " + t.listName);
				t.trie = trie;
			}
		}
	}

	public String graphvizDOT(String name) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("digraph ");
		buffer.append(name);
		buffer.append(" {\n");
		buffer.append("  label=\"").append(name).append("\";\n");
		buffer.append("  fontsize=20;\n");
		buffer.append("  rankdir=LR;\n");
		buffer.append("  node [shape=circle,fontsize=10];\n");
		for (Node n : in.connectedNodes()) {
			buffer.append("  ").append(n.graphvizSpec()).append("\n");
		}
		for (Node n : in.connectedNodes()) {
			for (String s : n.graphvizEdges()) {
				buffer.append("  ").append(s).append("\n");
			}
		}
		buffer.append("}\n");
		return buffer.toString();
	}
}
