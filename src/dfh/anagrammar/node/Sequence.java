package dfh.anagrammar.node;

import java.util.List;
import java.util.Map;

public class Sequence extends Node {

	public Sequence() {
	}

	public Sequence(List<Node> sequence) {
		this.edges = sequence.toArray(new Node[sequence.size()]);
	}

	@Override
	public boolean resolved() {
		for (Node n : edges) {
			if (!n.resolved())
				return false;
		}
		return true;
	}

	@Override
	protected Pipe reduce() {
		Node s = new Node(), e = new Node();
		Node n = s;
		for (Node sn : edges) {
			Pipe p = sn.reduce();
			n.addEdge(p.in);
			n = p.out;
		}
		n.addEdge(e);
		return new Pipe(s, e);
	}

	@Override
	public void replaceReferences(Map<String, Node> replacements) {
		for (int i = 0; i < edges.length; i++) {
			Node n = edges[i];
			if (n instanceof Placeholder) {
				Placeholder p = (Placeholder) n;
				Node r = replacements.get(p.id);
				if (r != null)
					edges[i] = r.dup();
			} else {
				n.replaceReferences(replacements);
			}
		}
	}
}
