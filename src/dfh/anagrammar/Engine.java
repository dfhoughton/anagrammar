package dfh.anagrammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import dfh.anagrammar.CharMap.Builder;
import dfh.anagrammar.node.End;
import dfh.anagrammar.node.MissingWordlistException;
import dfh.anagrammar.node.Node;
import dfh.anagrammar.node.Pipe;
import dfh.anagrammar.node.Terminal;

/**
 * Object that manages the processing of a phrase by an anagrammar DFA.
 * 
 * @author houghton
 *
 */
public class Engine {
	private static final Comparator<Object> RANDOM_COMPARATOR = new Comparator<Object>() {
		@Override
		public int compare(Object o1, Object o2) {
			return Math.random() > 0.5 ? 1 : -1;
		}
	};
	private final CharMap charmap;
	private final int threads;
	private final Pipe dfa;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final int sample;
	private final boolean random, sampling;
	private final AtomicInteger found = new AtomicInteger();

	public Engine(int threads, int sample, Boolean random, Map<String, List<String>> wordLists, Pipe dfa, Builder b)
			throws MissingWordlistException {
		this.threads = threads;
		this.sample = sample;
		this.sampling = sample > 0;
		this.random = random;
		this.charmap = makeCharMap(wordLists.values(), b);
		this.dfa = dfa;
		Map<String, Trie> tries = makeTries(wordLists);
		dfa.attachTries(tries);
	}

	/**
	 * Process input phrase in a synchronized fashion, notifying all threads
	 * waiting on this object upon completion.
	 * 
	 * @param inputPhrase
	 * @param handler
	 */
	public synchronized void run(String inputPhrase, OutputHandler handler) {
		running.set(true);

		CharCount cc = charmap.count(inputPhrase);
		BlockingDeque<WorkInProgress> deque = new LinkedBlockingDeque<>();
		for (Node n : edges(dfa.in)) {
			if (n instanceof Terminal) {
				Terminal term = (Terminal) n;
				WorkInProgress wip = new WorkInProgress(term, cc);
				deque.offerLast(wip);
			}
		}
		if (deque.isEmpty()) {
			running.set(false);
			notifyAll();
			return;
		}

		BlockingQueue<WorkInProgress> queue = new LinkedBlockingQueue<>();
		End e = (End) dfa.out;
		e.setOutput(queue);
		AtomicInteger activityCounter = new AtomicInteger(deque.size());
		found.set(0);

		List<Thread> threadsToKill = new ArrayList<>(threads + 1);

		for (int i = 0; i < threads; i++) {
			Thread t2 = new Thread(() -> {
				while (true) {
					WorkInProgress wip;
					try {
						wip = deque.take();
					} catch (InterruptedException e1) {
						break;
					}
					work(wip, deque, activityCounter);
					int count = activityCounter.decrementAndGet();
					if (count == 0) {
						synchronized (activityCounter) {
							activityCounter.notifyAll();
						}
					}
				}
			});
			t2.setDaemon(true);
			threadsToKill.add(t2);
		}

		final Thread t = new Thread(() -> {
			while (true) {
				WorkInProgress wip;
				try {
					wip = queue.take();
				} catch (InterruptedException e1) {
					break;
				}
				found.incrementAndGet();
				handler.handle(wip);
				if (sampling && found.get() == sample) {
					running.set(false);
					synchronized (activityCounter) {
						activityCounter.notifyAll();
					}
					break;
				}
				int count = activityCounter.decrementAndGet();
				if (count == 0) {
					synchronized (activityCounter) {
						activityCounter.notifyAll();
					}
				}
			}
		});
		t.setDaemon(true);
		threadsToKill.add(t);

		for (Thread th : threadsToKill)
			th.start();

		// keep the main thread alive until all the work is done
		synchronized (activityCounter) {
			while (running.get() && activityCounter.get() > 0) {
				try {
					activityCounter.wait(1000);
				} catch (InterruptedException e1) {
					break;
				}
			}
		}
		for (Thread th : threadsToKill) {
			th.interrupt();
		}

		running.set(false);
	}

	public boolean running() {
		return running.get();
	}

	private void work(WorkInProgress wip, BlockingDeque<WorkInProgress> queue, AtomicInteger activityCounter) {
		Node n = wip.n;
		// only encounterable if the start sometimes has an edge directly to the
		// end
		if (n instanceof End) {
			if (wip.cc.empty()) {
				activityCounter.incrementAndGet();
				((End) n).getOutput().offer(wip);
			}
		} else {
			CharCount cc = wip.cc;
			Trie t = wip.t;
			boolean active = !cc.empty();
			if (active) {
				for (int i : jumplist(t)) {
					CharCount cc2 = cc.decrement(i);
					if (cc2 != null) {
						Trie t2 = t.children[i];
						activityCounter.incrementAndGet();
						enqueue(wip, queue, n, cc2, t2, false);
					}
				}
			}
			if (t.terminal) {
				for (Node o : edges(n)) {
					if (o instanceof Terminal) {
						if (active) {
							Terminal term2 = (Terminal) o;
							activityCounter.incrementAndGet();
							enqueue(wip, queue, o, cc, term2.trie, true);
						}
					} else if (!active) { // must be End
						End e = (End) o;
						activityCounter.incrementAndGet();
						e.getOutput().offer(wip);
					}
				}
			}
		}
	}

	private Node[] edges(Node n) {
		Node[] edges = n.edges;
		if (random) {
			edges = Arrays.copyOf(edges, edges.length);
			Arrays.parallelSort(edges, RANDOM_COMPARATOR);
		}
		return edges;
	}

	private int[] jumplist(Trie t) {
		if (random) {
			int[] jumplist = Arrays.copyOf(t.jumpList, t.jumpList.length);
			for (int i = 0; i < jumplist.length; i++) {
				int j = (int) (Math.random() * jumplist.length);
				int temp = jumplist[i];
				jumplist[i] = jumplist[j];
				jumplist[j] = temp;
			}
			return jumplist;
		} else {
			return t.jumpList;
		}
	}

	private void enqueue(WorkInProgress wip, BlockingDeque<WorkInProgress> queue, Node n, CharCount cc, Trie t,
			boolean overEdge) {
		WorkInProgress wip2 = new WorkInProgress(t, n, cc, wip, overEdge);
		queue.offerFirst(wip2);
	}

	/**
	 * @return the number of anagrams handled on the most recent run
	 */
	public int found() {
		return found.get();
	}

	private Map<String, Trie> makeTries(Map<String, List<String>> wordLists) {
		Map<String, Trie> catalog = new HashMap<>();
		for (Entry<String, List<String>> e : wordLists.entrySet()) {
			Trie t = new Trie();
			catalog.put(e.getKey(), t);
			for (String word : e.getValue()) {
				int[] translation = charmap.translate(word);
				t.add(word, translation, 0);
			}
			t.done();
		}
		return catalog;
	}

	private CharMap makeCharMap(Collection<List<String>> wordLists, Builder b) {
		for (List<String> list : wordLists) {
			b.add(list);
		}
		return b.build();
	}
}
