package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.keyconfigs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.statistics.RunDescriptionPrinter;


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
}
