package gr.ntua.softlab.protocolStateFuzzer.sul.config;

import com.beust.jcommander.Parameter;
import gr.ntua.softlab.protocolStateFuzzer.sul.sulWrappers.ProcessLaunchTrigger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public abstract class SulConfig {
    public static final String MAPPER_TO_SUL_CONFIG = "mapper_to_sul.config";
    public static final String FUZZER_DIR = "fuzzer.dir";
    public static final String SULS_DIR = "suls.dir";

    @Parameter(names = "-protocolVersion", required = true, description = "Protocol version to be analyzed",
            converter = ProtocolVersionConverter.class)
    protected ProtocolVersion protocolVersion = null;

    @Parameter(names = "-timeout", description = "Time the SUL spends waiting for a response")
    protected Integer timeout = 100;

    @Parameter(names = "-inputResponseTimeout", description = "Time the SUL spends waiting for a "
            + "response to a particular input. Expected format is: \"input1:value1,input2:value2...\" ",
            converter = InputResponseTimeoutConverter.class)
    protected InputResponseTimeoutMap inputResponseTimeout;

    @Parameter(names = "-rstWait", description = "Time (ms) the SUL waits after executing each query")
    protected Long resetWait = 0L;

    @Parameter(names = {"-command", "-cmd"}, description = "Command for starting the client/server process")
    protected String command = null;

    @Parameter(names = {"-terminateCommand", "-termCmd"}, description = "Command for terminating "
            + "the client/server process. If specified, it is used instead of java.lang.Process#destroy()")
    protected String terminateCommand = null;

    @Parameter(names = "-processDir", description = "The directory of the client/server process")
    protected String processDir = null;

    @Parameter(names = "-processTrigger", description = "When is the process launched")
    protected ProcessLaunchTrigger processTrigger = ProcessLaunchTrigger.NEW_TEST;

    @Parameter(names = "-runWait", description = "Time (ms) waited after running each command")
    protected Long runWait = 0L;

    // In case a launch server is used to execute the SUL
    @Parameter(names = "-resetPort", description = "Port to which to send a reset command")
    protected Integer resetPort = null;

    @Parameter(names = "-resetAddress", description = "Address to which to send a reset command")
    protected String resetAddress = "localhost";

    @Parameter(names = "-resetCommandWait", description = "Time (ms) waited after sending a reset command")
    protected Long resetCommandWait = 0L;

    @Parameter(names = "-resetAck", description = "Wait from acknowledgement from the other side")
    protected boolean resetAck = false;

    @Parameter(names = "-mapperToSulConfig", description = "Configuration for the Mapper to SUL connection")
    protected String mapperToSulConfig = null;

    public abstract String getRole();

    public abstract boolean isClient();

    public abstract void applyDelegate(MapperToSulConfig config) throws MapperToSulConfigException;

    public InputStream getMapperToSulConfigInputStream() throws IOException {
        if (mapperToSulConfig == null) {
            return Objects.requireNonNull(SulConfig.class.getResource(MAPPER_TO_SUL_CONFIG)).openStream();
        } else {
            return new FileInputStream(mapperToSulConfig);
        }
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public InputResponseTimeoutMap getInputResponseTimeout() {
        return inputResponseTimeout;
    }

    public Long getResetWait() {
        return resetWait;
    }

    public String getCommand() {
        return command;
    }

    public String getTerminateCommand() {
        return terminateCommand;
    }

    public String getProcessDir() {
        return processDir;
    }

    public ProcessLaunchTrigger getProcessTrigger() {
        return processTrigger;
    }

    public Long getRunWait() {
        return runWait;
    }

    public void setRunWait(Long runWait) {
        this.runWait = runWait;
    }

    public Integer getResetPort() {
        return resetPort;
    }

    public String getResetAddress() {
        return resetAddress;
    }

    public Long getResetCommandWait() {
        return resetCommandWait;
    }

    public boolean isResetAck() {
        return resetAck;
    }

}
