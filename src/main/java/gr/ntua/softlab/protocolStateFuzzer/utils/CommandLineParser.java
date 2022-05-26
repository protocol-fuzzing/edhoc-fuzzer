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

    public CommandLineParser(StateFuzzerConfigBuilder stateFuzzerConfigBuilder, StateFuzzerBuilder stateFuzzerBuilder,
                             TestRunnerBuilder testRunnerBuilder, TimingProbeBuilder timingProbeBuilder){
        Configurator.setLevel(LOGGER, Level.INFO);
        this.stateFuzzerBuilder = stateFuzzerBuilder;
        this.stateFuzzerConfigBuilder = stateFuzzerConfigBuilder;
        this.testRunnerBuilder = testRunnerBuilder;
        this.timingProbeBuilder =  timingProbeBuilder;
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

        } catch (ParameterException E) {
            LOGGER.error("Could not parse provided parameters: " + E.getMessage());
        } catch (Exception E) {
            LOGGER.error("Encountered an exception. See debug for more info.");
            E.printStackTrace();
        }
    }

    protected void executeCommand(String[] args, JCommander commander, StateFuzzerConfig stateFuzzerConfig)
            throws IOException {

        if (stateFuzzerConfig.isHelp()) {
            commander.usage();
            return;
        }

        String parentLogger = getBasePackageName();
        if (stateFuzzerConfig.isDebug()) {
            Configurator.setAllLevels(parentLogger, Level.DEBUG);
        } else if (stateFuzzerConfig.isQuiet()) {
            Configurator.setAllLevels(parentLogger, Level.ERROR);
        } else {
            Configurator.setAllLevels(parentLogger, Level.INFO);
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
            LOGGER.info("State-fuzzing a " + stateFuzzerConfig.getSulConfig().getFuzzingRole() + " implementation");

            // this is an extra step done to store the running arguments
            prepareOutputDir(args, stateFuzzerConfig.getOutput());

            stateFuzzerBuilder.build(stateFuzzerConfig).startFuzzing();
        }
    }

    protected String getBasePackageName(){
        String currentPackageName = this.getClass().getPackageName();
        // pattern matches {a}.{a}.{a}.{a}, where a is anything other than '.'
        // at first {a} (anything other than '.') and then 3 times '.{a}'
        // implying that basename is in format suffix.inner2.inner1.base
        Matcher matcher = Pattern.compile("[^\\.]*(\\.[^\\.]*){3}").matcher(currentPackageName);
        return matcher.find() ? matcher.group() : currentPackageName;
    }

    /*
     * Creates the output directory in advance in order to store in it the arguments file before the tool is executed.
     */
    protected void prepareOutputDir(String[] args, String dirPath) {
        File outputFolder = new File(dirPath);
        outputFolder.mkdirs();

        try {
            copyArgsToOutDir(args, dirPath);
        } catch (IOException E) {
            LOGGER.error("Failed to copy arguments file");
            E.printStackTrace();
            LOGGER.error(E);
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
