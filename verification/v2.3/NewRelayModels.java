import app.pelican.check.*;
import java.nio.file.*;
import com.google.gson.*;
public class NewRelayModels {
 public static void main(String[] args)throws Exception{
  if(args.length!=1)throw new IllegalArgumentException("Usage: NewRelayModels KEY_FILE; configure PELICAN_TEST_BASE_URL first");
  ManualTestConfig test=ManualTestConfig.load("v2.3",false);
  LineConfig c=new LineConfig();c.baseUrl=test.baseUrl;c.apiKey=ManualTestConfig.readKey(args[0]);
  ApiClient.ModelResult r=new ApiClient(null).fetchModels(c);
  JsonObject report=new JsonObject();report.addProperty("endpoint",r.endpoint);report.addProperty("status",r.status);report.addProperty("ok",r.ok);report.addProperty("message",RequestInputs.safeError(r.message,c.apiKey));report.add("models",Json.gson().toJsonTree(r.models));
  Files.write(test.outputDir.resolve("relay-b-models.json"),Json.gson().toJson(report).getBytes("UTF-8"));
  System.out.println("endpoint="+r.endpoint+" HTTP="+r.status+" models="+r.models.size()+" ok="+r.ok);System.out.println(RequestInputs.safeError(r.message,c.apiKey));
  if(!test.model.isEmpty())System.out.println("Requested model available="+r.models.contains(test.model));System.out.println("examples="+r.models.subList(0,Math.min(20,r.models.size())));
 }
}
