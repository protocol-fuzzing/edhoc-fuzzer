package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.keyconfigs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.statistics.RunDescriptionPrinter;

import java.io.PrintWriter;

public interface KeyConfig extends RunDescriptionPrinter {
    public String getMapPrivateFilename();

    public String getMapPublicFilename();

    public byte[] getMapKid();

    public String getMapX509Filename();

    public String getMapX5uLink();

    public String getSulPublicFilename();

    public byte[] getSulKid();

    public String getSulX509Filename();

    public String getSulX5uLink();

    @Override
    default void printRunDescriptionSelf(PrintWriter printWriter) {
        printWriter.println("Map Private Filename: " + getMapPrivateFilename());
        printWriter.println("Map Public Filename: " + getMapPublicFilename());
        printWriter.println("Map Kid: " + getMapKid());
        printWriter.println("Map X509 Filename: " + getMapX509Filename());
        printWriter.println("Map X5u Link: " + getMapX5uLink());
        printWriter.println("Sul Public Filename: " + getSulPublicFilename());
        printWriter.println("Sul Kid: " + getSulKid());
        printWriter.println("Sul X509 Filename: " + getSulX509Filename());
        printWriter.println("Sul X5u Link: " + getSulX5uLink());
    }
}
