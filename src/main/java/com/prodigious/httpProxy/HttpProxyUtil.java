package com.prodigious.httpProxy;

public class HttpProxyUtil {
    public static String extractSessionId(String line)
            throws HttpProxyException {
        int idx = line.indexOf(':');

        if (idx == -1) {
            throw new HttpProxyException("Malformed Cookie");
        }

        line = line.substring(idx + 1);

        String sessionId= "";
        String[] parts = line.split(";");

        for (String part : parts) {
            part.trim().startsWith("SESSIONID");
            int id = part.indexOf('=');

            if (id == -1) {
                throw new HttpProxyException("Malformed session cookie");
            }
            sessionId = part.substring(id + 1);
            break;
        }
        return sessionId;
    }
}
