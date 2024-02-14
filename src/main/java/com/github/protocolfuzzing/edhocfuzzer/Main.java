package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInput;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import com.github.protocolfuzzing.protocolstatefuzzer.entrypoints.CommandLineParser;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        MultiBuilder mb = new MultiBuilder();
        String[] parentLoggers = {Main.class.getPackageName()};

        CommandLineParser<EdhocInput, EdhocOutput> commandLineParser = new CommandLineParser<>(mb, mb, mb, mb);
        commandLineParser.setExternalParentLoggers(parentLoggers);

        commandLineParser.parse(args, true, List.of(EdhocDotProcessor::beautify));
    }
}
