package dfh.anagrammar.node;

/**
 * Temporary {@link Node} used during compilation to directly translate the
 * grammar into a network.
 * 
 * @author houghton
 *
 */
public class Placeholder extends Node {
	public String id;

	public Placeholder(String id) {
		this.id = id;
	}

	@Override
	public boolean resolved() {
		return false;
	}
}
