package gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol;

import net.i2p.crypto.eddsa.Utils;
import org.eclipse.californium.edhoc.Util;

public class EdhocUtil extends Util {

    /** Adapted from {@link org.eclipse.californium.edhoc.Util#nicePrint} */
    public static String byteArrayToString(String header, byte[] content) {
        if (content == null) {
            return "\n" + header + ": null\n";
        }

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

    public static String byteArrayToString(byte[] content) {
        StringBuilder stringBuilder = new StringBuilder();

        String contentStr = Utils.bytesToHex(content);
        for (int i = 0; i < (content.length * 2); i++) {
            stringBuilder.append(contentStr.charAt(i));

            if ((i % 2) == 1) {
                stringBuilder.append(" ");
            }
        }

        return stringBuilder.toString();
    }
}
