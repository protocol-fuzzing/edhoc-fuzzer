package io.github.protocolfuzzing.edhocfuzzer;

import io.github.protocolfuzzing.protocolstatefuzzer.entrypoints.CommandLineParser;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Multibuilder implements all necessary builders
        MultiBuilder mb = new MultiBuilder();

        // single parentLogger, if Main resides in the outermost package
        String[] parentLoggers = {Main.class.getPackageName()};

        CommandLineParser<?> commandLineParser = new CommandLineParser<>(mb, mb, mb, mb, mb, mb, mb, mb, mb);
        commandLineParser.setExternalParentLoggers(parentLoggers);

        commandLineParser.process(args, true, List.of(EdhocDotProcessor::beautify));
    }
}
