package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.authentication;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.statistics.RunDescriptionPrinter;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.authentication.keyconfigs.Ed25519KeySigConfig;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.authentication.keyconfigs.P256KeySigConfig;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.authentication.keyconfigs.P256KeyStatConfig;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.authentication.keyconfigs.X25519KeyStatConfig;
import org.eclipse.californium.edhoc.Constants;

import java.io.PrintWriter;
import java.util.List;

public class ManyFilesAuthenticationConfig implements RunDescriptionPrinter {

    @Parameter(names = "-mapAuthMethod", description = "The authentication method of the mapper.")
    protected AuthMethod mapAuthenticationMethod = null;

    @Parameter(names = "-mapCipherSuites", description = "The comma-separated supported cipher suites of the mapper. "
            + "The cipher suite order defines the decreasing preference order. "
            + "Available cipher suites: [CS_0, CS_1, CS_2, CS_3]")
    protected List<CipherSuite> mapSupportedCipherSuites = null;

    @ParametersDelegate
    protected Ed25519KeySigConfig ed25519KeySigConfig;

    @ParametersDelegate
    protected X25519KeyStatConfig x25519KeyStatConfig;

    @ParametersDelegate
    protected P256KeySigConfig p256KeySigConfig;

    @ParametersDelegate
    protected P256KeyStatConfig p256KeyStatConfig;

    public ManyFilesAuthenticationConfig() {
        this.ed25519KeySigConfig = new Ed25519KeySigConfig();
        this.x25519KeyStatConfig = new X25519KeyStatConfig();
        this.p256KeySigConfig = new P256KeySigConfig();
        this.p256KeyStatConfig = new P256KeyStatConfig();
    }

    public Integer getMapAuthenticationMethod() {
        return mapAuthenticationMethod.toInteger();
    }

    public List<Integer> getMapSupportedCipherSuites() {
        return mapSupportedCipherSuites.stream().map(cs -> cs.toInteger()).toList();
    }

    public Ed25519KeySigConfig getEd25519KeySigConfig() {
        return ed25519KeySigConfig;
    }

    public X25519KeyStatConfig getX25519KeyStatConfig() {
        return x25519KeyStatConfig;
    }

    public P256KeySigConfig getP256KeySigConfig() {
        return p256KeySigConfig;
    }

    public P256KeyStatConfig getP256KeyStatConfig() {
        return p256KeyStatConfig;
    }

    public boolean isUsed() {
        return mapAuthenticationMethod != null && mapSupportedCipherSuites != null;
    }

    @Override
    public void printRunDescriptionSelf(PrintWriter printWriter) {
        printWriter.println("ManyFilesAuthenticationConfig Parameters");
        printWriter.println("Map Authentication Method" + mapAuthenticationMethod);
        printWriter.println("Map Supported Cipher Suites" + mapSupportedCipherSuites);
    }

    @Override
    public void printRunDescriptionRec(PrintWriter printWriter) {
        getEd25519KeySigConfig().printRunDescription(printWriter);
        getX25519KeyStatConfig().printRunDescription(printWriter);
        getP256KeySigConfig().printRunDescription(printWriter);
        getP256KeyStatConfig().printRunDescription(printWriter);
    }

    protected enum AuthMethod {
        M0_SigSig(Constants.EDHOC_AUTH_METHOD_0),
        M1_SigStat(Constants.EDHOC_AUTH_METHOD_1),
        M2_StatSig(Constants.EDHOC_AUTH_METHOD_2),
        M3_StatStat(Constants.EDHOC_AUTH_METHOD_3);

        private final Integer integer;

        private AuthMethod(final Integer integer) {
            this.integer = integer;
        }

        public Integer toInteger() {
            return this.integer;
        }
    }

    protected enum CipherSuite {
        CS_0(Constants.EDHOC_CIPHER_SUITE_0),
        CS_1(Constants.EDHOC_CIPHER_SUITE_1),
        CS_2(Constants.EDHOC_CIPHER_SUITE_2),
        CS_3(Constants.EDHOC_CIPHER_SUITE_3);

        private final Integer integer;

        private CipherSuite(final Integer integer) {
            this.integer = integer;
        }

        public Integer toInteger() {
            return this.integer;
        }
    }
}
