package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import net.i2p.crypto.eddsa.Utils;
import org.eclipse.californium.edhoc.Util;

public class EdhocUtil extends Util {
    
    /** Adapted from {@link org.eclipse.californium.edhoc.Util#nicePrint} */
    public static String byteArrayToString(String header, byte[] content) {
        String headerStr = header + " (" + content.length + " bytes):";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n").append(headerStr).append("\n");

        String contentStr = Utils.bytesToHex(content);
        for (int i = 0; i < (content.length * 2); i++) {
            if ((i != 0) && (i % 20) == 0) {
                stringBuilder.append("\n");
            }

            stringBuilder.append(contentStr.charAt(i));

            if ((i % 2) == 1) {
                stringBuilder.append(" ");
            }
        }

        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
