package gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.keyConfigs;

import com.beust.jcommander.Parameter;

public class X25519KeyStatConfig implements KeyConfig {
    @Parameter(names = "-mapX25519PrivKeyStat", description = "The private X25519 key DER file for the mapper")
    protected String mapPrivateFilename = null;

    @Parameter(names = "-mapX25519PubKeyStat", description = "The public X25519 key DER file for the mapper")
    protected String mapPublicFilename = null;

    @Parameter(names = "-mapX25519X509CertStat", description = "The x509 certificate DER file signed with Ed25519 private "
            + "key of the mapper. (X25519 cannot sign certificates)")
    protected String mapX509Filename = null;

    @Parameter(names = "-mapX25519X5uLinkStat", description = "The x5u link for the mapper of Ed25519 x509 certificate")
    protected String mapX5uLink = null;

    @Parameter(names = "-sulX25519PubKeyStat", description = "The public X25519 key DER file for the sul")
    protected String sulPublicFilename = null;

    @Parameter(names = "-sulX25519X509CertStat", description = "The x509 certificate DER file signed with the Ed25519 "
            + "private key of the sul")
    protected String sulX509Filename = null;

    @Parameter(names = "-sulX25519X5uLinkStat", description = "The x5u link for the sul of X25519 x509 certificate")
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
