package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class FileUtil {
    private FileUtil() {
    }

    public static java.lang.String readText(java.lang.String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        try {
            java.io.FileInputStream fileInputStream = new java.io.FileInputStream(str);
            try {
                java.lang.String text = readText(fileInputStream);
                fileInputStream.close();
                return text;
            } catch (java.lang.Throwable th) {
                try {
                    fileInputStream.close();
                } catch (java.lang.Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (java.lang.Throwable th3) {
            return "";
        }
    }

    public static java.lang.String readText(java.io.InputStream inputStream) throws java.lang.Exception {
        java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
        byte[] bArr = new byte[8192];
        while (true) {
            int i = inputStream.read(bArr);
            if (i < 0) {
                return new java.lang.String(byteArrayOutputStream.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
            }
            byteArrayOutputStream.write(bArr, 0, i);
        }
    }

    public static java.lang.String saveDownload(android.content.Context context, java.lang.String name, java.lang.String content, java.lang.String mime) {
        try {
            String safe = name == null ? "pelican.svg" : name.replaceAll("[^A-Za-z0-9._-]", "_");
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, safe);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime);
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS + "/Pelican");
                android.net.Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return "无法创建下载文件";
                try (java.io.OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    if (out == null) return "无法打开下载文件";
                    out.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)); out.flush();
                }
                return "已存到 下载/Pelican/" + safe;
            }
            java.io.File dir = new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS), "Pelican");
            dir.mkdirs();
            try (java.io.OutputStream out = new java.io.FileOutputStream(new java.io.File(dir, safe))) {
                out.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)); out.flush();
            }
            return "已存到 " + new java.io.File(dir, safe).getAbsolutePath();
        } catch (Throwable error) { return "保存失败：" + error.getMessage(); }
    }
}
