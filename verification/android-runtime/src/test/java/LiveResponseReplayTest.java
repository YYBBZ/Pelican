import app.pelican.check.*;
import com.google.gson.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import android.app.Application;
import java.nio.file.*;
import static org.junit.Assert.*;

/** Replays already captured provider output through AndroidSVG; sends no new requests. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28,application=Application.class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class LiveResponseReplayTest {
 @Test public void allCapturedRealResponsesParseWithAndroidSvg()throws Exception{
  Path dir=Paths.get("/workspace/Pelican/verification/v2.4");int count=0;
  try(DirectoryStream<Path> files=Files.newDirectoryStream(dir,"live-*.json")){
   for(Path file:files){JsonElement parsed=JsonParser.parseString(new String(Files.readAllBytes(file),"UTF-8"));if(!parsed.isJsonObject())continue;JsonObject o=parsed.getAsJsonObject();if(!o.has("generatedText"))continue;
    String text=o.get("generatedText").getAsString();SvgAnalyzer.Result analyzed=SvgAnalyzer.analyze(text);assertTrue(file.getFileName()+": "+analyzed.error,analyzed.ok);assertNotNull(com.caverock.androidsvg.SVG.getFromString(analyzed.svg));count++;
   }
  }
  assertEquals(13,count);System.out.println("Replayed 13 real provider responses: AndroidSVG parsing passed in the JVM Android runtime; no physical-device rendering claim.");
 }
}
