import app.pelican.check.*;
import com.google.gson.*;
import okhttp3.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Manual bounded batch only, no scheduled monitoring. Secret read from a temporary file. */
public class LiveAcceptance {
 public static void main(String[] args)throws Exception{
  Path dir=Paths.get("/workspace/Pelican/verification/v2.4");String key=new String(Files.readAllBytes(Paths.get(args[1])),"UTF-8").trim();
  OkHttpClient http=new OkHttpClient.Builder().connectTimeout(20,TimeUnit.SECONDS).readTimeout(180,TimeUnit.SECONDS).callTimeout(210,TimeUnit.SECONDS).retryOnConnectionFailure(false).build();
  LineConfig line=LineConfig.createNew();line.baseUrl="https://new.nbapi.cc/v1";line.apiKey=key;line.model="gpt-6.1-sol";line.retryCount=0;line.maxTokens=512;line.enabled=false;
  ApiClient api=new ApiClient(null,http);
  try{
   if(args[0].equals("models")){
    ApiClient.ModelResult m=api.fetchModels(line);JsonObject report=new JsonObject();report.addProperty("status",m.status);report.addProperty("ok",m.ok);report.addProperty("message",RequestInputs.safeError(m.message,key));report.add("models",Json.gson().toJsonTree(m.models));Files.write(dir.resolve("live-models.json"),Json.gson().toJson(report).getBytes("UTF-8"));System.out.println("Model query: HTTP="+m.status+" ok="+m.ok+" count="+m.models.size()+" "+RequestInputs.safeError(m.message,key));return;
   }
   line.protocol=args[2];line.effort=args[3];int batch=Integer.parseInt(args[4]);if(batch<1||batch>10)throw new IllegalArgumentException("Explicit batch must be 1–10");
   String prompt="Return exactly this SVG, without explanation or Markdown: <svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><title>鹈鹕</title><circle cx=\"50\" cy=\"50\" r=\"30\" fill=\"green\"/></svg>";
   for(int i=1;i<=batch;i++){
    RunData.SampleData sample=api.generateSample(line,prompt,i);RunData run=new RunData();run.samples.add(sample);run.model=line.model;TokenGuard.assess(run,line);InputAudit.Evidence e=sample.inputAudit;
    JsonObject report=Json.gson().toJsonTree(sample).getAsJsonObject();report.addProperty("generatedText",sample.rawTextForSave);report.addProperty("protocol",line.protocol);report.addProperty("effort",line.effort);report.addProperty("inputNote",run.inputTokenNote);report.addProperty("modelRequested",line.model);
    String text=Json.gson().toJson(report);if(text.contains(key))throw new IllegalStateException("Secret in output");String name="live-"+line.protocol+"-"+line.effort+"-"+i+".json";Files.write(dir.resolve(name),text.getBytes("UTF-8"));
    System.out.println("batch="+i+"/"+batch+" protocol="+line.protocol+" effort="+line.effort+" ok="+sample.requestOk+" attempts="+sample.attempts.size()+" input="+sample.inTokens+" estimated="+(e==null?0:e.estimatedInputTokens)+" decision="+sample.inputDecision+" wireVerified="+(e!=null&&e.wireBytesVerified));
    if(!sample.error.isEmpty()&&!sample.requestOk)System.out.println("error="+RequestInputs.safeError(sample.error,key));
   }
  }finally{http.connectionPool().evictAll();http.dispatcher().executorService().shutdown();}
 }
}
