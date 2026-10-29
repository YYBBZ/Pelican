package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class CrashLog {
    private CrashLog() {
    }

    public static java.lang.String text(java.lang.Throwable th) {
        java.io.StringWriter stringWriter = new java.io.StringWriter();
        java.io.PrintWriter printWriter = new java.io.PrintWriter(stringWriter);
        printWriter.println("设备：" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + " / Android " + android.os.Build.VERSION.SDK_INT);
        printWriter.println("应用：Pelican 2.4");
        printWriter.println();
        if (th != null) {
            th.printStackTrace(printWriter);
        }
        printWriter.flush();
        return stringWriter.toString();
    }

    public static java.lang.String save(android.content.Context context, java.lang.Throwable th) {
        java.lang.String strText = text(th);
        try {
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(new java.io.File(context.getFilesDir(), "crash.log"));
            fileOutputStream.write(strText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (java.lang.Throwable th2) {
        }
        try {
            app.pelican.check.FileUtil.saveDownload(context, "pelican-crash-" + java.lang.System.currentTimeMillis() + ".txt", strText, "text/plain");
        } catch (java.lang.Throwable th3) {
        }
        return strText;
    }
}
