package dfh.anagrammar.node;

import java.util.Map;

public class Repeater extends Node {

	Node repeated;
	int low, high;

	public Repeater(Node repeated, int low, int high) {
		this.repeated = repeated;
		this.low = low;
		this.high = high;
	}

	public Repeater() {
	}

	@Override
	protected Node dupSelf() {
		return new Repeater(repeated.dup(), low, high);
	}

	@Override
	protected Pipe reduce() {
		Node s = new Node(), e = new Node(), openEnd = s;
		Pipe p1 = repeated.reduce(), p2 = dup(p1);
		for (int i = 0; i < low; i++) {
			openEnd.addEdge(p2.in);
			openEnd = p2.out;
			p2 = dup(p1);
		}
		if (high == Integer.MAX_VALUE) { // * or +
			openEnd.addEdge(e);
			openEnd.addEdge(p2.in);
			p2.out.addEdge(p2.in);
			openEnd = p2.out;
		} else {
			for (int i = low; i < high; i++) {
				openEnd.addEdge(e);
				openEnd.addEdge(p2.in);
				openEnd = p2.out;
				p2 = dup(p1);
			}
		}
		openEnd.addEdge(e);
		return new Pipe(s, e);
	}

	@Override
	public void replaceReferences(Map<String, Node> replacements) {
		if (repeated instanceof Placeholder) {
			Placeholder p = (Placeholder) repeated;
			Node r = replacements.get(p.id);
			if (r != null)
				repeated = r.dup();
		} else {
			repeated.replaceReferences(replacements);
		}
	}

	@Override
	public boolean resolved() {
		return repeated.resolved();
	}
}
