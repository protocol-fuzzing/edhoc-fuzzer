package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer;


import de.learnlib.api.algorithm.LearningAlgorithm.MealyLearner;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.query.DefaultQuery;
import gr.ntua.softlab.protocolStateFuzzer.learner.LearnerResult;
import gr.ntua.softlab.protocolStateFuzzer.learner.StateMachine;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetBuilder;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearningConfig;
import gr.ntua.softlab.protocolStateFuzzer.learner.factory.EquivalenceAlgorithmName;
import gr.ntua.softlab.protocolStateFuzzer.learner.statistics.Statistics;
import gr.ntua.softlab.protocolStateFuzzer.learner.statistics.StatisticsTracker;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.StateFuzzerConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.sulWrappers.ExperimentTimeoutException;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Taken/adapted from StateVulnFinder tool (Extractor Component).
 */
public class StateFuzzerStandard implements StateFuzzer {
    private static final Logger LOGGER = LogManager.getLogger(StateFuzzerStandard.class);
    protected StateFuzzerComposer stateFuzzerComposer;
    protected Alphabet<AbstractInput> alphabet;
    protected File outputFolder;
    protected CleanupTasks cleanupTasks;
    protected StateFuzzerConfig stateFuzzerConfig;

    public StateFuzzerStandard(StateFuzzerComposer stateFuzzerComposer) {
        this.stateFuzzerComposer = stateFuzzerComposer;
        this.stateFuzzerConfig = stateFuzzerComposer.getStateFuzzerConfig();
        this.alphabet = stateFuzzerComposer.getAlphabet();
        this.outputFolder = stateFuzzerComposer.getOutputFolder();
        this.cleanupTasks = stateFuzzerComposer.getCleanupTasks();
    }

    @Override
    public void startFuzzing() {
        try {
            inferStateMachine();
        } catch (RuntimeException e) {
            cleanupTasks.execute();
            throw e;
        }
        cleanupTasks.execute();
    }


    protected void inferStateMachine() {
        // for convenience, we copy all the input files/streams
        // to the output folder before starting the arduous learning process
        copyInputsToOutputFolder(outputFolder);

        // setting up statistics tracker, learner and equivalence oracle
        StatisticsTracker statisticsTracker = stateFuzzerComposer.getStatisticsTracker();

        MealyLearner<AbstractInput, AbstractOutput> learner = stateFuzzerComposer.getLearner();

        EquivalenceOracle<MealyMachine<?, AbstractInput, ?, AbstractOutput>, AbstractInput, Word<AbstractOutput>>
                equivalenceOracle = stateFuzzerComposer.getEquivalenceOracle();

        // running learning and collecting important statistics
        MealyMachine<?, AbstractInput, ?, AbstractOutput> hypothesis;
        StateMachine stateMachine = null;
        LearnerResult learnerResult = new LearnerResult();
        DefaultQuery<AbstractInput, Word<AbstractOutput>> counterExample;
        boolean finished = false;
        String notFinishedReason = null;
        int rounds = 0;

        try {
            statisticsTracker.setRuntimeStateTracking(new FileOutputStream(
                    new File(outputFolder, LEARNING_STATE_FILENAME)));
        } catch (FileNotFoundException e1) {
            throw new RuntimeException("Could not create runtime state tracking output stream");
        }

        statisticsTracker.startLearning(stateFuzzerConfig, alphabet);
        learner.startLearning();

        try {
            do {
                hypothesis = learner.getHypothesisModel();
                stateMachine = new StateMachine(hypothesis, alphabet);
                learnerResult.addHypothesis(stateMachine);
                // it is useful to print intermediate hypothesis as learning is running
                serializeHypothesis(stateMachine, outputFolder, "hyp" + (rounds + 1) + ".dot", false);
                statisticsTracker.newHypothesis(stateMachine);
                counterExample = equivalenceOracle.findCounterExample(hypothesis, alphabet);
                if (counterExample != null) {
                    LOGGER.warn("Counterexample: " + counterExample);
                    statisticsTracker.newCounterExample(counterExample);
                    // we create a copy, since the hypothesis reference will not be valid after refinement,
                    // but we may still need it (if learning abruptly terminates)
                    stateMachine = stateMachine.copy();
                    learner.refineHypothesis(counterExample);
                }
                rounds++;
            } while (counterExample != null);

            finished = true;

        } catch (ExperimentTimeoutException exc) {
            LOGGER.fatal("Learning timed out after a duration of " + exc.getDuration() + " (i.e. "
                    + exc.getDuration().toHours() + " hours, or" + exc.getDuration().toMinutes() + " minutes" + " )");
            notFinishedReason = "learning timed out";

        } catch (Exception exc) {
            notFinishedReason = exc.getMessage();
            LOGGER.fatal("Exception generated during learning");
            // useful to log what actually went wrong
            try (FileWriter fw = new FileWriter(new File(outputFolder, ERROR_FILENAME))) {
                PrintWriter pw = new PrintWriter(fw);
                pw.println(exc.getMessage());
                exc.printStackTrace(pw);
                pw.close();
            } catch (IOException e) {
                LOGGER.fatal("Could not create error file writer");
            }
        }

        // building results:
        statisticsTracker.finishedLearning(stateMachine, finished, notFinishedReason);
        Statistics statistics = statisticsTracker.generateStatistics();

        LOGGER.info("Finished Experiment");
        LOGGER.info("Number of Rounds:" + rounds);
        LOGGER.info(statistics.toString());

        learnerResult.setLearnedModel(stateMachine);
        learnerResult.setStatistics(statistics);

        // exporting to output files
        serializeHypothesis(stateMachine, outputFolder, LEARNED_MODEL_FILENAME, true);

        // we disable this feature for now, as models are too large for it
        // serializeHypothesis(stateMachine, outputFolder,
        //        LEARNED_MODEL_FILENAME.replace(".dot", "FullOutput.dot"), false, true);

        learnerResult.setLearnedModelFile(new File(outputFolder, LEARNED_MODEL_FILENAME));

        try {
            statistics.export(new FileWriter(new File(outputFolder, STATISTICS_FILENAME)));
        } catch (IOException e) {
            LOGGER.fatal("Could not copy statistics to output folder");
        }
    }

    protected void copyInputsToOutputFolder(File outputFolder) {
        try {
            Path originalAlphabetPath = AlphabetBuilder.getAlphabetFile(stateFuzzerConfig).toPath();
            Path outputAlphabetPath = Path.of(outputFolder.getPath(), ALPHABET_FILENAME);
            Files.copy(originalAlphabetPath, outputAlphabetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.fatal("Could not copy alphabet to output folder");
        }

        LearningConfig learningConfig = stateFuzzerConfig.getLearningConfig();
        if (learningConfig.getEquivalenceAlgorithms().contains(EquivalenceAlgorithmName.SAMPLED_TESTS)) {
            try {
                Path originalTestFilePath = Path.of(learningConfig.getTestFile());
                Path outputTestFilePath = Path.of(outputFolder.getPath(), learningConfig.getTestFile());
                Files.copy(originalTestFilePath, outputTestFilePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOGGER.fatal("Could not copy sampled tests file to output folder");
            }
        }

        try {
            dumpToFile(stateFuzzerConfig.getSulConfig().getMapperToSulConfigInputStream(),
                    new File(outputFolder, MAPPER_TO_SUL_CONFIG_FILENAME));
        } catch (IOException e) {
            LOGGER.fatal("Could not copy mapper_to_sul configuration to output folder");
        }
    }

    protected void dumpToFile(InputStream inputStream, File outputFile) throws IOException {
        try (inputStream; FileOutputStream fw = new FileOutputStream(outputFile)) {
            byte[] bytes = new byte[1000];
            while (inputStream.read(bytes) > 0) {
                fw.write(bytes);
            }
        }
    }

    protected void serializeHypothesis(StateMachine hypothesis, File folder, String name, boolean genPdf) {
        if (hypothesis != null) {
            File graphFile = new File(folder, name);
            hypothesis.export(graphFile, genPdf);
        } else {
            LOGGER.info("Provided null hypothesis to be serialized");
        }
    }

}
