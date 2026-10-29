import app.pelican.check.*;
import java.nio.file.*;
public class LiveModelsCheck {
 public static void main(String[] args)throws Exception{
  if(args.length!=1)throw new IllegalArgumentException("Usage: LiveModelsCheck KEY_FILE; configure PELICAN_TEST_BASE_URL first");
  ManualTestConfig test=ManualTestConfig.load("v2.3",false);
  LineConfig c=new LineConfig();c.baseUrl=test.baseUrl;c.apiKey=ManualTestConfig.readKey(args[0]);
  ApiClient.ModelResult r=new ApiClient(null).fetchModels(c);
  System.out.println("endpoint="+r.endpoint);System.out.println("HTTP="+r.status+" models="+r.models.size()+" ok="+r.ok);System.out.println(r.message);
  System.out.println("Generation requests: 0. Only the model-list endpoint was queried.");
 }
}
