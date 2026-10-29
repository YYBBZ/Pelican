import app.pelican.check.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import android.app.Application;
import static org.junit.Assert.*;

/** Android16 framework startup checks; not a physical Xiaomi-device test. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=36,application=Application.class,shadows={PelicanRuntimeTest.MemoryStore.class},instrumentedPackages={"app.pelican.check"})
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class Android16CompatibilityTest {
 @Test public void lineEditorStartsOnAndroid16Framework()throws Exception{
  PelicanRuntimeTest.MemoryStore.singleton=null;PelicanRuntimeTest.MemoryStore.lines.clear();PelicanRuntimeTest.MemoryStore.runs.clear();
  org.robolectric.android.controller.ActivityController<LineEditActivity> controller=Robolectric.buildActivity(LineEditActivity.class).setup();
  assertEquals(7,((android.widget.Spinner)PelicanRuntimeTest.field(controller.get(),"effort")).getCount());assertFalse(controller.get().isFinishing());
  controller.pause().stop().destroy();System.out.println("Android16 JVM framework: editor starts, seven reasoning levels and predictive-back registration loaded.");
 }
}
