package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class Json {
    private static final com.google.gson.Gson G = new com.google.gson.GsonBuilder().disableHtmlEscaping().create();

    private Json() {
    }

    public static com.google.gson.Gson gson() {
        return G;
    }
}
