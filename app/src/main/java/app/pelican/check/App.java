package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class App extends android.app.Application {
    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        final java.lang.Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = java.lang.Thread.getDefaultUncaughtExceptionHandler();
        java.lang.Thread.setDefaultUncaughtExceptionHandler(new java.lang.Thread.UncaughtExceptionHandler() { // from class: app.pelican.check.App.1
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public void uncaughtException(java.lang.Thread thread, java.lang.Throwable th) {
                try {
                    app.pelican.check.CrashLog.save(app.pelican.check.App.this, th);
                } catch (java.lang.Throwable th2) {
                }
                if (defaultUncaughtExceptionHandler == null) {
                    android.os.Process.killProcess(android.os.Process.myPid());
                } else {
                    defaultUncaughtExceptionHandler.uncaughtException(thread, th);
                }
            }
        });
        try {
            app.pelican.check.Store.get(this).recoverInterruptedRuns();
            if(!Prefs.get(this).getBoolean("migration24Done",false))Store.get(this).migrate24Async();
        } catch (java.lang.Throwable th) {
            try {
                app.pelican.check.CrashLog.save(this, th);
            } catch (java.lang.Throwable th2) {
            }
        }
    }
}
