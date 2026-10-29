package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class SvgImages {
    private static long lastTrim;
    private static final java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    private static final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final android.util.LruCache<java.lang.String, android.graphics.Bitmap> memory = new android.util.LruCache<java.lang.String, android.graphics.Bitmap>(12582912) { // from class: app.pelican.check.SvgImages.1
        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.util.LruCache
        public int sizeOf(java.lang.String str, android.graphics.Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };

    private SvgImages() {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static synchronized void trimCache(java.io.File file) {
        long jCurrentTimeMillis = java.lang.System.currentTimeMillis();
        if (jCurrentTimeMillis - lastTrim < 60000) {
            return;
        }
        lastTrim = jCurrentTimeMillis;
        java.io.File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        long length = 0;
        for (java.io.File file2 : fileArrListFiles) {
            length += file2.length();
        }
        if (length <= 67108864) {
            return;
        }
        java.util.Arrays.sort(fileArrListFiles, new java.util.Comparator<java.io.File>() { // from class: app.pelican.check.SvgImages.2
            @Override // java.util.Comparator
            public int compare(java.io.File file3, java.io.File file4) {
                return java.lang.Long.compare(file3.lastModified(), file4.lastModified());
            }
        });
        for (java.io.File file3 : fileArrListFiles) {
            if (length <= 50331648) {
                break;
            }
            long length2 = file3.length();
            if (file3.delete()) {
                length -= length2;
            }
        }
    }

    public static void load(android.widget.ImageView imageView, java.lang.String str, java.lang.String str2, int i, int i2) {
        imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        android.content.Context applicationContext = imageView.getContext().getApplicationContext();
        if (str == null) {
            str = "";
        }
        java.io.File file = new java.io.File(str);
        java.lang.String str3 = "fit3-" + java.lang.Integer.toHexString(file.getAbsolutePath().hashCode()) + "-" + file.lastModified() + "-" + file.length() + "-" + i + "x" + i2;
        imageView.setTag(str3);
        android.graphics.Bitmap bitmap = memory.get(str3);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }
        app.pelican.check.SvgImages.C1VisibleLoader c1VisibleLoader = new app.pelican.check.SvgImages.C1VisibleLoader(imageView, str3, applicationContext, file, i, i2, str2);
        imageView.addOnAttachStateChangeListener(c1VisibleLoader);
        imageView.getViewTreeObserver().addOnPreDrawListener(c1VisibleLoader);
    }

    /* JADX INFO: renamed from: app.pelican.check.SvgImages$1VisibleLoader, reason: invalid class name */
    static class C1VisibleLoader implements android.view.ViewTreeObserver.OnPreDrawListener, android.view.View.OnAttachStateChangeListener {
        boolean requested;
        final /* synthetic */ android.content.Context val$app;
        final /* synthetic */ int val$height;
        final /* synthetic */ android.widget.ImageView val$image;
        final /* synthetic */ java.lang.String val$key;
        final /* synthetic */ java.lang.String val$oldThumb;
        final /* synthetic */ java.io.File val$source;
        final /* synthetic */ int val$width;

        C1VisibleLoader(android.widget.ImageView imageView, java.lang.String str, android.content.Context context, java.io.File file, int i, int i2, java.lang.String str2) {
            this.val$image = imageView;
            this.val$key = str;
            this.val$app = context;
            this.val$source = file;
            this.val$width = i;
            this.val$height = i2;
            this.val$oldThumb = str2;
        }

        void detach() {
            if (this.val$image.getViewTreeObserver().isAlive()) {
                this.val$image.getViewTreeObserver().removeOnPreDrawListener(this);
            }
            this.val$image.removeOnAttachStateChangeListener(this);
        }

        @Override // android.view.ViewTreeObserver.OnPreDrawListener
        public boolean onPreDraw() {
            if (!this.val$key.equals(this.val$image.getTag())) {
                detach();
                return true;
            }
            if (!this.requested && this.val$image.getGlobalVisibleRect(new android.graphics.Rect())) {
                this.requested = true;
                detach();
                final java.lang.ref.WeakReference weakReference = new java.lang.ref.WeakReference(this.val$image);
                app.pelican.check.SvgImages.pool.execute(new java.lang.Runnable() { // from class: app.pelican.check.SvgImages.1VisibleLoader.1
                    @Override // java.lang.Runnable
                    public void run() {
                        android.graphics.Bitmap bitmap = (android.graphics.Bitmap) app.pelican.check.SvgImages.memory.get(app.pelican.check.SvgImages.C1VisibleLoader.this.val$key);
                        if (bitmap == null) {
                            java.io.File file = new java.io.File(app.pelican.check.SvgImages.C1VisibleLoader.this.val$app.getCacheDir(), "svg-fit3");
                            file.mkdirs();
                            java.io.File file2 = new java.io.File(file, app.pelican.check.SvgImages.C1VisibleLoader.this.val$key + ".png");
                            android.graphics.Bitmap bitmapDecodeFile = android.graphics.BitmapFactory.decodeFile(file2.getPath());
                            if (bitmapDecodeFile == null && app.pelican.check.SvgImages.C1VisibleLoader.this.val$source.isFile()) {
                                bitmapDecodeFile = app.pelican.check.SvgAnalyzer.renderBitmap(app.pelican.check.FileUtil.readText(app.pelican.check.SvgImages.C1VisibleLoader.this.val$source.getPath()), app.pelican.check.SvgImages.C1VisibleLoader.this.val$width, app.pelican.check.SvgImages.C1VisibleLoader.this.val$height);
                            }
                            if (bitmapDecodeFile == null && app.pelican.check.SvgImages.C1VisibleLoader.this.val$oldThumb != null && !app.pelican.check.SvgImages.C1VisibleLoader.this.val$oldThumb.isEmpty()) {
                                bitmapDecodeFile = android.graphics.BitmapFactory.decodeFile(app.pelican.check.SvgImages.C1VisibleLoader.this.val$oldThumb);
                            }
                            if (bitmapDecodeFile != null) {
                                app.pelican.check.SvgImages.memory.put(app.pelican.check.SvgImages.C1VisibleLoader.this.val$key, bitmapDecodeFile);
                                if (!file2.isFile()) {
                                    try {
                                        java.io.FileOutputStream fileOutputStream = new java.io.FileOutputStream(file2);
                                        try {
                                            bitmapDecodeFile.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, fileOutputStream);
                                            fileOutputStream.close();
                                        } catch (java.lang.Throwable th) {
                                            fileOutputStream.close();
                                            throw th;
                                        }
                                    } catch (java.lang.Throwable th2) {
                                    }
                                }
                                app.pelican.check.SvgImages.trimCache(file);
                            }
                            bitmap = bitmapDecodeFile;
                        }
                        final android.graphics.Bitmap deliveredBitmap = bitmap;
                        app.pelican.check.SvgImages.main.post(new java.lang.Runnable() { // from class: app.pelican.check.SvgImages.1VisibleLoader.1.1
                            @Override // java.lang.Runnable
                            public void run() {
                                android.widget.ImageView imageView = (android.widget.ImageView) weakReference.get();
                                if (imageView != null && app.pelican.check.SvgImages.C1VisibleLoader.this.val$key.equals(imageView.getTag())) {
                                    if (deliveredBitmap == null) {
                                        imageView.setContentDescription("SVG 预览失败，可查看源码");
                                    } else {
                                        imageView.setImageBitmap(deliveredBitmap);
                                    }
                                }
                            }
                        });
                    }
                });
            }
            return true;
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewAttachedToWindow(android.view.View view) {
            if (!this.requested && this.val$key.equals(this.val$image.getTag())) {
                this.val$image.getViewTreeObserver().removeOnPreDrawListener(this);
                this.val$image.getViewTreeObserver().addOnPreDrawListener(this);
            }
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewDetachedFromWindow(android.view.View view) {
            if (this.val$image.getViewTreeObserver().isAlive()) {
                this.val$image.getViewTreeObserver().removeOnPreDrawListener(this);
            }
        }
    }

    public static void open(android.app.Activity activity, app.pelican.check.RunData.SampleData sampleData) {
        final android.app.Dialog dialog = new android.app.Dialog(activity, android.R.style.Theme_Material_Light_NoActionBar);
        dialog.requestWindowFeature(1);
        android.widget.LinearLayout linearLayoutColumn = app.pelican.check.Ui.column(activity);
        linearLayoutColumn.setBackgroundColor(app.pelican.check.Ui.BG);
        android.widget.Button button = app.pelican.check.Ui.button(activity, "‹ 返回检测详情", false);
        linearLayoutColumn.addView(button);
        android.widget.ImageView imageView = new android.widget.ImageView(activity);
        imageView.setBackgroundColor(-1);
        linearLayoutColumn.addView(imageView, new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f));
        dialog.setContentView(linearLayoutColumn);
        dialog.show();
        dialog.getWindow().setLayout(-1, -1);
        load(imageView, sampleData.svgPath, sampleData.thumbPath, 1600, 1600);
        button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.SvgImages.3
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                dialog.dismiss();
            }
        });
    }
}
