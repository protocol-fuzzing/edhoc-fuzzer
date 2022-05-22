package gr.ntua.softlab.edhocFuzzer;

import gr.ntua.softlab.protocolStateFuzzer.utils.CommandLineParser;

public class Main {
    public static void main(String[] args) {
        MultiBuilder mb = new MultiBuilder();
        CommandLineParser commandLineParser = new CommandLineParser(mb, mb, mb, mb);
        commandLineParser.parse(args);
    }
}
