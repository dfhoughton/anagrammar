package dfh.anagrammar.grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dfh.anagrammar.node.Alternater;
import dfh.anagrammar.node.Node;
import dfh.anagrammar.node.Pipe;
import dfh.anagrammar.node.Placeholder;
import dfh.anagrammar.node.Repeater;
import dfh.anagrammar.node.Sequence;
import dfh.anagrammar.node.Start;
import dfh.anagrammar.node.Terminal;

/**
 * Takes a top-down grammar definition and generates a state transition
 * representation for all the possible terminal symbol sequences defined by this
 * grammar. This state machine can then be used to guide the generation of an
 * anagram from a set of character counts.
 * 
 * This code is purely procedural.
 * 
 * @author houghton
 *
 */
public class Grammar {
	private static final Pattern commentPattern = Pattern.compile("^\\s*(.*?)\\s*(?:#.*)?$");

	public static Pipe parse(String[] lines) throws BadRuleException, RecursionException {
		Map<String, Node> parsed = new LinkedHashMap<>();
		Set<String> seen = new HashSet<>();
		for (String line : lines) {
			Matcher m = commentPattern.matcher(line);
			m.lookingAt();
			String l = m.group(1);
			if (l.length() == 0)
				continue;
			l = l.replaceAll("\\s+", " ").replaceFirst(" ?-> ?", "->");
			if (seen.contains(l)) {
				throw new BadRuleException("duplicate rule: " + line);
			}
			seen.add(l);
			Rule r = parse(l);
			Node n = parsed.get(r.name);
			if (n == null) {
				Alternater alternates = new Alternater();
				mergeNodes(alternates, r.node);
				parsed.put(r.name, alternates);
			} else {
				mergeNodes((Alternater) n, r.node);
			}
		}
		if (parsed.isEmpty()) {
			throw new BadRuleException("no rules in grammar");
		}
		// the first rule defined is assumed to be the "top" rule that must
		// always match
		Node topRule = parsed.values().iterator().next();
		int numberUnderstood = 0;
		while (containsAnyReferences(parsed)) {
			Map<String, Node> understoodReferences = simplestReferences(parsed);
			if (numberUnderstood == understoodReferences.size()) {
				throw new RecursionException();
			}
			numberUnderstood = understoodReferences.size();
			for (Node n : parsed.values()) {
				n.replaceReferences(understoodReferences);
			}
		}
		Start s = new Start();
		s.addEdge(topRule);
		return s.toDFA();
	}

	/**
	 * Merge alternates into a single list
	 * 
	 * @param alternates
	 * @param node
	 */
	private static void mergeNodes(Alternater alternates, Node node) {
		if (node instanceof Alternater) {
			for (Node n : node.edges) {
				alternates.addEdge(n);
			}
		} else {
			alternates.addEdge(node);
		}
	}

	private static Map<String, Node> simplestReferences(Map<String, Node> parsed) {
		Map<String, Node> sr = new HashMap<>();
		for (Entry<String, Node> pair : parsed.entrySet()) {
			if (pair.getValue().resolved()) {
				sr.put(pair.getKey(), pair.getValue());
			}
		}
		return sr;
	}

	private static boolean containsAnyReferences(Map<String, Node> parsed) {
		for (Node n : parsed.values()) {
			if (!n.resolved())
				return true;
		}
		return false;
	}

	private static Pattern repetitionPattern = Pattern.compile("(\\d++)(,(\\d++)?)?");
	private static Pattern elementPattern = Pattern.compile("(<\\w++>|\\w++)([?+*]|\\{" + repetitionPattern + "})?");
	private static Pattern alternatePattern = Pattern
			.compile("((?:" + elementPattern + ")" + "(?: " + elementPattern + ")*+)");
	private static Pattern rulePattern = Pattern
			.compile("^(\\w++)->((?:" + alternatePattern + ")" + "(?: ?\\| ?" + alternatePattern + ")*+)$");

	private static Rule parse(String line) throws BadRuleException {
		Matcher m = rulePattern.matcher(line);
		if (m.find()) {
			String label = m.group(1);
			String[] alternates = m.group(2).split(" ?\\| ?");
			Node n;
			if (alternates.length == 1) {
				n = parseAlternate(alternates[0]);
			} else {
				n = new Alternater();
				for (String s : alternates) {
					n.addEdge(parseAlternate(s));
				}
			}
			return new Rule(label, n);
		} else {
			throw new BadRuleException("cannot parse rule: " + line);
		}
	}

	private static Node parseAlternate(String alternate) throws BadRuleException {
		String[] expressions = alternate.split("\\s+");
		Node n;
		if (expressions.length == 1) {
			n = parseExpression(expressions[0]);
		} else {
			List<Node> nodes = new ArrayList<>();
			for (String s : expressions) {
				nodes.add(parseExpression(s));
			}
			n = new Sequence(nodes);
		}
		return n;
	}

	private static Node parseExpression(String string) throws BadRuleException {
		Node n, base;
		Matcher m = elementPattern.matcher(string);
		m.find();
		String rulePart = m.group(1);
		if (rulePart.charAt(0) == '<') {
			base = new Terminal(rulePart.substring(1, rulePart.length() - 1));
		} else {
			base = new Placeholder(rulePart);
		}
		String repetitionPart = m.group(2);
		if (repetitionPart != null) {
			int low = 0, high = Integer.MAX_VALUE;
			switch (repetitionPart.charAt(0)) {
			case '?':
				high = 1;
				break;
			case '*':
				break;
			case '+':
				low = 1;
				break;
			case '{':
				repetitionPart = repetitionPart.substring(1, repetitionPart.length() - 1);
				m = repetitionPattern.matcher(repetitionPart);
				if (m.matches()) {
					low = Integer.parseInt(m.group(1));
					if (m.group(2) == null) {
						high = low;
					} else if (m.group(3) == null) {
						high = Integer.MAX_VALUE;
					} else {
						high = Integer.parseInt(m.group(3));
					}
				} else {
					throw new BadRuleException("cannot parse expression: " + string);
				}
			}
			n = new Repeater(base, low, high);
		} else {
			n = base;
		}
		return n;
	}
}
