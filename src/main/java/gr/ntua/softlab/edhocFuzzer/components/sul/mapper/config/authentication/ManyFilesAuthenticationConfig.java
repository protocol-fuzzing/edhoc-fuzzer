package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.keyConfigs.Ed25519KeySigConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.keyConfigs.P256KeySigConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.keyConfigs.P256KeyStatConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.keyConfigs.X25519KeyStatConfig;

import java.util.List;

public class ManyFilesAuthenticationConfig {
    @Parameter(names = "-mapAuthMethod", description = "The authentication method as an int for the mapper. "
            + "Available: 0 [Sig-Sig], 1 [Sig-Stat], 2 [Stat-Sig], 3 [Stat-Stat]")
    protected Integer mapAuthenticationMethod = null;

    @Parameter(names = "-mapCipherSuites", description = "The supported cipher suites as comma-separated ints. "
            + "The cipher suite order defines the decreasing  preference order. "
            + "Available: 0, 1, 2, 3 (details in RFC)")
    protected List<Integer> mapSupportedCipherSuites = null;

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
        return mapAuthenticationMethod;
    }

    public List<Integer> getMapSupportedCipherSuites() {
        return mapSupportedCipherSuites;
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
}
