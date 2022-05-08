package gr.ntua.softlab.protocolStateFuzzer.utils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.*;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.config.TimingProbeEnabler;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.StateFuzzerBuilder;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.TestRunnerBuilder;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.TimingProbeBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class CommandLineParser {
    private static final Logger LOGGER = LogManager.getLogger(CommandLineParser.class);
    protected static final String CMD_STATE_FUZZER_CLIENT = "state-fuzzer-client";
    protected static final String CMD_STATE_FUZZER_SERVER = "state-fuzzer-server";
    protected static final String ARGS_FILE = "command.args";

    protected StateFuzzerBuilder stateFuzzerBuilder;
    protected StateFuzzerConfigBuilder stateFuzzerConfigBuilder;
    protected TestRunnerBuilder testRunnerBuilder;
    protected TimingProbeBuilder timingProbeBuilder;

    public CommandLineParser(StateFuzzerBuilder stateFuzzerBuilder, StateFuzzerConfigBuilder stateFuzzerConfigBuilder,
                             TestRunnerBuilder testRunnerBuilder, TimingProbeBuilder timingProbeBuilder){
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
                .programName("protocol-state-fuzzer")
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
                case CMD_STATE_FUZZER_CLIENT -> {
                    if (stateFuzzerClientConfig.isHelp()) {
                        commander.usage();
                        break;
                    }

                    stateFuzzerClientConfig.applyDelegate();
                    debugOptionCheck(stateFuzzerClientConfig);
                    LOGGER.info("State-fuzzing a client implementation");

                    // this is an extra step done to store the running arguments
                    prepareOutputDir(args, stateFuzzerClientConfig.getOutput());

                    stateFuzzerBuilder.build(stateFuzzerClientConfig).startFuzzing();
                }
                case CMD_STATE_FUZZER_SERVER -> {
                    if (stateFuzzerServerConfig.isHelp()) {
                        commander.usage();
                        break;
                    }

                    stateFuzzerServerConfig.applyDelegate();
                    debugOptionCheck(stateFuzzerServerConfig);
                    LOGGER.info("State-fuzzing a server implementation");

                    // this is an extra step done to store the running arguments
                    prepareOutputDir(args, stateFuzzerServerConfig.getOutput());

                    stateFuzzerBuilder.build(stateFuzzerServerConfig).startFuzzing();
                }
            }
        } catch (ParameterException E) {
            LOGGER.error("Could not parse provided parameters. " + E.getLocalizedMessage());
            LOGGER.debug(E);
            commander.usage();
        } catch (Exception E) {
            LOGGER.error("Encountered an exception. See debug for more info.");
            E.printStackTrace();
            LOGGER.error(E);
        }
    }

    /*
     * Checks if debug options have been supplied for launching the test runner/timing probe.
     * Executes these tools and exits if that is the case.
     */
    protected void debugOptionCheck(TestRunnerEnabler config) throws IOException {
        if (config.getTestRunnerConfig().getTest() != null) {
            LOGGER.info("Debug operation is engaged");
            if (config instanceof TimingProbeEnabler) {
                LOGGER.info("Running timing probe");
                timingProbeBuilder.build((TimingProbeEnabler) config).run();
            } else {
                LOGGER.info("Running test runner");
                testRunnerBuilder.build(config).run();
            }
            System.exit(0);
        }
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
