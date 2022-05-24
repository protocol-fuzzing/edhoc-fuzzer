package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer;

public interface StateFuzzer {
    String LEARNED_MODEL_FILENAME = "learnedModel.dot";
    String STATISTICS_FILENAME = "statistics.txt";
    String MAPPER_TO_SUL_CONFIG_FILENAME = "mapper_to_sul.config";
    String ALPHABET_FILENAME = "alphabet.xml";
    String ERROR_FILENAME = "error.msg";
    String LEARNING_STATE_FILENAME = "state.log";

    void startFuzzing();

}
