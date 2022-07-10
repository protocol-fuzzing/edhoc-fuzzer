package gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.keyConfigs;

import com.beust.jcommander.Parameter;

public class Ed25519KeySigConfig implements KeyConfig {

    @Parameter(names = "-mapEd25519PrivKeySig", description = "The private Ed25519 key DER file for the mapper")
    protected String mapPrivateFilename = null;

    @Parameter(names = "-mapEd25519PubKeySig", description = "The public Ed25519 key DER file for the mapper")
    protected String mapPublicFilename = null;

    @Parameter(names = "-mapEd25519X509CertSig", description = "The x509 certificate DER file signed with Ed25519 private "
            + "key of the mapper")
    protected String mapX509Filename = null;

    @Parameter(names = "-mapEd25519X5uLinkSig", description = "The x5u link for the mapper of Ed25519 x509 certificate")
    protected String mapX5uLink = null;

    @Parameter(names = "-sulEd25519PubKeySig", description = "The public Ed25519 key DER file for the sul")
    protected String sulPublicFilename = null;

    @Parameter(names = "-sulEd25519X509CertSig", description = "The x509 certificate DER file signed with the Ed25519 "
            + "private key of the sul")
    protected String sulX509Filename = null;

    @Parameter(names = "-sulEd25519X5uLinkSig", description = "The x5u link for the sul of Ed25519 x509 certificate")
    protected String sulX5uLink = null;

    @Override
    public String getMapPrivateFilename() {
        return mapPrivateFilename;
    }

    @Override
    public String getMapPublicFilename() {
        return mapPublicFilename;
    }

    @Override
    public String getMapX509Filename() {
        return mapX509Filename;
    }

    @Override
    public String getMapX5uLink() {
        return mapX5uLink;
    }

    @Override
    public String getSulPublicFilename() {
        return sulPublicFilename;
    }

    @Override
    public String getSulX509Filename() {
        return sulX509Filename;
    }

    @Override
    public String getSulX5uLink() {
        return sulX5uLink;
    }
}
