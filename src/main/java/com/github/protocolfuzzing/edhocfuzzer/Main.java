package com.github.protocolfuzzing.edhocfuzzer;

import com.github.protocolfuzzing.protocolstatefuzzer.entrypoints.CommandLineParser;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        boolean concretize = false;
        ArrayList<String> newArgs = new ArrayList<String>();
        for (String arg : args) {
            if (arg.equals("-concretize")) {
                concretize = true;
                continue;
            }
            newArgs.add(arg);
        }
        MultiBuilder mb = new MultiBuilder(concretize);
        String[] parentLoggers = {Main.class.getPackageName()};

        CommandLineParser commandLineParser = new CommandLineParser(mb, mb, mb, mb);
        commandLineParser.setExternalParentLoggers(parentLoggers);

        commandLineParser.parse(newArgs.toArray(new String[0]), true, List.of(EdhocDotProcessor::beautify));
    }
}
