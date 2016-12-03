package dfh.anagrammar.grammar;

import dfh.anagrammar.node.Node;

/**
 * A rule and its label.
 * 
 * @author houghton
 *
 */
public class Rule {
	public String name;
	public Node node;

	public Rule(String name, Node node) {
		this.name = name;
		this.node = node;
	}
}
