package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.keyConfigs;

import com.beust.jcommander.Parameter;

import java.io.PrintWriter;
import java.util.HexFormat;

public class P256KeySigConfig implements KeyConfig {

    @Parameter(names = "-mapP256PrivKeySig", description = "The mapper's private P-256 key DER file")
    protected String mapPrivateFilename = null;

    @Parameter(names = "-mapP256PubKeySig", description = "The mapper's public P-256 key DER file")
    protected String mapPublicFilename = null;

    @Parameter(names = "-mapP256KidSig", description = "The mapper's kid identifier of the P-256 authentication credential "
            + "(Restricted to hex numbers with prefix 0x, e.g. 0x01)")
    protected String mapKid = null;

    @Parameter(names = "-mapP256X509CertSig", description = "The mapper's x509 certificate DER file containing the "
            + "P-256 public key")
    protected String mapX509Filename = null;

    @Parameter(names = "-mapP256X5uLinkSig", description = "The mapper's x5u link of P-256 x509 certificate")
    protected String mapX5uLink = null;

    @Parameter(names = "-sulP256PubKeySig", description = "The SUL's public P-256 key DER file")
    protected String sulPublicFilename = null;

    @Parameter(names = "-sulP256KidSig", description = "The SUL's kid identifier of the P-256 authentication credential "
            + "(Restricted to hex numbers with prefix 0x, e.g. 0x01)")
    protected String sulKid = null;

    @Parameter(names = "-sulP256X509CertSig", description = "The SUL's x509 certificate DER file containing the "
            + "P-256 public key")
    protected String sulX509Filename = null;

    @Parameter(names = "-sulP256X5uLinkSig", description = "The SUL's x5u link of P-256 x509 certificate")
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
        printWriter.println("P256KeySigConfig Parameters");
        KeyConfig.super.printRunDescriptionSelf(printWriter);
    }
}
