package dfh.anagrammar.node;

import java.util.Map;

public class Alternater extends Node {
	@Override
	protected Pipe reduce() {
		Node s = new Node(), e = new Node();
		for (Node n : edges) {
			Pipe p = n.reduce();
			s.addEdge(p.in);
			p.out.addEdge(e);
		}
		return new Pipe(s, e);
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
	public void replaceReferences(Map<String, Node> replacements) {
		for (int i = 0; i < edges.length; i++) {
			Node n = edges[i];
			if (n instanceof Placeholder) {
				Placeholder p = (Placeholder) n;
				Node o = replacements.get(p.id);
				if (o != null)
					edges[i] = o.dup();
			} else {
				n.replaceReferences(replacements);
			}
		}
	}
}
