package com.comanda;

/** Central domain fixture matching the app.domain system property configured by Maven Surefire. */
public final class TestDomain {

    private TestDomain() {}

    public static String root() {
        String domain = System.getProperty("app.domain");
        if (domain == null || domain.isBlank()) {
            throw new IllegalStateException("The backend test domain is not configured");
        }
        return domain;
    }

    public static String host(String subdomain) {
        return subdomain + "." + root();
    }

    public static String origin(String scheme, String subdomain) {
        return scheme + "://" + host(subdomain);
    }
}
