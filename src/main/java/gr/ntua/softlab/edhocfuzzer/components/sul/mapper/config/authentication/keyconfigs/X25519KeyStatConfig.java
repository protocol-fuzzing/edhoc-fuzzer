package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.authentication.keyconfigs;

import com.beust.jcommander.Parameter;

import java.io.PrintWriter;
import java.util.HexFormat;

public class X25519KeyStatConfig implements KeyConfig {
    @Parameter(names = "-mapX25519PrivKeyStat", description = "The mapper's private X25519 key DER file")
    protected String mapPrivateFilename = null;

    @Parameter(names = "-mapX25519PubKeyStat", description = "The mapper's public X25519 key DER file")
    protected String mapPublicFilename = null;

    @Parameter(names = "-mapX25519KidStat", description = "The mapper's kid identifier of the X25519 authentication credential "
            + "(Restricted to hex numbers with prefix 0x, e.g. 0x01)")
    protected String mapKid = null;

    @Parameter(names = "-mapX25519X509CertStat", description = "The mapper's x509 certificate DER file containing the "
            + "X25519 public key")
    protected String mapX509Filename = null;

    @Parameter(names = "-mapX25519X5uLinkStat", description = "The mapper's x5u link of Ed25519 x509 certificate")
    protected String mapX5uLink = null;

    @Parameter(names = "-sulX25519PubKeyStat", description = "The SUL's public X25519 key DER file")
    protected String sulPublicFilename = null;

    @Parameter(names = "-sulX25519KidStat", description = "The SUL's kid identifier of the X25519 authentication credential "
            + "(Restricted to hex numbers with prefix 0x, e.g. 0x01)")
    protected String sulKid = null;

    @Parameter(names = "-sulX25519X509CertStat", description = "The SUL's x509 certificate DER file containing the "
            + "X25519 public key")
    protected String sulX509Filename = null;

    @Parameter(names = "-sulX25519X5uLinkStat", description = "The SUL's x5u link of X25519 x509 certificate")
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

    @Override
    public void printRunDescriptionSelf(PrintWriter printWriter) {
        printWriter.println("X25519KeyStatConfig Parameters");
        KeyConfig.super.printRunDescriptionSelf(printWriter);
    }
}
