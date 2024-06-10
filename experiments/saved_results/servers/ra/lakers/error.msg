Index -1 out of bounds for length 16
java.lang.ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 16
	at de.learnlib.ralib.learning.CounterexampleAnalysis.binarySearch(CounterexampleAnalysis.java:263)
	at de.learnlib.ralib.learning.CounterexampleAnalysis.analyzeCounterexample(CounterexampleAnalysis.java:75)
	at de.learnlib.ralib.learning.rastar.RaStar.analyzeCounterExample(RaStar.java:173)
	at de.learnlib.ralib.learning.rastar.RaStar.learn(RaStar.java:113)
	at com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzerRA.inferRegisterAutomata(StateFuzzerRA.java:148)
	at com.github.protocolfuzzing.protocolstatefuzzer.statefuzzer.core.StateFuzzerRA.startFuzzing(StateFuzzerRA.java:89)
	at com.github.protocolfuzzing.protocolstatefuzzer.entrypoints.CommandLineParser.executeCommand(CommandLineParser.java:355)
	at com.github.protocolfuzzing.protocolstatefuzzer.entrypoints.CommandLineParser.parseAndExecuteCommand(CommandLineParser.java:226)
	at com.github.protocolfuzzing.protocolstatefuzzer.entrypoints.CommandLineParser.parse(CommandLineParser.java:168)
	at com.github.protocolfuzzing.edhocfuzzer.MainRA.main(MainRA.java:15)
