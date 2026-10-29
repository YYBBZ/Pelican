import app.pelican.check.*;
import java.nio.file.*;
import com.google.gson.*;
public class NewRelayModels {
 public static void main(String[] args)throws Exception{
  LineConfig c=new LineConfig();c.baseUrl="https://new.nbapi.cc/v1";c.apiKey=new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8").trim();
  ApiClient.ModelResult r=new ApiClient(null).fetchModels(c);
  JsonObject report=new JsonObject();report.addProperty("endpoint",r.endpoint);report.addProperty("status",r.status);report.addProperty("ok",r.ok);report.addProperty("message",RequestInputs.safeError(r.message,c.apiKey));report.add("models",Json.gson().toJsonTree(r.models));
  Files.write(Paths.get("/workspace/Pelican/verification/v2.3/nbapi-models.json"),Json.gson().toJson(report).getBytes("UTF-8"));
  System.out.println("endpoint="+r.endpoint+" HTTP="+r.status+" models="+r.models.size()+" ok="+r.ok);System.out.println(RequestInputs.safeError(r.message,c.apiKey));
  System.out.println("gpt-6.1-sol available="+r.models.contains("gpt-6.1-sol"));System.out.println("examples="+r.models.subList(0,Math.min(20,r.models.size())));
 }
}
