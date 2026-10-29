import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.app.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28,application=Application.class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class RuntimeSmokeTest {
 @Test public void androidViewAndSvgParser()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();
  assertEquals("test button",app.pelican.check.Ui.button(a,"test button",true).getText().toString());
  com.caverock.androidsvg.SVG svg=com.caverock.androidsvg.SVG.getFromString("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"30\"/></svg>");
  assertNotNull(svg);System.out.println("Android framework JVM View construction and real AndroidSVG parsing passed.");
 }
}
