import app.pelican.check.*;
import okhttp3.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

/** 恢复工程的本地合成冒烟检查；不是原 82 项回归测试，也不是 Android 真机测试。 */
public class RecoverySmokeTest {
    static int passed;
    static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        passed++; System.out.println("PASS " + label);
    }
    static LineConfig config() {
        LineConfig c = new LineConfig(); c.baseUrl = "https://test.invalid/v1";
        c.apiKey = "synthetic-test-key"; c.model = "synthetic-model"; c.retryCount = 2; return c;
    }
    static Response response(Interceptor.Chain chain, int status, String text) {
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(status).message("synthetic fixture").header("Content-Type", "text/event-stream")
                .body(ResponseBody.create(text, MediaType.get("text/event-stream"))).build();
    }
    public static void main(String[] args) throws Exception {
        check(ApiClient.parseModelIds("{\"data\":[{\"id\":\"one\"},{\"id\":\"two\"},{\"id\":\"one\"}]}")
                .equals(Arrays.asList("one", "two")), "模型解析与去重");
        check(ApiClient.endpoints("https://test.invalid/v1/chat/completions", "openai", "/models").get(0)
                .equals("https://test.invalid/v1/models"), "地址补全");
        check(RetryPolicy.maxAttempts(0) == 1 && RetryPolicy.maxAttempts(2) == 3, "重试次数边界");
        check(RetryPolicy.retryException(new SSLHandshakeException("connection closed")), "意外握手中断可补试");
        check(!RetryPolicy.retryException(new SSLPeerUnverifiedException("certificate hostname mismatch")), "证书错误不补试");
        check(!RetryPolicy.retryStatus(429, "insufficient_quota"), "额度错误不是临时限流");
        check(RetryPolicy.parseRetryAfter("2", 0) == 2000, "Retry-After 秒数");
        Baseline baseline = new Baseline();
        for (double score : new double[]{70, 75, 72, 78}) baseline.addSuccessfulScore(score);
        check(baseline.lower == null, "不足五次不建立基线");
        baseline.addSuccessfulScore(74); double lower = baseline.lower;
        check(Math.abs(lower - 59.2) < 0.001, "五次中位数乘八成");
        baseline.addSuccessfulScore(5); check(baseline.lower == lower, "固定基线不漂移");
        RunData live = new RunData(); live.lineId = "synthetic-line"; live.status = "RUNNING"; live.stage = "接收内容";
        ApiClient.Progress progress = new ApiClient.Progress(); progress.delta = "hello";
        LiveRuns.publish(live, progress);
        check("hello".equals(LiveRuns.read(live.id).output), "实时正文快照");
        LiveRuns.read(live.id).run.status = "FAIL";
        check(LiveRuns.read(live.id).run.isActive(), "快照不改后台状态");
        LiveRuns.remove(live.id); check(LiveRuns.read(live.id) == null, "结束后清理快照");
        RunData.SampleData temp = new RunData.SampleData(); temp.rawTextForSave = "transient-marker";
        check(!Json.gson().toJson(temp).contains("transient-marker"), "临时正文不写入 JSON");
        AtomicInteger count = new AtomicInteger();
        ApiClient api = new ApiClient(null, new OkHttpClient.Builder().addInterceptor(chain -> {
            if (count.getAndIncrement() == 0) throw new SSLHandshakeException("connection closed");
            return response(chain, 200, "data: {\"choices\":[{\"delta\":{\"content\":\"hello\"},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n");
        }).build());
        List<String> output = new ArrayList<>();
        RunData.SampleData result = api.generateSample(config(), "synthetic prompt", 1, p -> { if (!p.delta.isEmpty()) output.add(p.delta); });
        check(result.requestOk && result.attempts.size() == 2 && count.get() == 2, "实际合成请求握手失败后成功补试");
        check(output.contains("hello"), "流式正文回调");
        System.out.println("\nPassed " + passed + " local recovery checks. No real relay was contacted.");
    }
}
