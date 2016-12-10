package dfh.anagrammar;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import dfh.anagrammar.CharMap.Builder;
import dfh.anagrammar.grammar.BadRuleException;
import dfh.anagrammar.grammar.Grammar;
import dfh.anagrammar.grammar.RecursionException;
import dfh.anagrammar.node.MissingWordlistException;
import dfh.anagrammar.node.Pipe;

public class EngineTest {

	@Test
	public void sample() throws BadRuleException, RecursionException, MissingWordlistException {
		String[] bnf = new String[] { "TOP -> <a> <b> <c> | <b> <c> <a>" };
		Map<String, List<String>> wordLists = new HashMap<>();
		for (String s : "a b c".split(" ")) {
			List<String> words = new ArrayList<>(2);
			words.add(s);
			words.add("d");
			words.add("e");
			wordLists.put(s, words);
		}
		List<String> outputList = collectMatches("abc", bnf, wordLists, 1, false);
		assertEquals(1, outputList.size());
	}

	@Test
	public void simple() throws BadRuleException, RecursionException, MissingWordlistException {
		String[] bnf = new String[] { "TOP -> <a> <b> <c>" };
		Map<String, List<String>> wordLists = new HashMap<>();
		for (String s : "a b c".split(" ")) {
			List<String> words = new ArrayList<>(1);
			words.add(s);
			wordLists.put(s, words);
		}
		List<String> outputList = collectMatches("abc", bnf, wordLists, 0, false);
		assertEquals(1, outputList.size());
		assertEquals("a b c", outputList.get(0));
	}

	@Test
	public void multiCharacterSimple() throws BadRuleException, RecursionException, MissingWordlistException {
		String[] bnf = new String[] { "TOP -> <ab> <bb>" };
		Map<String, List<String>> wordLists = new HashMap<>();
		for (String s : "ab bb".split(" ")) {
			List<String> words = new ArrayList<>(1);
			words.add(s);
			wordLists.put(s, words);
		}
		List<String> outputList = collectMatches("abbb", bnf, wordLists, 0, false);
		assertEquals(1, outputList.size());
		assertEquals("ab bb", outputList.get(0));
	}

	private List<String> collectMatches(String input, String[] bnf, Map<String, List<String>> wordLists, int sample,
			boolean random) throws BadRuleException, RecursionException, MissingWordlistException {
		Pipe p = Grammar.parse(bnf);
		Builder b = new Builder();
		Engine e = new Engine(1, sample, random, wordLists, p, b);
		List<String> outputList = new ArrayList<>();
		e.run(input, new OutputHandler() {
			@Override
			public void handle(WorkInProgress wip) {
				for (List<String> phrase : wip.phrases()) {
					StringBuffer buffer = new StringBuffer();
					for (String word : phrase) {
						buffer.append(word);
						buffer.append(' ');
					}
					outputList.add(buffer.toString().trim());
				}
			}
		});
		return outputList;
	}

}
