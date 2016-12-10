package dfh.anagrammar.ui;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import dfh.anagrammar.CharMap;
import dfh.anagrammar.CharMap.Builder;
import dfh.anagrammar.Engine;
import dfh.anagrammar.OutputHandler;
import dfh.anagrammar.WorkInProgress;
import dfh.anagrammar.grammar.BadRuleException;
import dfh.anagrammar.grammar.Grammar;
import dfh.anagrammar.grammar.RecursionException;
import dfh.anagrammar.node.MissingWordlistException;
import dfh.anagrammar.node.Pipe;
import dfh.anagrammar.ui.Yamlizer.ConfigurationNode;
import dfh.cli.Cli;
import dfh.cli.coercions.FileCoercion;
import dfh.cli.rules.Range;

public class CLI {
	private static ConfigurationNode config;
	private static Cli cli;
	private static PrintStream out = System.out;

	public static void main(String[] args) {
		Object[][][] spec = {
				//
				{ { Cli.Opt.USAGE, "compute the anagrams of a phrase that obey a specified grammar" },
						{ "usage.txt" } }, //
				{ { Cli.Opt.ARGS, "word", Cli.Opt.STAR } }, //
				{ { Cli.Opt.NAME, "anagrammar" } }, //
				{ { Cli.Opt.VERSION, "1.0.0" } }, //
				{ { "grammar", 'g', String.class },
						{ "specify a grammar to use rather than the default listed in " + configFile() } }, //
				{ { "out", 'o', FileCoercion.C },
						{ "dump output -- anagrams, grammar, or Graphviz spec -- into this file" } }, //
				{ { "sample", 's', Integer.class }, { "produce only a sample of anagrams" }, { Range.positive() } }, //
				{ { "random", 'r' }, { "generate anagrams in random order" } }, //
				{ { "unique", 'u' },
						{ "in case the grammar can produce the same phrase in more than one way, "
								+ "this ensures that each name is only listed once; "
								+ "NOTE: for the sake of memory efficiency and speed, hashcodes are used to determine uniqueness, "
								+ "so some anagrams may be dropped altogether" } }, //
				{ { "count", 'c' }, { "print out the number of anagrams found" } }, //
				{}, //
				{ { "list", 'l' }, { "list available grammars" } }, //
				{ { "word-lists" }, { "show the list of word lists used by the grammars" } }, //
				{ { "dot" },
						{ "print out a Graphviz graph specification for the "
								+ "finite state automaton representation of a grammar" } }, //
				{ { "show-grammar" }, { "dump out the selected grammar" } }, //
				{}, //
				{ { "threads", Integer.class, Runtime.getRuntime().availableProcessors() },
						{ "maximum number of threads" }, { Range.positive() } }, //
				{}, //
				{ { "initialize" }, { "generate a skeleton configuration file in " + configurationDirectory()
						+ "; you must then modify this configuration file to specify grammars and word lists" } }, //
				{ { "force" }, { "in conjunction with --initialize, this overwrites an existing configuration file" } }, //
		};
		cli = new Cli(spec);
		cli.parse(args);

		boolean didSomething = false;
		boolean unique = cli.bool("unique");
		if (cli.isSet("out")) {
			File outFile = (File) cli.object("out");
			try {
				out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile)), true);
			} catch (FileNotFoundException e) {
				cli.die("could not write to " + outFile);
			}
		}
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
		if (cli.bool("word-lists")) {
			didSomething = true;
			try {
				checkConfig();
				System.out.println("available word lists:");
				Map<String, String> wordLists = new TreeMap<>();
				for (String w : config.getKeys("word_lists")) {
					String v = config.getValue("word_lists." + w);
					wordLists.put(w, v);
				}
				if (wordLists.isEmpty())
					System.out.println("no word lists configured!");
				else {
					int nm = -1;
					for (String s : wordLists.keySet()) {
						int n = s.length() + 1;
						if (n > nm)
							nm = n;
					}
					for (Entry<String, String> e : wordLists.entrySet()) {
						System.out.printf("  %-" + nm + "s %s\n", e.getKey() + ':', e.getValue());
					}
				}
			} catch (IOException | BadConfigurationException e) {
				cli.die("could not list available word lists:" + e.getMessage());
			}
		}
		if (cli.bool("show-grammar")) {
			didSomething = true;
			try {
				checkConfig();
				String grammar;
				if (cli.isSet("grammar"))
					grammar = cli.string("grammar");
				else
					grammar = config().getValue("grammars.default");
				if (grammar == null)
					cli.die("grammar " + grammar + " is not defined in the configuration file " + configFile());
				String fn = config().getValue("grammars.definitions." + grammar);
				File f = new File( configurationDirectory(), fn);
				BufferedReader reader = new BufferedReader(new FileReader(f));
				String line;
				while ((line = reader.readLine()) != null)
					out.println(line);
				reader.close();
				out.flush();
			} catch (IOException | BadConfigurationException e) {
				cli.die("could not produce dot file: " + e.getMessage());
			}
		}
		if (cli.bool("dot")) {
			didSomething = true;
			try {
				checkConfig();
				String grammar;
				if (cli.isSet("grammar"))
					grammar = cli.string("grammar");
				else
					grammar = config().getValue("grammars.default");
				if (grammar == null)
					cli.die("grammar " + grammar + " is not defined in the configuration file " + configFile());
				Pipe p = getGrammar(grammar);
				out.println(p.graphvizDOT(grammar));
				out.flush();
			} catch (IOException | BadConfigurationException | BadRuleException | RecursionException e) {
				cli.die("could not produce dot file: " + e.getMessage());
			}
		}
		if (didSomething)
			return;
		try {
			checkConfig();
			if (cli.argList().isEmpty())
				cli.die("Cannot make anagrams if no phrase is provided.");
			StringBuffer buffer = new StringBuffer();
			for (String s : cli.argList())
				buffer.append(s).append(' ');
			String inputPhrase = buffer.toString().trim();
			System.out.println("collecting anagrams of " + inputPhrase);
			System.out.println();
			String grammar;
			if (cli.isSet("grammar"))
				grammar = cli.string("grammar");
			else
				grammar = config().getValue("grammars.default");
			Pipe p = getGrammar(grammar);
			Map<String, List<String>> wordLists = getWordLists(p.requiredTries());
			Builder b = new CharMap.Builder();
			int sample = cli.isSet("sample") ? cli.integer("sample") : -1;
			Engine e = new Engine(cli.integer("threads"), sample, cli.bool("random"), wordLists, p, b);
			Set<Integer> seen = new HashSet<>();
			e.run(inputPhrase, new OutputHandler() {
				@Override
				public void handle(WorkInProgress wip) {
					for (List<String> phrase : wip.phrases()) {
						StringBuffer b = new StringBuffer();
						for (String word : phrase) {
							b.append(word);
							b.append(' ');
						}
						String w = b.toString().trim();
						if (w.length() > 0) {
							if (unique) {
								Integer i = w.hashCode();
								if (!seen.contains(i)) {
									seen.add(i);
									out.println(w);
								}
							} else
								out.println(w);
						}
					}
				}
			});
			out.flush();
			if (cli.bool("count"))
				System.out.printf("\nfound %d anagram%s\n", e.found(), e.found() == 1 ? "" : "s");
		} catch (IOException | BadConfigurationException | BadRuleException | RecursionException
				| MissingWordlistException e) {
			cli.die(e.getMessage());
		}
	}

	private static Map<String, List<String>> getWordLists(Collection<String> requiredTries)
			throws BadConfigurationException, IOException {
		Map<String, List<String>> wordLists = new HashMap<>();
		for (String listName : requiredTries) {
			List<String> words = new LinkedList<>();
			wordLists.put(listName, words);
			String path = config().getValue("word_lists." + listName);
			File f = new File(configurationDirectory(), path);
			if (f.exists()) {
				BufferedReader reader = new BufferedReader(new FileReader(f));
				String line;
				while ((line = reader.readLine()) != null)
					words.add(line.trim());
				reader.close();
			} else {
				throw new BadConfigurationException("cannot find file of words for " + listName);
			}
		}
		return wordLists;
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(CLI.class.getResourceAsStream("/README")));
			String line = null;
			while ((line = reader.readLine()) != null) {
				out.println(line);
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
