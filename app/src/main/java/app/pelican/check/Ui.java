package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public final class Ui {
    public static final int CARD = -1;
    public static final int BG = android.graphics.Color.rgb(247, 245, 239);
    public static final int GREEN = android.graphics.Color.rgb(31, 91, 76);
    public static final int GREEN_DARK = android.graphics.Color.rgb(22, 68, 55);
    public static final int TEXT = android.graphics.Color.rgb(38, 50, 46);
    public static final int MUTED = android.graphics.Color.rgb(100, 113, 105);
    public static final int BORDER = android.graphics.Color.rgb(229, 224, 214);
    public static final int ORANGE = android.graphics.Color.rgb(173, 112, 28);
    public static final int RED = android.graphics.Color.rgb(177, 66, 62);
    public static final int BLUEGRAY = android.graphics.Color.rgb(82, 112, 122);
    public static final int PALE_GREEN = android.graphics.Color.rgb(232, 242, 236);
    public static final int PALE_ORANGE = android.graphics.Color.rgb(251, 241, 222);
    public static final int PALE_RED = android.graphics.Color.rgb(250, 232, 230);
    public static final int PALE_GRAY = android.graphics.Color.rgb(239, 240, 237);

    private Ui() {
    }

    public static int dp(android.content.Context context, float f) {
        return java.lang.Math.round(f * context.getResources().getDisplayMetrics().density);
    }

    public static android.graphics.drawable.GradientDrawable round(android.content.Context context, int i, float f) {
        android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dp(context, f));
        return gradientDrawable;
    }

    public static android.graphics.drawable.GradientDrawable bordered(android.content.Context context, int i, int i2, float f) {
        android.graphics.drawable.GradientDrawable gradientDrawableRound = round(context, i, f);
        gradientDrawableRound.setStroke(dp(context, 1.0f), i2);
        return gradientDrawableRound;
    }

    public static android.widget.LinearLayout column(android.content.Context context) {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(context);
        linearLayout.setOrientation(1);
        return linearLayout;
    }

    public static android.widget.LinearLayout row(android.content.Context context) {
        android.widget.LinearLayout linearLayout = new android.widget.LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        return linearLayout;
    }

    public static android.widget.LinearLayout card(android.content.Context context) {
        android.widget.LinearLayout linearLayoutColumn = column(context);
        linearLayoutColumn.setPadding(dp(context, 14.0f), dp(context, 14.0f), dp(context, 14.0f), dp(context, 14.0f));
        linearLayoutColumn.setBackground(bordered(context, -1, BORDER, 18.0f));
        return linearLayoutColumn;
    }

    public static android.widget.TextView text(android.content.Context context, java.lang.String str, float f, int i) {
        android.widget.TextView textView = new android.widget.TextView(context);
        if (str == null) {
            str = "";
        }
        textView.setText(str);
        textView.setTextSize(f);
        textView.setTextColor(i);
        textView.setLineSpacing(dp(context, 2.0f), 1.0f);
        return textView;
    }

    public static android.widget.TextView heading(android.content.Context context, java.lang.String str, float f) {
        android.widget.TextView textViewText = text(context, str, f, TEXT);
        textViewText.setTypeface(android.graphics.Typeface.DEFAULT, 1);
        return textViewText;
    }

    public static android.widget.Button button(android.content.Context context, java.lang.String str, boolean z) {
        android.widget.Button button = new android.widget.Button(context);
        button.setText(str);
        button.setTextSize(14.0f);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setStateListAnimator(null);
        button.setElevation(0.0f);
        button.setAllCaps(false);
        button.setMinHeight(dp(context, 48.0f));
        button.setMinimumHeight(dp(context, 48.0f));
        button.setPadding(dp(context, 9.0f), dp(context, 6.0f), dp(context, 9.0f), dp(context, 6.0f));
        button.setTextColor(z ? -1 : GREEN_DARK);
        button.setBackground(round(context, z ? GREEN : PALE_GREEN, 12.0f));
        return button;
    }

    public static android.widget.EditText input(android.content.Context context, java.lang.String str, java.lang.String str2) {
        android.widget.EditText editText = new android.widget.EditText(context);
        editText.setHint(str);
        editText.setHintTextColor(MUTED);
        if (str2 == null) {
            str2 = "";
        }
        editText.setText(str2);
        editText.setTextSize(15.0f);
        editText.setTextColor(TEXT);
        editText.setSingleLine(true);
        editText.setMinHeight(dp(context,50));
        editText.setPadding(dp(context, 12.0f), dp(context, 10.0f), dp(context, 12.0f), dp(context, 10.0f));
        editText.setBackground(bordered(context, -1, BORDER, 12.0f));
        return editText;
    }

    public static android.widget.EditText number(android.content.Context context, java.lang.String str, java.lang.String str2) {
        android.widget.EditText editTextInput = input(context, str, str2);
        editTextInput.setInputType(8194);
        return editTextInput;
    }

    public static android.widget.TextView badge(android.content.Context context, java.lang.String str, int i, int i2) {
        android.widget.TextView textViewText = text(context, str, 11.0f, i);
        textViewText.setTypeface(android.graphics.Typeface.DEFAULT, 1);
        textViewText.setGravity(17);
        textViewText.setPadding(dp(context, 8.0f), dp(context, 4.0f), dp(context, 8.0f), dp(context, 4.0f));
        textViewText.setBackground(round(context, i2, 8.0f));
        return textViewText;
    }

    public static android.widget.TextView statusBadge(android.content.Context context, java.lang.String str) {
        if ("RUNNING".equals(str)) {
            return badge(context, "进行中", BLUEGRAY, PALE_GREEN);
        }
        if ("QUEUED".equals(str)) {
            return badge(context, "排队中", BLUEGRAY, PALE_GRAY);
        }
        if ("PASS".equals(str)) {
            return badge(context, "通过", GREEN, PALE_GREEN);
        }
        if ("SUSPECT".equals(str)) {
            return badge(context, "疑似降智", ORANGE, PALE_ORANGE);
        }
        if ("ERROR".equals(str)) {
            return badge(context, "错误", RED, PALE_RED);
        }
        return "FAIL".equals(str) ? badge(context, "检测失败", BLUEGRAY, PALE_GRAY) : badge(context, "尚未检测", MUTED, PALE_GRAY);
    }

    public static java.lang.String statusText(java.lang.String str) {
        if ("RUNNING".equals(str)) {
            return "进行中";
        }
        if ("QUEUED".equals(str)) {
            return "排队中";
        }
        if ("PASS".equals(str)) {
            return "通过";
        }
        if ("SUSPECT".equals(str)) {
            return "疑似降智";
        }
        if ("ERROR".equals(str)) {
            return "错误";
        }
        return "FAIL".equals(str) ? "检测失败" : "尚未检测";
    }

    public static void action(android.widget.LinearLayout linearLayout, android.view.View view, float f) {
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(0, -2, f);
        if (linearLayout.getChildCount() > 0) {
            layoutParams.leftMargin = dp(linearLayout.getContext(), 8.0f);
        }
        linearLayout.addView(view, layoutParams);
    }

    public static java.lang.String elapsed(long j, long j2) {
        if (j2 <= 0) {
            j2 = java.lang.System.currentTimeMillis();
        }
        long jMax = java.lang.Math.max(0L, j2 - j) / 1000;
        return (jMax >= 60 ? new java.lang.StringBuilder().append(jMax / 60).append("分").append(jMax % 60) : new java.lang.StringBuilder().append(jMax)).append("秒").toString();
    }

    public static void systemBars(android.app.Activity activity) {
        activity.getWindow().setStatusBarColor(-1);
        activity.getWindow().setNavigationBarColor(-1);
        activity.getWindow().getDecorView().setSystemUiVisibility(8208);
    }

    public static java.lang.String time(long j) {
        return j <= 0 ? "—" : new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(j));
    }

    public static java.lang.String dateTime(long j) {
        return j <= 0 ? "—" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date(j));
    }

    public static java.lang.String bytes(long j) {
        if (j < okhttp3.internal.ws.RealWebSocket.DEFAULT_MINIMUM_DEFLATE_SIZE) {
            return j + " B";
        }
        if (j < 1048576) {
            return java.lang.String.format(java.util.Locale.getDefault(), "%.1f KB", java.lang.Double.valueOf(j / 1024.0d));
        }
        return j < 1073741824 ? java.lang.String.format(java.util.Locale.getDefault(), "%.1f MB", java.lang.Double.valueOf((j / 1024.0d) / 1024.0d)) : java.lang.String.format(java.util.Locale.getDefault(), "%.2f GB", java.lang.Double.valueOf(((j / 1024.0d) / 1024.0d) / 1024.0d));
    }

    public static android.widget.LinearLayout.LayoutParams params(int i, int i2) {
        return new android.widget.LinearLayout.LayoutParams(i, i2);
    }

    public static android.widget.LinearLayout.LayoutParams weight(android.content.Context context, float f) {
        return new android.widget.LinearLayout.LayoutParams(0, -2, f);
    }

    public static void add(android.widget.LinearLayout linearLayout, android.view.View view) {
        linearLayout.addView(view);
    }

    public static void add(android.widget.LinearLayout linearLayout, android.view.View view, int i) {
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = dp(linearLayout.getContext(), i);
        linearLayout.addView(view, layoutParams);
    }

    public static void space(android.widget.LinearLayout linearLayout, int i) {
        linearLayout.addView(new android.view.View(linearLayout.getContext()), new android.widget.LinearLayout.LayoutParams(1, dp(linearLayout.getContext(), i)));
    }

    public static void toast(android.content.Context context, java.lang.String str) {
        if (str == null) {
            str = "";
        }
        android.widget.Toast.makeText(context, str, 0).show();
    }

    public static android.widget.Spinner choice(final android.content.Context c, java.util.List<String> values) {
        android.widget.Spinner spinner = new android.widget.Spinner(c, android.widget.Spinner.MODE_DROPDOWN);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, values) {
            @Override public android.view.View getView(int position, android.view.View convert, android.view.ViewGroup parent) {
                android.widget.LinearLayout row = Ui.row(c);
                row.setPadding(dp(c, 12), 0, dp(c, 12), 0); row.setMinimumHeight(dp(c, 48));
                android.widget.TextView label = Ui.text(c, getItem(position), 14, TEXT);
                label.setMaxLines(3);
                row.addView(label, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
                android.widget.TextView arrow = Ui.text(c, "⌄", 17, GREEN); arrow.setPadding(dp(c, 8),0,0,0);row.addView(arrow);
                return row;
            }
            @Override public android.view.View getDropDownView(int position, android.view.View convert, android.view.ViewGroup parent) {
                android.widget.TextView text = Ui.text(c, getItem(position), 14, TEXT);
                text.setPadding(dp(c, 16),dp(c, 12),dp(c, 16),dp(c, 12));
                text.setMinHeight(dp(c, 50)); text.setMaxLines(3); text.setGravity(android.view.Gravity.CENTER_VERTICAL);
                text.setBackgroundColor(CARD); return text;
            }
        };
        spinner.setAdapter(adapter);spinner.setMinimumHeight(dp(c, 48));
        spinner.setBackground(Ui.bordered(c, CARD, BORDER, 10));
        return spinner;
    }

    public static android.widget.Spinner choiceField(android.widget.LinearLayout parent, String label, java.util.List<String> values) {
        android.content.Context c = parent.getContext();
        Ui.add(parent, Ui.text(c, label, 11, MUTED), 8);
        android.widget.Spinner spinner=choice(c,values);spinner.setContentDescription(label);
        Ui.add(parent,spinner,4);return spinner;
    }
}
