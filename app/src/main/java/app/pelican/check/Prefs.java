package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class Prefs {
    private Prefs() {
    }

    public static android.content.SharedPreferences get(android.content.Context context) {
        return context.getSharedPreferences("pelican_settings", 0);
    }
}
