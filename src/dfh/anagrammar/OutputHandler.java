package dfh.anagrammar;

/**
 * Type signature of funtion handling output from the DFA.
 * 
 * @author houghton
 *
 */
public interface OutputHandler {
	public void handle(WorkInProgress wip);
}
