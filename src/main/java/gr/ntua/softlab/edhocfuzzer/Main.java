package gr.ntua.softlab.edhocfuzzer;

import com.github.protocolfuzzing.protocolstatefuzzer.utils.CommandLineParser;

public class Main {
    public static void main(String[] args) {
        // multibuilder implements all necessary builders
        MultiBuilder mb = new MultiBuilder();

        // single parentLogger, only for this package
        String[] parentLoggers = {Main.class.getPackageName()};

        CommandLineParser commandLineParser = new CommandLineParser(mb, mb, mb, mb, parentLoggers);
        commandLineParser.parse(args);
    }
}
