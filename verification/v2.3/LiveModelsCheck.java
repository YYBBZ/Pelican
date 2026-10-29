import app.pelican.check.*;
import java.nio.file.*;
public class LiveModelsCheck {
 public static void main(String[] args)throws Exception{
  LineConfig c=new LineConfig();c.baseUrl="https://api.fluxapi.vip";c.apiKey=new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8").trim();
  ApiClient.ModelResult r=new ApiClient(null).fetchModels(c);
  System.out.println("endpoint="+r.endpoint);System.out.println("HTTP="+r.status+" models="+r.models.size()+" ok="+r.ok);System.out.println(r.message);
  System.out.println("Generation requests used: 0 / 3. Authentication did not succeed; no generation requests sent.");
 }
}
