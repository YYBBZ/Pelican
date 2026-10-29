package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class BackupUtil {

    public static class Result {
        public int lines;
        public java.lang.String message = "";
        public int runs;
    }

    private BackupUtil() {
    }

    public static void export(android.content.Context context, java.io.OutputStream outputStream) throws java.lang.Exception {
        app.pelican.check.Store store = app.pelican.check.Store.get(context);
        java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream(outputStream);
        com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();
        jsonObject.addProperty("format", "pelican-backup");
        jsonObject.addProperty("version", (java.lang.Number) 2);
        jsonObject.addProperty("exportedAt", java.lang.Long.valueOf(java.lang.System.currentTimeMillis()));
        put(zipOutputStream, "manifest.json", app.pelican.check.Json.gson().toJson((com.google.gson.JsonElement) jsonObject).getBytes("UTF-8"));
        java.util.ArrayList arrayList = new java.util.ArrayList();
        java.util.Iterator<app.pelican.check.LineConfig> it = store.getLines().iterator();
        while (it.hasNext()) {
            app.pelican.check.LineConfig lineConfigCopyWithoutKey = it.next().copyWithoutKey();
            lineConfigCopyWithoutKey.nextRunAt = 0L;
            lineConfigCopyWithoutKey.enabled = false;
            arrayList.add(lineConfigCopyWithoutKey);
        }
        put(zipOutputStream, "lines.json", app.pelican.check.Json.gson().toJson(arrayList).getBytes("UTF-8"));
        put(zipOutputStream, "baselines.json", app.pelican.check.Json.gson().toJson(store.getBaselines()).getBytes("UTF-8"));
        app.pelican.check.Store.RunFilter runFilter = new app.pelican.check.Store.RunFilter();
        runFilter.completedOnly = true;
        java.util.List<app.pelican.check.RunData> runs = store.getRuns(runFilter, kotlin.time.DurationKt.NANOS_IN_MILLIS, 0);
        put(zipOutputStream, "runs.json", app.pelican.check.Json.gson().toJson(runs).getBytes("UTF-8"));
        java.util.Iterator<app.pelican.check.RunData> it2 = runs.iterator();
        while (it2.hasNext()) {
            for (app.pelican.check.RunData.SampleData sampleData : it2.next().samples) {
                putFile(zipOutputStream, sampleData.svgPath, "svgs/" + new java.io.File(sampleData.svgPath == null ? "" : sampleData.svgPath).getName());
                putFile(zipOutputStream, sampleData.thumbPath, "thumbs/" + new java.io.File(sampleData.thumbPath == null ? "" : sampleData.thumbPath).getName());
                for (app.pelican.check.RunData.AttemptData attemptData : sampleData.attempts) {
                    putFile(zipOutputStream, attemptData.outputPath, "responses/" + new java.io.File(attemptData.outputPath == null ? "" : attemptData.outputPath).getName());
                }
            }
        }
        zipOutputStream.finish();
    }

    private static void put(java.util.zip.ZipOutputStream zipOutputStream, java.lang.String str, byte[] bArr) throws java.lang.Exception {
        zipOutputStream.putNextEntry(new java.util.zip.ZipEntry(str));
        zipOutputStream.write(bArr);
        zipOutputStream.closeEntry();
    }

    private static void putFile(java.util.zip.ZipOutputStream zipOutputStream, java.lang.String str, java.lang.String str2) {
        if (str == null) {
            return;
        }
        try {
            if (str.isEmpty()) {
                return;
            }
            java.io.File file = new java.io.File(str);
            if (!file.isFile()) {
                return;
            }
            zipOutputStream.putNextEntry(new java.util.zip.ZipEntry(str2));
            java.io.FileInputStream fileInputStream = new java.io.FileInputStream(file);
            byte[] bArr = new byte[8192];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i < 0) {
                    fileInputStream.close();
                    zipOutputStream.closeEntry();
                    return;
                }
                zipOutputStream.write(bArr, 0, i);
            }
        } catch (java.lang.Throwable th) {
        }
    }

    public static app.pelican.check.BackupUtil.Result importBackup(android.content.Context context, java.io.InputStream inputStream) throws java.lang.Exception {
        int i;
        app.pelican.check.Store store = app.pelican.check.Store.get(context);
        java.io.File file = new java.io.File(context.getCacheDir(), "import-" + java.lang.System.currentTimeMillis());
        file.mkdirs();
        try {
            java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(inputStream);
            byte[] bArr = new byte[8192];
            while (true) {
                java.util.zip.ZipEntry nextEntry = zipInputStream.getNextEntry();
                if (nextEntry == null) {
                    break;
                }
                if (!nextEntry.isDirectory()) {
                    java.io.File file2 = new java.io.File(file, nextEntry.getName().replace("..", "_"));
                    if (file2.getCanonicalPath().startsWith(file.getCanonicalPath() + java.io.File.separator)) {
                        file2.getParentFile().mkdirs();
                        java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(file2);
                        while (true) {
                            int i2 = zipInputStream.read(bArr);
                            if (i2 < 0) {
                                break;
                            }
                            fileOutputStream.write(bArr, 0, i2);
                        }
                        fileOutputStream.close();
                        zipInputStream.closeEntry();
                    }
                }
            }
            java.io.File file3 = new java.io.File(file, "manifest.json");
            if (!file3.isFile()) {
                throw new java.lang.Exception("不是鹈鹕监控备份（缺少 manifest）");
            }
            com.google.gson.JsonObject jsonObject = (com.google.gson.JsonObject) app.pelican.check.Json.gson().fromJson(app.pelican.check.FileUtil.readText(file3.getPath()), com.google.gson.JsonObject.class);
            if (!jsonObject.has("format") || !"pelican-backup".equals(jsonObject.get("format").getAsString())) {
                throw new java.lang.Exception("备份格式不匹配，未导入任何数据");
            }
            app.pelican.check.BackupUtil.Result result = new app.pelican.check.BackupUtil.Result();
            java.io.File file4 = new java.io.File(file, "lines.json");
            if (file4.isFile()) {
                for (app.pelican.check.LineConfig lineConfig : (app.pelican.check.LineConfig[]) app.pelican.check.Json.gson().fromJson(app.pelican.check.FileUtil.readText(file4.getPath()), app.pelican.check.LineConfig[].class)) {
                    if (store.getLine(lineConfig.id) == null) {
                        lineConfig.apiKey = "";
                        lineConfig.enabled = false;
                        lineConfig.nextRunAt = 0L;
                        lineConfig.lastStatus = "";
                        lineConfig.lastBadge = "";
                        lineConfig.consecutiveIssues = 0;
                        store.saveLine(lineConfig);
                        result.lines++;
                    }
                }
            }
            java.io.File file5 = new java.io.File(file, "baselines.json");
            if (file5.isFile()) {
                for (app.pelican.check.Baseline baseline : (app.pelican.check.Baseline[]) app.pelican.check.Json.gson().fromJson(app.pelican.check.FileUtil.readText(file5.getPath()), app.pelican.check.Baseline[].class)) {
                    store.saveBaseline(baseline);
                }
            }
            java.io.File file6 = new java.io.File(file, "runs.json");
            if (file6.isFile()) {
                for (app.pelican.check.RunData runData : (app.pelican.check.RunData[]) app.pelican.check.Json.gson().fromJson(app.pelican.check.FileUtil.readText(file6.getPath()), app.pelican.check.RunData[].class)) {
                    if (store.getRun(runData.id) == null) {
                        for (app.pelican.check.RunData.SampleData sampleData : runData.samples) {
                            sampleData.svgPath = restoreFile(context, file, "svgs", sampleData.svgPath);
                            sampleData.thumbPath = restoreFile(context, file, "thumbs", sampleData.thumbPath);
                            for (app.pelican.check.RunData.AttemptData attemptData : sampleData.attempts) {
                                attemptData.outputPath = restoreFile(context, file, "responses", attemptData.outputPath);
                            }
                        }
                        if (runData.isActive()) {
                            runData.status = "FAIL";
                            runData.interrupted = true;
                            runData.endedAt = java.lang.System.currentTimeMillis();
                            runData.summary = "导入的历史包含未完成任务，已标记中断，不重新请求";
                        }
                        store.insertRun(runData);
                        result.runs++;
                    }
                }
            }
            result.message = "导入完成：新增 " + result.lines + " 条线路、" + result.runs + " 次历史；请重新填写密钥";
            deleteRecursive(file);
            store.migrate24Async();
            return result;
        } catch (java.lang.Throwable th) {
            deleteRecursive(file);
            throw th;
        }
    }

    private static java.lang.String restoreFile(android.content.Context context, java.io.File file, java.lang.String str, java.lang.String str2) {
        try {
            if (str2 == null) {
                str2 = "";
            }
            java.lang.String name = new java.io.File(str2).getName();
            if (name.isEmpty()) {
                return "";
            }
            java.io.File file2 = new java.io.File(new java.io.File(file, str), name);
            if (!file2.isFile()) {
                return "";
            }
            java.io.File file3 = new java.io.File(context.getFilesDir(), str);
            file3.mkdirs();
            java.io.File file4 = new java.io.File(file3, name);
            java.io.FileInputStream fileInputStream = new java.io.FileInputStream(file2);
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(file4);
            byte[] bArr = new byte[8192];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i < 0) {
                    fileInputStream.close();
                    fileOutputStream.close();
                    return file4.getAbsolutePath();
                }
                fileOutputStream.write(bArr, 0, i);
            }
        } catch (java.lang.Throwable th) {
            return "";
        }
    }

    private static void deleteRecursive(java.io.File file) {
        if (file == null || !file.exists()) {
            return;
        }
        java.io.File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (java.io.File file2 : fileArrListFiles) {
                deleteRecursive(file2);
            }
        }
        file.delete();
    }
}
