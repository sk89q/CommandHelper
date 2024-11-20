package com.laytonsmith.core.compiler;

import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.Documentation;
import com.laytonsmith.core.Prefs;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public final class FileOptions {
	/*
	 * These value names are used in the syntax highlighter, and should remain the name they are in code.
	 */
	@Option("Strict Mode On")
	private final Boolean strict;
	@Option("Suppressed Warnings")
	private final Set<SuppressWarning> suppressWarnings;
	@Option("File Name")
	private final String name;
	@Option("Author")
	private final String author;
	@Option("Creation Date")
	private final String created;
	@Option("File Description")
	private final String description;
	@Option("Required Extensions")
	private final Set<String> requiredExtensions;
	@Option("Compiler Options")
	private final Set<CompilerOption> compilerOptions;
	@Option("Copyright information")
	private final String copyright;
	@Option("Distribution License Information")
	private final String license;
	@Option("Disables undefined proc errors in the typechecker")
	private final Boolean allDynamicProcs;
	@Option("Do not close terminal when an exception occurs in a cmdline mode script")
	private final Boolean cmdlinePauseOnException;

	private final Map<String, String> rawOptions;
	//TODO: Make this non-public once this is all finished.

	public FileOptions(Map<String, String> parsedOptions) {
		// Note that the keys are lowercased in the map, so they should be lowercase when selecting them from the
		// map as well.
		rawOptions = new HashMap<>(parsedOptions);
		strict = parseBoolean(getDefault(parsedOptions, "strict", null));
		suppressWarnings = parseEnumSet(getDefault(parsedOptions, "suppresswarnings", ""), SuppressWarning.class);
		name = getDefault(parsedOptions, "name", "").trim();
		author = getDefault(parsedOptions, "author", "").trim();
		created = getDefault(parsedOptions, "created", "").trim();
		description = getDefault(parsedOptions, "description", "").trim();
		requiredExtensions = Collections.unmodifiableSet(parseSet(getDefault(parsedOptions, "requiredextensions", "")));
		compilerOptions = parseEnumSet(getDefault(parsedOptions, "compileroptions", ""), CompilerOption.class);
		copyright = getDefault(parsedOptions, "copyright", "").trim();
		license = getDefault(parsedOptions, "license", "").trim();
		allDynamicProcs = parseBoolean(getDefault(parsedOptions, "alldynamicprocs", "false"));
		cmdlinePauseOnException = parseBoolean(getDefault(parsedOptions, "cmdlinepauseonexception", "false"));
	}

	private String getDefault(Map<String, String> map, String key, String defaultIfNone) {
		if(map.containsKey(key)) {
			return map.get(key);
		} else {
			return defaultIfNone;
		}
	}

	private Boolean parseBoolean(String bool) {
		if(bool == null) {
			return null;
		}
		return !(bool.equalsIgnoreCase("false") || bool.equalsIgnoreCase("off"));
	}

	private List<String> parseList(String list) {
		List<String> l = new ArrayList<>();
		for(String part : list.split(",")) {
			if(!part.trim().isEmpty()) {
				l.add(part.trim().toLowerCase());
			}
		}
		return l;
	}

	private Set<String> parseSet(String list) {
		return new HashSet<>(parseList(list));
	}

	private <T extends Enum<T>> Set<T> parseEnumSet(String list, Class<T> type) {
		EnumSet<T> set = EnumSet.noneOf(type);
		List<String> sList = parseList(list);
		for(String s : sList) {
			for(T e : type.getEnumConstants()) {
				if(e.name().equalsIgnoreCase(s)) {
					set.add(e);
					break;
				}
			}
		}
		return set;
	}

	/**
	 * Returns whether or not this file is in strict mode. Unlike most options, this one depends on both the file
	 * options and the config value. In the config, if strict mode is turned on or off, this value only serves as the
	 * default. File options will override the global setting.
	 *
	 * @return
	 */
	public boolean isStrict() {
		if(strict != null) {
			return strict;
		} else {
			return Prefs.StrictMode();
		}
	}

	/**
	 * Returns true if the specified warning has been suppressed.
	 * @param warning
	 * @return
	 */
	public boolean isWarningSuppressed(SuppressWarning warning) {
		return suppressWarnings.contains(warning);
	}

	/**
	 * Returns true if the specified compiler option is present.
	 * @param option
	 * @return
	 */
	public boolean hasCompilerOption(CompilerOption option) {
		return compilerOptions.contains(option);
	}

	/**
	 * Gets the file name. If the actual file name and this value do not match, this is a compiler warning. The default
	 * is an empty string, which should suppress the warning.
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the file author(s). This is not used programmatically, and is only for reference.
	 * @return
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Gets the file creation date. This is not used programmatically, and is only for reference.
	 * @return
	 */
	public String getCreated() {
		return created;
	}

	/**
	 * Gets the file description. This is not used programmatically, and is only for reference.
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns whether or not this file has required extensions.
	 * @return
	 */
	public boolean requiresExtensions() {
		return !requiredExtensions.isEmpty();
	}

	/**
	 * Returns the list of required extensions for this file. It should be a compile error if the file requires an
	 * extension, but the extension is not present.
	 * @return
	 */
	public Set<String> getRequiredExtensions() {
		return requiredExtensions;
	}

	/**
	 * Gets the license that this file is released under. This is not used programmatically, and is only for reference.
	 * @return
	 */
	public String getLicense() {
		return license;
	}

	/**
	 * Gets the copyright data that pertains to this file. This is not used programmatically, and is only for reference.
	 * @return
	 */
	public String getCopyright() {
		return copyright;
	}

	/**
	 * If true, all proc reference errors are suppressed, and it is assumed that the signature of any unknown
	 * procs that are called have the signature <code>auto proc _name(auto... @values);</code>.
	 * @return
	 */
	public boolean isAllDynamicProcs() {
		return allDynamicProcs;
	}

	/**
	 * Get whether the terminal should be held open when an exception occurs in a cmdline script.
	 * @return
	 */
	public boolean isCmdlinePauseOnException() {
		return cmdlinePauseOnException;
	}

	/**
	 * The specification for FileOptions states that options that are not recognized are not an error. Given that,
	 * it should be possible to retrieve these unknown options from the list of options. In general, fully supported
	 * options should be accessed using the other methods in this class, but extensions and some other code may benefit
	 * from being able to access the raw options. There is no processing done on this list, so it contains the first
	 * class options as well, in their unparsed format.
	 * @return A copy of the raw options Map.
	 */
	public Map<String, String> getRawOptions() {
		return new HashMap<>(rawOptions);
	}

	/**
	 * Gets a list of known options, that should be used for things like documentation, syntax highlighting, etc.
	 * @return
	 */
	public static List<String> getKnownOptions() {
		List<String> list = new ArrayList<>();
		for(Field f : ClassDiscovery.getDefaultInstance().loadFieldsWithAnnotation(Option.class)) {
			list.add(f.getName());
		}
		return list;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for(Field f : ClassDiscovery.getDefaultInstance().loadFieldsWithAnnotation(Option.class)) {
			String desc = f.getAnnotation(Option.class).value();
			try {
				b.append(desc).append(": ").append(f.get(this)).append("; ");
			} catch (IllegalArgumentException | IllegalAccessException ex) {
				// uh
			}
		}
		return b.toString();
	}

	public static enum CompilerOption implements Documentation {
		AllowAmbiguousCommands("Disables compiler validation for ambiguous commands (only applicable to MSA files).",
			MSVersion.V3_3_4),
		UltraStrict("Provides an extra strict programming environment. Nitpicky details may be covered in ultra strict"
				+ " mode, and will turn almost all warnings into compiler errors. This will also apply all lint"
				+ " settings that would be warnings into errors as well, and is generally the most pedantic"
				+ " version of strict mode available. This is used in native code, but is not necessarily recommended,"
				+ " since it offers no flexibility, however, code that passes ultra strict mode will generally be"
				+ " considered \"ideal\" code, and enshrines the standard code layout. Warnings that"
				+ " are explicitly suppressed are not errors in this mode.", MSVersion.V3_3_4);

		private final String docs;
		private final Version version;

		private CompilerOption(String docs, Version version) {
			this.docs = docs;
			this.version = version;
		}

		@Override
		public URL getSourceJar() {
			return ClassDiscovery.GetClassContainer(this.getClass());
		}

		@Override
		public Class<? extends Documentation>[] seeAlso() {
			return new Class[]{};
		}

		@Override
		public String getName() {
			return this.name();
		}

		@Override
		public String docs() {
			return docs;
		}

		@Override
		public Version since() {
			return version;
		}

	}

	/**
	 * This determines the severity level of the warning. Different severities may be handled differently by
	 * the view code.
	 */
	public static enum SeverityLevel {
		/**
		 * High impact warnings are ones that have a performance impact, or can cause issues with upgrading in
		 * the future. These should usually be shown in the UI and not ignored.
		 */
		HIGH,
		/**
		 * Medium impact warnings are ones that are likely to point to a bug, but may not. These should usually
		 * not show up readily in the UI, but should show up in lists of issues.
		 */
		MEDIUM,
		/**
		 * Low impact warnings are ones that are generally safe to completely ignore, but may point to poor
		 * coding practices or less readability. These should only show up in the UI if the user is looking
		 * directly at the code.
		 */
		LOW;
	}

	// TODO: Make these extensible, so extensions can add their own easily
	public static enum SuppressWarning implements Documentation {
		UnreachableCode("Code that comes after methods such as return() or exit() won't be run, and represents dead"
				+ " code, which should usually be removed, or can represent an error with your branching logic.",
			MSVersion.V3_3_4, SeverityLevel.MEDIUM),
		HardcodedDynamicParameter("Code that is hardcoded and sent to eval is going to perform worse than simply"
				+ " writing the code normally.", MSVersion.V3_3_4, SeverityLevel.HIGH),
		OverrideArguments("Defining a variable called @arguments overrides the built in @arguments value,"
				+ " making it impossible to access.", MSVersion.V3_3_4, SeverityLevel.HIGH),
		UseBareStrings("Using bare strings can cause code to error or worse silently change behavior when using future"
				+ " versions of MethodScript that introduce new keywords. Therefore, it is always recommended to quote"
				+ " all strings. In strict mode, this is always an error that can't be suppressed.", MSVersion.V3_3_4,
				SeverityLevel.HIGH),
		IncludedFileNotFound("When an include is encountered by the compiler, it checks to ensure that the file being"
				+ " included exists. It doesn't actually need to exist until runtime, but a warning is issued at"
				+ " compile time if it can't be found.", MSVersion.V3_3_4, SeverityLevel.MEDIUM),
		CodeUpgradeNotices("Code that uses old formats should generally be upgraded to newer versions."
				+ " This is encouraged to make code more readable, and is not a deprecation notice. This type of"
				+ " warning is only displayed in strict mode, and is even still suppressible.", MSVersion.V3_3_4,
				SeverityLevel.LOW),
		UseOfSecureString("When storing sensitive information such as passwords, it is advisable to use the"
				+ " secure_string class instead of string. There is first class language support for this in"
				+ " many places, but in general makes it harder to accidentally leak sensitive data in for"
				+ " example log messages, even when passing the data around to code that accepts strings.",
				MSVersion.V3_3_4, SeverityLevel.MEDIUM),
		FutureNestedCommentChange("In version 3.3.6 or later, nested comment blocks will be allowed."
				+ " This will cause the code here to suddenly change behavior by opening a new comment"
				+ " block, which will never be closed (as that's impossible with 3.3.5 behavior)."
				+ " If you accept that this will suddenly break in a future upgrade, you may do nothing"
				+ " now and deal with it later by closing the comment, but this (suppressable) "
				+ " warning is provided now during"
				+ " a phase in period in case you would like to deal with it by removing the opening"
				+ " comment symbol.",
				MSVersion.V3_3_5, SeverityLevel.HIGH),
		UselessCode("The code or element at this location serves no purpose, and should be removed. Its existence"
				+ " may indicate a problem with the code.",
				MSVersion.V3_3_5, SeverityLevel.MEDIUM),
		UnexpectedStatement("In strict mode, unexpected statements are an error, but in non-strict mode, they are"
				+ " a warning.", MSVersion.V3_3_5, SeverityLevel.HIGH),
		PossibleUnexpectedExecution("If the parenthesis following a token is on a different line as the previous"
				+ " token, and it in general looks like a value that might be executable, this warning is"
				+ " issued, as it will cause an attempt at executing the previous statement. This"
				+ " warning will be removed in 3.3.7, and code will always attempt to execute,"
				+ " but in the meantime, to get"
				+ " rid of it, place a semicolon at the end of the previous line (if you don't mean"
				+ " for it to be executed) or move the left parenthesis up to the same line (if you "
				+ " do mean for it to be executed).", MSVersion.V3_3_5, SeverityLevel.HIGH),
		MalformedComment("The comment related to this element is malformed.", MSVersion.V3_3_5, SeverityLevel.HIGH),
		FutureError("This warning will be changed into a compile error in future versions, and should not be suppressed.",
			MSVersion.V3_3_5, SeverityLevel.HIGH);

		private SuppressWarning(String docs, Version version, SeverityLevel severityLevel) {
			this.docs = docs;
			this.version = version;
			this.severityLevel = severityLevel;
		}

		private final String docs;
		private final Version version;
		private final SeverityLevel severityLevel;

		@Override
		public URL getSourceJar() {
			return ClassDiscovery.GetClassContainer(this.getClass());
		}

		@Override
		public Class<? extends Documentation>[] seeAlso() {
			return new Class[]{};
		}

		@Override
		public String getName() {
			return this.name();
		}

		@Override
		public String docs() {
			return docs;
		}

		@Override
		public Version since() {
			return version;
		}

		public SeverityLevel getSeverityLevel() {
			return severityLevel;
		}

	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface Option {
		/**
		 * The human readable description of the option
		 * @return
		 */
		String value();
	}

}
