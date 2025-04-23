package nez.main;

import java.io.IOException;

import nez.ParserGenerator;
import nez.Version;
import nez.ast.Source;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.ast.NezGrammarCombinator;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.parser.io.CommonSource;
import nez.tool.ast.TreeJSONWriter;
import nez.tool.ast.TreeWriter;
import nez.tool.ast.TreeXMLWriter;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;
import nez.util.UList;
import nez.util.Verbose;

public abstract class Command {

	public final static String ProgName = "Nez";
	public final static String CodeName = "beta";
	public final static int MajorVersion = 1;
	public final static int MinerVersion = 0;
	public final static int PatchLevel = nez.Version.REV;
	public static boolean dumpMaster = false;
	public static boolean dumpUser = false;

	public static void main(String[] args) {
		try {
			Command com = newCommand(args);
			if (Verbose.enabled) {
				Verbose.println("nez-%d.%d.%d %s", MajorVersion, MinerVersion, PatchLevel, com.getClass().getName());
				Verbose.println("strategy: %s", com.strategy);
			}
                        if (dumpMaster) {
                            Grammar grammar = new Grammar("nez");
                            Grammar ng = new NezGrammarCombinator().load(grammar, "File");
                            ConsoleUtils.println("//");
                            ConsoleUtils.println("// If you'll try to excute this grammar you'll need");
                            ConsoleUtils.println("// replace '$format' by any other name like '$xformat'");
                            ConsoleUtils.println("// because 'format' is a keyword");
                            ConsoleUtils.println("//");
                            ng.dump();
                        }
                        else if (dumpUser) {
                            Grammar grammar = com.getSpecifiedGrammar();
                            grammar.dump();
                        }
                        else com.exec();
		} catch (IOException e) {
			ConsoleUtils.println(e);
			Verbose.traceException(e);
			System.exit(1);
		}
	}

	private static Command newCommand(String[] args) {
		try {
			String className = args[0];
			if (className.indexOf('.') == -1) {
				className = "nez.main.C" + className;
			}
			Command cmd = (Command) Class.forName(className).newInstance();
			cmd.parseCommandOption(args);
			return cmd;
		} catch (Exception e) {
			// Verbose.traceException(e);
			showUsage("unknown command");
		}
		return null;
	}

	public void exec() throws IOException {
		System.out.println(strategy);
	}

	protected String command = null;
	protected ParserStrategy strategy = new ParserStrategy();
	protected String grammarFile = null;
	protected String grammarSource = null;
	protected String grammarType = null;
	protected UList<String> grammarFiles = new UList<String>(new String[4]);
	protected String startProduction = null;
	protected String inputText = null;
	protected UList<String> inputFiles = new UList<String>(new String[4]);
	protected String outputFormat = null;
	protected String outputDirectory = null;

	private void parseCommandOption(String[] args) {
		for (int index = 1; index < args.length; index++) {
			String as = args[index];
                        if (as.equals("-m") || as.equals("--masterDump")) {
                            dumpMaster = true;
                            break;
                        }
                        if (as.equals("-u") || as.equals("--userDump")) {
                            dumpUser = true;
                            if(grammarFile == null) continue;
                            break;
                        }
			if (index + 1 < args.length) {
				if (as.equals("-g") || as.equals("--grammar") || as.equals("-p")) {
					grammarFile = args[index + 1];
					grammarSource = null;
					index++;
                                        if(dumpUser) break;
					continue;
				}
				if (as.equals("-e") || as.equals("--expression")) {
					grammarFile = null;
					grammarSource = args[index + 1];
					grammarType = "nez";
					index++;
					continue;
				}
				if (as.equals("-a") || as.equals("--aux")) {
					grammarFiles.add(args[index + 1]);
					index++;
					continue;
				}
				if (as.equals("-s") || as.equals("--start")) {
					startProduction = args[index + 1];
					index++;
					continue;
				}
				if (as.equals("--input")) {
					inputText = args[index + 1];
					index++;
					continue;
				}
				if (as.equals("-f") || as.equals("--format")) {
					outputFormat = args[index + 1];
					index++;
					continue;
				}
				if (as.equals("-d") || as.equals("--dir")) {
					outputDirectory = args[index + 1];
					index++;
					continue;
				}
			}
			if (as.equals("--verbose")) {
				Verbose.enabled = true;
				continue;
			}
			if (!strategy.setOption(as)) {
				if (as.equals("-") && as.length() > 1) {
					showUsage("undefined option: " + as);
				}
				this.inputFiles.add(as);
			}
		}
	}

	public final static void displayVersion() {
		ConsoleUtils.bold();
		ConsoleUtils.println(ProgName + "-" + nez.Version.Version + " (" + CodeName + ") on Java JVM-" + System.getProperty("java.version"));
		ConsoleUtils.end();
		ConsoleUtils.println(Version.Copyright);
	}

	protected static void showUsage(String msg) {
		displayVersion();
		ConsoleUtils.println("Usage: nez <command> options inputs");
		ConsoleUtils.println("  -g | --grammar <file>      Specify a grammar file");
		ConsoleUtils.println("  -f | --format <string>     Specify an output format");
		// ConsoleUtils.println("  -e <text>      Specify a Nez parsing expression");
		// ConsoleUtils.println("  -a <file>      Specify a Nez auxiliary grammar files");
		ConsoleUtils.println("  -s | --start <NAME>        Specify a starting production");
		ConsoleUtils.println("  -d | --dir <dirname>       Specify an output dir");
		ConsoleUtils.println("  -m | --masterDump          Dump master grammar and exit");
		ConsoleUtils.println("  -u | --userDump            Dump user grammar and exit");
		ConsoleUtils.println("Example:");
		ConsoleUtils.println("  nez parse -g js.nez jquery.js --format json");
		ConsoleUtils.println("  nez match -g js.nez *.js");
		ConsoleUtils.println("  nez parser -g math.nez --format c");
		ConsoleUtils.println("");

		ConsoleUtils.println("The most commonly used nez commands are:");
		ConsoleUtils.println("  parse      parse inputs and construct ASTs");
		ConsoleUtils.println("  match      match inputs without ASTs");
		ConsoleUtils.println("  inez       an interactive parser");
		ConsoleUtils.println("  code       generate a parser source code for --format");
		ConsoleUtils.println("  cnez       generate a C-based fast parser");
		ConsoleUtils.println("  peg        translate a grammar into PEG specified with --format");
		ConsoleUtils.println("  compile    compile a grammar into Nez bytecode .moz");
		ConsoleUtils.println("  bench      perform benchmark tests");
		ConsoleUtils.println("  example    display examples in a grammar");
		ConsoleUtils.println("  test       perform grammar tests");
		ConsoleUtils.println("  format     perform grammar formating");
		ConsoleUtils.println("  dump       perform grammar dump with stats");
		ConsoleUtils.exit(0, msg);
	}

	public final Grammar getSpecifiedGrammar() throws IOException {
		if (grammarFile != null) {
			ParserGenerator pg = new ParserGenerator();
			Grammar grammar = pg.loadGrammar(grammarFile);
			for (String f : this.grammarFiles) {
				pg.updateGrammar(grammar, f);
			}
			if (this.startProduction != null) {
				if (!grammar.hasProduction(this.startProduction)) {
					String s = startProduction.substring(0, 1);
					StringBuilder sb = new StringBuilder();
					for (Production p : grammar) {
						if (p.getLocalName().startsWith(s)) {
							sb.append(" ");
							sb.append(p.getLocalName());
						}
					}
					ConsoleUtils.println("No such production -s " + this.startProduction + ". Try " + sb.toString());
					throw new IOException("No such start production: " + this.startProduction);
				}
				grammar.setStartProduction(this.startProduction);
			}
			return grammar;
		}
		return new Grammar();
	}

	public final Parser newParser() throws IOException {
		return this.strategy.newParser(getSpecifiedGrammar());
	}

	public final Parser getNezParser() {
		Grammar grammar = new Grammar("nez");
		Parser parser = new NezGrammarCombinator().load(grammar, "File").newParser(ParserStrategy.newSafeStrategy());
		return parser;
	}

	private int fileIndex = 0;

	public final void checkInputSource() {
		if (inputFiles.size() == 0 && inputText == null) {
			ConsoleUtils.exit(1, "no input specified");
		}
	}

	public final boolean hasInputSource() {
		return fileIndex < inputFiles.size() || inputText != null;
	}

	public final Source nextInputSource() throws IOException {
		if (hasInputSource()) {
			if (inputText != null) {
				Source s = CommonSource.newStringSource(inputText);
				inputText = null;
				return s;
			}
			String path = this.inputFiles.ArrayValues[fileIndex];
			fileIndex++;
			return CommonSource.newFileSource(path);
		}
		return CommonSource.newStringSource(""); // empty input
	}

	public final String getOutputFileName(Source input, String ext) {
		if (outputDirectory != null) {
			return FileBuilder.toFileName(input.getResourceName(), outputDirectory, ext);
		} else {
			return null; // stdout
		}
	}

	public final TreeWriter getTreeWriter(String options, String defaultFormat) {
		if (outputFormat == null) {
			outputFormat = defaultFormat;
		}
		switch (outputFormat) {
		case "ast":
			return new TreeWriter.AstWriter();
		case "line":
			return new TreeWriter.LineWriter();
		case "xml":
			return new TreeXMLWriter();
		case "json":
			return new TreeJSONWriter();
		}
		return (TreeWriter) newExtendedOutputHandler("", options);
	}

	public final Object newExtendedOutputHandler(String classPath, String options) {
		if (outputFormat.indexOf('.') > 0) {
			classPath = outputFormat;
		} else {
			classPath += outputFormat;
		}
		try {
			return Class.forName(classPath).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			Verbose.traceException(e);
			if (options != null) {
				ConsoleUtils.println("Available format: " + options);
				ConsoleUtils.exit(1, "undefined format: " + outputFormat);
			}
		}
		return null;
	}

}
