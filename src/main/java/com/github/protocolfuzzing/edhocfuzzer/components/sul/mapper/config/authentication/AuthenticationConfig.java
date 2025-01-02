package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.statistics.RunDescriptionPrinter;
import org.eclipse.californium.edhoc.Constants;

import java.io.PrintWriter;

public class AuthenticationConfig implements RunDescriptionPrinter {

    @Parameter(names = "-mapCredType", required = true, description = "The credential type of the mapper.")
    protected CredType mapCredType = null;

    @Parameter(names = "-mapIdCredType", required = true, description = "The credential identifier type of the mapper. "
            + "Notes: X5T is x509 cert by hash reference, X5U is x509 cert by retrieval link and X5CHAIN is x509 cert by value")
    protected IdCredType mapIdCredType = null;

    @Parameter(names = "-sulCredType", description = "The credential type of the SUL. "
            + "If it is not provided, then all possible types are generated.")
    protected CredType sulCredType = null;

    @Parameter(names = "-sulIdCredType", description = "The credential identifier type of the SUL. "
            + "Notes: X5T is x509 cert by hash reference, X5U is x509 cert by retrieval link and X5CHAIN is x509 cert by value. "
            + "If it is not provided, then all possible types are generated.")
    protected IdCredType sulIdCredType = null;

    @Parameter(names = "-trustModel", description = "Trust Model for verifying authentication credentials of the SUL. "
            + "Notes: NO_LEARNING means 'Trust and use only a stored and valid credential', "
            + "LEARNING means 'Trust and use any (new) valid credential'.")
    protected TrustModel trustModel = TrustModel.NO_LEARNING;

    @ParametersDelegate
    protected ManyFilesAuthenticationConfig manyFilesAuthenticationConfig;

    @ParametersDelegate
    protected TestVectorAuthenticationConfig testVectorAuthenticationConfig;

    public AuthenticationConfig() {
        this.manyFilesAuthenticationConfig = new ManyFilesAuthenticationConfig();
        this.testVectorAuthenticationConfig = new TestVectorAuthenticationConfig();
    }

    public Integer getMapCredType() {
        return mapCredType.toInteger();
    }

    public Integer getMapIdCredType() {
        return mapIdCredType.toInteger();
    }

    public Integer getSulCredType() {
        return sulCredType.toInteger();
    }

    public Integer getSulIdCredType() {
        return sulIdCredType.toInteger();
    }

    public Integer getTrustModel() {
        return trustModel.toInteger();
    }

    public ManyFilesAuthenticationConfig getManyFilesAuthenticationConfig() {
        return manyFilesAuthenticationConfig;
    }

    public TestVectorAuthenticationConfig getTestVectorAuthenticationConfig() {
        return testVectorAuthenticationConfig;
    }

    @Override
    public void printRunDescriptionSelf(PrintWriter printWriter) {
        printWriter.println("AuthenticationConfig Parameters");
        printWriter.println("Map Cred Type: " + mapCredType);
        printWriter.println("Map Id Cred Type: " + mapIdCredType);
        printWriter.println("Sul Cred Type: " + sulCredType);
        printWriter.println("Sul Id Cred Type: " + sulIdCredType);
        printWriter.println("Trust Model: " + trustModel);
    }

    @Override
    public void printRunDescriptionRec(PrintWriter printWriter) {
        if (getManyFilesAuthenticationConfig().isUsed()) {
            getManyFilesAuthenticationConfig().printRunDescription(printWriter);
        } else if (getTestVectorAuthenticationConfig().isUsed()) {
            getTestVectorAuthenticationConfig().printRunDescription(printWriter);
        }
    }

    protected enum CredType {
        CWT(Constants.CRED_TYPE_CWT),
        CCS(Constants.CRED_TYPE_CCS),
        X509(Constants.CRED_TYPE_X509);

        private final Integer integer;

        private CredType(final Integer integer) {
            this.integer = integer;
        }

        public Integer toInteger() {
            return this.integer;
        }
    }

    protected enum IdCredType {
        KID(Constants.ID_CRED_TYPE_KID),
        CWT(Constants.ID_CRED_TYPE_CWT),
        CCS(Constants.ID_CRED_TYPE_CCS),
        X5T(Constants.ID_CRED_TYPE_X5T),
        X5U(Constants.ID_CRED_TYPE_X5U),
        X5CHAIN(Constants.ID_CRED_TYPE_X5CHAIN);

        private final Integer integer;

        private IdCredType(final Integer integer) {
            this.integer = integer;
        }

        public Integer toInteger() {
            return this.integer;
        }
    }

    protected enum TrustModel {
        NO_LEARNING(Constants.TRUST_MODEL_NO_LEARNING),
        LEARNING(Constants.TRUST_MODEL_LEARNING);

        private final Integer integer;

        private TrustModel(final Integer integer) {
            this.integer = integer;
        }

        public Integer toInteger() {
            return this.integer;
        }
    }
}
