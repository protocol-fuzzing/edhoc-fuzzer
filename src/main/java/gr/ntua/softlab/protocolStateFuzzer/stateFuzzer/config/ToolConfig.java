package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.DynamicParameter;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.ProtocolVersion;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToolConfig {

	@Parameter(names = { "-h", "-help" }, help = true, description = "Print usage for all existing commands")
	protected boolean help = false;

	@Parameter(names = "-debug", description = "Debug output shown")
	protected boolean debug = false;

	@Parameter(names = "-quiet", description = "No output shown")
	protected boolean quiet = false;

	public boolean isHelp() {
		return help;
	}

	public boolean isDebug() {
		return debug;
	}

	public boolean isQuiet() {
		return quiet;
	}

	public static final String FUZZER_PROPS = "fuzzer.properties";
	public static final String DEFAULT_FUZZER_PROPS = "default_fuzzer.properties";
	
	public static final String FUZZER_DIR = "fuzzer.dir";
	public static final String SULS_DIR = "suls.dir";
	public static final String SUL_PORT = "sul.port";
	public static final String FUZZER_PORT = "fuzzer.port";

	/* delimiter-separated strings */
	public static final String PROTOCOL_VERSIONS = "protocol.versions";
	public static final String PROTOCOL_VERSION_DELIMITER = ",";

	/* Stores system properties */
	@DynamicParameter(names = "-D", description = "Definitions for variables, which can be referred to in arguments by ${var}. "
			+ "Variables are replaced with their corresponding values before the arguments are parsed."
			+ "Can be passed as either JVM properties (after java) or as application properties.")
	protected static Map<String, String> props = new LinkedHashMap<>();
	
	// initialize system properties
	static {
		Properties fuzzerProps = new Properties();
		String fuzzerPropsLocation = System.getProperty(FUZZER_PROPS);
		try {
			if (fuzzerPropsLocation == null) {
				InputStream resource = ToolConfig.class.getClassLoader().getResourceAsStream(DEFAULT_FUZZER_PROPS);
				fuzzerProps.load(resource);
			} else {
				fuzzerProps.load(new FileReader(fuzzerPropsLocation));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not load properties");
		}
		
		for (String propName : fuzzerProps.stringPropertyNames()) {
			String systemPropValue = System.getProperty(propName);
			if (systemPropValue != null) {
				props.put(propName, systemPropValue);
			} else {
				props.put(propName, fuzzerProps.getProperty(propName));
			}
		}
		
		String fuzzerDir = System.getProperty(FUZZER_DIR);
		if (fuzzerDir == null) {
			fuzzerDir = System.getProperty("user.dir");
		}
		props.put(FUZZER_DIR, fuzzerDir);
		
		String sulsDir = fuzzerProps.getProperty(SULS_DIR);
		if (sulsDir == null) {
			sulsDir = fuzzerDir + File.separator + "suls";
		}
		props.put(SULS_DIR, sulsDir);
		
		/*
		 * SUL port: between 10000 and 39999
		 */
		String sulPort = fuzzerProps.getProperty(SUL_PORT);
		if (sulPort == null) {
			long sulSec = (System.currentTimeMillis() / 1000 % 30000) + 10000;
			sulPort = Long.toString(sulSec);
		}
		props.put(SUL_PORT, sulPort);
		
		/*
		 * Fuzzer port: between 40000 and 65535 (= 0xFFFF or max port)
		 */
		String fuzzerPort = fuzzerProps.getProperty(FUZZER_PORT);
		if (fuzzerPort == null) {
			long fuzzSec = (System.currentTimeMillis() / 1000 % 25536) + 40000;
			fuzzerPort = Long.toString(fuzzSec);
		}
		props.put(FUZZER_PORT, fuzzerPort);

		/*
		 * Initialize static map of  ProtocolVersion class
		 */
		 String protocolVersionsString = props.get(PROTOCOL_VERSIONS);
		 if (protocolVersionsString == null) {
			 throw new RuntimeException("Property " + PROTOCOL_VERSIONS + " is missing");
		 }

		String[] protocolVersions = protocolVersionsString.split(PROTOCOL_VERSION_DELIMITER);
		 if (protocolVersions.length > 0) {
			 ProtocolVersion.fillMapOnce(protocolVersions);
		 } else {
			 throw new RuntimeException("Property " + PROTOCOL_VERSIONS + " is empty");
		 }
	}
	
	// cache userStrings in need of resolution, so as not to parse duplicates
	protected static Map<String, String> resolutionCache = new HashMap<>();
	
	/**
	 * Resolves are the system properties in a given user string.
	 */
	public static String resolve(String userString) {
		if (userString == null) {
			return null;
		}

		if (resolutionCache.containsKey(userString)) {
			return resolutionCache.get(userString);
		}

		boolean neededResolution = false;
		StringBuilder resolvedSB = new StringBuilder();
		// pattern starts with '${' then follows anything other than '$' and ends with '}'
		Matcher matcher = Pattern.compile("(\\$\\{)([^\\$]*)(\\})").matcher(userString);

		while (matcher.find()) {
			if (!neededResolution) {
				neededResolution = true;
			}
			// replace found pattern '${prop_key}' with the prop_key's value in props
			// if the provided prop_key is unknown, the pattern is not resolved
			if (props.containsKey(matcher.group(2))) {
				matcher.appendReplacement(resolvedSB, props.get(matcher.group(2)));
			}
		}

		// append remaining characters from userString to resolvedSB
		matcher.appendTail(resolvedSB);

		// in case of no resolution, resolvedString has the same value as userString
		String resolvedString = resolvedSB.toString();
		if (neededResolution) {
			resolutionCache.put(userString, resolvedString);
		}
		return resolvedString;
	}
}
