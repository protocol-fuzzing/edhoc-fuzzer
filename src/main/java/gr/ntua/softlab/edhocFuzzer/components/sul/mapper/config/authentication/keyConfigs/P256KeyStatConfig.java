package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.keyConfigs;

import com.beust.jcommander.Parameter;

import java.util.HexFormat;

public class P256KeyStatConfig implements KeyConfig {

    @Parameter(names = "-mapP256PrivKeyStat", description = "The private P-256 key DER file for the mapper")
    protected String mapPrivateFilename = null;

    @Parameter(names = "-mapP256PubKeyStat", description = "The public P-256 key DER file for the mapper")
    protected String mapPublicFilename = null;

    @Parameter(names = "-mapP256KidStat", description = "The kid identifier of the P-256 authentication credential "
            + "for the mapper (Restricted to hex numbers with prefix 0x, e.g. 0x01)")
    protected String mapKid = null;

    @Parameter(names = "-mapP256X509CertStat", description = "The x509 certificate DER file containing the "
            + "P-256 public key of the mapper")
    protected String mapX509Filename = null;

    @Parameter(names = "-mapP256X5uLinkStat", description = "The x5u link for the mapper of P-256 x509 certificate")
    protected String mapX5uLink = null;

    @Parameter(names = "-sulP256PubKeyStat", description = "The public P-256 key DER file for the sul")
    protected String sulPublicFilename = null;

    @Parameter(names = "-sulP256KidStat", description = "The kid identifier of the authentication credential "
            + "for the sul (Restricted to hex numbers with prefix 0x, e.g. 0x01)")
    protected String sulKid = null;

    @Parameter(names = "-sulP256X509CertStat", description = "The x509 certificate DER file containing the "
            + "P-256 public key of the sul")
    protected String sulX509Filename = null;

    @Parameter(names = "-sulP256X5uLinkStat", description = "The x5u link for the sul of P-256 x509 certificate")
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
    public byte[] getMapKid() {
        return mapKid == null ? null : HexFormat.of().withPrefix("0x").parseHex(mapKid);
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
    public byte[] getSulKid() {
        return sulKid == null ? null : HexFormat.of().withPrefix("0x").parseHex(sulKid);
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
