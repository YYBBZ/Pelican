package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class ModelPickerDialog {

    public interface OnPick {
        void pick(java.lang.String str);
    }

    public static void show(final android.content.Context context, java.util.List<java.lang.String> list, final java.lang.String str, final app.pelican.check.ModelPickerDialog.OnPick onPick) {
        final android.app.Dialog dialog = new android.app.Dialog(context);
        android.widget.LinearLayout linearLayoutColumn = app.pelican.check.Ui.column(context);
        linearLayoutColumn.setPadding(app.pelican.check.Ui.dp(context, 18.0f), app.pelican.check.Ui.dp(context, 16.0f), app.pelican.check.Ui.dp(context, 18.0f), app.pelican.check.Ui.dp(context, 12.0f));
        linearLayoutColumn.setBackground(app.pelican.check.Ui.round(context, app.pelican.check.Ui.BG, 20.0f));
        linearLayoutColumn.addView(app.pelican.check.Ui.heading(context, "选择模型 · 共 " + list.size() + " 个", 19.0f));
        android.view.View viewText = app.pelican.check.Ui.text(context, "可输入关键字筛选；没有想要的模型时，可返回后手动填写。", 12.0f, app.pelican.check.Ui.MUTED);
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams.topMargin = app.pelican.check.Ui.dp(context, 4.0f);
        linearLayoutColumn.addView(viewText, layoutParams);
        android.widget.EditText editTextInput = app.pelican.check.Ui.input(context, "搜索模型名", "");
        android.widget.LinearLayout.LayoutParams layoutParams2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams2.topMargin = app.pelican.check.Ui.dp(context, 12.0f);
        linearLayoutColumn.addView(editTextInput, layoutParams2);
        android.widget.ListView listView = new android.widget.ListView(context);
        listView.setDividerHeight(app.pelican.check.Ui.dp(context, 1.0f));
        final android.widget.ArrayAdapter<java.lang.String> arrayAdapter = new android.widget.ArrayAdapter<java.lang.String>(context, android.R.layout.simple_list_item_1, new java.util.ArrayList(list)) { // from class: app.pelican.check.ModelPickerDialog.1
            @Override // android.widget.ArrayAdapter, android.widget.Adapter
            public android.view.View getView(int i, android.view.View view, android.view.ViewGroup viewGroup) {
                android.widget.TextView textView = (android.widget.TextView) super.getView(i, view, viewGroup);
                textView.setTextColor(app.pelican.check.Ui.TEXT);
                textView.setTextSize(15.0f);
                textView.setPadding(app.pelican.check.Ui.dp(context, 12.0f), app.pelican.check.Ui.dp(context, 14.0f), app.pelican.check.Ui.dp(context, 12.0f), app.pelican.check.Ui.dp(context, 14.0f));
                java.lang.String item = getItem(i);
                if (item != null && item.equals(str)) {
                    item = item + "\u3000✓";
                }
                textView.setText(item);
                return textView;
            }
        };
        listView.setAdapter((android.widget.ListAdapter) arrayAdapter);
        android.widget.LinearLayout.LayoutParams layoutParams3 = new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f);
        layoutParams3.topMargin = app.pelican.check.Ui.dp(context, 10.0f);
        linearLayoutColumn.addView(listView, layoutParams3);
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() { // from class: app.pelican.check.ModelPickerDialog.2
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(android.widget.AdapterView<?> adapterView, android.view.View view, int i, long j) {
                java.lang.String str2 = (java.lang.String) arrayAdapter.getItem(i);
                if (str2 != null) {
                    onPick.pick(str2);
                }
                dialog.dismiss();
            }
        });
        editTextInput.addTextChangedListener(new android.text.TextWatcher() { // from class: app.pelican.check.ModelPickerDialog.3
            @Override // android.text.TextWatcher
            public void beforeTextChanged(java.lang.CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override // android.text.TextWatcher
            public void onTextChanged(java.lang.CharSequence charSequence, int i, int i2, int i3) {
                arrayAdapter.getFilter().filter(charSequence);
            }

            @Override // android.text.TextWatcher
            public void afterTextChanged(android.text.Editable editable) {
            }
        });
        android.widget.Button button = app.pelican.check.Ui.button(context, "关闭", false);
        android.widget.LinearLayout.LayoutParams layoutParams4 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        layoutParams4.topMargin = app.pelican.check.Ui.dp(context, 8.0f);
        linearLayoutColumn.addView(button, layoutParams4);
        button.setOnClickListener(new android.view.View.OnClickListener() { // from class: app.pelican.check.ModelPickerDialog.4
            @Override // android.view.View.OnClickListener
            public void onClick(android.view.View view) {
                dialog.dismiss();
            }
        });
        dialog.setContentView(linearLayoutColumn);
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(-1, -1);
        }
        dialog.show();
        if (window != null) {
            window.setLayout((int) (((double) context.getResources().getDisplayMetrics().widthPixels) * 0.94d), (int) (((double) context.getResources().getDisplayMetrics().heightPixels) * 0.82d));
            window.setGravity(17);
        }
    }
}
