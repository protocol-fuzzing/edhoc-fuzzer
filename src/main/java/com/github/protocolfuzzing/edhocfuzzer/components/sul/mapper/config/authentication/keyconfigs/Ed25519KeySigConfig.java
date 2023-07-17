package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.keyconfigs;

import com.beust.jcommander.Parameter;

import java.io.PrintWriter;
import java.util.HexFormat;

public class Ed25519KeySigConfig implements KeyConfig {

    @Parameter(names = "-mapEd25519PrivKeySig", description = "The mapper's private Ed25519 key DER file")
    protected String mapPrivateFilename = null;

    @Parameter(names = "-mapEd25519PubKeySig", description = "The mapper's public Ed25519 key DER file")
    protected String mapPublicFilename = null;

    @Parameter(names = "-mapEd25519KidSig", description = "The mapper's kid identifier of the Ed25519 authentication credential "
           + "(Restricted to hex numbers with prefix 0x, e.g. 0x01)")
    protected String mapKid = null;

    @Parameter(names = "-mapEd25519X509CertSig", description = "The mapper's x509 certificate DER file containing the "
            + "Ed25519 public key")
    protected String mapX509Filename = null;

    @Parameter(names = "-mapEd25519X5uLinkSig", description = "The mapper's x5u link of Ed25519 x509 certificate")
    protected String mapX5uLink = null;

    @Parameter(names = "-sulEd25519PubKeySig", description = "The SUL's public Ed25519 key DER file")
    protected String sulPublicFilename = null;

    @Parameter(names = "-sulEd25519KidSig", description = "The SUL's kid identifier of the Ed25519 authentication credential "
            + "(Restricted to hex numbers with prefix 0x, e.g. 0x01)")
    protected String sulKid = null;

    @Parameter(names = "-sulEd25519X509CertSig", description = "The SUL's x509 certificate DER file containing the "
            + "Ed25519 public key")
    protected String sulX509Filename = null;

    @Parameter(names = "-sulEd25519X5uLinkSig", description = "The SUL's x5u link of Ed25519 x509 certificate")
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
        printWriter.println("Ed25519KeySigConfig Parameters");
        KeyConfig.super.printRunDescriptionSelf(printWriter);
    }
}
