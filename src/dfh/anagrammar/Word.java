package dfh.anagrammar;

import java.util.LinkedList;
import java.util.List;

public class Word {
	private Word previousWord;
	private Trie t;

	Word(Trie t) {
		this.t = t;
	}

	Word add(Trie t) {
		Word w = new Word(t);
		w.previousWord = this;
		return w;
	}

	private Word(Trie t, Word previousWord) {
		this.t = t;
		this.previousWord = previousWord;
	}

	Word dup() {
		return new Word(t, previousWord);
	}

	List<List<String>> toSentence() {
		List<List<String>> sentence = new LinkedList<>();
		for (String s : t.values) {
			LinkedList<String> variant = new LinkedList<>();
			variant.add(s);
		}
		if (previousWord == null)
			return sentence;
		else
			return previousWord.toSentence(sentence);
	}

	private List<List<String>> toSentence(List<List<String>> sentence) {
		if (previousWord == null)
			return sentence;
		if (t.values.length == 1) {
			for (List<String> list : sentence) {
				list.add(0, t.values[0]);
			}
			return sentence;
		} else {
			List<List<String>> newSentence = new LinkedList<>();
			for (List<String> l : sentence) {
				for (String s : t.values) {
					LinkedList<String> newL = new LinkedList<>(l);
					newL.addFirst(s);
					newSentence.add(newL);
				}
			}
			return newSentence;
		}
	}
}
