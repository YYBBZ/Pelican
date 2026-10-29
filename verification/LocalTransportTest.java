import app.pelican.check.*;
import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;

/** Isolated loopback fixture: verifies real HTTP bytes and callbacks, never contacts user providers. */
public class LocalTransportTest {
 static int passed;
 static void check(boolean ok,String title){if(!ok)throw new AssertionError(title);passed++;System.out.println("PASS "+title);}
 static byte[] read(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];for(int n;(n=in.read(b))>=0;)out.write(b,0,n);return out.toByteArray();}
 static void reply(HttpExchange ex,String body,String type)throws IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type",type);ex.sendResponseHeaders(200,bytes.length);try(OutputStream out=ex.getResponseBody()){out.write(bytes);}}
 public static void main(String[] args)throws Exception{
  HttpServer server=HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0),0);
  final List<byte[]> sent=new ArrayList<>();final List<JsonObject> counted=new ArrayList<>();final AtomicInteger modelCalls=new AtomicInteger();
  server.createContext("/v1/models",ex->{modelCalls.incrementAndGet();reply(ex,"{\"data\":[{\"id\":\"fixture-model\"}]}","application/json");});
  server.createContext("/v1/responses/input_tokens",ex->{counted.add(JsonParser.parseString(new String(read(ex.getRequestBody()),StandardCharsets.UTF_8)).getAsJsonObject());reply(ex,"{\"object\":\"response.input_tokens\",\"input_tokens\":1000}","application/json");});
  server.createContext("/v1/responses",ex->{sent.add(read(ex.getRequestBody()));reply(ex,"data: {\"type\":\"response.output_text.delta\",\"delta\":\"fixture reply\"}\n\ndata: {\"type\":\"response.completed\",\"response\":{\"id\":\"local-r1\",\"model\":\"fixture-model\",\"usage\":{\"input_tokens\":1601,\"output_tokens\":2}}}\n\n","text/event-stream");});
  server.createContext("/v1/chat/completions",ex->{sent.add(read(ex.getRequestBody()));reply(ex,"data: {\"choices\":[{\"delta\":{\"content\":\"fixture reply\"},\"finish_reason\":\"stop\"}]}\n\ndata: {\"choices\":[],\"usage\":{\"prompt_tokens\":50000,\"completion_tokens\":2}}\n\ndata: [DONE]\n\n","text/event-stream");});
  server.start();OkHttpClient http=new OkHttpClient();
  try{
   LineConfig c=new LineConfig();c.baseUrl="http://127.0.0.1:"+server.getAddress().getPort()+"/v1";c.apiKey="isolated-synthetic-fixture";c.model="fixture-model";c.protocol="responses";c.retryCount=0;c.maxTokens=512;c.inputTokenExcess=500;
   ApiClient client=new ApiClient(null,http);ApiClient.ModelResult models=client.fetchModels(c);check(models.ok&&models.models.equals(Arrays.asList("fixture-model")),"模型列表真实HTTP往返");
   String prompt="按原样：鹈鹕 🚲\n引号 \"SVG\"\n末尾空白  \n";
   List<String> deltas=new ArrayList<>();RunData.SampleData sample=client.generateSample(c,prompt,1,p->{if(!p.delta.isEmpty())deltas.add(p.delta);});
   check(sample.requestOk&&sample.attempts.size()==1,"Responses真实HTTP流式完成");check(deltas.contains("fixture reply"),"流式增量回调保留正文");
   InputAudit.Evidence e=sample.inputAudit;byte[] actual=sent.get(0);JsonObject body=JsonParser.parseString(new String(actual,StandardCharsets.UTF_8)).getAsJsonObject();
   check(e.contentVerified&&e.wireBytesVerified,"EventListener确认发送字节数量");check(e.bodyBytes==actual.length&&e.bodySha256.equals(InputAudit.sha256(actual)),"实际到达服务端字节与冻结正文摘要一致");
   check(prompt.equals(body.getAsJsonArray("input").get(0).getAsJsonObject().get("content").getAsString()),"到达服务端的提示词中文表情引号与尾空白逐字一致");
   check(body.get("max_output_tokens").getAsInt()==512,"发送端输出上限没有被调高");check(counted.size()==0,"2.4 不多发额外计数预检请求");
   check(!e.countReliable&&e.estimatedInputTokens>0,"按本地估算执行筛查");check(sample.inTokens==1601,"流式实际输入用量解析正确");
   RunData run=new RunData();run.samples.add(sample);TokenGuard.assess(run,c);check(run.inputTokenAlert,"严格差额601大于容许500时告警");
   c.protocol="openai";sample=client.generateSample(c,prompt,1);check(sample.requestOk,"Chat真实HTTP流式完成");
   check(sent.size()==2&&counted.size()==0,"Chat不偷偷多发计数请求");
   run=new RunData();run.samples.add(sample);TokenGuard.assess(run,c);check(run.inputTokenAlert&&run.inputTokenNote.contains("不能据此确认"),"接口回报5万Token时出现疑似注入筛查提示");
   check(sample.inputAudit.wireBytesVerified&&sample.inputAudit.bodySha256.equals(InputAudit.sha256(sent.get(1))),"Chat真实发送正文与证据一致");
   check(!new String(actual,StandardCharsets.UTF_8).contains(c.apiKey),"认证密钥不进入实际JSON正文");
  }finally{server.stop(0);http.connectionPool().evictAll();http.dispatcher().executorService().shutdown();}
  System.out.println("\nPassed "+passed+" isolated loopback transport checks. No real provider contacted.");
 }
}
