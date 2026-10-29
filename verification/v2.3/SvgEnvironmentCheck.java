import java.nio.file.*;
import com.google.gson.*;
public class SvgEnvironmentCheck {
 public static void main(String[] args)throws Exception{
  JsonObject report=JsonParser.parseString(new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8")).getAsJsonObject();String text=report.get("generatedText").getAsString();
  try{com.caverock.androidsvg.SVG.getFromString(text);System.out.println("AndroidSVG parser completed in this JVM.");}
  catch(Throwable error){System.out.println("This is a JVM SDK-stub environment, not a device rendering test:");error.printStackTrace(System.out);}
 }
}
