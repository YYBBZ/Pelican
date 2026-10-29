import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Explicit configuration for manual, billable probes. Not part of the Android app. */
public final class ManualTestConfig {
    public final String baseUrl;
    public final String host;
    public final String model;
    public final Path outputDir;

    private ManualTestConfig(String baseUrl, String host, String model, Path outputDir) {
        this.baseUrl = baseUrl;
        this.host = host;
        this.model = model;
        this.outputDir = outputDir;
    }

    public static ManualTestConfig load(String version, boolean needsModel) throws Exception {
        String base = setting("PELICAN_TEST_BASE_URL");
        if (base.isEmpty()) throw new IllegalArgumentException("Set PELICAN_TEST_BASE_URL explicitly; no default provider is used");
        URI uri;
        try { uri = URI.create(base); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid PELICAN_TEST_BASE_URL; use an HTTPS URL without credentials"); }
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || host.equalsIgnoreCase("example.com") || host.equalsIgnoreCase("invalid")
                || host.toLowerCase(java.util.Locale.ROOT).endsWith(".invalid")) {
            throw new IllegalArgumentException("Use a real authorized HTTPS test URL, without embedded credentials, query or fragment");
        }
        String model = setting("PELICAN_TEST_MODEL");
        if (needsModel && model.isEmpty()) throw new IllegalArgumentException("Set PELICAN_TEST_MODEL before running a generation test");
        String output = setting("PELICAN_TEST_OUTPUT_DIR");
        Path root = Paths.get(System.getProperty("pelican.root", ".")).toAbsolutePath().normalize();
        Path dir = output.isEmpty() ? root.resolve("verification/local/" + version) : Paths.get(output).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return new ManualTestConfig(base, host, model, dir);
    }

    public static String readKey(String file) throws Exception {
        String key = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8).trim();
        if (key.isEmpty()) throw new IllegalArgumentException("The key file is empty");
        return key;
    }

    private static String setting(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim();
    }
}
