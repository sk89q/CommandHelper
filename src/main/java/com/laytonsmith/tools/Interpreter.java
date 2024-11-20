package com.laytonsmith.tools;

import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import com.laytonsmith.PureUtilities.CommandExecutor;
import com.laytonsmith.PureUtilities.Common.FileUtil;
import com.laytonsmith.PureUtilities.Common.FileWriteMode;
import com.laytonsmith.PureUtilities.Common.HTMLUtils;
import com.laytonsmith.PureUtilities.Common.OSUtils;
import com.laytonsmith.PureUtilities.Common.ReflectionUtils;
import com.laytonsmith.PureUtilities.Common.StreamUtils;
import com.laytonsmith.PureUtilities.Common.StringUtils;
import com.laytonsmith.PureUtilities.Common.WinRegistry;
import com.laytonsmith.PureUtilities.SignalHandler;
import com.laytonsmith.PureUtilities.SignalType;
import com.laytonsmith.PureUtilities.Signals;
import com.laytonsmith.PureUtilities.TermColors;
import com.laytonsmith.abstraction.AbstractConvertor;
import com.laytonsmith.abstraction.Implementation;
import com.laytonsmith.abstraction.MCAttributeModifier;
import com.laytonsmith.abstraction.MCColor;
import com.laytonsmith.abstraction.MCEntity;
import com.laytonsmith.abstraction.MCFireworkBuilder;
import com.laytonsmith.abstraction.MCInventory;
import com.laytonsmith.abstraction.MCInventoryHolder;
import com.laytonsmith.abstraction.MCItemMeta;
import com.laytonsmith.abstraction.MCItemStack;
import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.MCMetadataValue;
import com.laytonsmith.abstraction.MCNamespacedKey;
import com.laytonsmith.abstraction.MCNote;
import com.laytonsmith.abstraction.MCPattern;
import com.laytonsmith.abstraction.MCPlugin;
import com.laytonsmith.abstraction.MCPluginMeta;
import com.laytonsmith.abstraction.MCPotionData;
import com.laytonsmith.abstraction.MCRecipe;
import com.laytonsmith.abstraction.MCServer;
import com.laytonsmith.abstraction.MCWorld;
import com.laytonsmith.abstraction.blocks.MCMaterial;
import com.laytonsmith.abstraction.enums.MCAttribute;
import com.laytonsmith.abstraction.enums.MCDyeColor;
import com.laytonsmith.abstraction.enums.MCEquipmentSlot;
import com.laytonsmith.abstraction.enums.MCEquipmentSlotGroup;
import com.laytonsmith.abstraction.enums.MCPatternShape;
import com.laytonsmith.abstraction.enums.MCPotionType;
import com.laytonsmith.abstraction.enums.MCRecipeType;
import com.laytonsmith.abstraction.enums.MCTone;
import com.laytonsmith.annotations.api;
import com.laytonsmith.annotations.convert;
import com.laytonsmith.annotations.seealso;
import com.laytonsmith.annotations.typeof;
import com.laytonsmith.commandhelper.CommandHelperPlugin;
import com.laytonsmith.core.Documentation;
import com.laytonsmith.core.Installer;
import com.laytonsmith.core.LogLevel;
import com.laytonsmith.core.MethodScriptCompiler;
import com.laytonsmith.core.MethodScriptComplete;
import com.laytonsmith.core.MethodScriptFileLocations;
import com.laytonsmith.core.ParseTree;
import com.laytonsmith.core.Prefs;
import com.laytonsmith.core.Profiles;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.compiler.TokenStream;
import com.laytonsmith.core.compiler.analysis.Declaration;
import com.laytonsmith.core.compiler.analysis.Namespace;
import com.laytonsmith.core.compiler.analysis.StaticAnalysis;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CClosure;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.IVariable;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.constructs.Variable;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.environments.GlobalEnv;
import com.laytonsmith.core.environments.InvalidEnvironmentException;
import com.laytonsmith.core.environments.RuntimeMode;
import com.laytonsmith.core.environments.StaticRuntimeEnv;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;
import com.laytonsmith.core.events.drivers.CmdlineEvents;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.exceptions.CRE.CREIOException;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.AbstractCompileException;
import com.laytonsmith.core.exceptions.CancelCommandException;
import com.laytonsmith.core.exceptions.ConfigCompileException;
import com.laytonsmith.core.exceptions.ConfigCompileGroupException;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.Cmdline;
import com.laytonsmith.core.functions.Cmdline.prompt_char;
import com.laytonsmith.core.functions.Echoes;
import com.laytonsmith.core.functions.ExampleScript;
import com.laytonsmith.core.functions.Function;
import com.laytonsmith.core.functions.FunctionBase;
import com.laytonsmith.core.functions.FunctionList;
import com.laytonsmith.core.profiler.ProfilePoint;
import com.laytonsmith.persistence.DataSourceException;
import com.laytonsmith.tools.docgen.DocGen;
import com.laytonsmith.tools.docgen.DocGenTemplates;
import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.StringsCompleter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.laytonsmith.PureUtilities.TermColors.BLUE;
import static com.laytonsmith.PureUtilities.TermColors.RED;
import static com.laytonsmith.PureUtilities.TermColors.YELLOW;
import static com.laytonsmith.PureUtilities.TermColors.p;
import static com.laytonsmith.PureUtilities.TermColors.pl;
import static com.laytonsmith.PureUtilities.TermColors.reset;
import com.laytonsmith.abstraction.entities.MCTransformation;
import com.laytonsmith.core.natives.interfaces.Mixed;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * This is a command line implementation of the in game interpreter mode. This should only be run while the server is
 * stopped, as it has full access to filesystem resources. Many things won't work as intended, but pure abstract
 * functions should still work fine.
 */
public final class Interpreter {

	/**
	 * THIS MUST NEVER EVER EVER EVER EVER EVER EVER CHANGE. EVER.
	 *
	 * BAD THINGS WILL HAPPEN TO EVERYBODY YOU LOVE IF THIS IS CHANGED!
	 */
	private static final String UNIX_INTERPRETER_INSTALLATION_LOC = "/usr/local/bin/";

	private boolean multilineMode = false;
	private boolean inShellMode = false;
	private String script = "";
	private Environment env;
	private StaticAnalysis staticAnalysis;
	private Thread scriptThread = null;

	private volatile boolean isExecuting = false;

	/**
	 * If they mash ctrlC a bunch, they probably really want to quit, so we'll keep track of this, and reset it only if
	 * they then run an actual command.
	 */
	private volatile int ctrlCcount = 0;

	/**
	 * After this many mashes of Ctrl+C, clearly they want to exit, so we'll exit the shell.
	 */
	private static final int MAX_CTRL_C_MASHES = 5;

	public static void startWithTTY(File file, List<String> args, boolean systemExitOnFailure) throws IOException, DataSourceException, URISyntaxException, Profiles.InvalidProfileException {
		startWithTTY(file.getCanonicalPath(), args, systemExitOnFailure);
	}

	public static void startWithTTY(String file, List<String> args) throws Profiles.InvalidProfileException, IOException, DataSourceException, URISyntaxException {
		startWithTTY(file, args, true);
	}

	public static void startWithTTY(String file, List<String> args, boolean systemExitOnFailure) throws IOException, DataSourceException, URISyntaxException, Profiles.InvalidProfileException {
		File fromFile = new File(file).getCanonicalFile();
		Interpreter interpreter = new Interpreter(args, fromFile.getParentFile().getPath(), true);
		try {
			interpreter.execute(FileUtil.read(fromFile), args, fromFile);
		} catch (ConfigCompileException ex) {
			ConfigRuntimeException.HandleUncaughtException(ex, null, null);
			StreamUtils.GetSystemOut().println(TermColors.reset());
			if(systemExitOnFailure) {
				System.exit(1);
			}
		} catch (ConfigCompileGroupException ex) {
			ConfigRuntimeException.HandleUncaughtException(ex, null);
			StreamUtils.GetSystemOut().println(TermColors.reset());
			if(systemExitOnFailure) {
				System.exit(1);
			}
		}
	}

	private String getHelpMsg() {
		String msg = YELLOW + "You are now in cmdline interpreter mode.\n"
				+ "- on a line by itself (outside of mulitline mode), or the exit() command exits the shell.\n"
				+ ">>> on a line by itself starts multiline mode, where multiple lines can be written, but not yet executed.\n"
				+ "<<< on a line by itself ends multiline mode, and executes the buffered script.\n"
				+ "- on a line by itself while in multiline mode cancels multiline mode, and clears the buffer, without executing the buffered script.\n"
				+ "If the line starts with $$, then the rest of the line is taken to be a shell command. The command is taken as a string, wrapped\n"
				+ "in shell_adv(), (where system out and system err are piped to the corresponding outputs).\n"
				+ "If $$ is on a line by itself, it puts the shell in shell_adv mode, and each line is taken as if it started\n"
				+ "with $$. Use - on a line by itself to exit this mode as well. Using ~ on a line by itself clears\n"
				+ "the environment (i.e. unsets all variables and procs, etc).\n\n"
				+ "For more information about a specific function, type \"help function\"\n"
				+ "and for documentation plus examples, type \"examples function\". See the api tool\n"
				+ "for more information about this feature.";
		try {
			msg += "\nYour current working directory is: " + env.getEnv(GlobalEnv.class).GetRootFolder().getCanonicalPath();
		} catch (IOException ex) {
			//
		}
		return msg;
	}

	/**
	 * Creates a new Interpreter object. This object can then be manipulated via the cmdline interactively, or
	 * standalone, via the execute method.
	 *
	 * @param args Any arguments passed in to the script. They are set as $vars
	 * @param cwd The initial working directory.
	 * @throws IOException
	 * @throws DataSourceException
	 * @throws URISyntaxException
	 * @throws Profiles.InvalidProfileException
	 */
	public Interpreter(List<String> args, String cwd) throws IOException, DataSourceException, URISyntaxException, Profiles.InvalidProfileException {
		this(args, cwd, false);
	}

	private Interpreter(List<String> args, String cwd, boolean inTTYMode) throws IOException, DataSourceException, URISyntaxException, Profiles.InvalidProfileException {
		doStartup();
		env.getEnv(GlobalEnv.class).SetRootFolder(new File(cwd));
		if(inTTYMode) {
			//Ok, done. They'll have to execute from here.
			return;
		}
		//We have two modes here, piped input, or interactive console.
		if(System.console() == null) {
			Scanner scanner = new Scanner(System.in);
			//We need to read in everything, it's basically in multiline mode
			StringBuilder s = new StringBuilder();
			String line;
			try {
				while((line = scanner.nextLine()) != null) {
					s.append(line).append("\n");
				}
			} catch (NoSuchElementException e) {
				//Done
			}
			try {
				execute(s.toString(), args);
				StreamUtils.GetSystemOut().print(TermColors.reset());
				System.exit(0);
			} catch (ConfigCompileException ex) {
				ConfigRuntimeException.HandleUncaughtException(ex, null, null);
				StreamUtils.GetSystemOut().print(TermColors.reset());
				System.exit(1);
			} catch (ConfigCompileGroupException ex) {
				ConfigRuntimeException.HandleUncaughtException(ex, null);
				StreamUtils.GetSystemOut().println(TermColors.reset());
				System.exit(1);
			}

		} else {
			final ConsoleReader reader = new ConsoleReader();
			reader.setExpandEvents(false);
			//Get a list of all the function names. This will be provided to the auto completer.
			Set<FunctionBase> functions = FunctionList.getFunctionList(api.Platforms.INTERPRETER_JAVA,
					env.getEnvClasses());
			List<String> names = new ArrayList<>();
			for(FunctionBase f : functions) {
				if(f.appearInDocumentation()) {
					names.add(f.getName());
				}
			}
			reader.addCompleter(new ArgumentCompleter(new ArgumentCompleter.AbstractArgumentDelimiter() {

				@Override
				public boolean isDelimiterChar(CharSequence buffer, int pos) {
					char c = buffer.charAt(pos);
					return !Character.isLetter(c) && c != '_';
				}
			}, new StringsCompleter(names) {

				@Override
				public int complete(String buffer, int cursor, List<CharSequence> candidates) {
					//The autocomplete can be improved a bit, instead of putting a space after it,
					//let's put a parenthesis.
					int ret = super.complete(buffer, cursor, candidates);
					if(candidates.size() == 1) {
						String functionName = candidates.get(0).toString().trim();
						candidates.set(0, functionName + "()");
					}
					return ret;
				}

			}));
			while(true) {
				String prompt;
				if(multilineMode) {
					prompt = TermColors.WHITE + ">" + reset();
				} else {
					prompt = getPrompt();
				}
				String line = reader.readLine(prompt);
				if(line == null || !textLine(line)) {
					break;
				}
			}
			//TODO: Add syntax highlighting, function keys, etc, but in order
			//to do that, history, command completion, etc, will all have to be re-implemented,
			//and implemented around readCharacter, which is a lot of work.
		}
	}

	private String getPrompt() {
		CClosure c = (CClosure) env.getEnv(GlobalEnv.class).GetCustom("cmdline_prompt");
		if(c != null) {
			try {
				String val = c.executeCallable(CBoolean.get(inShellMode)).val();
				return Static.MCToANSIColors(val) + TermColors.RESET;
			} catch (ConfigRuntimeException ex) {
				ConfigRuntimeException.HandleUncaughtException(ex, env);
			}
		}
		return BLUE + ":" + TermColors.RESET;
	}

	private void doStartup() throws IOException, DataSourceException, URISyntaxException, Profiles.InvalidProfileException {

		Installer.Install(MethodScriptFileLocations.getDefault().getConfigDirectory());
		Installer.InstallCmdlineInterpreter();

		env = Static.GenerateStandaloneEnvironment(false, EnumSet.of(RuntimeMode.CMDLINE, RuntimeMode.INTERPRETER));
		staticAnalysis = new StaticAnalysis(true);
		if(Prefs.UseColors()) {
			TermColors.EnableColors();
		} else {
			TermColors.DisableColors();
		}

		String autoInclude = FileUtil.read(MethodScriptFileLocations.getDefault().getCmdlineInterpreterAutoIncludeFile());
		try {
			MethodScriptCompiler.execute(autoInclude, MethodScriptFileLocations.getDefault()
					.getCmdlineInterpreterAutoIncludeFile(), true, env, env.getEnvClasses(), null, null, null);
		} catch (ConfigCompileException ex) {
			ConfigRuntimeException.HandleUncaughtException(ex, "Interpreter will continue to run, however.", null);
		} catch (ConfigCompileGroupException ex) {
			ConfigRuntimeException.HandleUncaughtException(ex, null);
		}
		//Install our signal handlers.
		SignalHandler.SignalCallback signalHandler = new SignalHandler.SignalCallback() {

			@Override
			public boolean handle(SignalType type) {
				if(isExecuting) {
					env.getEnv(GlobalEnv.class).SetInterrupt(true);
					if(scriptThread != null) {
						scriptThread.interrupt();
					}
					for(Thread t : env.getEnv(StaticRuntimeEnv.class).GetDaemonManager().getActiveThreads()) {
						t.interrupt();
					}
				} else {
					ctrlCcount++;
					if(ctrlCcount > MAX_CTRL_C_MASHES) {
						//Ok, ok, we get the hint.
						StreamUtils.GetSystemOut().println();
						StreamUtils.GetSystemOut().flush();
						System.exit(130); //Standard Ctrl+C exit code
					}
					pl(YELLOW + "\nUse exit() to exit the shell." + reset());
					p(getPrompt());
				}
				return true;
			}
		};
		try {
			SignalHandler.addHandler(Signals.SIGTERM, signalHandler);
		} catch (IllegalArgumentException ex) {
			// Oh well.
		}
		try {
			SignalHandler.addHandler(Signals.SIGINT, signalHandler);
		} catch (IllegalArgumentException ex) {
			// Oh well again.
		}
	}

	/**
	 * This evaluates each line of text
	 *
	 * @param line
	 * @return
	 * @throws IOException
	 */
	private boolean textLine(String line) throws IOException {
		switch(line) {
			case "-":
				//Exit interpreter mode
				if(multilineMode) {
					script = "";
				} else if(inShellMode) {
					inShellMode = false;
				} else {
					return false;
				}
				break;
			case ">>>":
				//Start multiline mode
				if(multilineMode) {
					pl(RED + "You are already in multiline mode!");
				} else {
					multilineMode = true;
					pl(YELLOW + "You are now in multiline mode. Type <<< on a line by itself to execute.");
				}
				break;
			case "<<<":
				//Execute multiline
				multilineMode = false;
				try {
					execute(script, null);
					script = "";
				} catch (ConfigCompileException e) {
					ConfigRuntimeException.HandleUncaughtException(e, null, null);
				} catch (ConfigCompileGroupException e) {
					ConfigRuntimeException.HandleUncaughtException(e, null);
				}
				break;
			case "$$":
				inShellMode = true;
				break;
			default: {
					Pattern p = Pattern.compile("(help|examples) (.*)");
					Matcher m;
					if((m = p.matcher(line)).find()) {
						String helpCommand = m.group(2);
						try {
							List<FunctionBase> fl = new ArrayList<>();
							for(FunctionBase fb : FunctionList.getFunctionList(api.Platforms.INTERPRETER_JAVA,
									env.getEnvClasses())) {
								if(fb.getName().matches("^" + helpCommand + "$")) {
									fl.add(fb);
								}
							}
							if(fl.isEmpty()) {
								StreamUtils.GetSystemErr().println("Could not find function of name " + helpCommand);
							} else if(fl.size() == 1) {
								StreamUtils.GetSystemOut().println(formatDocsForCmdline(helpCommand,
										m.group(1).equals("examples")));
							} else {
								StreamUtils.GetSystemOut().println("Multiple function matches found:");
								for(FunctionBase fb : fl) {
									StreamUtils.GetSystemOut().println(fb.getName());
								}
							}
						} catch (IOException | DataSourceException | URISyntaxException
								| DocGenTemplates.Generator.GenerateException | ConfigCompileException e) {
							e.printStackTrace(StreamUtils.GetSystemErr());
						}
						break;
					}
					if(multilineMode) {
						//Queue multiline
						script = script + line + "\n";
					} else {
						try {
							//Execute single line
							execute(line, null);
						} catch (ConfigCompileException ex) {
							ConfigRuntimeException.HandleUncaughtException(ex, null, null);
						} catch (ConfigCompileGroupException ex) {
							ConfigRuntimeException.HandleUncaughtException(ex, null);
						}
					}
					break;
				}
		}
		return true;
	}

	/**
	 * Given a function name, returns a string that is suitable for printing to the command line. This mechanism
	 * is standardized, so that the display of this information is standardized across different methods. The returned
	 * string will contain usages of {@link TermColors}.
	 * @param function
	 * @param showExamples
	 * @return
	 * @throws ConfigCompileException
	 * @throws IOException
	 * @throws DataSourceException
	 * @throws URISyntaxException
	 * @throws DocGenTemplates.Generator.GenerateException
	 */
	public static String formatDocsForCmdline(String function, boolean showExamples) throws ConfigCompileException,
			IOException, DataSourceException, URISyntaxException, DocGenTemplates.Generator.GenerateException {
		StringBuilder b = new StringBuilder();
		FunctionBase f = FunctionList.getFunction(function, null, Target.UNKNOWN);
		DocGen.DocInfo d = new DocGen.DocInfo(f.docs());
		b.append(TermColors.CYAN).append(d.ret).append(" ");
		b.append(TermColors.RESET).append(f.getName()).append("(")
				.append(TermColors.MAGENTA).append(d.originalArgs).append(TermColors.RESET).append(")\n");
		if(f instanceof Function function1) {
			Class<? extends CREThrowable>[] thrown = function1.thrown();
			if(thrown != null && thrown.length > 0) {
				b.append("Throws: ");
				Set<String> th = new HashSet<>();
				for(Class<? extends CREThrowable> c : thrown) {
					if(ClassDiscovery.GetClassAnnotation(c, typeof.class) != null) {
						typeof t = ClassDiscovery.GetClassAnnotation(c, typeof.class);
						th.add(t.value());
					}
				}
				b.append(TermColors.RED).append(StringUtils.Join(th, ", ")).append(TermColors.RESET).append("\n");
			}
		}
		b.append("\n");
		{
			String desc = reverseHTML(d.desc);
			b.append(TermColors.WHITE).append(desc).append("\n");
		}
		if(d.extendedDesc != null) {
			String desc = reverseHTML(d.extendedDesc);
			b.append(TermColors.WHITE).append(desc).append("\n");
		}
		if(f instanceof Function function1) {
			if(f.getClass().getAnnotation(seealso.class) != null) {
				List<String> seeAlso = new ArrayList<>();
				for(Class c : function1.seeAlso()) {
					Object i = ReflectionUtils.newInstance(c);
					if(i instanceof Documentation seeAlsoDocumentation) {
						String color = TermColors.YELLOW;
						if(i instanceof Function) {
							if(function1.isRestricted()) {
								color = TermColors.CYAN;
							} else {
								color = TermColors.GREEN;
							}
						}
						seeAlso.add(color + seeAlsoDocumentation.getName() + TermColors.RESET);
					}
					// TODO: also support Templates at some point, though this method will have to also be able
					// to support the display of them, which it currently is unable to do.
				}
				if(!seeAlso.isEmpty()) {
					b.append("See also: ");
					b.append(StringUtils.Join(seeAlso, ", ")).append("\n");
				}
			}
		}
		if(f instanceof Function && showExamples) {
			ExampleScript[] examples = ((Function) f).examples();
			if(examples != null && examples.length > 0) {
				b.append(TermColors.BOLD).append("\nExamples").append(TermColors.RESET).append("\n");
				b.append("----------------------------------------------\n\n");
				for(int i = 0; i < examples.length; i++) {
					b.append(TermColors.BRIGHT_WHITE).append(TermColors.BOLD).append(TermColors.UNDERLINE)
							.append("Example ").append(i + 1).append(TermColors.RESET).append("\n");
					if(i > 0) {
						b.append("\n\n");
					}
					ExampleScript e = examples[i];
					b.append(e.getDescription()).append("\n\n");
					b.append(TermColors.UNDERLINE).append("Code").append(TermColors.RESET).append("\n")
							.append(reverseHTML(DocGenTemplates.CODE.generate(e.getScript()))).append("\n\n");
					b.append(TermColors.UNDERLINE).append("Output").append(TermColors.RESET).append("\n")
							.append(e.getOutput()).append("\n\n");
				}
			}
		}
		b.append(TermColors.RESET).append("\n");
		return b.toString();
	}

	public static String reverseHTML(String input) {
		input = input
				.replaceAll("\\<br(.*?)>", "\n")
				.replaceAll("</div>", "\n")
				.replaceAll("\\<.*?>", "")
				.replaceAll("(?s)\\<!--.*?-->", "");
		input = HTMLUtils.unescapeHTML(input);
		input = input.replaceAll("\\{\\{keyword\\|(.*?)\\}\\}", TermColors.BLUE + "$1" + TermColors.RESET);
		input = input.replaceAll("\\{\\{object\\|(.*?)\\}\\}", TermColors.BRIGHT_BLUE + "$1" + TermColors.RESET);
		input = input.replaceAll("\\\\\n", "\n");
		input = input.replaceAll("(?s)\\{\\{Warning\\|text=(.*?)\\}\\}", TermColors.RED + "$1" + TermColors.RESET);
		while(true) {
			Matcher functionMatcher = Pattern.compile("\\{\\{function\\|(.*?)\\}\\}").matcher(input);
			if(functionMatcher.find()) {
				String function = functionMatcher.group(1);
				String color;
				try {
					FunctionBase f = FunctionList.getFunction(function, null, Target.UNKNOWN);
					if(f instanceof Function function1) {
						if(function1.isRestricted()) {
							color = TermColors.CYAN;
						} else {
							color = TermColors.GREEN;
						}
					} else {
						color = TermColors.YELLOW;
					}
				} catch (ConfigCompileException ex) {
					color = TermColors.YELLOW;
				}
				input = input.replaceAll("\\{\\{function\\|" + function + "\\}\\}", color + function
						+ TermColors.RESET);
			} else {
				break;
			}
		}
		{
			StringBuilder b = new StringBuilder();
			StringBuilder headerLine = new StringBuilder();
			boolean inTable = false;
			boolean inTableHeader = false;
			boolean inTableHeaderField = false;
			for(int i = 0; i < input.length(); i++) {
				char c = input.charAt(i);
				char c2 = '\0';
				char c3 = '\0';
				if(i < input.length() - 1) {
					c2 = input.charAt(i + 1);
				}
				if(i < input.length() - 2) {
					c3 = input.charAt(i + 2);
				}
				if(c == '{' && c2 == '|') {
					inTable = true;
					inTableHeader = true;
					b.append("\n");
					i++;
					continue;
				}
				if(c == '|' && c2 == '}') {
					inTable = false;
					i++;
					b.append('\n');
					continue;
				}
				if(inTable) {
					if(inTableHeader) {
						if(c == '|' && c2 == '-') {
							b.append("\n");
							i++;
							continue;
						}
						if(c == '\n') {
							inTableHeader = false;
						}
						continue;
					}
					if(inTableHeaderField) {
						if(c == '|') {
							inTableHeaderField = false;
							b.append(TermColors.RESET).append("\n|").append(TermColors.MAGENTA);
						} else if(c == '\n') {
							b.append(TermColors.RESET).append("| ").append(TermColors.MAGENTA)
									.append(headerLine.toString()).append(TermColors.RESET)
									.append("\n");
							if(c2 != '!') {
								inTableHeaderField = false;
							} else {
								headerLine = new StringBuilder();
								i++;
							}
							continue;
						}
						headerLine.append(c);
						continue;
					}
					if(c == '\n' && c2 == '!') {
						headerLine = new StringBuilder();
						inTableHeaderField = true;
						i++;
						continue;
					}
					if((c == '\n' && c2 == '|' && c3 == '-') || (c == '|' && c2 == '-')) {
						b.append(TermColors.RESET).append("\n").append(StringUtils.stringMultiply(80, "-"));
						if(c == '\n') {
							i += 2;
						} else {
							i++;
							b.append("\n");
						}
						continue;
					}
				}
				b.append(c);
			}
			input = b.toString() + "\n";
		}
		return input;
	}

	/**
	 * This executes a script
	 *
	 * @param script
	 * @param args
	 * @throws ConfigCompileException
	 * @throws IOException
	 * @throws ConfigCompileGroupException
	 */
	public void execute(String script, List<String> args) throws ConfigCompileException, IOException, ConfigCompileGroupException {
		execute(script, args, null);
	}

	/**
	 * This executes an entire script. The cmdline_prompt_event is first triggered (if used) and if the event is
	 * cancelled, nothing happens.
	 *
	 * @param script
	 * @param args
	 * @param fromFile
	 * @throws ConfigCompileException
	 * @throws IOException
	 * @throws ConfigCompileGroupException
	 */
	public void execute(String script, List<String> args, File fromFile) throws ConfigCompileException, IOException, ConfigCompileGroupException {
		CmdlineEvents.cmdline_prompt_input.CmdlinePromptInput input = new CmdlineEvents.cmdline_prompt_input.CmdlinePromptInput(script, inShellMode);
		EventUtils.TriggerListener(Driver.CMDLINE_PROMPT_INPUT, "cmdline_prompt_input", input);
		if(input.isCancelled()) {
			return;
		}
		ctrlCcount = 0;
		if("exit".equals(script)) {
			if(inShellMode) {
				inShellMode = false;
				return;
			}
			pl(YELLOW + "Use exit() if you wish to exit.");
			return;
		}
		if("help".equals(script)) {
			pl(getHelpMsg());
			return;
		}
		if("~".equals(script)) {
			pl("Clearing environment.");
			env.getEnv(GlobalEnv.class).GetProcs().clear();
			env.getEnv(GlobalEnv.class).GetVarList().clear();
			for(Thread t : env.getEnv(StaticRuntimeEnv.class).GetDaemonManager().getActiveThreads()) {
				t.interrupt();
			}
			env.getEnv(StaticRuntimeEnv.class).getExecutionQueue().stopAll();
			env.getEnv(StaticRuntimeEnv.class).getIncludeCache().clear();
			staticAnalysis = new StaticAnalysis(true);
			return;
		}
		if(fromFile == null) {
			fromFile = new File("Interpreter");
		}
		boolean localShellMode = false;
		if(!inShellMode && script.startsWith("$$")) {
			localShellMode = true;
			script = script.substring(2);
		}

		if(inShellMode || localShellMode) {
			// Wrap this in shell_adv
			if(doBuiltin(script)) {
				return;
			}
			List<String> shellArgs = StringUtils.ArgParser(script);
			List<String> escapedArgs = new ArrayList<>();
			for(String arg : shellArgs) {
				escapedArgs.add(new CString(arg, Target.UNKNOWN).getQuote());
			}
			script = "shell_adv("
					+ "array("
					+ StringUtils.Join(escapedArgs, ",")
					+ "),"
					+ "array("
					+ "'stdout':closure(@l){sys_out(@l);},"
					+ "'stderr':closure(@l){sys_err(@l);})"
					+ ");";
		}
		isExecuting = true;
		if(args != null) {
			staticAnalysis.getStartScope().addDeclaration(
					new Declaration(Namespace.IVARIABLE, "@arguments", CArray.TYPE, null, Target.UNKNOWN));
		}
		ProfilePoint compile = env.getEnv(StaticRuntimeEnv.class).GetProfiler().start("Compilation", LogLevel.VERBOSE);
		final ParseTree tree;
		try {
			TokenStream stream = MethodScriptCompiler.lex(script, env, fromFile, true);
			try {
				tree = MethodScriptCompiler.compile(stream, env, env.getEnvClasses(), staticAnalysis);
			} catch (AbstractCompileException e) {

				// Pause on script exception in cmdline mode if set in the file options.
				if(env.getEnv(GlobalEnv.class).inCmdlineMode()
						&& System.console() != null && stream.getFileOptions().isCmdlinePauseOnException()) {
					compile.stop();
					if(e instanceof ConfigCompileException ex) {
						ConfigRuntimeException.HandleUncaughtException(ex, null, null);
					} else if(e instanceof ConfigCompileGroupException ex) {
						ConfigRuntimeException.HandleUncaughtException(ex, null);
					} else {
						throw e;
					}
					StreamUtils.GetSystemOut().println(TermColors.reset());
					prompt_char.promptChar("Press any key to continue...");
					return;
				}

				// Pass exception to caller.
				throw e;
			}
			staticAnalysis = new StaticAnalysis(staticAnalysis.getEndScope(), true); // Continue analysis in end scope.
		} finally {
			compile.stop();
		}
		//Environment env = Environment.createEnvironment(this.env.getEnv(GlobalEnv.class));
		final List<Variable> vars = new ArrayList<>();
		if(args != null) {
			//Build the @arguments variable, the $ vars, and $ itself. Note that
			//we have special handling for $0, that is the script name, like bash.
			//However, it doesn't get added to either $ or @arguments, due to the
			//uncommon use of it.
			StringBuilder finalArgument = new StringBuilder();
			CArray arguments = new CArray(Target.UNKNOWN);
			{
				//Set the $0 argument
				Variable v = new Variable("$0", "", Target.UNKNOWN);
				v.setVal(fromFile.toString());
				v.setDefault(fromFile.toString());
				vars.add(v);
			}
			for(int i = 0; i < args.size(); i++) {
				String arg = args.get(i);
				if(i > 0) {
					finalArgument.append(" ");
				}
				Variable v = new Variable("$" + Integer.toString(i + 1), "", Target.UNKNOWN);
				v.setVal(new CString(arg, Target.UNKNOWN));
				v.setDefault(arg);
				vars.add(v);
				finalArgument.append(arg);
				arguments.push(new CString(arg, Target.UNKNOWN), Target.UNKNOWN);
			}
			Variable v = new Variable("$", "", false, true, Target.UNKNOWN);
			v.setVal(new CString(finalArgument.toString(), Target.UNKNOWN));
			v.setDefault(finalArgument.toString());
			vars.add(v);
			env.getEnv(GlobalEnv.class).GetVarList().set(new IVariable(CArray.TYPE, "@arguments", arguments,
					Target.UNKNOWN));
		}
		try {
			ProfilePoint p = this.env.getEnv(StaticRuntimeEnv.class)
					.GetProfiler().start("Interpreter Script", LogLevel.ERROR);
			try {
				scriptThread = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							if(tree != null && tree.getChildren().size() == 1
									&& tree.getChildAt(0).getData() instanceof IVariable ivar) {
								Mixed i = env.getEnv(GlobalEnv.class).GetVarList()
										.get(ivar.getVariableName(), ivar.getTarget(), env).ival();
								StreamUtils.GetSystemOut().println(i.val());
								return;
							}
							MethodScriptCompiler.execute(tree, env, new MethodScriptComplete() {
								@Override
								public void done(String output) {
									if(System.console() != null && !"".equals(output.trim())) {
										StreamUtils.GetSystemOut().println(output);
									}
								}
							}, null, vars);
							env.getEnv(StaticRuntimeEnv.class).GetDaemonManager().waitForThreads();
						} catch (CancelCommandException | InterruptedException e) {
							// Nothing, though we could have been Ctrl+C cancelled, so we need to reset
							// the interrupt flag. But we do that unconditionally below, in the finally,
							// in the other thread.
							// However, interrupt all the underlying threads
							for(Thread t : env.getEnv(StaticRuntimeEnv.class).GetDaemonManager().getActiveThreads()) {
								t.interrupt();
							}
						} catch (ConfigRuntimeException e) {
							ConfigRuntimeException.HandleUncaughtException(e, env);

							// No need for the full stack trace.
							if(System.console() == null) {
								System.exit(1);
							}

							// Pause on script exception in cmdline mode if set in the file options.
							if(env.getEnv(GlobalEnv.class).inCmdlineMode()
									&& tree.getFileOptions().isCmdlinePauseOnException()) {
								try {
									prompt_char.promptChar("Press any key to continue...");
								} catch (IOException e1) {
									// Ignore.
								}
							}
						} catch (NoClassDefFoundError e) {
							StreamUtils.GetSystemErr().println(RED + Static.getNoClassDefFoundErrorMessage(e) + reset());
							StreamUtils.GetSystemErr().println("Since you're running from standalone interpreter mode, this is not a fatal error, but one of the functions you just used required"
									+ " an actual backing engine that isn't currently loaded. (It still might fail even if you load the engine though.) You simply won't be"
									+ " able to use that function here.");
							if(System.console() == null) {
								System.exit(1);
							}
						} catch (InvalidEnvironmentException ex) {
							StreamUtils.GetSystemErr().println(RED + ex.getMessage() + " " + ex.getData() + "() cannot be used in this context.");
							if(System.console() == null) {
								System.exit(1);
							}
						} catch (RuntimeException e) {
							pl(RED + e.toString());
							e.printStackTrace(StreamUtils.GetSystemErr());
							if(System.console() == null) {
								System.exit(1);
							}
						}
					}
				}, "MethodScript-Main");
				scriptThread.start();
				try {
					scriptThread.join();
				} catch (InterruptedException ex) {
					//
				}
			} finally {
				p.stop();
			}
		} finally {
			env.getEnv(GlobalEnv.class).SetInterrupt(false);
			isExecuting = false;
		}
	}

	/**
	 * Works like {@link #execute(String, List, File)} but reads the file in for you.
	 *
	 * @param script Path the the file
	 * @param args Arguments to be passed to the script
	 * @throws ConfigCompileException If there is a compile error in the script
	 * @throws IOException
	 * @throws ConfigCompileGroupException
	 */
	public void execute(File script, List<String> args) throws ConfigCompileException, IOException, ConfigCompileGroupException {
		String scriptString = FileUtil.read(script);
		execute(scriptString, args, script);
	}

	public boolean doBuiltin(String script) {
		List<String> args = StringUtils.ArgParser(script);
		if(!args.isEmpty()) {
			String command = args.get(0);
			args.remove(0);
			command = command.toLowerCase(Locale.ENGLISH);
			switch(command) {
				case "help":
					pl(getHelpMsg());
					pl("Shell builtins:");
					pl("cd <dir> - Runs cd() with the provided argument.");
					pl("s - equivalent to cd('..').");
					pl("echo - Prints the arguments. If -e is set as the first argument, arguments are sent to colorize() first.");
					pl("exit - Exits shellMode, and returns back to normal mscript mode.");
					pl("logout - Exits the shell entirely with a return code of 0.");
					pl("pwd - Runs pwd()");
					pl("help - Prints this message.");
					return true;
				case "cd":
				case "s":
					if("s".equals(command)) {
						args.add("..");
					}
					if(args.size() > 1) {
						pl(RED + "Too many arguments passed to cd");
						return true;
					}
					Construct[] a = new Construct[0];
					if(args.size() == 1) {
						a = new Construct[]{new CString(args.get(0), Target.UNKNOWN)};
					}
					try {
						new Cmdline.cd().exec(Target.UNKNOWN, env, a);
					} catch (CREIOException ex) {
						pl(RED + ex.getMessage());
					}
					return true;
				case "pwd":
					pl(new Cmdline.pwd().exec(Target.UNKNOWN, env).val());
					return true;
				case "exit":
					// We need previous code to intercept, we cannot do this here.
					throw new Error("I should not run");
				case "logout":
					new Cmdline.exit().exec(Target.UNKNOWN, env, new CInt(0, Target.UNKNOWN));
					return true; // won't actually run
				case "echo":
					// TODO Probably need some variable interpolation maybe? Otherwise, I don't think this command
					// is actually useful as is, because this is not supposed to be a scripting environment.. that's
					// what the normal shell is for.
					boolean colorize = false;
					if(!args.isEmpty() && "-e".equals(args.get(0))) {
						colorize = true;
						args.remove(0);
					}
					String output = StringUtils.Join(args, " ");
					if(colorize) {
						output = new Echoes.colorize().exec(Target.UNKNOWN, env, new CString(output, Target.UNKNOWN)).val();
					}
					pl(output);
					return true;
			}
		}
		return false;
	}

	public static void install(String commandName) {
		switch(OSUtils.GetOS()) {
			case LINUX:
			case MAC: {
				try {
					URL jar = Interpreter.class.getProtectionDomain().getCodeSource().getLocation();
					File exe = new File(UNIX_INTERPRETER_INSTALLATION_LOC + commandName);
					String bashScript = Static.GetStringResource("/interpreter-helpers/bash.sh");
					try {
						bashScript = bashScript.replaceAll("%%LOCATION%%", jar.toURI().getPath());
					} catch (URISyntaxException ex) {
						ex.printStackTrace(System.err);
					}
					exe.createNewFile();
					if(!exe.canWrite()) {
						throw new IOException();
					}
					FileUtil.write(bashScript, exe);
					exe.setExecutable(true, false);
					File manDir = new File("/usr/local/man/man1");
					if(manDir.exists()) {
						//Don't do this installation if the man pages aren't already there.
						String manPage = Static.GetStringResource("/interpreter-helpers/manpage");
						try {
							manPage = DocGenTemplates.DoTemplateReplacement(manPage, DocGenTemplates.GetGenerators());
							File manPageFile = new File(manDir, commandName + ".1");
							FileUtil.write(manPage, manPageFile);
						} catch (DocGenTemplates.Generator.GenerateException ex) {
							Logger.getLogger(Interpreter.class.getName()).log(Level.SEVERE, null, ex);
						}
					}
				} catch (IOException e) {
					StreamUtils.GetSystemErr().println("Cannot install. You must run the command with sudo for it to succeed, however, did you do that?");
					return;
				}
				break;
			}
			case WINDOWS: {
				try {
					// C# installer, not really uninstallable, so temporarily removing this, so the other installer
					// can be used with no risk.
//					// 1. Unpack the csharp installer program in a temporary directory
//					File root = new File(Interpreter.class.getResource("/interpreter-helpers/csharp").toExternalForm());
//					ZipReader zReader = new ZipReader(root);
//					tmp = Files.createTempDirectory("methodscript-installer", new FileAttribute[]{});
//					zReader.recursiveCopy(tmp.toFile(), false);

					// 2. Write the location of this jar to the registry
					String me = ClassDiscovery.GetClassContainer(Interpreter.class).toExternalForm().substring(6);
					String keyName = "Software\\MethodScript";
					WinRegistry.createKey(WinRegistry.HKEY_CURRENT_USER, keyName);
					WinRegistry.writeStringValue(WinRegistry.HKEY_CURRENT_USER, keyName, "JarLocation", me);

//					// 3. Execute the setup.exe file
//					File setup = new File(tmp.toFile(), "setup.exe");
//					int setupResult = new CommandExecutor(new String[]{setup.getAbsolutePath()}).start().waitFor();
//					if(setupResult != 0) {
//						StreamUtils.GetSystemErr().println("Setup failed to complete successfully (exit code " + setupResult + ")");
//						System.exit(setupResult);
//					} else {
//						StreamUtils.GetSystemOut().println("Setup has begun. Finish the installation in the GUI.");
//					}

					// 4. Write MethodScript.psm1 to the powershell module directory,
					// C:\Program Files\WindowsPowerShell\Modules
					// as well as MethodScript.psd1
					String powershellModule = Static.GetStringResource("/interpreter-helpers/windows/MethodScript.psm1");
					FileUtil.write(powershellModule, new File("C:/Program Files/WindowsPowerShell/Modules/"
							+ "MethodScript/MethodScript.psm1"));
					String powershellManifest = Static.GetStringResource("/interpreter-helpers/windows/MethodScript.psd1");
					FileUtil.write(powershellManifest, new File("C:/Program Files/WindowsPowerShell/Modules/"
							+ "MethodScript/MethodScript.psd1"));

					// 5. Put the mscript.exe file in C:\Program Files\MethodScript
					byte[] exe = StreamUtils.GetBytes(Interpreter.class
							.getResource("/interpreter-helpers/windows/mscript.exe").openStream());

					//= Static.GetStringResource("/interpreter-helpers/windows/mscript.cmd");
					FileUtil.write(exe,
							new File(MethodScriptFileLocations.getDefault().getWindowsNativeDirectory(), "mscript.exe"),
							FileWriteMode.OVERWRITE, true);

					// 6. Add C:\Program Files\MethodScript to the PATH, checking first if it's already
					// there.
					if(!System.getenv("PATH").contains("MethodScript")) {
						String pathKey = "System\\CurrentControlSet\\Control\\Session Manager\\Environment";
						String path = System.getenv("Path");
						if(path != null) {
							WinRegistry.writeStringValue(WinRegistry.HKEY_LOCAL_MACHINE, pathKey, "Path", path + ";"
								+ MethodScriptFileLocations.getDefault().getWindowsNativeDirectory());
						}
						CommandExecutor.Execute("powershell -command \"& {$md=\\\"[DllImport(`\\\"user32.dll\\\"\\\",SetLastError=true,CharSet=CharSet.Auto)]public static extern IntPtr SendMessageTimeout(IntPtr hWnd,uint Msg,UIntPtr wParam,string lParam,uint fuFlags,uint uTimeout,out UIntPtr lpdwResult);\\\"; $sm=Add-Type -MemberDefinition $md -Name NativeMethods -Namespace Win32 -PassThru;$result=[uintptr]::zero;$sm::SendMessageTimeout(0xffff,0x001A,[uintptr]::Zero,\\\"Environment\\\",2,5000,[ref]$result)}\"");
					}

				} catch (IOException | IllegalAccessException | InterruptedException | InvocationTargetException ex) {
					StreamUtils.GetSystemErr().println("Could not install: " + ex + ". You need to run this in Administrator Mode however, did you do that?");
					System.exit(1);
				}
				break;
			}
			default: {
				StreamUtils.GetSystemErr().println("Cmdline MethodScript is only supported on Unix and Windows");
				return;
			}
		}
		StreamUtils.GetSystemOut().println("MethodScript has successfully been installed on your system. Note that you may need to rerun the install command"
				+ " if you change locations of the jar, or rename it. Be sure to put \"#!" + UNIX_INTERPRETER_INSTALLATION_LOC + commandName + "\" at the top of all your scripts,"
				+ " if you wish them to be executable on unix systems, and set the execution bit with chmod +x <script name> on unix systems. (Or use the '" + commandName + " -- new' cmdline utility.)");
		StreamUtils.GetSystemOut().println("Try this script to test out the basic features of the scripting system:\n");
		StreamUtils.GetSystemOut().println(Static.GetStringResource("/interpreter-helpers/sample.ms"));

		if(OSUtils.GetOS() == OSUtils.OS.WINDOWS) {
			StreamUtils.GetSystemOut().println("Additionally, MethodScript has been installed as a PowerShell module.\n"
					+ "You may activate this module with `Import-Module -Name MethodScript` and then execute the\n"
					+ "command `Invoke-MethodScript` for interpeter mode. To run a script, use\n"
					+ "`Invoke-MethodScript script.ms args1 args2` and to use the command line tools,\n"
					+ "use `Invoke-MethodScript -Tool tool args`"
					+ "In cmd.exe, you can use the `mscript` command instead, but otherwise the arguments\n"
					+ "are the same as to the PowerShell command.");
			StreamUtils.GetSystemOut().println(TermColors.RED + "YOU MUST REBOOT YOUR COMPUTER TO USE THIS IN CMD.EXE"
					+ TermColors.RESET);
		}
	}

	public static void uninstall(String commandName) {
		if(null == OSUtils.GetOS()) {
			StreamUtils.GetSystemErr().println("Sorry, cmdline functionality is currently only supported on unix systems! Check back soon though!");
			return;
		}
		switch(OSUtils.GetOS()) {
			case LINUX:
			case MAC:
				try {
					File exe = new File(UNIX_INTERPRETER_INSTALLATION_LOC + commandName);
					if(!exe.delete()) {
						throw new IOException();
					}
				} catch (IOException e) {
					StreamUtils.GetSystemErr().println("Cannot uninstall. You must specify the correct instance name and run the command with sudo for it to succeed, however, did you do that?");
					return;
				}
				break;
			case WINDOWS:
				StreamUtils.GetSystemOut().println("To uninstall on windows, please uninstall from the Add or Remove Programs application.");
				return;
			default:
				StreamUtils.GetSystemErr().println("Sorry, cmdline functionality is currently only supported on unix systems! Check back soon though!");
				return;
		}
		StreamUtils.GetSystemOut().println("MethodScript has been uninstalled from this system.");
	}

	@convert(type = Implementation.Type.SHELL)
	public static class ShellConvertor extends AbstractConvertor {

		@Override
		public MCLocation GetLocation(MCWorld w, double x, double y, double z, float yaw, float pitch) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public Class GetServerEventMixin() {
			return ShellEventMixin.class;
		}

		@Override
		public MCServer GetServer() {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCItemStack GetItemStack(MCMaterial type, int qty) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCItemStack GetItemStack(String type, int qty) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCPotionData GetPotionData(MCPotionType type, boolean extended, boolean upgraded) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCAttributeModifier GetAttributeModifier(MCAttribute attr, UUID id, String name, double amt, MCAttributeModifier.Operation op, MCEquipmentSlot slot) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCAttributeModifier GetAttributeModifier(MCAttribute attr, UUID id, String name, double amt, MCAttributeModifier.Operation op, MCEquipmentSlotGroup slot) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCAttributeModifier GetAttributeModifier(MCAttribute attr, MCNamespacedKey key, double amt, MCAttributeModifier.Operation op, MCEquipmentSlot slot) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCAttributeModifier GetAttributeModifier(MCAttribute attr, MCNamespacedKey key, double amt, MCAttributeModifier.Operation op, MCEquipmentSlotGroup slot) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public void Startup(CommandHelperPlugin chp) {

		}

		@Override
		public MCEntity GetCorrectEntity(MCEntity e) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCInventory GetEntityInventory(MCEntity entity) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCInventory GetLocationInventory(MCLocation location) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCInventoryHolder CreateInventoryHolder(String id, String title) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCNote GetNote(int octave, MCTone tone, boolean sharp) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCColor GetColor(int red, int green, int blue) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCColor GetColor(int red, int green, int blue, int alpha) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCColor GetColor(String colorName, Target t) throws CREFormatException {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCPattern GetPattern(MCDyeColor color, MCPatternShape shape) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCFireworkBuilder GetFireworkBuilder() {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCPluginMeta GetPluginMeta() {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCMaterial[] GetMaterialValues() {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCMaterial GetMaterialFromLegacy(String name, int data) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCMaterial GetMaterialFromLegacy(int id, int data) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCItemMeta GetCorrectMeta(MCItemMeta im) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public List<MCEntity> GetEntitiesAt(MCLocation loc, double radius) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCRecipe GetNewRecipe(String key, MCRecipeType type, MCItemStack result) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCRecipe GetRecipe(MCRecipe unspecific) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCMaterial GetMaterial(String name) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCMetadataValue GetMetadataValue(Object value, MCPlugin plugin) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public String GetPluginName() {
			return "MethodScript";
		}

		@Override
		public MCPlugin GetPlugin() {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public String GetUser(Environment env) {
			return System.getProperty("user.name");
		}

		@Override
		public MCNamespacedKey GetNamespacedKey(String key) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public MCTransformation GetTransformation(Quaternionf leftRotation, Quaternionf rightRotation, Vector3f scale, Vector3f translation) {
			throw new UnsupportedOperationException("This method is not supported from a shell.");
		}

		@Override
		public boolean IsMainThread() {
			return false;
		}

	}

}
