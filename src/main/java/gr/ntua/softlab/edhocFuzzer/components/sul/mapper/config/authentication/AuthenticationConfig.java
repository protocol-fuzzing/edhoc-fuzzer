package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class AuthenticationConfig {

    @Parameter(names = "-mapCredType", required = true, description = "The credential type as an int for the mapper. "
            + "Available: 0 [CWT], 1 [CCS], 2 [x509 cert]")
    protected Integer mapCredType = null;

    @Parameter(names = "-mapIdCredType", required = true, description = "The credential identifier type as an int for "
            + "the mapper. Available: 0 [KID], 1 [CWT], 2 [CCS], 3 [X5T (x509 cert by hash reference)], "
            + "4 [X5U (x509 cert by retrieval link)], 5 [X5CHAIN (x509 cert by value)]")
    protected Integer mapIdCredType = null;

    @Parameter(names = "-sulCredType", description = "The credential type as an int for the sul. "
            + "Available: 0 [CWT], 1 [CCS], 2 [x509 cert]. In case of 'empty' all possible are generated.")
    protected Integer sulCredType = null;

    @Parameter(names = "-sulIdCredType", description = "The credential identifier type as an int for the "
            + "sul. Available: 0 [KID], 1 [CWT], 2 [CCS], 3 [X5T (x509 cert by hash reference)], "
            + "4 [X5U (x509 cert by retrieval link)], 5 [X5CHAIN (x509 cert by value)]. "
            + "In case of 'empty' all possible are generated.")
    protected Integer sulIdCredType = null;

    @Parameter(names = "-trustModel", description = "Trust Model for veifying authentication "
            + "credentials of other peers Available: "
            + "0 [Trust and use only a stored and valid credential] "
            + "1 [Trust and use a stored and valid credential or a valid credential with stored credential identifier] "
            + "2 [Trust and use any (new) valid credential]")
    protected Integer trustModel = 0;

    @ParametersDelegate
    protected ManyFilesAuthenticationConfig manyFilesAuthenticationConfig;

    @ParametersDelegate
    protected TestVectorAuthenticationConfig testVectorAuthenticationConfig;

    public AuthenticationConfig() {
        this.manyFilesAuthenticationConfig = new ManyFilesAuthenticationConfig();
        this.testVectorAuthenticationConfig = new TestVectorAuthenticationConfig();
    }

    public int getMapCredType() {
        return mapCredType;
    }

    public int getMapIdCredType() {
        return mapIdCredType;
    }

    public Integer getSulCredType() {
        return sulCredType;
    }

    public Integer getSulIdCredType() {
        return sulIdCredType;
    }

    public Integer getTrustModel() {
        return trustModel;
    }

    public ManyFilesAuthenticationConfig getManyFilesAuthenticationConfig() {
        return manyFilesAuthenticationConfig;
    }

    public TestVectorAuthenticationConfig getTestVectorAuthenticationConfig() {
        return testVectorAuthenticationConfig;
    }
}
