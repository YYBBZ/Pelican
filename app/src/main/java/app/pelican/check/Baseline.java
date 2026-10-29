package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class Baseline {
    public java.lang.String key;
    public java.lang.String lineId;
    public java.lang.String model;
    public java.lang.String promptKey;
    public long updatedAt;
    public java.util.List<java.lang.Double> values = new java.util.ArrayList();
    public java.lang.Double lower = null;

    public static java.lang.String makeKey(java.lang.String str, java.lang.String str2, java.lang.String str3) {
        return str + "\n" + str2 + "\n" + str3;
    }

    public java.lang.Double addSuccessfulScore(double d) {
        if (this.lower != null) {
            return this.lower;
        }
        this.values.add(java.lang.Double.valueOf(d));
        if (this.values.size() > 5) {
            this.values = new java.util.ArrayList(this.values.subList(this.values.size() - 5, this.values.size()));
        }
        if (this.values.size() == 5) {
            java.util.ArrayList arrayList = new java.util.ArrayList(this.values);
            java.util.Collections.sort(arrayList);
            this.lower = java.lang.Double.valueOf(java.lang.Math.round((((java.lang.Double) arrayList.get(2)).doubleValue() * 0.8d) * 10.0d) / 10.0d);
        }
        return this.lower;
    }
}
