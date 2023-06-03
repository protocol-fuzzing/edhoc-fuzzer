package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.authentication;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestVector {
    @SerializedName("method")
    private Integer authenticationMethod;

    // this will be parsed from gson once
    @SerializedName("SUITES_I")
    private String cipherSuitesString;

    // this is the normal member
    private List<Integer> cipherSuites = null;

    @SerializedName("pk_i_raw")
    private String publicKeyI;

    @SerializedName("sk_i_raw")
    private String privateKeyI;

    @SerializedName("cred_i")
    private String credICbor;

    @SerializedName("id_cred_i")
    private String idCredICbor;

    @SerializedName("pk_r_raw")
    private String publicKeyR;

    @SerializedName("sk_r_raw")
    private String privateKeyR;

    @SerializedName("cred_r")
    private String credRCbor;

    @SerializedName("id_cred_r")
    private String idCredRCbor;

    public Integer getAuthenticationMethod() {
        return authenticationMethod;
    }

    public List<Integer> getCipherSuites() {
        if (cipherSuites == null) {
            // initialize cipherSuites variable
            cipherSuites = Arrays.stream(cipherSuitesString.split(",")).map(Integer::valueOf)
                    .collect(Collectors.toList());
        }
        return cipherSuites;
    }

    public String getPublicKey(boolean isInitiator) {
        return isInitiator ? publicKeyI : publicKeyR;
    }

    public String getPrivateKey(boolean isInitiator) {
        return isInitiator ? privateKeyI : privateKeyR;
    }

    public String getCredCbor(boolean isInitiator) {
        return isInitiator ? credICbor : credRCbor;
    }

    public String getIdCredCbor(boolean isInitiator) {
        return isInitiator ? idCredICbor : idCredRCbor;
    }
}
