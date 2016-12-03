package dfh.anagrammar.grammar;

import dfh.anagrammar.AnagrammarException;

public class BadRuleException extends AnagrammarException {
	public BadRuleException(String line) {
		super(line);
	}

	private static final long serialVersionUID = 1L;

}
