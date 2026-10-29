package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class LegacyMigration {
    private LegacyMigration() {
    }

    public static void runOnce(android.content.Context context) {
        java.lang.String str = "anthropic";
        android.content.SharedPreferences sharedPreferences = app.pelican.check.Prefs.get(context);
        if (sharedPreferences.getBoolean("legacyImported", false) || !app.pelican.check.Store.get(context).getLines().isEmpty()) {
            sharedPreferences.edit().putBoolean("legacyImported", true).apply();
            return;
        }
        try {
            java.lang.String string = context.getSharedPreferences("pelican", 0).getString("pelican.cfg.v3", "");
            if (string != null && !string.trim().isEmpty()) {
                com.google.gson.JsonObject asJsonObject = com.google.gson.JsonParser.parseString(string).getAsJsonObject();
                app.pelican.check.LineConfig lineConfig = new app.pelican.check.LineConfig();
                lineConfig.name = "原线路（已迁移）";
                lineConfig.baseUrl = str(asJsonObject, "base");
                lineConfig.apiKey = str(asJsonObject, "key");
                lineConfig.model = str(asJsonObject, "model");
                if (!"anthropic".equals(str(asJsonObject, "protocol"))) {
                    str = "openai";
                }
                lineConfig.protocol = str;
                lineConfig.effort = str(asJsonObject, "effort");
                lineConfig.customPrompt = str(asJsonObject, "prompt");
                lineConfig.promptId = "custom";
                for (app.pelican.check.LineConfig.Prompt prompt : app.pelican.check.LineConfig.PROMPTS) {
                    if (!prompt.id.equals("custom") && prompt.text.equals(lineConfig.customPrompt)) {
                        lineConfig.promptId = prompt.id;
                        break;
                    }
                }
                lineConfig.sampleCount = java.lang.Math.max(1, java.lang.Math.min(99, integer(asJsonObject, "count", 1)));
                lineConfig.maxTokens = integer(asJsonObject, "maxTok", 16000);
                try {
                    lineConfig.temperature = java.lang.Double.parseDouble(str(asJsonObject, "temp"));
                } catch (java.lang.Throwable th) {
                    lineConfig.temperature = 0.7d;
                }
                lineConfig.enabled = false;
                lineConfig.nextRunAt = 0L;
                if (!lineConfig.baseUrl.isEmpty() && !lineConfig.model.isEmpty()) {
                    app.pelican.check.Store.get(context).saveLine(lineConfig);
                }
                sharedPreferences.edit().putBoolean("legacyImported", true).apply();
                return;
            }
            sharedPreferences.edit().putBoolean("legacyImported", true).apply();
            sharedPreferences.edit().putBoolean("legacyImported", true).apply();
        } catch (java.lang.Throwable th2) {
        }
    }

    private static java.lang.String str(com.google.gson.JsonObject jsonObject, java.lang.String str) {
        try {
            return (!jsonObject.has(str) || jsonObject.get(str).isJsonNull()) ? "" : jsonObject.get(str).getAsString();
        } catch (java.lang.Throwable th) {
            return "";
        }
    }

    private static int integer(com.google.gson.JsonObject jsonObject, java.lang.String str, int i) {
        try {
            return jsonObject.get(str).getAsInt();
        } catch (java.lang.Throwable th) {
            return i;
        }
    }
}
