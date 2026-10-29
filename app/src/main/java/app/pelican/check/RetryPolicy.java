package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class RetryPolicy {
    private RetryPolicy() {
    }

    public static int maxAttempts(int i) {
        return java.lang.Math.max(0, java.lang.Math.min(20, i)) + 1;
    }

    public static boolean permanentMessage(java.lang.String str) {
        return (str == null ? "" : str.toLowerCase(java.util.Locale.ROOT)).matches("(?s).*(insufficient[_ ](quota|balance|credit)|quota[_ ]exceeded|billing[_ ]hard[_ ]limit|credit balance|balance.{0,15}(insufficient|not enough)|invalid[_ ]api[_ ]key|invalid[_ ]key|authentication[_ ]error|model[_ ]not[_ ]found|model.{0,40}(does not exist|not found)|余额不足|额度不足|欠费|无效.{0,6}密钥|模型不存在).*");
    }

    public static boolean certificateFailure(java.lang.Throwable th) {
        int i = 0;
        while (th != null) {
            int i2 = i + 1;
            if (i >= 12) {
                break;
            }
            if ((th instanceof java.security.cert.CertificateException) || (th instanceof javax.net.ssl.SSLPeerUnverifiedException)) {
                return true;
            }
            java.lang.String lowerCase = th.getMessage() == null ? "" : th.getMessage().toLowerCase(java.util.Locale.ROOT);
            if ((th instanceof javax.net.ssl.SSLException) && lowerCase.matches("(?s).*(certificate|certpath|trust anchor|hostname|peer not authenticated|protocol_version|no appropriate protocol|no cipher suites|certificate verify|证书).*")) {
                return true;
            }
            if (th == th.getCause()) {
                break;
            }
            th = th.getCause();
            i = i2;
        }
        return false;
    }

    public static boolean retryException(java.lang.Throwable th) {
        if (java.lang.Thread.currentThread().isInterrupted() || certificateFailure(th)) {
            return false;
        }
        if (!(th instanceof java.io.InterruptedIOException) || (th instanceof java.net.SocketTimeoutException)) {
            return th instanceof java.io.IOException;
        }
        return false;
    }

    public static boolean retryStatus(int i, java.lang.String str) {
        if (permanentMessage(str)||parameterError(str)) {
            return false;
        }
        return i == 408 || i == 409 || i == 425 || i == 429 || (i >= 500 && i <= 599);
    }

    public static long parseRetryAfter(java.lang.String str, long j) {
        if (str == null) {
            return 0L;
        }
        try {
            long j2 = java.lang.Long.parseLong(str.trim());
            if (j2 <= 0) {
                return 0L;
            }
            return j2 > 9223372036854775L ? kotlin.jvm.internal.LongCompanionObject.MAX_VALUE : 1000 * j2;
        } catch (java.lang.NumberFormatException e) {
            try {
                return java.lang.Math.max(0L, java.time.ZonedDateTime.parse(str.trim(), java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - j);
            } catch (java.lang.RuntimeException e2) {
                return 0L;
            }
        }
    }

    public static long delayMs(long j, int i) {
        if (j > 0) {
            return j;
        }
        return i == 1 ? 0L : 1000L;
    }
    public static boolean parameterError(String message){
        String text=message==null?"":message.toLowerCase(java.util.Locale.ROOT);
        return text.matches("(?s).*(unsupported|invalid|unknown|unrecognized|not supported|not allowed|不支持|无效|不识别).{0,120}(reasoning|effort|xhigh|thinking|budget|max_tokens|max_output_tokens|参数).*" )
          ||text.matches("(?s).*(reasoning|effort|xhigh|thinking|budget).{0,120}(unsupported|invalid|unknown|unrecognized|not supported|不支持|无效).*" );
    }
}
