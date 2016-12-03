package dfh.anagrammar;

import dfh.anagrammar.node.Node;

public class BitOfWork {
	Node n;
	CharCount c;
	String word;
	BitOfWork previousWork;
	BitOfWork(Node n, CharCount c, BitOfWork previousWork) {
		this.n = n;
		this.c = c;
		this.previousWork = previousWork;
	}
}
