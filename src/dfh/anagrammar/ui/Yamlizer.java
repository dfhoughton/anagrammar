package dfh.anagrammar.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Something for hacking together readable configuration in a rough YAML format.
 * I'm writing this to minimize dependencies and because I don't feel like using
 * the Java standards, whose documentation I find painful to read.
 * 
 * @author houghton
 *
 */
public class Yamlizer {
	static class ConfigurationNode {
		private static final int DEPTH = 2;
		private static final Pattern depthPattern = Pattern.compile("^((?: {" + DEPTH + "})*)\\S");
		private static final Pattern commentPattern = Pattern.compile("^\\s*#");
		private static final Pattern keyPattern = Pattern.compile("(\\w+):(?:\\s*(\\S.*?))?\\s*$");
		private static final Pattern pathPattern = Pattern.compile("^(\\w+)(?:\\.(.*))?");
		private static final Pattern anythingPattern = Pattern.compile("\\S");
		String name, value;
		boolean comment = false;
		ConfigurationNode parent;
		LinkedHashMap<String, ConfigurationNode> children = new LinkedHashMap<>();
		int depth, childDepth = -1;

		static ConfigurationNode rootNode() {
			return new ConfigurationNode(null, null, null, true);
		}

		ConfigurationNode(String name, String value, ConfigurationNode parent, boolean comment) {
			this.name = name;
			this.value = value;
			this.parent = parent;
			this.comment = comment;
			if (parent == null)
				depth = -2;
			else {
				if (parent.childDepth == -1) {
					parent.childDepth = parent.depth + DEPTH;
				}
				depth = parent.childDepth;
			}
		}

		boolean isRoot() {
			return parent == null;
		}

		ConfigurationNode parse(String line) throws BadConfigurationException {
			if (commentPattern.matcher(line).lookingAt())
				return this;
			if (!anythingPattern.matcher(line).find()) // ignore blank lines
				return this;
			Matcher m = depthPattern.matcher(line);
			if (m.lookingAt()) {
				int depth = m.group(1).length();
				int offset = m.end(1);
				m = keyPattern.matcher(line);
				m.region(offset, line.length());
				if (m.lookingAt()) {
					String name = m.group(1), value = m.group(2);
					if (depth == this.childDepth || this.childDepth == -1 && depth > this.depth) {
						ConfigurationNode child = new ConfigurationNode(name, value, this, false);
						addChild(child);
						return child;
					} else if (this.childDepth != -1 && depth > this.childDepth) {
						ConfigurationNode n = lastChild();
						ConfigurationNode child = new ConfigurationNode(name, value, n, false);
						n.addChild(child);
						return child;
					} else {
						ConfigurationNode n = this.parent;
						while (n != null && n.childDepth > depth) {
							n = n.parent;
						}
						if (n == null)
							throw new BadConfigurationException(
									"cannot find parent configuration key for line: " + line);
						ConfigurationNode child = new ConfigurationNode(name, value, n, false);
						n.addChild(child);
						return child;
					}
				} else {
					throw new BadConfigurationException("line: " + line);
				}
			} else {
				return null;
			}
		}

		private ConfigurationNode lastChild() {
			ConfigurationNode n = null;
			// the linked hashmap should preserve insertion order
			// I'd think the collection would therefore expose a method which lets you do this
			// directly, but if so I missed it
			for (ConfigurationNode n2: children.values())
				n = n2;
			return n;
		}

		private void addChild(ConfigurationNode child) {
			children.put(child.name, child);
		}

		public ConfigurationNode addValue(String name, String value) throws BadConfigurationException {
			ConfigurationNode child = children.get(name);
			if (child != null)
				throw new BadConfigurationException("attempting to add child node " + name + " to node " + keyPath()
						+ " when child already exists by that name");
			child = new ConfigurationNode(name, value, this, false);
			children.put(name, child);
			return child;
		}

		/**
		 * Retrieves configuration value.
		 * 
		 * @param path
		 *            string of the form "some.configuration.value"
		 * @return configuration value
		 * @throws BadConfigurationException
		 */
		public String getValue(String path) throws BadConfigurationException {
			ConfigurationNode node = getNode(path);
			if (node == null)
				return null;
			return node.value;
		}

		ConfigurationNode getNode(String path) throws BadConfigurationException {
			Matcher m = pathPattern.matcher(path);
			if (m.lookingAt()) {
				ConfigurationNode child = children.get(m.group(1));
				if (child == null)
					return null;
				if (child.comment)
					return null;
				if (m.group(2) == null)
					return child;
				else
					return child.getNode(m.group(2));
			} else {
				throw new BadConfigurationException(
						"bad configuration key expression " + path + " for node " + keyPath());
			}
		}

		/**
		 * Comment out this node and all its children.
		 */
		public void commentOut() {
			this.comment = true;
			for (ConfigurationNode n : children.values())
				n.commentOut();
		}

		public Collection<String> getKeys(String path) throws BadConfigurationException {
			ConfigurationNode n = getNode(path);
			if (n == null)
				return new ArrayList<>();
			return n.keys();
		}

		Collection<String> keys() {
			List<String> keys = new LinkedList<>();
			for (ConfigurationNode n : children.values()) {
				if (!n.comment)
					keys.add(n.name);
			}
			return keys;
		}

		String keyPath() {
			StringBuffer b = new StringBuffer();
			if (depth > 0) {
				b.append(parent.keyPath()).append('.');
			}
			b.append(name);
			return b.toString();
		}

		@Override
		public String toString() {
			StringBuffer b = new StringBuffer();
			if (comment)
				b.append('#');
			for (int i = 0; i < depth; i++)
				b.append(' ');
			if (name != null) {
				b.append(name);
				b.append(':');
				if (value != null)
					b.append(' ');
			}
			if (value != null)
				b.append(value);
			return b.toString();
		}

		void serialize(Writer writer) throws IOException {
			if (!isRoot()) {
				writer.write(this.toString());
				writer.write("\n");
			}
			for (ConfigurationNode n : children.values())
				n.serialize(writer);
		}

		public void addComment(String string) {
			ConfigurationNode c = new ConfigurationNode(null, "# " + string, this, true);
			String name = Long.toString(System.currentTimeMillis());
			children.put(name, c);
		}

		/**
		 * Add a key with no associated value.
		 * 
		 * @param string
		 * @return
		 * @throws BadConfigurationException
		 */
		public ConfigurationNode addKey(String string) throws BadConfigurationException {
			return addValue(string, null);
		}
	}

	public static ConfigurationNode emptyConfig() {
		return ConfigurationNode.rootNode();
	}

	public static ConfigurationNode parse(File f) throws IOException, BadConfigurationException {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		ConfigurationNode config = ConfigurationNode.rootNode(), n = config;
		String line = null;
		while ((line = reader.readLine()) != null) {
			n = n.parse(line);
		}
		reader.close();
		return config;
	}

	public static void save(ConfigurationNode config, File f) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(f));
		config.serialize(writer);
		writer.close();
	}
}
