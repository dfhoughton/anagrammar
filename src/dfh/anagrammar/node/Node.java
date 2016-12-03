package dfh.anagrammar.node;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Node {
	private static final AtomicInteger addressSupply = new AtomicInteger();
	protected int id = addressSupply.incrementAndGet();
	public Node[] edges = new Node[0];

	public Iterable<Node> connectedNodes() {
		Set<Node> seen = new LinkedHashSet<>();
		Queue<Node> toVisit = new LinkedList<>();
		toVisit.add(this);
		while (!toVisit.isEmpty()) {
			Node n = toVisit.remove();
			if (!seen.contains(n)) {
				seen.add(n);
				for (Node o: n.edges) {
					toVisit.add(o);
				}
			}
		}
		return seen;
	}

	/**
	 * Replace complex nodes like repeaters with networks of simple nodes like
	 * {@link Start}, {@link End}, {@link Node} itself, or {@link Terminal}. The
	 * {@link Pipe} return value represents the input and output nodes of this
	 * network. The reduce method should preserve internal edges of this
	 * network, but the output node should *not*. These external edges will be
	 * restored by {@link #reduce(Pipe)}, which itself calls {@link #reduce()}.
	 * 
	 * @return
	 */
	protected Pipe reduce() {
		Node n = new Node();
		return new Pipe(n, n);
	}

	protected Node dup() {
		Node n = dupSelf();
		return dupEdges(n);
	}

	protected Node dupSelf() {
		try {
			return getClass().getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(
					"accessible zero-argument constructure for " + getClass().getName() + " is not yet implemented", e);
		}
	}

	protected Node dupEdges(Node n) {
		n.edges = new Node[edges.length];
		for (int i = 0; i < edges.length; i++) {
			n.edges[i] = edges[i].dup();
		}
		return n;
	}

	protected Node dup(Map<Node, Node> seen) {
		Node n = new Node();
		n.edges = new Node[edges.length];
		for (int i = 0; i < edges.length; i++) {
			Node o = edges[i];
			Node d = seen.get(o);
			if (d == null) {
				d = o.dup(seen);
				seen.put(o, d);
			}
			n.edges[i] = d;
		}
		return n;
	}

	public void addEdge(Node n) {
		for (Node e : edges) {
			if (e == n)
				return;
		}
		this.edges = Arrays.copyOf(edges, edges.length + 1);
		edges[edges.length - 1] = n;
	}

	void removeEdge(Node n) {
		int i = -1;
		for (int j = 0; j < edges.length; j++) {
			if (edges[j] == n) {
				i = j;
				break;
			}
		}
		if (i > -1) {
			Node[] newEdges = new Node[edges.length - 1];
			for (int j = 0; j < i; j++) {
				newEdges[j] = edges[j];
			}
			for (int j = i + 1, k = i; k < newEdges.length; j++, k++) {
				newEdges[k] = edges[j];
			}
			this.edges = newEdges;
		}
	}

	/**
	 * Duplicate an entire network of nodes.
	 * 
	 * @param p
	 *            a {@link Pipe} representing the input and output nodes of the
	 *            network
	 * @return a {@link Pipe} representing the input and output nodes of the new
	 *         network
	 */
	Pipe dup(Pipe p) {
		Node start = p.in.dup();
		Map<Node, Node> map = new HashMap<>();
		map.put(p.in, start);
		Set<Node> done = new HashSet<>();
		Queue<Node> workQueue = new LinkedList<>();
		workQueue.add(p.in);
		while (!workQueue.isEmpty()) {
			Node n = workQueue.remove();
			if (done.contains(n))
				continue;
			Node duped = map.get(n);
			for (int i = 0; i < n.edges.length; ++i) {
				Node e1 = n.edges[i];
				workQueue.add(e1);
				Node e2 = map.get(e1);
				if (e2 == null) {
					e2 = e1.dup();
					map.put(e1, e2);
				}
				duped.edges[i] = e2;
			}
			done.add(n);
		}
		return new Pipe(map.get(p.in), map.get(p.out));
	}

	public void replaceReferences(Map<String, Node> replacements) {
	}

	String graphvizID() {
		return "n" + id;
	}

	String graphvizSpec() {
		return graphvizID() + ';';
	}

	String[] graphvizEdges() {
		String[] gves = new String[edges.length];
		for (int i = 0; i < gves.length; i++) {
			gves[i] = graphvizID() + " -> " + edges[i].graphvizID() + ';';
		}
		return gves;
	}

	/**
	 * @return whether all {@link Placeholder} nodes dominated by this
	 *         {@link Node} have been replaced
	 */
	public boolean resolved() {
		return true;
	}
}
