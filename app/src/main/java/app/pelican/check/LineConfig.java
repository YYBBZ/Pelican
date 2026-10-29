package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class LineConfig {
    public static final app.pelican.check.LineConfig.Prompt[] PROMPTS = {new app.pelican.check.LineConfig.Prompt("pelican_en", "鹈鹕骑自行车（英文原题）", "Generate an SVG of a pelican riding a bicycle. Reply with the SVG code only"), new app.pelican.check.LineConfig.Prompt("pelican_zh", "鹈鹕骑自行车（中文）", "用 SVG 画一只正在骑自行车的鹈鹕。只返回完整的 SVG 代码，不要解释。"), new app.pelican.check.LineConfig.Prompt("saturn", "土星环上的鹈鹕（困难）", "用 SVG 画一只正在土星环上骑自行车的鹈鹕，背景要有行星和星环。只返回完整的 SVG 代码，不要解释。"), new app.pelican.check.LineConfig.Prompt("garfield", "加菲猫骑缅因猫", "Generate an SVG of Garfield riding a Maine Coon cat. Reply with the SVG code only."), new app.pelican.check.LineConfig.Prompt("custom", "自定义题目", "")};
    public java.lang.String id = java.util.UUID.randomUUID().toString();
    public long createdAt = java.lang.System.currentTimeMillis();
    public java.lang.String name = "新线路";
    public java.lang.String protocol = "openai";
    public java.lang.String baseUrl = "";
    public java.lang.String apiKey = "";
    public java.lang.String model = "";
    public java.lang.String promptMode = "fixed";
    public java.lang.String promptId = "pelican_en";
    public java.lang.String customPrompt = "";
    public int sampleCount = 1;
    public java.lang.String effort = "auto";
    public double temperature = 0.7d;
    public int configVersion;
    public String inputTokenMode="amount";
    public double inputTokenRatio=5.0;
    public int inputTokenExcess = 800;
    public double complexityMultiplier=0.9;
    public java.util.List<PromptSequence.Entry> promptSequence=null;
    public int sequenceIndex;
    public String sequenceRevision="",lastSequenceRunId="",sequenceFailureNote="";
    public boolean sequenceBlocked;

    public boolean tokenAuditEnabled = true;
    public String resolvedApiBase="";
    public String cachedModelsBase="";
    public String cachedModelsIdentity="";
    @Deprecated public Double inputTokenBaselineOverride = null;
    @Deprecated public double inputTokenAlertRatio = 2.0;
    public int maxTokens = 16000;
    public int intervalMin = 10;
    public boolean enabled = false;
    public int retryCount = 2;
    public boolean retryPartial = true;
    public boolean notifyEnabled = true;
    public java.lang.Double baselineLowerOverride = null;
    public long nextRunAt = 0;
    public long lastRunAt = 0;
    public java.lang.String lastStatus = "";
    public java.lang.String lastBadge = "";
    public int consecutiveIssues = 0;
    public java.util.List<java.lang.String> cachedModels = new java.util.ArrayList();
    public long modelsFetchedAt = 0;

    public java.lang.String displayName() {
        return (this.name == null || this.name.trim().isEmpty()) ? "未命名线路" : this.name.trim();
    }

    public app.pelican.check.LineConfig copyWithoutKey() {
        app.pelican.check.LineConfig lineConfig = (app.pelican.check.LineConfig) app.pelican.check.Json.gson().fromJson(app.pelican.check.Json.gson().toJson(this), app.pelican.check.LineConfig.class);
        lineConfig.apiKey = "";
        lineConfig.cachedModelsIdentity = "";
        return lineConfig;
    }

    public static class Prompt {
        public final java.lang.String id;
        public final java.lang.String label;
        public final java.lang.String text;

        public Prompt(java.lang.String str, java.lang.String str2, java.lang.String str3) {
            this.id = str;
            this.label = str2;
            this.text = str3;
        }
    }

    public static app.pelican.check.LineConfig.Prompt promptById(java.lang.String str) {
        for (app.pelican.check.LineConfig.Prompt prompt : PROMPTS) {
            if (prompt.id.equals(str)) {
                return prompt;
            }
        }
        return PROMPTS[0];
    }

    public app.pelican.check.LineConfig.Prompt promptForRun(long unusedTime) {return PromptSequence.current(this).prompt();}

    public java.lang.String promptText(app.pelican.check.LineConfig.Prompt prompt) {
        if(promptSequence!=null)for(PromptSequence.Entry e:promptSequence)if(e.id.equals(prompt.id))return e.text;
        return "custom".equals(prompt.id) ? this.customPrompt : prompt.text;
    }
    public void normalize(){
        if(inputTokenMode==null||(!"amount".equals(inputTokenMode)&&!"ratio".equals(inputTokenMode)))inputTokenMode="amount";
        if(!Double.isFinite(inputTokenRatio)||inputTokenRatio<1)inputTokenRatio=5;
        if(!Double.isFinite(complexityMultiplier)||complexityMultiplier<0.5||complexityMultiplier>1.5)complexityMultiplier=0.9;
        if(inputTokenExcess<0)inputTokenExcess=800;
        if(cachedModels==null)cachedModels=new java.util.ArrayList<>();
        PromptSequence.normalize(this);configVersion=4;
    }
    public static LineConfig createNew(){LineConfig c=new LineConfig();c.promptSequence=PromptSequence.defaults();c.configVersion=4;return c;}
    public static LineConfig fromJson(String json){LineConfig c=Json.gson().fromJson(json,LineConfig.class);if(c!=null)c.normalize();return c;}
    public void validateSettings(){
        if(sampleCount<1||sampleCount>99)throw new IllegalArgumentException("每轮样本须为 1～99 张");
        if(retryCount<0||retryCount>20)throw new IllegalArgumentException("最多补试须为 0～20 次");
        if(intervalMin<1||intervalMin>1440)throw new IllegalArgumentException("检测间隔须为 1～1440 分钟");
        if(inputTokenExcess<0||inputTokenExcess>10000000)throw new IllegalArgumentException("容许数量须为 0～10,000,000 Token");
        if(!Double.isFinite(inputTokenRatio)||inputTokenRatio<1||inputTokenRatio>1000000)throw new IllegalArgumentException("输入倍率须为 1～1,000,000");
        if(!Double.isFinite(complexityMultiplier)||complexityMultiplier<0.5||complexityMultiplier>1.5)throw new IllegalArgumentException("复杂度倍率须为 0.5～1.5");
        if(baselineLowerOverride!=null&&(!Double.isFinite(baselineLowerOverride)||baselineLowerOverride<0||baselineLowerOverride>100))throw new IllegalArgumentException("复杂度固定下限须为 0～100");
        if(maxTokens<1||maxTokens>1000000)throw new IllegalArgumentException("输出上限须为 1～1,000,000 Token");
        if(!Double.isFinite(temperature)||temperature<0||temperature>2)throw new IllegalArgumentException("温度须为 0～2");
        PromptSequence.validate(this);ReasoningPolicy.validate(this);
    }
}
