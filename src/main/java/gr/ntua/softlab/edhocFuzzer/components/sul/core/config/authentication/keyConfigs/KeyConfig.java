package gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.keyConfigs;

public interface KeyConfig {
    String getMapPrivateFilename();

    String getMapPublicFilename();

    String getMapX509Filename();

    String getMapX5uLink();

    String getSulPublicFilename();

    String getSulX509Filename();

    String getSulX5uLink();
}
