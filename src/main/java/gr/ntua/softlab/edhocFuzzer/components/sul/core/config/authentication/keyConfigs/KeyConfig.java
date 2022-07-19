package gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.keyConfigs;

public interface KeyConfig {
    String getMapPrivateFilename();

    String getMapPublicFilename();

    byte[] getMapKid();

    String getMapX509Filename();

    String getMapX5uLink();

    String getSulPublicFilename();

    byte[] getSulKid();

    String getSulX509Filename();

    String getSulX5uLink();
}
