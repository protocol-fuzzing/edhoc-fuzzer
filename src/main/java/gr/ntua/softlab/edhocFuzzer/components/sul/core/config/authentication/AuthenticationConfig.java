package gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.keyConfigs.Ed25519KeySigConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.keyConfigs.P256KeySigConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.keyConfigs.P256KeyStatConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.keyConfigs.X25519KeyStatConfig;
import org.eclipse.californium.edhoc.Constants;

import java.util.List;

public class AuthenticationConfig {
    @Parameter(names = "-mapAuthMethod", required = true, description = "The authentication method as an int for "
            + "the mapper. Available: 0 [Sig-Sig], 1 [Sig-Stat], 2 [Stat-Sig], 3 [Stat-Stat]")
    protected Integer mapAuthenticationMethod = null;

    @Parameter(names = "-mapCredType", required = true, description = "The credential type as an int for the mapper. "
            + "Available: 0 [CWT], 1 [CCS], 2 [x509 cert]")
    protected Integer mapCredType = null;

    @Parameter(names = "-mapIdCredType", required = true, description = "The credential identifier type as an int for the "
            + "mapper. Available: 0 [KID], 1 [CWT], 2 [CCS], 3 [X5T (x509 cert by hash reference)], "
            + "4 [X5U (x509 cert by retrieval link)], 5 [X5CHAIN (x509 cert by value)]")
    protected Integer mapIdCredType = null;

    @Parameter(names = "-mapCipherSuites", required = true , description = "The supported cipher suites as "
            + "comma-separated ints. The cipher suite order defines the decreasing  preference order. "
            + "Available: 0, 1, 2, 3 (details in RFC)")
    protected List<Integer> mapSupportedCipherSuites = null;

    @Parameter(names = "-sulCredType", description = "The credential type as an int for the sul. "
            + "Available: 0 [CWT], 1 [CCS], 2 [x509 cert]. In case of 'empty' all possible are generated.")
    protected Integer sulCredType = null;

    @Parameter(names = "-sulIdCredType", description = "The credential identifier type as an int for the "
            + "sul. Available: 0 [KID], 1 [CWT], 2 [CCS], 3 [X5T (x509 cert by hash reference)], "
            + "4 [X5U (x509 cert by retrieval link)], 5 [X5CHAIN (x509 cert by value)]. "
            + "In case of null all possible are generated.")
    protected Integer sulIdCredType = null;

    @ParametersDelegate
    protected Ed25519KeySigConfig ed25519KeySigConfig;

    @ParametersDelegate
    protected X25519KeyStatConfig x25519KeyStatConfig;

    @ParametersDelegate
    protected P256KeySigConfig p256KeySigConfig;

    @ParametersDelegate
    protected P256KeyStatConfig p256KeyStatConfig;

    public AuthenticationConfig() {
        this.ed25519KeySigConfig = new Ed25519KeySigConfig();
        this.x25519KeyStatConfig = new X25519KeyStatConfig();
        this.p256KeySigConfig = new P256KeySigConfig();
        this.p256KeyStatConfig = new P256KeyStatConfig();
    }

    public Integer getMapAuthenticationMethod() {
        return mapAuthenticationMethod;
    }

    public int getMapCredType() {
        return mapCredType;
    }

    public int getMapIdCredType() {
        return mapIdCredType;
    }

    public List<Integer> getMapSupportedCipherSuites() {
        return mapSupportedCipherSuites;
    }

    public Integer getSulCredType() {
        return sulCredType;
    }

    public Integer getSulIdCredType() {
        return sulIdCredType;
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
}
