package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import org.eclipse.californium.edhoc.AppProfile;

import java.util.Set;

public class AppProfileBuilder {

    public static AppProfile build(Set<Integer> authMethods) {
        // enable all options, thus mode 6
        return build(authMethods, 6);
    }

    public static AppProfile build(Set<Integer> authMethods, int mode) {
        // Use of message_4 as expected to be sent by the Responder
        boolean useMessage4;

        // Use of EDHOC for keying OSCORE
        boolean usedForOSCORE;

        // Supporting for the EDHOC+OSCORE request
        boolean supportCombinedRequest;

        switch (mode) {
            case 1 -> {
                // m3 no app
                useMessage4 = false;
                usedForOSCORE = false;
                supportCombinedRequest = false;
            }
            case 2 -> {
                // m3 app
                useMessage4 = false;
                usedForOSCORE = true;
                supportCombinedRequest = false;
            }
            case 3 -> {
                // m3 combined app
                useMessage4 = false;
                usedForOSCORE = true;
                supportCombinedRequest = true;
            }
            case 4 -> {
                // m4 no app
                useMessage4 = true;
                usedForOSCORE = false;
                supportCombinedRequest = false;
            }
            case 5 -> {
                // m4 app
                useMessage4 = true;
                usedForOSCORE = true;
                supportCombinedRequest = false;
            }
            case 6 -> {
                // all true (not in edhoc, just for state fuzzing)
                useMessage4 = true;
                usedForOSCORE = true;
                supportCombinedRequest = true;
            }
            default -> throw new RuntimeException(String.format(
                    "Invalid application profile mode: %d. Available application profile modes are 1, 2, 3, 4, 5, 6",
                    mode));
        }

        return new AppProfile(authMethods, useMessage4, usedForOSCORE, supportCombinedRequest);
    }
}
