package dfh.anagrammar.ui;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import dfh.anagrammar.CharCount;
import dfh.anagrammar.CharMap;
import dfh.anagrammar.CharMap.Builder;
import dfh.anagrammar.Trie;
import dfh.anagrammar.WorkInProgress;
import dfh.anagrammar.grammar.BadRuleException;
import dfh.anagrammar.grammar.Grammar;
import dfh.anagrammar.grammar.RecursionException;
import dfh.anagrammar.node.End;
import dfh.anagrammar.node.MissingWordlistException;
import dfh.anagrammar.node.Node;
import dfh.anagrammar.node.Pipe;
import dfh.anagrammar.node.Terminal;
import dfh.anagrammar.ui.Yamlizer.ConfigurationNode;
import dfh.cli.Cli;
import dfh.cli.rules.Range;

public class CLI {
	private static ConfigurationNode config;
	private static Cli cli;

	public static void main(String[] args) {
		Object[][][] spec = {
				//
				{ { Cli.Opt.USAGE, "compute the anagrams of a phrase that obey a specified grammar" },
						{ "usage.txt" } }, //
				{ { Cli.Opt.ARGS, "word", Cli.Opt.STAR } }, //
				{ { Cli.Opt.NAME, "anagrammar" } }, //
				{ { Cli.Opt.VERSION, "0.0.1" } }, //
				{ { "initialize" }, { "generate a skeleton configuration file in " + configurationDirectory()
						+ "; you must then modify this configuration file to specify grammars and word lists" } }, //
				{ { "force" }, { "in conjunction with --initialize, this overwrites an existing configuration file" } }, //
				{ { "list", 'l' }, { "list available grammars" } }, //
				{ { "grammar", 'g', String.class }, { "specify a grammar to use" } }, //
				{ { "threads", Integer.class, Runtime.getRuntime().availableProcessors() + 1 },
						{ "maximum number of threads" }, { Range.positive() } },//
		};
		cli = new Cli(spec);
		cli.parse(args);
		boolean didSomething = false;
		if (cli.bool("initialize")) {
			didSomething = true;
			try {
				makeSkeletonConfig();
				makeREADME();
			} catch (BadConfigurationException | IOException e) {
				cli.die("failed to initialize anagrammar: " + e.getMessage());
			}
			System.out.println(
					"Please edit " + configFile() + ", specifying the grammars and word lists you want to use.");
			return;
		}
		if (cli.bool("list")) {
			didSomething = true;
			try {
				checkConfig();
				String defaultGrammar = config().getValue("grammars.default");
				System.out.println("available grammars:");
				for (String g : config().getKeys("grammars.definitions")) {
					System.out.print("  ");
					System.out.print(g);
					if (g.equals(defaultGrammar))
						System.out.println(" (default)");
					else
						System.out.println();
				}
			} catch (IOException | BadConfigurationException e) {
				cli.die("could not list available grammars: " + e.getMessage());
			}
		}
		if (didSomething)
			return;
		try {
			checkConfig();
			String grammar;
			if (cli.isSet("grammar"))
				grammar = cli.string("grammar");
			else
				grammar = config().getValue("grammars.default");
			Pipe p = getGrammar(grammar);
			if (cli.argList().isEmpty())
				cli.die("Cannot make anagrams if no phrase is provided.");
			Map<String, Trie> catalog = new HashMap<>();
			Builder b = new CharMap.Builder();
			Collection<String> lists = p.requiredTries();
			for (String listName : lists)
				addWords(listName, b);
			CharMap cm = b.build();
			for (String listName : lists) {
				catalog.put(listName, makeTrie(listName, cm));
			}
			p.attachTries(catalog);
			findAnagrams(p, cm);
		} catch (IOException | BadConfigurationException | BadRuleException | RecursionException
				| MissingWordlistException e) {
			cli.die(e.getMessage());
		}
	}

	private static void findAnagrams(Pipe p, CharMap cm) {
		CharCount cc = cm.count(cli.argList());
		BlockingDeque<WorkInProgress> deque = new LinkedBlockingDeque<>(10000);
		BlockingQueue<WorkInProgress> queue = new LinkedBlockingQueue<>();
		End e = (End) p.out;
		e.setOutput(queue);
		AtomicInteger activityCounter = new AtomicInteger();
		Thread t = new Thread(() -> {
			while (true) {
				WorkInProgress wip = queue.remove();
				activityCounter.incrementAndGet();
				for (List<String> phrase: wip.phrases()) {
					for (String s: phrase) {
						System.out.print(s);
						System.out.print(' ');
					}
					System.out.println();
				}
				synchronized (activityCounter) {
					activityCounter.decrementAndGet();
					activityCounter.notifyAll();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		for (Node n: p.in.edges) {
			if (n instanceof Terminal) {
				Terminal term = (Terminal) n;
				WorkInProgress wip = new WorkInProgress(term, cc);
				deque.addLast(wip);
			}
		}
		for (int i = 0; i < cli.integer("threads"); i++) {
			Thread t2 = new Thread(()->{
				while (true) {
					WorkInProgress wip = deque.remove();
					activityCounter.incrementAndGet();
					wip.work(deque);
					synchronized (activityCounter) {
						activityCounter.decrementAndGet();
						activityCounter.notifyAll();
					}
				}
			});
			t2.setDaemon(true);
			t2.start();
		}
		// keep the main thread alive until all the work is done
		synchronized (activityCounter) {
			while (!(deque.isEmpty() || queue.isEmpty() || activityCounter.get() == 0)) {
				try {
					activityCounter.wait(1000);
				} catch (InterruptedException e1) {
					System.out.println("interrupted; exiting...");
					System.exit(1);
				}
			}
		}
	}

	private static void addWords(String listName, Builder b) throws BadConfigurationException, IOException {
		String path = config().getValue("word_lists." + listName);
		File f = new File(configurationDirectory(), path);
		if (f.exists()) {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			List<String> words = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null)
				words.add(line);
			reader.close();
			b.add(words);
		} else {
			throw new BadConfigurationException("cannot find file of words for " + listName);
		}
	}

	private static Trie makeTrie(String listName, CharMap cm) throws BadConfigurationException, IOException {
		String path = config().getValue("word_lists." + listName);
		File f = new File(configurationDirectory(), path);
		if (f.exists()) {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			Trie t = new Trie();
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				int[] translation = cm.translate(line);
				t.add(line, translation, 0);
			}
			reader.close();
			return t;
		} else {
			throw new BadConfigurationException("cannot find file of words for " + listName);
		}
	}

	private static Pipe getGrammar(String grammar)
			throws BadConfigurationException, IOException, BadRuleException, RecursionException {
		if (grammar == null)
			cli.die("no default grammar specified; edit " + configFile());
		String path = config().getValue("grammars.definitions." + grammar);
		File f = new File(configurationDirectory(), path);
		if (!f.exists())
			cli.die("cannot find grammar " + grammar);
		List<String> lines = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line;
		while ((line = reader.readLine()) != null)
			lines.add(line);
		reader.close();
		return Grammar.parse(lines.toArray(new String[lines.size()]));
	}

	public static void checkConfig() throws IOException, BadConfigurationException {
		if (!configurationDirectory().exists())
			cli.die("you have not yet initialized anagrammar and defined any grammars for it; use --initialize");
		if (config() == null)
			cli.die("you have not yet edited the configuration file to define any grammars");
	}

	private static void makeREADME() throws IOException {
		File dir = configurationDirectory();
		if (!dir.exists())
			dir.mkdir();
		File f = new File(dir, "README");
		if (!f.exists()) {
			PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(f)));
			BufferedReader reader = new BufferedReader(new InputStreamReader(CLI.class.getResourceAsStream("README")));
			String line = null;
			while ((line = reader.readLine()) != null) {
				out.println(line.trim());
			}
			out.close();
			reader.close();
		}
	}

	private static void makeSkeletonConfig() throws BadConfigurationException, IOException {
		File dir = configurationDirectory();
		if (!dir.exists())
			dir.mkdir();
		File f = configFile();
		if (f.exists() && !cli.bool("force"))
			cli.die("configuration file already exists; initialize with --force to overwrite");
		ConfigurationNode config = Yamlizer.emptyConfig();
		config.addComment("a simple key-value map after the style of YAML; see README");
		ConfigurationNode grammars = config.addKey("grammars");
		grammars.addComment("these define the grammars available to anagrammar");
		grammars.addValue("default", "grammar1");
		ConfigurationNode definitions = grammars.addKey("definitions");
		definitions.addValue("grammar1", "relative/file/path");
		definitions.addValue("grammar2", "relative/file/path");
		ConfigurationNode wordList = config.addKey("word_lists");
		wordList.addComment("these define the words and list names available to the grammars");
		wordList.addValue("female_names", "relative/file/path");
		wordList.addValue("male_names", "relative/file/path");
		wordList.addValue("surnames", "relative/file/path");
		wordList.addValue("epithets", "relative/file/path");
		wordList.addValue("titles", "relative/file/path");
		wordList.addValue("etc", "...");
		config.commentOut();
		Yamlizer.save(config, f);
	}

	private static File configurationDirectory() {
		File home = new File(System.getProperty("user.home"));
		File configDir = new File(home, ".anagrammar");
		return configDir;
	}

	private static File configFile() {
		return new File(configurationDirectory(), "config");
	}

	private static ConfigurationNode config() throws IOException, BadConfigurationException {
		if (config == null) {
			File f = configFile();
			if (f.exists()) {
				config = Yamlizer.parse(f);
			}
		}
		return config;
	}
}
