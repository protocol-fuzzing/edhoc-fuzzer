package gr.ntua.softlab.protocolStateFuzzer.utils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.StateFuzzerBuilder;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config.*;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.TestRunnerBuilder;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.TimingProbeBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineParser {
    private static final Logger LOGGER = LogManager.getLogger(CommandLineParser.class);
    protected static final String CMD_STATE_FUZZER_CLIENT = "state-fuzzer-client";
    protected static final String CMD_STATE_FUZZER_SERVER = "state-fuzzer-server";
    protected static final String ARGS_FILE = "command.args";

    protected StateFuzzerBuilder stateFuzzerBuilder;
    protected StateFuzzerConfigBuilder stateFuzzerConfigBuilder;
    protected TestRunnerBuilder testRunnerBuilder;
    protected TimingProbeBuilder timingProbeBuilder;

    protected String[] externalParentLoggers;

    /*
     * Extracts from packageName the basePackageName containing, at most, the first four components
     * For example if packageName = "suffix.inner2.inner1.base.name" then basePackageName = "suffix.inner2.inner1.base"
     */
    public static String getBasePackageName(String packageName){
        // pattern matches {a}.{a}.{a}.{a}, where a is anything other than '.'
        // at first {a} (anything other than '.') and then 3 times '.{a}'
        Matcher matcher = Pattern.compile("[^\\.]*(\\.[^\\.]*){3}").matcher(packageName);
        return matcher.find() ? matcher.group() : packageName;
    }

    public CommandLineParser(StateFuzzerConfigBuilder stateFuzzerConfigBuilder, StateFuzzerBuilder stateFuzzerBuilder,
                             TestRunnerBuilder testRunnerBuilder, TimingProbeBuilder timingProbeBuilder,
                             String[] externalParentLoggers){
        Configurator.setLevel(LOGGER, Level.INFO);
        this.stateFuzzerBuilder = stateFuzzerBuilder;
        this.stateFuzzerConfigBuilder = stateFuzzerConfigBuilder;
        this.testRunnerBuilder = testRunnerBuilder;
        this.timingProbeBuilder =  timingProbeBuilder;
        this.externalParentLoggers = externalParentLoggers;
    }

    public void parse(String[] args){
        int startCmd;
        int endCmd = 0;
        String[] cmdArgs;

        if (args.length == 0) {
            // to show global usage
            processCommand(args);
        }

        while (args.length > endCmd) {
            startCmd = endCmd;
            while (args.length > endCmd && !args[endCmd].equals("--")) {
                endCmd++;
            }
            cmdArgs = Arrays.copyOfRange(args, startCmd, endCmd);
            processCommand(cmdArgs);
            endCmd++;
        }
    }

    protected void processCommand(String[] args) {
        StateFuzzerClientConfig stateFuzzerClientConfig = stateFuzzerConfigBuilder.buildClientConfig();
        StateFuzzerServerConfig stateFuzzerServerConfig = stateFuzzerConfigBuilder.buildServerConfig();

        JCommander commander = JCommander.newBuilder()
                .allowParameterOverwriting(true)
                .programName("")
                .addCommand(CMD_STATE_FUZZER_CLIENT, stateFuzzerClientConfig)
                .addCommand(CMD_STATE_FUZZER_SERVER, stateFuzzerServerConfig)
                .addConverterFactory(new ToolPropertyAwareConverterFactory())
                .build();

        if (args.length > 0
                && !commander.getCommands().containsKey(args[0])
                && !args[0].startsWith("@")
                && new File(args[0]).exists()) {
            LOGGER.info("Noticed that the first argument is a file. Processing it as an argument file.");
            args[0] = "@" + args[0];
        }

        try {
            commander.parse(args);
            if (commander.getParsedCommand() == null) {
                commander.usage();
                return;
            }

            LOGGER.info("Processing command {}", commander.getParsedCommand());
            switch (commander.getParsedCommand()) {
                case CMD_STATE_FUZZER_CLIENT -> executeCommand(args, commander, stateFuzzerClientConfig);
                case CMD_STATE_FUZZER_SERVER -> executeCommand(args, commander, stateFuzzerServerConfig);
            }

        } catch (ParameterException e) {
            LOGGER.error("Could not parse provided parameters: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Encountered an exception, see below for more info");
            e.printStackTrace();
        }
    }

    protected void executeCommand(String[] args, JCommander commander, StateFuzzerConfig stateFuzzerConfig)
            throws IOException {

        if (stateFuzzerConfig.isHelp()) {
            commander.usage();
            return;
        }

        String ownParentLogger = getBasePackageName(this.getClass().getPackageName());
        if (stateFuzzerConfig.isDebug()) {
            updateLoggingLevels(ownParentLogger, externalParentLoggers, Level.DEBUG);
        } else if (stateFuzzerConfig.isQuiet()) {
            updateLoggingLevels(ownParentLogger, externalParentLoggers, Level.ERROR);
        } else {
            updateLoggingLevels(ownParentLogger, externalParentLoggers, Level.INFO);
        }

        // check if test options have been supplied for launching the available test runners
        if (stateFuzzerConfig.getTestRunnerConfig().getTest() != null) {
            LOGGER.info("Test option is found");

            if (stateFuzzerConfig.getTimingProbeConfig().getProbeCmd() != null) {
                LOGGER.info("Running timing probe");
                timingProbeBuilder.build(stateFuzzerConfig).run();
            } else {
                LOGGER.info("Running test runner");
                testRunnerBuilder.build(stateFuzzerConfig).run();
            }
        } else {
            // run state fuzzer
            LOGGER.info("State-fuzzing a {} implementation", stateFuzzerConfig.getSulConfig().getFuzzingRole());

            // this is an extra step done to store the running arguments
            prepareOutputDir(args, stateFuzzerConfig.getOutput());

            stateFuzzerBuilder.build(stateFuzzerConfig).startFuzzing();
        }
    }

    protected void updateLoggingLevels(String ownParentLogger, String[] externalParentLoggers, Level level) {
        Configurator.setAllLevels(ownParentLogger, level);
        for (String externalParentLogger: externalParentLoggers) {
            Configurator.setAllLevels(externalParentLogger, level);
        }
    }

    /*
     * Creates the output directory in advance in order to store in it the arguments file before the tool is executed.
     */
    protected void prepareOutputDir(String[] args, String dirPath) {
        File outputFolder = new File(dirPath);
        if (outputFolder.exists()) {
            // output folder exists from previous run, so delete contents
            File[] fileList = outputFolder.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    f.delete();
                }
            }
        } else {
            outputFolder.mkdirs();
        }

        try {
            copyArgsToOutDir(args, dirPath);
        } catch (IOException e) {
            LOGGER.error("Failed to copy arguments");
            e.printStackTrace();
            LOGGER.error(e);
        }
    }

    /*
     * Generates a file comprising the entire command given to fuzzer.
     */
    protected void copyArgsToOutDir(String[] args, String outDir) throws IOException {
        Path outputCommandArgsPath = Path.of(outDir, ARGS_FILE);
        FileOutputStream fw = new FileOutputStream(outputCommandArgsPath.toString());
        PrintStream ps = new PrintStream(fw);

        for (String arg : args) {
            if (arg.startsWith("@")) {
                String argsFileName = arg.substring(1);
                File argsFile = new File(argsFileName);

                if (!argsFile.exists()) {
                    LOGGER.warn("Arguments file " + argsFile + " has been moved");
                } else {
                    Files.copy(argsFile.toPath(), outputCommandArgsPath, StandardCopyOption.REPLACE_EXISTING);
                }

            } else {
                ps.println(arg);
            }
        }
        ps.close();
        fw.close();
    }
}
