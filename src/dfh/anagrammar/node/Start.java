package dfh.anagrammar.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * The start node of the grammar's DFA. This {@link Node} should be unique in
 * the DFA.
 * 
 * @author houghton
 *
 */
public class Start extends Node {
	boolean isDFA = false;

	/**
	 * The completing step that reduces the network of nodes compiled out of the
	 * grammar into a DFA usable by anagrammar.
	 * 
	 * @return {@link Pipe} representing the initial network reduced to a simple
	 *         DFA
	 */
	public Pipe toDFA() {
		return reduce();
	}

	private End findEnd() {
		End end = null;
		for (Node n : connectedNodes()) {
			if (n instanceof End) {
				end = (End) n;
				break;
			}
		}
		return end;
	}

	private void simplify() {
		Set<Node> done = new HashSet<>();
		Queue<Node> work = new LinkedList<>();
		work.add(this);
		done.add(this);
		while (!work.isEmpty()) {
			Node n = work.remove();
			boolean reAdd = false;
			for (Node e : n.edges) {
				if (e.getClass().equals(Node.class)) {
					// e's edges might also contain Nodes, so we need to repeat
					reAdd = true;
					n.removeEdge(e);
					for (Node e2 : e.edges) {
						n.addEdge(e2);
						if (!done.contains(e2)) {
							work.add(e2);
						}
					}
					done.add(e);
				} else if (!done.contains(e)) {
					work.add(e);
				}
			}
			if (reAdd)
				work.add(n);
			else
				done.add(n);
		}
	}
	
	@Override
	protected Pipe reduce() {
		if (isDFA) {
			End end = findEnd();
			return new Pipe(this, end);
		} else {
			End e = new End();
			Node[] nodes = edges;
			this.edges = new Node[0];
			for (Node n: nodes) {
				Pipe p = n.reduce();
				addEdge(p.in);
				p.out.addEdge(e);
			}
			isDFA = true;
			simplify();
			return new Pipe(this, e);
		}
	}

	public Pipe reduce(End end) {
		Queue<Node> workQueue = new LinkedList<>();
		Map<Node, Node> reduced = new HashMap<>();
		Map<Node, Set<Node>> backLinks = new HashMap<>();
		workQueue.add(this);
		Pipe rv = new Pipe(this, null);
		while (!workQueue.isEmpty()) {
			Node n = workQueue.remove();
			if (reduced.containsKey(n))
				continue;
			Pipe r = n.reduce();
			if (n == this) {
				rv.in = r.in;
			} else if (n == end) {
				rv.out = r.out;
			}
			reduced.put(n, r.in);
			for (Node e : n.edges) {
				Node r2 = reduced.get(e);
				if (r2 == null) {
					Set<Node> bl = backLinks.get(e);
					if (bl == null) {
						bl = new HashSet<>();
						backLinks.put(e, bl);
					}
					bl.add(r.out);
				} else {
					r.out.addEdge(r2);
				}
			}
			Set<Node> bl = backLinks.get(n);
			if (bl != null) {
				for (Node e : bl) {
					e.addEdge(r.in);
				}
			}
		}
		return rv;
	}

	String graphvizSpec() {
		return graphvizID() + " [shape=doublecircle;label=\"IN\"];";
	}
}
