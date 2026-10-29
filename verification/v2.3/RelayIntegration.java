import app.pelican.check.*;
import com.google.gson.*;
import okhttp3.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Explicitly invoked test utility. Never packaged in the app. Persistent budget counts requests, not successes. */
public class RelayIntegration {
 static Path DIR;
 static Path LEDGER;
 static int used;
 public static void main(String[] args)throws Exception{
  if(args.length<2||(!"models".equals(args[0])&&!"generate".equals(args[0])))throw new IllegalArgumentException("Usage: models KEY_FILE or generate KEY_FILE MODEL PROTOCOL PROMPT_FILE");
  if("generate".equals(args[0])&&args.length<5)throw new IllegalArgumentException("Usage: generate KEY_FILE MODEL PROTOCOL PROMPT_FILE");
  final ManualTestConfig test=ManualTestConfig.load("v2.3",!"models".equals(args[0]));
  if("generate".equals(args[0])&&!test.model.equals(args[2]))throw new IllegalArgumentException("MODEL must match the explicitly configured PELICAN_TEST_MODEL");
  DIR=test.outputDir;LEDGER=DIR.resolve("relay-c-test-budget.json");
  if(Files.exists(LEDGER))used=JsonParser.parseString(new String(Files.readAllBytes(LEDGER),"UTF-8")).getAsJsonObject().get("generationRequests").getAsInt();
  String secret=ManualTestConfig.readKey(args[1]);
  OkHttpClient http=new OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS).writeTimeout(30,TimeUnit.SECONDS).callTimeout(150,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).addInterceptor(chain->{
   Request r=chain.request();String path=r.url().encodedPath();
   if(!test.host.equalsIgnoreCase(r.url().host()))throw new java.io.IOException("Test blocked: unexpected host");
   boolean generation="POST".equals(r.method())&&(path.endsWith("/chat/completions")||path.endsWith("/responses")||path.endsWith("/messages"));
   if(generation){
    okio.Buffer buf=new okio.Buffer();r.body().writeTo(buf);JsonObject body=JsonParser.parseString(buf.readUtf8()).getAsJsonObject();
    int cap=body.has("max_output_tokens")?body.get("max_output_tokens").getAsInt():body.get("max_tokens").getAsInt();
    if(cap>512||cap<1)throw new java.io.IOException("Test blocked: invalid output cap");
    if(used>=3)throw new java.io.IOException("Test blocked: total request budget exhausted");
    used++;JsonObject budget=new JsonObject();budget.addProperty("provider",test.host);budget.addProperty("generationRequests",used);budget.addProperty("maximum",3);budget.addProperty("outputLimitPerRequest",512);Files.write(LEDGER,Json.gson().toJson(budget).getBytes("UTF-8"));
    System.out.println("GENERATION "+used+"/3 route="+path+" output-cap="+cap);
   }
   return chain.proceed(r);
  }).build();
  LineConfig c=new LineConfig();c.baseUrl=test.baseUrl;c.apiKey=secret;c.retryCount=0;c.retryPartial=false;c.maxTokens=512;c.effort="auto";
  ApiClient client=new ApiClient(null,http);
  if("models".equals(args[0])){
   ApiClient.ModelResult r=client.fetchModels(c);
   JsonObject report=new JsonObject();report.addProperty("endpoint",r.endpoint);report.addProperty("apiBase",r.apiBase);report.addProperty("status",r.status);report.addProperty("ok",r.ok);report.addProperty("message",RequestInputs.safeError(r.message,secret));report.add("models",Json.gson().toJsonTree(r.models));
   Files.write(DIR.resolve("relay-c-models.json"),Json.gson().toJson(report).getBytes("UTF-8"));
   System.out.println("HTTP="+r.status+" ok="+r.ok+" count="+r.models.size()+" route="+r.endpoint);System.out.println(RequestInputs.safeError(r.message,secret));
   System.out.println("examples="+r.models.subList(0,Math.min(25,r.models.size())));
  }else{
   if(args.length<5)throw new IllegalArgumentException("generate KEY MODEL PROTOCOL PROMPT_FILE");
   c.model=args[2];c.protocol=args[3];String prompt=new String(Files.readAllBytes(Paths.get(args[4])),"UTF-8");
   final List<String> deltas=new ArrayList<>();RunData.SampleData sample=client.generateSample(c,prompt,1,p->{if(!p.delta.isEmpty())deltas.add(p.delta);});
   RunData run=new RunData();run.lineId="live-integration";run.model=c.model;run.promptKey="test";run.samples.add(sample);TokenGuard.assess(run,c);
   JsonObject report=Json.gson().toJsonTree(sample).getAsJsonObject();report.addProperty("modelRequested",c.model);report.addProperty("protocol",c.protocol);report.addProperty("receivedDeltaCount",deltas.size());report.addProperty("generatedText",sample.rawTextForSave);report.addProperty("strictInputNote",run.inputTokenNote);report.addProperty("generationRequestsUsed",used);
   String text=Json.gson().toJson(report);if(text.contains(secret))throw new IllegalStateException("Secret found in report");
   Files.write(DIR.resolve("relay-c-generation-"+used+".json"),text.getBytes("UTF-8"));
   InputAudit.Evidence e=sample.inputAudit;
   System.out.println("requestOk="+sample.requestOk+" svgOk="+sample.svgOk+" attempts="+sample.attempts.size()+" deltas="+deltas.size()+" input="+sample.inTokens+" output="+sample.outTokens+" reason="+sample.reasonTokens);
   System.out.println("error="+RequestInputs.safeError(sample.error,secret));
   if(e!=null)System.out.println("bodyBytes="+e.bodyBytes+" contentVerified="+e.contentVerified+" wireBytesVerified="+e.wireBytesVerified+" localEstimate="+e.estimatedInputTokens+" countReliable="+e.countReliable+" expected="+e.expectedInputTokens+" note="+e.countNote);
   System.out.println(run.inputTokenNote);
  }
  System.out.println("TOTAL GENERATION REQUESTS="+used+"/3. No scheduler started.");
  http.connectionPool().evictAll();http.dispatcher().executorService().shutdown();
 }
}
