package dfh.anagrammar.node;

import java.util.concurrent.BlockingQueue;

import dfh.anagrammar.WorkInProgress;

public class End extends Node {
	private BlockingQueue<WorkInProgress> output;

	public BlockingQueue<WorkInProgress> getOutput() {
		return output;
	}

	public void setOutput(BlockingQueue<WorkInProgress> output) {
		this.output = output;
	}

	String graphvizSpec() {
		return graphvizID() + " [shape=doublecircle;label=\"OUT\"];";
	}
}
