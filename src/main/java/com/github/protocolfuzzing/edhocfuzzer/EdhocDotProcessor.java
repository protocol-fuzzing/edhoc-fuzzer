package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.LearnerResult;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.config.StateFuzzerEnabler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EdhocDotProcessor {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void beautify(LearnerResult<?> learnerResult) {
        if (learnerResult.isFromTest()) {
            return;
        }

        if (learnerResult.isEmpty()) {
            LOGGER.warn("Provided empty LearnerResult");
            return;
        }

        if (learnerResult.getLearnedModelFile() == null) {
            LOGGER.warn("Provided null learned model file in LearnerResult");
            return;
        }

        StateFuzzerEnabler stateFuzzerEnabler = learnerResult.getStateFuzzerEnabler();
        if (stateFuzzerEnabler == null) {
            LOGGER.warn("Provided null StateFuzzerEnabler");
            return;
        }

        String script = "scripts/beautify_model.sh";
        String learnedModelPath = learnerResult.getLearnedModelFile().getAbsolutePath();
        List<String> commandArgList = new ArrayList<>();
        commandArgList.add(script);
        commandArgList.add(learnedModelPath);

        if (stateFuzzerEnabler.isFuzzingClient()) {
            // SUL is a client implementation
            MapperConfig mapperConfig = stateFuzzerEnabler.getSulConfig().getMapperConfig();

            if (!(mapperConfig instanceof EdhocMapperConfig)) {
                LOGGER.error("MapperConfig of StateFuzzerEnabler is not EdhocMapperConfig");
                return;
            }

            boolean isFuzzerInitiator = ((EdhocMapperConfig) mapperConfig).isInitiator();

            // when Fuzzer is Initiator then SUL is Responder
            // when Fuzzer is Responder then SUL is Initiator
            String cIR = isFuzzerInitiator ? "--clientResponder" : "--clientInitiator";
            commandArgList.add(1, cIR);
        }

        try {
            LOGGER.info("Running {}", script);
            new ProcessBuilder(commandArgList)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor();
        } catch (IOException | InterruptedException e) {
            LOGGER.warn("Could not beautify {}: {}", learnedModelPath, e.getMessage());
        }
    }
}
