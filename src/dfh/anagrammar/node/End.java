package dfh.anagrammar.node;

import java.util.Queue;

import dfh.anagrammar.WorkInProgress;

public class End extends Node {
	private Queue<WorkInProgress> output;

	public Queue<WorkInProgress> getOutput() {
		return output;
	}

	public void setOutput(Queue<WorkInProgress> output) {
		this.output = output;
	}

	String graphvizSpec() {
		return graphvizID() + " [shape=doublecircle;label=\"OUT\"];";
	}
}
