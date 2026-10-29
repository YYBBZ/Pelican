package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class SvgAnalyzer {

    public static class Result {
        public int colors;
        public int nodes;
        public boolean ok;
        public int pathLength;
        public int score;
        public int size;
        public java.lang.String error = "";
        public java.lang.String svg = "";
        public java.util.List<java.lang.String> cheats = new java.util.ArrayList();
    }

    private SvgAnalyzer() {
    }

    public static java.lang.String extractSvg(java.lang.String str) {
        if (str == null) {
            return null;
        }
        java.lang.String strReplace = str.replace("\r", "");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("```(?:svg|xml)?\\s*([\\s\\S]*?)```", 2).matcher(strReplace);
        if (matcher.find() && java.util.regex.Pattern.compile("<svg", 2).matcher(matcher.group(1)).find()) {
            strReplace = matcher.group(1);
        }
        java.util.regex.Matcher matcher2 = java.util.regex.Pattern.compile("<svg[\\s\\S]*?</svg>", 2).matcher(strReplace);
        if (matcher2.find()) {
            return matcher2.group().trim();
        }
        int iIndexOfIgnoreCase = indexOfIgnoreCase(strReplace, "<svg");
        if (iIndexOfIgnoreCase >= 0) {
            return strReplace.substring(iIndexOfIgnoreCase).trim();
        }
        return null;
    }

    private static int indexOfIgnoreCase(java.lang.String str, java.lang.String str2) {
        return str.toLowerCase().indexOf(str2.toLowerCase());
    }

    public static app.pelican.check.SvgAnalyzer.Result analyze(java.lang.String str) {
        app.pelican.check.SvgAnalyzer.Result result = new app.pelican.check.SvgAnalyzer.Result();
        java.lang.String strExtractSvg = extractSvg(str);
        if (strExtractSvg == null) {
            result.error = "没有返回完整 SVG";
            return result;
        }
        if (!java.util.regex.Pattern.compile("</svg>\\s*$", 2).matcher(strExtractSvg).find()) {
            result.error = "SVG 被截断或缺少结束标签";
            return result;
        }
        detectCheats(strExtractSvg, result.cheats);
        java.lang.String strReplaceAll = escapeBareAmpersands(strExtractSvg.replaceAll("(?i)<\\?xml[^>]*\\?>", "").trim()).replaceAll("(?is)<script[\\s\\S]*?</script>", "").replaceAll("(?is)<foreignObject[\\s\\S]*?</foreignObject>", "");
        try {
            com.caverock.androidsvg.SVG.getFromString(strReplaceAll);
            result.ok = true;
            result.svg = strReplaceAll;
            result.size = strReplaceAll.getBytes().length;
            result.nodes = count(strReplaceAll, "<[A-Za-z][A-Za-z0-9:_-]*(?=[\\s/>])");
            result.pathLength = pathLength(strReplaceAll);
            result.colors = colorCount(strReplaceAll);
            result.score = (int) java.lang.Math.max(0L, java.lang.Math.min(100L, java.lang.Math.round((java.lang.Math.log10(result.nodes + 1) * 25.0d) + (java.lang.Math.log10((((double) result.pathLength) / 10.0d) + 1.0d) * 12.0d) + ((double) (result.colors > 3 ? 8 : 0)) + ((double) (count(strReplaceAll, "(?i)<text\\b") > 0 ? 3 : 0)))));
            return result;
        } catch (java.lang.Throwable th) {
            result.error = "SVG 结构无法解析";
            return result;
        }
    }

    private static java.lang.String escapeBareAmpersands(java.lang.String str) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("&(?!(amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)").matcher(str);
        java.lang.StringBuffer stringBuffer = new java.lang.StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuffer, java.util.regex.Matcher.quoteReplacement("&amp;"));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    private static void detectCheats(java.lang.String str, java.util.List<java.lang.String> list) {
        if (find(str, "(?i)<image\\b")) {
            list.add("贴了位图");
        }
        if (find(str, "(?i)<(?:text|tspan)\\b")) {
            list.add("用文字代替");
        }
        if (find(str, "(?i)(?:xlink:)?href\\s*=\\s*[\"']\\s*(?:https?:)?//")) {
            list.add("引用外部素材");
        }
        if (find(str, "(?i)<script\\b|<foreignObject\\b")) {
            list.add("塞了脚本");
        }
    }

    private static boolean find(java.lang.String str, java.lang.String str2) {
        return java.util.regex.Pattern.compile(str2).matcher(str).find();
    }

    private static int count(java.lang.String str, java.lang.String str2) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(str2).matcher(str);
        int i = 0;
        while (matcher.find()) {
            i++;
        }
        return i;
    }

    private static int pathLength(java.lang.String str) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?is)<path\\b[^>]*?\\bd\\s*=\\s*([\"'])(.*?)\\1").matcher(str);
        int length = 0;
        while (matcher.find()) {
            length += matcher.group(2).length();
        }
        return length;
    }

    private static int colorCount(java.lang.String str) {
        java.util.HashSet hashSet = new java.util.HashSet();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)(?:fill|stroke)\\s*=\\s*([\"'])(.*?)\\1").matcher(str);
        while (matcher.find()) {
            java.lang.String lowerCase = matcher.group(2).trim().toLowerCase();
            if (lowerCase.length() > 0 && !lowerCase.equals("none") && !lowerCase.equals("transparent") && !lowerCase.equals("currentcolor")) {
                hashSet.add(lowerCase);
            }
        }
        return hashSet.size();
    }

    public static android.graphics.Bitmap renderBitmap(java.lang.String str, int i, int i2) {
        try {
            com.caverock.androidsvg.SVG fromString = com.caverock.androidsvg.SVG.getFromString(str);
            if (fromString.getDocumentViewBox() == null) {
                float documentWidth = fromString.getDocumentWidth();
                float documentHeight = fromString.getDocumentHeight();
                float documentAspectRatio = fromString.getDocumentAspectRatio();
                if (documentWidth <= 0.0f) {
                    documentWidth = (documentAspectRatio <= 0.0f || documentHeight <= 0.0f) ? i : documentHeight * documentAspectRatio;
                }
                if (documentHeight <= 0.0f) {
                    documentHeight = documentAspectRatio > 0.0f ? documentWidth / documentAspectRatio : i2;
                }
                fromString.setDocumentViewBox(0.0f, 0.0f, documentWidth, documentHeight);
            }
            fromString.setDocumentWidth(i);
            fromString.setDocumentHeight(i2);
            fromString.setDocumentPreserveAspectRatio(com.caverock.androidsvg.PreserveAspectRatio.LETTERBOX);
            android.graphics.Picture pictureRenderToPicture = fromString.renderToPicture(i, i2);
            android.graphics.Bitmap bitmapCreateBitmap = android.graphics.Bitmap.createBitmap(i, i2, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmapCreateBitmap);
            canvas.drawColor(-1);
            canvas.drawPicture(pictureRenderToPicture);
            return bitmapCreateBitmap;
        } catch (java.lang.Throwable th) {
            return null;
        }
    }

    public static boolean saveThumb(java.lang.String str, java.io.File file, int i, int i2) {
        android.graphics.Bitmap bitmapRenderBitmap = renderBitmap(str, i, i2);
        if (bitmapRenderBitmap == null) {
            return false;
        }
        try {
            java.io.File parentFile = file.getParentFile();
            if (parentFile != null) {
                parentFile.mkdirs();
            }
            java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(file);
            bitmapRenderBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (java.lang.Throwable th) {
            return false;
        }
    }
}
