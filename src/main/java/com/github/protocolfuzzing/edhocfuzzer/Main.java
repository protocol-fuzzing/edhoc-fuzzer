package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.protocolstatefuzzer.entrypoints.CommandLineParser;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        MultiBuilder mb = new MultiBuilder();
        String[] parentLoggers = {Main.class.getPackageName()};

        CommandLineParser<?> commandLineParser = new CommandLineParser<>(mb, mb, mb, mb);
        commandLineParser.setExternalParentLoggers(parentLoggers);

        commandLineParser.parse(args, true, List.of(EdhocDotProcessor::beautify));
    }
}
