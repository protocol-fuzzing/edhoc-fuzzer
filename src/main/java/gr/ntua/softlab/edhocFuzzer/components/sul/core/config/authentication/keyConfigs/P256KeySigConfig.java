package gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.keyConfigs;

import com.beust.jcommander.Parameter;

public class P256KeySigConfig implements KeyConfig {

    @Parameter(names = "-mapP256PrivKeySig", description = "The private P-256 key DER file for the mapper")
    protected String mapPrivateFilename = null;

    @Parameter(names = "-mapP256PubKeySig", description = "The public P-256 key DER file for the mapper")
    protected String mapPublicFilename = null;

    @Parameter(names = "-mapP256X509CertSig", description = "The x509 certificate DER file signed with P-256 private "
            + "key of the mapper")
    protected String mapX509Filename = null;

    @Parameter(names = "-mapP256X5uLinkSig", description = "The x5u link for the mapper of P-256 x509 certificate")
    protected String mapX5uLink = null;

    @Parameter(names = "-sulP256PubKeySig", description = "The public P-256 key DER file for the sul")
    protected String sulPublicFilename = null;

    @Parameter(names = "-sulP256X509CertSig", description = "The x509 certificate DER file signed with P-256 private "
            + "key of the sul")
    protected String sulX509Filename = null;

    @Parameter(names = "-sulP256X5uLinkSig", description = "The x5u link for the sul of P-256 x509 certificate")
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
