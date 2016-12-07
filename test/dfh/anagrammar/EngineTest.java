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
	public void test() throws BadRuleException, RecursionException, MissingWordlistException {
		Map<String, List<String>> wordLists = new HashMap<>();
		for (String s: "a b c".split(" ")) {
			List<String> words = new ArrayList<>(1);
			words.add(s);
			wordLists.put(s, words);
		}
		Pipe p = Grammar.parse(new String[]{"TOP -> <a> <b> <c>"});
		Builder b = new Builder();
		Engine e = new Engine(1, 10, wordLists, p, b);
		List<String> outputList = new ArrayList<>();
		e.run("abc", new OutputHandler() {
			@Override
			public void handle(WorkInProgress wip) {
				for (List<String> phrase: wip.phrases()) {
					StringBuffer buffer = new StringBuffer();
					for (String word: phrase) {
						buffer.append(word);
						buffer.append(' ');
					}
					outputList.add(buffer.toString().trim());
				}
			}
		});
		assertEquals(1, outputList.size());
		assertEquals("a b c", outputList.get(0));
	}

}
