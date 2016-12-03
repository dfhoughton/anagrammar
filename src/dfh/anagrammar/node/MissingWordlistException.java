package dfh.anagrammar.node;

import dfh.anagrammar.AnagrammarException;

public class MissingWordlistException extends AnagrammarException {
	public MissingWordlistException(String string) {
		super(string);
	}

	private static final long serialVersionUID = 1L;

}
