package app.pelican.check;

/* JADX INFO: loaded from: classes.dex */
public class ApiClient {
    private static final okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json; charset=utf-8");
    private static final java.util.Set<java.lang.String> STANDARD_USAGE = new java.util.HashSet(java.util.Arrays.asList("prompt_tokens", "completion_tokens", "total_tokens", "prompt_tokens_details", "completion_tokens_details", "input_tokens", "output_tokens", "output_tokens_details", "cache_creation_input_tokens", "cache_read_input_tokens", "cache_creation", "service_tier", "inference_geo"));

    /* JADX INFO: renamed from: app, reason: collision with root package name */
    private final android.content.Context f4app;
    private volatile boolean cancelled;
    private final okhttp3.OkHttpClient http;

    public static class ModelResult {
        public java.lang.String message = "";
        public java.util.List<java.lang.String> models = new java.util.ArrayList();
        public boolean ok;
        public int status;
        public String endpoint="";
        public String apiBase="";
    }

    public static class Progress {
        public int attempt;
        public long attemptStartedAt;
        public app.pelican.check.RunData.AttemptData completedAttempt;
        public int httpStatus;
        public int maxAttempts;
        public int receivedChars;
        public boolean resetOutput;
        public long retryAt;
        public int sampleIndex;
        public java.lang.String stage = "";
        public java.lang.String delta = "";
        public java.lang.String note = "";
    }

    public interface ProgressListener {
        void onProgress(app.pelican.check.ApiClient.Progress progress);
    }

    public ApiClient(android.content.Context context) {
        this(context, null);
    }

    public ApiClient(android.content.Context context, okhttp3.OkHttpClient okHttpClient) {
        okhttp3.OkHttpClient okHttpClientBuild;
        this.f4app = context == null ? null : context.getApplicationContext();
        if (okHttpClient != null) {
            okHttpClientBuild = okHttpClient.newBuilder().retryOnConnectionFailure(false).build();
        } else {
            okHttpClientBuild = new okhttp3.OkHttpClient.Builder().connectTimeout(20L, java.util.concurrent.TimeUnit.SECONDS).writeTimeout(60L, java.util.concurrent.TimeUnit.SECONDS).readTimeout(300L, java.util.concurrent.TimeUnit.SECONDS).retryOnConnectionFailure(false).build();
        }
        this.http = okHttpClientBuild;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void emit(app.pelican.check.ApiClient.ProgressListener progressListener, app.pelican.check.ApiClient.Progress progress) {
        if (progressListener != null) {
            progressListener.onProgress(progress);
        }
    }

    public void cancelAll() {
        this.cancelled = true;
        this.http.dispatcher().cancelAll();
    }

    public static java.util.List<java.lang.String> parseModelIds(java.lang.String str) {
        java.util.LinkedHashSet linkedHashSet = new java.util.LinkedHashSet();
        try {
            com.google.gson.JsonElement string = com.google.gson.JsonParser.parseString(str);
            if (string != null && string.isJsonObject()) {
                com.google.gson.JsonObject asJsonObject = string.getAsJsonObject();
                if (asJsonObject.has("data")) {
                    string = asJsonObject.get("data");
                } else if (asJsonObject.has("models")) {
                    string = asJsonObject.get("models");
                }
            }
            collectModels(string, linkedHashSet);
        } catch (java.lang.Throwable th) {
        }
        return new java.util.ArrayList(linkedHashSet);
    }

    private static void collectModels(com.google.gson.JsonElement jsonElement, java.util.LinkedHashSet<java.lang.String> linkedHashSet) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return;
        }
        if (jsonElement.isJsonArray()) {
            java.util.Iterator<com.google.gson.JsonElement> it = jsonElement.getAsJsonArray().iterator();
            while (it.hasNext()) {
                collectModels(it.next(), linkedHashSet);
            }
            return;
        }
        if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()) {
            addModel(jsonElement.getAsString(), linkedHashSet);
            return;
        }
        if (jsonElement.isJsonObject()) {
            com.google.gson.JsonObject asJsonObject = jsonElement.getAsJsonObject();
            for(String container:new String[]{"data","models","items","result"})if(asJsonObject.has(container)){
                collectModels(asJsonObject.get(container),linkedHashSet);return;
            }
            java.lang.String[] strArr = {"id", "name", "model"};
            for (int i = 0; i < 3; i++) {
                java.lang.String str = strArr[i];
                if (asJsonObject.has(str) && asJsonObject.get(str).isJsonPrimitive()) {
                    addModel(asJsonObject.get(str).getAsString(), linkedHashSet);
                    return;
                }
            }
        }
    }

    private static void addModel(java.lang.String str, java.util.LinkedHashSet<java.lang.String> linkedHashSet) {
        if (str == null) {
            return;
        }
        java.lang.String strTrim = str.trim();
        if (strTrim.isEmpty() || strTrim.equals("[object Object]") || strTrim.length() >= 300) {
            return;
        }
        linkedHashSet.add(strTrim);
    }

    public app.pelican.check.ApiClient.ModelResult fetchModels(app.pelican.check.LineConfig config) {
        ModelResult result=new ModelResult();
        try {
            String key=app.pelican.check.RequestInputs.key(config.apiKey);
            java.util.List<String> urls=endpoints(config.baseUrl,config.protocol,"/models");
            for(int i=0;i<urls.size();i++){
                result.endpoint=urls.get(i);
                okhttp3.Request.Builder request=new okhttp3.Request.Builder().url(result.endpoint).get();
                authHeaders(request,config);
                try(okhttp3.Response response=http.newBuilder().followRedirects(false).followSslRedirects(false).callTimeout(30,java.util.concurrent.TimeUnit.SECONDS).build().newCall(request.build()).execute()){
                    result.status=response.code();
                    String body=response.body()==null?"":response.body().string();
                    if((result.status==404||result.status==405)&&i+1<urls.size())continue;
                    if(result.status>=300&&result.status<400){result.message="接口发生重定向，未转发密钥；请填写最终 API 地址";return result;}
                    if(!response.isSuccessful()){
                        String prefix=result.status==401?"鉴权未通过：请确认密钥属于这个站点且仍有效。":result.status==403?"此密钥没有访问模型列表的权限。":"";
                        result.message=prefix+"HTTP "+result.status+"\n"+app.pelican.check.RequestInputs.safeError(body.length()>1200?body.substring(0,1200):body,config.apiKey);
                        return result;
                    }
                    try{
                        com.google.gson.JsonElement json=com.google.gson.JsonParser.parseString(body);
                        if(!json.isJsonArray()&&!json.isJsonObject())throw new IllegalArgumentException();
                        if(json.isJsonObject()&&json.getAsJsonObject().has("error")&&!json.getAsJsonObject().get("error").isJsonNull()){
                            result.message="接口返回错误（HTTP 200）："+app.pelican.check.RequestInputs.safeError(json.getAsJsonObject().get("error").toString(),config.apiKey);return result;
                        }
                        result.models=parseModelIds(body);result.ok=true;
                        result.apiBase=result.endpoint.substring(0,result.endpoint.length()-"/models".length());
                        result.message=result.models.isEmpty()?"已连接，但列表为空；仍可手动填写模型名":"已获取 "+result.models.size()+" 个模型";
                        return result;
                    }catch(Exception e){result.message="地址可访问，但返回的不是模型 JSON；请检查是否填成了网站首页";return result;}
                }
            }
        }catch(IllegalArgumentException e){result.message=e.getMessage();}
        catch(Exception e){result.message=friendlyException(e);}
        return result;
    }

    public static java.util.List<String> endpoints(String base,String protocol,String path){return app.pelican.check.RequestInputs.endpoints(base,path);}

    private void authHeaders(okhttp3.Request.Builder request,app.pelican.check.LineConfig config){
        String key=app.pelican.check.RequestInputs.key(config.apiKey);
        if("anthropic".equals(config.protocol))request.header("x-api-key",key).header("anthropic-version","2023-06-01");
        else request.header("Authorization","Bearer "+key);
        request.header("User-Agent","Pelican/2.4 Android");
    }

    private com.google.gson.JsonObject payload(app.pelican.check.LineConfig lineConfig, java.lang.String str) {
        int i;
        com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();
        jsonObject.addProperty("model", lineConfig.model);
        jsonObject.addProperty("stream", (java.lang.Boolean) true);
        com.google.gson.JsonArray jsonArray = new com.google.gson.JsonArray();
        com.google.gson.JsonObject jsonObject2 = new com.google.gson.JsonObject();
        jsonObject2.addProperty("role", "user");
        jsonObject2.addProperty("content", str);
        jsonArray.add(jsonObject2);
        jsonObject.add("messages", jsonArray);
        ReasoningPolicy.validate(lineConfig);
        int iMax = lineConfig.maxTokens;
        if(iMax<1)throw new IllegalArgumentException("输出 Token 上限必须大于 0");
        if ("responses".equals(lineConfig.protocol)) {
            jsonObject.remove("messages");jsonObject.add("input",jsonArray);
            jsonObject.addProperty("max_output_tokens",iMax);
            if(!"auto".equals(lineConfig.effort)){
                com.google.gson.JsonObject reasoning=new com.google.gson.JsonObject();reasoning.addProperty("effort",lineConfig.effort);jsonObject.add("reasoning",reasoning);
            }
            return jsonObject;
        }
        if ("anthropic".equals(lineConfig.protocol)) {
            jsonObject.addProperty("max_tokens", java.lang.Integer.valueOf(iMax));
            if ("auto".equals(lineConfig.effort) || "none".equals(lineConfig.effort)) {
                jsonObject.addProperty("temperature", java.lang.Double.valueOf(lineConfig.temperature));
                if("none".equals(lineConfig.effort)){
                    com.google.gson.JsonObject disabled=new com.google.gson.JsonObject();disabled.addProperty("type","disabled");jsonObject.add("thinking",disabled);
                }
            } else {
                i=ReasoningPolicy.budget(lineConfig);
                com.google.gson.JsonObject jsonObject3 = new com.google.gson.JsonObject();
                jsonObject3.addProperty("type", "enabled");
                jsonObject3.addProperty("budget_tokens", java.lang.Integer.valueOf(i));
                jsonObject.add("thinking", jsonObject3);
                if (iMax <= i) throw new IllegalArgumentException("输出上限必须大于所选思考预算；不会自动提高输出上限或降低思考强度");
            }
        } else {
            jsonObject.addProperty("temperature", java.lang.Double.valueOf(lineConfig.temperature));
            jsonObject.addProperty("max_tokens", java.lang.Integer.valueOf(iMax));
            com.google.gson.JsonObject jsonObject4 = new com.google.gson.JsonObject();
            jsonObject4.addProperty("include_usage", (java.lang.Boolean) true);
            jsonObject.add("stream_options", jsonObject4);
            if (!"auto".equals(lineConfig.effort)) {
                jsonObject.addProperty("reasoning_effort", lineConfig.effort);
            }
        }
        return jsonObject;
    }

    private static class GenResult {
        java.lang.String error;
        java.lang.String errorClass;
        int httpStatus;
        int inTokens;
        InputAudit.Evidence inputAudit;
        int rawInputTokens;
        boolean separateCacheInput;
        int cacheReadInputTokens;
        int cacheCreationInputTokens;
        java.lang.String modelEcho;
        boolean ok;
        int outTokens;
        boolean partial;
        java.lang.Integer reasonTokens;
        java.lang.String responseId;
        long retryAfterMs;
        boolean retryable;
        java.lang.String stage;
        java.lang.String text;
        long totalMs;
        long ttftMs;
        java.util.List<java.lang.String> usageExtra;

        private GenResult() {
            this.error = "";
            this.errorClass = "";
            this.stage = "";
            this.text = "";
            this.modelEcho = "";
            this.responseId = "";
            this.usageExtra = new java.util.ArrayList();
        }
    }

    public app.pelican.check.RunData.SampleData generateSample(app.pelican.check.LineConfig lineConfig, java.lang.String str, int i) {
        return generateSample(lineConfig, str, i, null);
    }

    public app.pelican.check.RunData.SampleData generateSample(app.pelican.check.LineConfig lineConfig, java.lang.String str, final int i, final app.pelican.check.ApiClient.ProgressListener progressListener) {
        app.pelican.check.ApiClient.ProgressListener progressListener2;
        app.pelican.check.RunData.SampleData sampleData = new app.pelican.check.RunData.SampleData();
        sampleData.index = i;
        final int iMaxAttempts = app.pelican.check.RetryPolicy.maxAttempts(lineConfig.retryCount);
        long jCurrentTimeMillis = java.lang.System.currentTimeMillis();
        boolean z = true;
        int i2 = 1;
        app.pelican.check.ApiClient.GenResult genResultPerformOnce = null;
        while (true) {
            if (i2 > iMaxAttempts) {
                jCurrentTimeMillis = jCurrentTimeMillis;
                break;
            }
            if (this.cancelled) {
                jCurrentTimeMillis = jCurrentTimeMillis;
                break;
            }
            if (java.lang.Thread.currentThread().isInterrupted()) {
                jCurrentTimeMillis = jCurrentTimeMillis;
                break;
            }
            app.pelican.check.RunData.AttemptData attemptData2 = new app.pelican.check.RunData.AttemptData();
            attemptData2.number = i2;
            attemptData2.startedAt = java.lang.System.currentTimeMillis();
            app.pelican.check.ApiClient.Progress progress = new app.pelican.check.ApiClient.Progress();
            progress.sampleIndex = i;
            progress.attempt = i2;
            progress.maxAttempts = iMaxAttempts;
            progress.stage = "建立连接";
            progress.resetOutput = z;
            progress.attemptStartedAt = attemptData2.startedAt;
            emit(progressListener, progress);
            final app.pelican.check.RunData.AttemptData attemptData = attemptData2;
            if (progressListener == null) {
                progressListener2 = null;
            } else {
                final int i3 = i2;
                progressListener2 = new app.pelican.check.ApiClient.ProgressListener() { // from class: app.pelican.check.ApiClient.1
                    @Override // app.pelican.check.ApiClient.ProgressListener
                    public void onProgress(app.pelican.check.ApiClient.Progress progress2) {
                        progress2.sampleIndex = i;
                        progress2.attempt = i3;
                        progress2.maxAttempts = iMaxAttempts;
                        progress2.attemptStartedAt = attemptData.startedAt;
                        app.pelican.check.ApiClient.emit(progressListener, progress2);
                    }
                };
            }
            genResultPerformOnce = performOnce(lineConfig, str, progressListener2);
            app.pelican.check.RunData.AttemptData attemptData3 = attemptData;
            attemptData3.durationMs = java.lang.System.currentTimeMillis() - attemptData3.startedAt;
            attemptData3.success = genResultPerformOnce.ok;
            attemptData3.retryable = genResultPerformOnce.retryable;
            attemptData3.partial = genResultPerformOnce.partial;
            attemptData3.stage = genResultPerformOnce.stage;
            attemptData3.errorClass = genResultPerformOnce.errorClass;
            attemptData3.message = genResultPerformOnce.error;
            attemptData3.httpStatus = genResultPerformOnce.httpStatus;
            attemptData3.receivedChars = genResultPerformOnce.text == null ? 0 : genResultPerformOnce.text.length();
            attemptData3.responseTextForSave = genResultPerformOnce.text;
            sampleData.attempts.add(attemptData3);
            genResultPerformOnce.error = redact(genResultPerformOnce.error, lineConfig.apiKey);
            attemptData3.message = genResultPerformOnce.error;
            boolean z2 = (this.cancelled || java.lang.Thread.currentThread().isInterrupted() || i2 >= iMaxAttempts || !genResultPerformOnce.retryable || genResultPerformOnce.retryAfterMs > 300000 || (genResultPerformOnce.partial && !lineConfig.retryPartial)) ? false : z;
            if (!genResultPerformOnce.ok) {
                if (this.cancelled || java.lang.Thread.currentThread().isInterrupted()) {
                    attemptData3.retryDecision = "任务已停止，不再重试";
                } else if (genResultPerformOnce.partial && !lineConfig.retryPartial) {
                    attemptData3.retryDecision = "本线路已关闭断流重试";
                } else if (genResultPerformOnce.retryAfterMs > 300000) {
                    attemptData3.retryDecision = "服务器要求等待超过 5 分钟，本轮不再补试";
                } else if (i2 >= iMaxAttempts && genResultPerformOnce.retryable) {
                    attemptData3.retryDecision = lineConfig.retryCount == 0 ? "本线路设置为不重试" : "已用完本轮重试次数";
                } else if (z2) {
                    attemptData3.retryDecision = "将在本轮重试，不等待下个定时周期";
                } else {
                    attemptData3.retryDecision = "明确的配置、认证、证书或响应问题，不自动重试";
                }
            }
            app.pelican.check.ApiClient.Progress progress2 = new app.pelican.check.ApiClient.Progress();
            progress2.sampleIndex = i;
            progress2.attempt = i2;
            progress2.maxAttempts = iMaxAttempts;
            progress2.stage = genResultPerformOnce.ok ? "解析 SVG" : "请求失败";
            progress2.completedAttempt = attemptData3;
            progress2.receivedChars = attemptData3.receivedChars;
            emit(progressListener, progress2);
            if (genResultPerformOnce.ok) {
                sampleData.requestOk = true;
                break;
            }
            if (!z2) {
                break;
            }
            long jDelayMs = app.pelican.check.RetryPolicy.delayMs(genResultPerformOnce.retryAfterMs, i2);
            app.pelican.check.ApiClient.Progress progress3 = new app.pelican.check.ApiClient.Progress();
            progress3.sampleIndex = i;
            progress3.attempt = i2;
            progress3.maxAttempts = iMaxAttempts;
            progress3.retryAt = java.lang.System.currentTimeMillis() + jDelayMs;
            progress3.stage = "准备第 " + i2 + " 次重试";
            progress3.note = genResultPerformOnce.error + (jDelayMs > 0 ? "；短暂等待后重试" : "；立即重试");
            emit(progressListener, progress3);
            if (jDelayMs > 0) {
                try {
                    java.lang.Thread.sleep(jDelayMs);
                } catch (java.lang.InterruptedException e) {
                    java.lang.Thread.currentThread().interrupt();
                }
            }
            i2++;
            z = true;
            jCurrentTimeMillis = jCurrentTimeMillis;
        }
        sampleData.totalMs = java.lang.System.currentTimeMillis() - jCurrentTimeMillis;
        if (genResultPerformOnce != null) {
            sampleData.inputAudit = genResultPerformOnce.inputAudit;
            sampleData.finalAttemptMs = genResultPerformOnce.totalMs;
            sampleData.ttftMs = genResultPerformOnce.ttftMs;
            sampleData.outTokens = genResultPerformOnce.outTokens;
            sampleData.inTokens = genResultPerformOnce.inTokens;
            sampleData.reasonTokens = genResultPerformOnce.reasonTokens;
            sampleData.modelEcho = genResultPerformOnce.modelEcho;
            sampleData.responseId = genResultPerformOnce.responseId;
            sampleData.usageExtra = genResultPerformOnce.usageExtra;
            if (!genResultPerformOnce.ok) {
                sampleData.error = genResultPerformOnce.error;
                sampleData.errorClass = genResultPerformOnce.errorClass;
            }
        }
        if (this.cancelled || java.lang.Thread.currentThread().isInterrupted()) {
            sampleData.requestOk = false;
            sampleData.error = "监控已停止，本次检测中断";
            sampleData.errorClass = "任务已停止";
        }
        if (genResultPerformOnce == null && sampleData.error.isEmpty()) {
            sampleData.error = "检测尚未发起即被停止";
        }
        if (sampleData.requestOk && genResultPerformOnce != null) {
            sampleData.rawTextForSave = genResultPerformOnce.text;
            app.pelican.check.SvgAnalyzer.Result resultAnalyze = app.pelican.check.SvgAnalyzer.analyze(genResultPerformOnce.text);
            sampleData.cleanedSvgForSave = resultAnalyze.svg;
            sampleData.svgOk = resultAnalyze.ok;
            sampleData.score = resultAnalyze.score;
            sampleData.nodes = resultAnalyze.nodes;
            sampleData.colors = resultAnalyze.colors;
            sampleData.pathLength = resultAnalyze.pathLength;
            sampleData.size = resultAnalyze.size;
            sampleData.cheats = resultAnalyze.cheats;
            if (resultAnalyze.ok) {
                sampleData.evidence.add("SVG 结构有效");
            } else {
                sampleData.error = resultAnalyze.error;
            }
            if (!sampleData.modelEcho.isEmpty()) {
                if (sampleData.modelEcho.equals(lineConfig.model)) {
                    sampleData.evidence.add("回显模型一致");
                } else {
                    sampleData.evidence.add("回显模型不一致：" + sampleData.modelEcho);
                }
            }
            if (sampleData.reasonTokens != null) {
                sampleData.evidence.add("思考 token：" + sampleData.reasonTokens);
            } else {
                sampleData.evidence.add("思考 token 未回传");
            }
            if (!sampleData.usageExtra.isEmpty()) {
                sampleData.evidence.add("非标 usage：" + join(sampleData.usageExtra, "、"));
            }
            if (!sampleData.cheats.isEmpty()) {
                sampleData.evidence.add("取巧：" + join(sampleData.cheats, "、"));
            }
        }
        return sampleData;
    }

    /* JADX WARN: Multi-variable type inference failed */
    private app.pelican.check.ApiClient.GenResult performOnce(app.pelican.check.LineConfig lineConfig, java.lang.String str, final app.pelican.check.ApiClient.ProgressListener progressListener) {
        int i;
        okhttp3.Response response = null;
        final app.pelican.check.ApiClient.GenResult genResult = new app.pelican.check.ApiClient.GenResult();
        long jCurrentTimeMillis = java.lang.System.currentTimeMillis();
        genResult.stage = "建立连接";
        boolean z = true;
        try {
            final InputAudit.Frozen frozen=InputAudit.freeze(lineConfig,str,payload(lineConfig,str));
            genResult.inputAudit=frozen.evidence;
            // 2.4 uses the local estimate as the configured heuristic; no extra counting request.
            final byte[] requestBytes=frozen.bytes();
            okhttp3.RequestBody requestBody=new okhttp3.RequestBody(){
                @Override public okhttp3.MediaType contentType(){return JSON;}
                @Override public long contentLength(){return requestBytes.length;}
                @Override public void writeTo(okio.BufferedSink sink)throws java.io.IOException{
                    if(!frozen.matches(requestBytes))throw new java.io.IOException("APP 发送内容完整性校验失败");
                    sink.write(requestBytes);
                }
            };
            String route="responses".equals(lineConfig.protocol)?"/responses":"anthropic".equals(lineConfig.protocol)?"/messages":"/chat/completions";
            okhttp3.Request.Builder builderPost = new okhttp3.Request.Builder().url(RequestInputs.generationEndpoint(lineConfig,route)).post(requestBody);
            authHeaders(builderPost, lineConfig);
            builderPost.header("Content-Type", "application/json");
            builderPost.header("Accept", "text/event-stream, application/json");
            okhttp3.Response responseExecute = this.http.newBuilder().followRedirects(false).followSslRedirects(false).eventListener(new okhttp3.EventListener() { // from class: app.pelican.check.ApiClient.2
                private void stage(java.lang.String str2) {
                    genResult.stage = str2;
                    app.pelican.check.ApiClient.Progress progress = new app.pelican.check.ApiClient.Progress();
                    progress.stage = str2;
                    app.pelican.check.ApiClient.emit(progressListener, progress);
                }

                @Override // okhttp3.EventListener
                public void dnsStart(okhttp3.Call call, java.lang.String str2) {
                    stage("解析域名");
                }

                @Override // okhttp3.EventListener
                public void connectStart(okhttp3.Call call, java.net.InetSocketAddress inetSocketAddress, java.net.Proxy proxy) {
                    stage("建立连接");
                }

                @Override // okhttp3.EventListener
                public void secureConnectStart(okhttp3.Call call) {
                    stage("TLS 握手");
                }

                @Override // okhttp3.EventListener
                public void requestBodyEnd(okhttp3.Call call, long j) {
                    genResult.inputAudit.wireBytesVerified=j==genResult.inputAudit.bodyBytes;
                    stage("等待首字");
                }
            }).build().newCall(builderPost.build()).execute();
            try {
                genResult.httpStatus = responseExecute.code();
                genResult.stage = "等待首字";
                app.pelican.check.ApiClient.Progress progress = new app.pelican.check.ApiClient.Progress();
                progress.stage = genResult.stage;
                progress.httpStatus = genResult.httpStatus;
                emit(progressListener, progress);
                if (!responseExecute.isSuccessful()) {
                    java.lang.String strString = responseExecute.body() == null ? "" : responseExecute.body().string();
                    genResult.error = httpError(responseExecute.code(), strString);
                    genResult.httpStatus = responseExecute.code();
                    genResult.errorClass = statusClass(responseExecute.code());
                    genResult.retryable = app.pelican.check.RetryPolicy.retryStatus(responseExecute.code(), strString);
                    genResult.retryAfterMs = retryAfter(responseExecute.header("Retry-After"));
                    genResult.totalMs = java.lang.System.currentTimeMillis() - jCurrentTimeMillis;
                    if (responseExecute != null) {
                        responseExecute.close();
                    }
                    return genResult;
                }
                java.lang.String lowerCase = responseExecute.header("Content-Type", "").toLowerCase(java.util.Locale.ROOT);
                if (responseExecute.body() == null) {
                    throw new java.io.EOFException("响应没有正文");
                }
                java.io.BufferedReader bufferedReader = new java.io.BufferedReader(new java.io.InputStreamReader(responseExecute.body().byteStream(), java.nio.charset.StandardCharsets.UTF_8));
                boolean zContains = lowerCase.contains("event-stream");
                if (!zContains) {
                    bufferedReader.mark(4096);
                    java.lang.StringBuilder sb = new java.lang.StringBuilder();
                    for (int i2 = 0; i2 < 512 && (i = bufferedReader.read()) >= 0; i2++) {
                        if (sb.length() != 0 || (!java.lang.Character.isWhitespace(i) && i != 65279)) {
                            sb.append((char) i);
                            if (sb.length() >= 6) {
                                break;
                            }
                        }
                    }
                    bufferedReader.reset();
                    java.lang.String string = sb.toString();
                    zContains = string.startsWith("data:") || string.startsWith("event:") || string.startsWith(":");
                }
                if (zContains) {
                    parseStreamReader(bufferedReader, genResult, jCurrentTimeMillis, progressListener);
                } else {
                    app.pelican.check.ApiClient.Progress progress2 = new app.pelican.check.ApiClient.Progress();
                    progress2.stage = "接收非流式响应";
                    progress2.note = "接口未返回流式数据，正文会在完整收到后显示";
                    emit(progressListener, progress2);
                    java.lang.StringBuilder sb2 = new java.lang.StringBuilder();
                    char[] cArr = new char[8192];
                    while (true) {
                        int i3 = bufferedReader.read(cArr);
                        if (i3 == -1) {
                            break;
                        }
                        sb2.append(cArr, 0, i3);
                    }
                    parseNonStream(sb2.toString(), genResult);
                    if (!genResult.text.isEmpty()) {
                        app.pelican.check.ApiClient.Progress progress3 = new app.pelican.check.ApiClient.Progress();
                        progress3.stage = "解析响应";
                        progress3.delta = genResult.text;
                        progress3.receivedChars = genResult.text.length();
                        emit(progressListener, progress3);
                    }
                }
                genResult.totalMs = java.lang.System.currentTimeMillis() - jCurrentTimeMillis;
                if (!genResult.error.isEmpty()) {
                    if (responseExecute != null) {
                        responseExecute.close();
                    }
                    return genResult;
                }
                genResult.ok = true;
                genResult.stage = "完成";
                if (responseExecute != null) {
                    responseExecute.close();
                }
                return genResult;
            } catch (java.lang.Throwable th) {
                th = th;
                response = responseExecute;
                try {
                    genResult.totalMs = java.lang.System.currentTimeMillis() - jCurrentTimeMillis;
                    genResult.error = friendlyException(th);
                    genResult.errorClass = exceptionClass(th, genResult.partial);
                    if (!app.pelican.check.RetryPolicy.retryException(th) || (genResult.httpStatus >= 400 && !app.pelican.check.RetryPolicy.retryStatus(genResult.httpStatus, ""))) {
                        z = false;
                    }
                    genResult.retryable = z;
                    if (!hasValidatedInternet()) {
                        genResult.errorClass = "本机网络中断";
                    }
                    return genResult;
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
            }
        } catch (java.lang.Throwable th2) {
            genResult.totalMs = java.lang.System.currentTimeMillis() - jCurrentTimeMillis;
            genResult.error = friendlyException(th2);
            if(th2 instanceof IllegalArgumentException||th2 instanceof IllegalStateException){
                genResult.error="APP 请求自检："+th2.getMessage();genResult.errorClass="APP 配置或输入自检失败";genResult.retryable=false;return genResult;
            }
            genResult.errorClass = exceptionClass(th2, genResult.partial);
            genResult.retryable = app.pelican.check.RetryPolicy.retryException(th2)
                    && (genResult.httpStatus < 400 || app.pelican.check.RetryPolicy.retryStatus(genResult.httpStatus, ""));
            if (!hasValidatedInternet()) genResult.errorClass = "本机网络中断";
            if (response != null) response.close();
            return genResult;
        }
    }

    private static class StreamState {
        boolean done;
        boolean finish;
        java.lang.String id;
        java.lang.String model;
        java.lang.StringBuilder text;
        com.google.gson.JsonObject usage;

        private StreamState() {
            this.text = new java.lang.StringBuilder();
            this.model = "";
            this.id = "";
        }
    }

    private void parseStreamReader(java.io.BufferedReader bufferedReader, app.pelican.check.ApiClient.GenResult genResult, long j, app.pelican.check.ApiClient.ProgressListener progressListener) throws java.lang.Exception {
        app.pelican.check.ApiClient.StreamState streamState = new app.pelican.check.ApiClient.StreamState();
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        while (true) {
            try {
                java.lang.String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("\ufeff")) {
                    line = line.substring(1);
                }
                if (line.isEmpty()) {
                    if (sb.length() > 0) {
                        consumeFrame(sb.toString(), streamState, genResult, j, progressListener);
                        sb.setLength(0);
                        if (streamState.done || !genResult.error.isEmpty()) {
                            break;
                        }
                    } else {
                        continue;
                    }
                } else if (line.startsWith("data:")) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    java.lang.String strSubstring = line.substring(5);
                    if (strSubstring.startsWith(" ")) {
                        strSubstring = strSubstring.substring(1);
                    }
                    sb.append(strSubstring);
                }
            } catch (java.lang.Exception e) {
                genResult.partial = streamState.text.length() > 0;
                copyStream(streamState, genResult);
                throw e;
            }
        }
        if (!streamState.done && genResult.error.isEmpty() && sb.length() > 0) {
            consumeFrame(sb.toString(), streamState, genResult, j, progressListener);
        }
        copyStream(streamState, genResult);
        if (genResult.error.isEmpty() && !streamState.done && !streamState.finish) {
            genResult.partial = streamState.text.length() > 0;
            genResult.error = genResult.partial ? "收到一部分内容后连接提前结束" : "连接在没有完成标记时提前结束";
            genResult.errorClass = hasValidatedInternet() ? "响应中途断开（原因未确定）" : "本机网络中断";
            genResult.retryable = true;
        }
    }

    private void consumeFrame(java.lang.String str, app.pelican.check.ApiClient.StreamState streamState, app.pelican.check.ApiClient.GenResult genResult, long j, app.pelican.check.ApiClient.ProgressListener progressListener) {
        if ("[DONE]".equals(str.trim())) {
            streamState.done = true;
            return;
        }
        int length = streamState.text.length();
        try {
            com.google.gson.JsonElement string = com.google.gson.JsonParser.parseString(str);
            if (string == null || !string.isJsonObject()) {
                throw new com.google.gson.JsonParseException("SSE data must be a JSON object");
            }
            consumeData(string, streamState, genResult, j);
            if (streamState.text.length() > length) {
                app.pelican.check.ApiClient.Progress progress = new app.pelican.check.ApiClient.Progress();
                progress.stage = "接收内容";
                progress.delta = streamState.text.substring(length);
                progress.receivedChars = streamState.text.length();
                emit(progressListener, progress);
            }
        } catch (java.lang.RuntimeException e) {
            genResult.error = "接口返回的流式 JSON 无法解析";
            genResult.errorClass = "响应格式错误";
            genResult.retryable = false;
        }
    }

    private void copyStream(app.pelican.check.ApiClient.StreamState streamState, app.pelican.check.ApiClient.GenResult genResult) {
        genResult.text = streamState.text.toString();
        genResult.modelEcho = streamState.model;
        genResult.responseId = streamState.id;
        applyUsage(streamState.usage, genResult);
    }

    private void consumeData(com.google.gson.JsonElement jsonElement, app.pelican.check.ApiClient.StreamState streamState, app.pelican.check.ApiClient.GenResult genResult, long j) {
        if (jsonElement == null || !jsonElement.isJsonObject()) {
            return;
        }
        com.google.gson.JsonObject asJsonObject = jsonElement.getAsJsonObject();
        if (asJsonObject.has("id") && asJsonObject.get("id").isJsonPrimitive()) {
            streamState.id = asJsonObject.get("id").getAsString();
        }
        if (asJsonObject.has("model") && asJsonObject.get("model").isJsonPrimitive()) {
            streamState.model = asJsonObject.get("model").getAsString();
        }
        if (asJsonObject.has("usage") && asJsonObject.get("usage").isJsonObject()) {
            mergeUsage(streamState, asJsonObject.getAsJsonObject("usage"));
        }
        java.lang.String str = str(asJsonObject, "type");
        if ((asJsonObject.has("error") && !asJsonObject.get("error").isJsonNull()) || "error".equals(str)) {
            com.google.gson.JsonElement jsonElement2 = asJsonObject.get("error");
            java.lang.String string = jsonElement2 == null ? asJsonObject.toString() : jsonElement2.toString();
            java.lang.String lowerCase = string.toLowerCase(java.util.Locale.ROOT);
            genResult.error = "接口返回错误：" + (string.length() > 300 ? string.substring(0, 300) : string);
            genResult.errorClass = app.pelican.check.RetryPolicy.permanentMessage(string) ? "认证、额度或模型问题" : "接口返回流内错误";
            genResult.partial = streamState.text.length() > 0;
            genResult.retryable = !app.pelican.check.RetryPolicy.permanentMessage(string) && !RetryPolicy.parameterError(string) && lowerCase.matches("(?s).*(overload|rate.?limit|timeout|server.?error|upstream|temporar|限流|超时|繁忙).*");
            return;
        }
        if ("message_start".equals(str) && asJsonObject.has("message")) {
            com.google.gson.JsonObject asJsonObject2 = asJsonObject.getAsJsonObject("message");
            if (asJsonObject2.has("id")) {
                streamState.id = str(asJsonObject2, "id");
            }
            if (asJsonObject2.has("model")) {
                streamState.model = str(asJsonObject2, "model");
            }
            if (asJsonObject2.has("usage")) {
                mergeUsage(streamState, asJsonObject2.getAsJsonObject("usage"));
            }
        }
        if ("message_delta".equals(str)) {
            com.google.gson.JsonObject jsonObjectObj = obj(asJsonObject, "delta");
            if (jsonObjectObj != null && !str(jsonObjectObj, "stop_reason").isEmpty()) {
                streamState.finish = true;
            }
            if (asJsonObject.has("usage") && asJsonObject.get("usage").isJsonObject()) {
                mergeUsage(streamState, asJsonObject.getAsJsonObject("usage"));
            }
        }
        if ("message_stop".equals(str)) {
            streamState.done = true;
        }
        if(str.startsWith("response.")){
            com.google.gson.JsonObject completed=obj(asJsonObject,"response");
            if(completed!=null){
                if(!str(completed,"id").isEmpty())streamState.id=str(completed,"id");
                if(!str(completed,"model").isEmpty())streamState.model=str(completed,"model");
                if(streamState.text.length()==0&&completed.has("output")&&completed.get("output").isJsonArray()){
                    for(com.google.gson.JsonElement item:completed.getAsJsonArray("output"))if(item.isJsonObject()&&"message".equals(str(item.getAsJsonObject(),"type")))appendContent(streamState.text,item.getAsJsonObject().get("content"));
                }
                if(obj(completed,"usage")!=null)mergeUsage(streamState,obj(completed,"usage"));
                if("response.failed".equals(str)||"response.incomplete".equals(str)){
                    genResult.error="Responses 未完整完成："+completed.toString();genResult.errorClass="接口返回异常";genResult.retryable=false;
                }
            }
            if("response.completed".equals(str)){streamState.done=true;streamState.finish=true;}
        }
        int length = streamState.text.length();
        if("response.output_text.delta".equals(str))streamState.text.append(str(asJsonObject,"delta"));
        if ("content_block_start".equals(str) && obj(asJsonObject, "content_block") != null) {
            com.google.gson.JsonObject jsonObjectObj2 = obj(asJsonObject, "content_block");
            if ("text".equals(str(jsonObjectObj2, "type"))) {
                streamState.text.append(str(jsonObjectObj2, "text"));
            }
        }
        if ("content_block_delta".equals(str) && asJsonObject.has("delta")) {
            com.google.gson.JsonObject asJsonObject3 = asJsonObject.getAsJsonObject("delta");
            if (asJsonObject3.has("text")) {
                streamState.text.append(str(asJsonObject3, "text"));
            }
        }
        if (asJsonObject.has("choices") && asJsonObject.get("choices").isJsonArray()) {
            com.google.gson.JsonArray asJsonArray = asJsonObject.getAsJsonArray("choices");
            if (asJsonArray.size() > 0 && asJsonArray.get(0).isJsonObject()) {
                com.google.gson.JsonObject asJsonObject4 = asJsonArray.get(0).getAsJsonObject();
                if (asJsonObject4.has("finish_reason") && !asJsonObject4.get("finish_reason").isJsonNull()) {
                    streamState.finish = true;
                }
                if (asJsonObject4.has("delta") && asJsonObject4.get("delta").isJsonObject()) {
                    appendContent(streamState.text, asJsonObject4.getAsJsonObject("delta").get("content"));
                }
            }
        }
        if (streamState.text.length() <= length || genResult.ttftMs != 0) {
            return;
        }
        genResult.ttftMs = java.lang.Math.max(1L, java.lang.System.currentTimeMillis() - j);
    }

    private void mergeUsage(app.pelican.check.ApiClient.StreamState streamState, com.google.gson.JsonObject jsonObject) {
        if (jsonObject == null) {
            return;
        }
        if (streamState.usage == null) {
            streamState.usage = new com.google.gson.JsonObject();
        }
        for (java.lang.String str : jsonObject.keySet()) {
            streamState.usage.add(str, jsonObject.get(str));
        }
    }

    private void appendContent(java.lang.StringBuilder sb, com.google.gson.JsonElement jsonElement) {
        if (jsonElement == null || jsonElement.isJsonNull()) {
            return;
        }
        if (jsonElement.isJsonPrimitive()) {
            sb.append(jsonElement.getAsString());
            return;
        }
        if (jsonElement.isJsonArray()) {
            for (com.google.gson.JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                if (jsonElement2.isJsonObject()) {
                    com.google.gson.JsonObject asJsonObject = jsonElement2.getAsJsonObject();
                    if (asJsonObject.has("text")) {
                        sb.append(str(asJsonObject, "text"));
                    }
                }
            }
        }
    }

    private void parseNonStream(java.lang.String str, app.pelican.check.ApiClient.GenResult genResult) {
        try {
            com.google.gson.JsonObject asJsonObject = com.google.gson.JsonParser.parseString(str).getAsJsonObject();
            if (asJsonObject.has("error") && !asJsonObject.get("error").isJsonNull()) {
                java.lang.String string = asJsonObject.get("error").toString();
                genResult.error = "接口返回错误：" + string;
                genResult.errorClass = "接口返回错误";
                genResult.retryable = !app.pelican.check.RetryPolicy.permanentMessage(string) && !RetryPolicy.parameterError(string) && string.toLowerCase(java.util.Locale.ROOT).matches("(?s).*(overload|rate.?limit|timeout|server.?error|upstream|temporar).*");
                return;
            }
            if("incomplete".equals(str(asJsonObject,"status"))||"failed".equals(str(asJsonObject,"status"))){
                genResult.error="Responses 未完整完成："+str(asJsonObject,"status");genResult.errorClass="接口响应不完整";genResult.retryable=false;return;
            }
            genResult.modelEcho = str(asJsonObject, "model");
            genResult.responseId = str(asJsonObject, "id");
            if (asJsonObject.has("usage")) {
                applyUsage(asJsonObject.getAsJsonObject("usage"), genResult);
            }
            java.lang.StringBuilder sb = new java.lang.StringBuilder();
            if (asJsonObject.has("choices")) {
                com.google.gson.JsonObject asJsonObject2 = asJsonObject.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (asJsonObject2.has("message")) {
                    appendContent(sb, asJsonObject2.getAsJsonObject("message").get("content"));
                }
            } else if (asJsonObject.has("output")&&asJsonObject.get("output").isJsonArray()){
                for(com.google.gson.JsonElement output:asJsonObject.getAsJsonArray("output"))if(output.isJsonObject())appendContent(sb,output.getAsJsonObject().get("content"));
            } else if (asJsonObject.has("content")) {
                appendContent(sb, asJsonObject.get("content"));
            }
            genResult.text = sb.toString();
        } catch (java.lang.Throwable th) {
            genResult.error = "接口返回的不是可识别 JSON";
            genResult.errorClass = "中转站响应异常";
            genResult.retryable = false;
        }
    }

    private static java.lang.String str(com.google.gson.JsonObject jsonObject, java.lang.String str) {
        try {
            return (!jsonObject.has(str) || jsonObject.get(str).isJsonNull()) ? "" : jsonObject.get(str).getAsString();
        } catch (java.lang.Throwable th) {
            return "";
        }
    }

    private void applyUsage(com.google.gson.JsonObject jsonObject, app.pelican.check.ApiClient.GenResult genResult) {
        if (jsonObject == null) {
            return;
        }
        // Anthropic cache usage is additional input; OpenAI cached details are already included.
        // message_delta may contain only output_tokens: do not zero previously reported input.
        if (jsonObject.has("input_tokens") || jsonObject.has("prompt_tokens")) {
            genResult.rawInputTokens = Math.max(0, intValue(jsonObject, "input_tokens", intValue(jsonObject, "prompt_tokens", 0)));
            genResult.separateCacheInput = !jsonObject.has("prompt_tokens");
        }
        if (jsonObject.has("cache_read_input_tokens")) genResult.cacheReadInputTokens = Math.max(0, intValue(jsonObject, "cache_read_input_tokens", 0));
        if (jsonObject.has("cache_creation_input_tokens")) genResult.cacheCreationInputTokens = Math.max(0, intValue(jsonObject, "cache_creation_input_tokens", 0));
        long totalInput = (long) genResult.rawInputTokens + (genResult.separateCacheInput ? (long) genResult.cacheReadInputTokens + genResult.cacheCreationInputTokens : 0);
        genResult.inTokens = (int) Math.min(Integer.MAX_VALUE, totalInput);
        if (jsonObject.has("output_tokens") || jsonObject.has("completion_tokens"))
            genResult.outTokens = Math.max(0, intValue(jsonObject, "output_tokens", intValue(jsonObject, "completion_tokens", 0)));
        com.google.gson.JsonObject jsonObjectObj = obj(jsonObject, "completion_tokens_details");
        com.google.gson.JsonObject jsonObjectObj2 = obj(jsonObject, "output_tokens_details");
        if (jsonObjectObj != null && jsonObjectObj.has("reasoning_tokens")) {
            genResult.reasonTokens = java.lang.Integer.valueOf(intValue(jsonObjectObj, "reasoning_tokens", 0));
        } else if (jsonObjectObj2 != null && jsonObjectObj2.has("reasoning_tokens")) {
            genResult.reasonTokens = java.lang.Integer.valueOf(intValue(jsonObjectObj2, "reasoning_tokens", 0));
        } else if (jsonObjectObj2 != null && jsonObjectObj2.has("thinking_tokens")) {
            genResult.reasonTokens = java.lang.Integer.valueOf(intValue(jsonObjectObj2, "thinking_tokens", 0));
        }
        genResult.usageExtra = new java.util.ArrayList();
        for (java.lang.String str : jsonObject.keySet()) {
            if (!STANDARD_USAGE.contains(str)) {
                genResult.usageExtra.add(str);
            }
        }
        java.util.Collections.sort(genResult.usageExtra);
    }

    private static com.google.gson.JsonObject obj(com.google.gson.JsonObject jsonObject, java.lang.String str) {
        if (jsonObject.has(str) && jsonObject.get(str).isJsonObject()) {
            return jsonObject.getAsJsonObject(str);
        }
        return null;
    }

    private static int intValue(com.google.gson.JsonObject jsonObject, java.lang.String str, int i) {
        try {
            return (!jsonObject.has(str) || jsonObject.get(str).isJsonNull()) ? i : jsonObject.get(str).getAsInt();
        } catch (java.lang.Throwable th) {
            return i;
        }
    }

    private boolean hasValidatedInternet() {
        try {
            android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) this.f4app.getSystemService("connectivity");
            android.net.NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return networkCapabilities != null && networkCapabilities.hasCapability(12);
        } catch (java.lang.Throwable th) {
            return true;
        }
    }

    private java.lang.String exceptionClass(java.lang.Throwable th, boolean z) {
        if (!hasValidatedInternet()) {
            return "本机网络中断";
        }
        if (app.pelican.check.RetryPolicy.certificateFailure(th)) {
            return "证书校验失败";
        }
        if (z) {
            return "响应中途断开（原因未确定）";
        }
        if ((th instanceof java.io.EOFException) || (th instanceof java.net.SocketException)) {
            return "连接中断（原因未确定）";
        }
        if (th instanceof java.net.UnknownHostException) {
            return "域名解析失败";
        }
        if (th instanceof java.net.ConnectException) {
            return "入口连接失败（原因未确定）";
        }
        if (th instanceof javax.net.ssl.SSLException) {
            return "TLS 连接中断（原因未确定）";
        }
        return th instanceof java.net.SocketTimeoutException ? "中转站或上游超时" : "原因未确定";
    }

    private java.lang.String friendlyException(java.lang.Throwable th) {
        if (!hasValidatedInternet()) {
            return "本机当前没有可用互联网连接";
        }
        java.lang.String simpleName = th == null ? "未知错误" : th.getClass().getSimpleName();
        java.lang.String message = (th == null || th.getMessage() == null) ? "" : th.getMessage();
        if (th instanceof java.net.UnknownHostException) {
            return "域名解析失败：" + message;
        }
        if (th instanceof java.net.ConnectException) {
            return "无法连接中转站：" + message;
        }
        if (th instanceof java.net.SocketTimeoutException) {
            return "连接或读取超时：" + message;
        }
        if (app.pelican.check.RetryPolicy.certificateFailure(th)) {
            return "证书校验失败（不会忽略证书）：" + message;
        }
        if (th instanceof javax.net.ssl.SSLException) {
            return "TLS 连接意外中断：" + message;
        }
        return simpleName + (message.isEmpty() ? "" : "：" + message);
    }

    private java.lang.String statusClass(int i) {
        if (i == 502 || i == 503 || i == 504) {
            return "中转站报告上游异常";
        }
        if (i == 429) {
            return "中转站限流";
        }
        if (i >= 500) {
            return "中转站或上游异常";
        }
        if (i == 402) {
            return "额度或计费问题";
        }
        if (i == 408) {
            return "请求超时";
        }
        if (i >= 300 && i < 400) {
            return "接口发生跳转，请填写最终 API 地址";
        }
        if (i == 401 || i == 403) {
            return "密钥或权限错误";
        }
        return i == 404 ? "接口路径或模型不存在" : "请求参数错误";
    }

    private long retryAfter(java.lang.String str) {
        return app.pelican.check.RetryPolicy.parseRetryAfter(str, java.lang.System.currentTimeMillis());
    }

    private static java.lang.String redact(java.lang.String str, java.lang.String str2) {
        return (str2 == null || str2.isEmpty() || str == null) ? str : str.replace(str2, "[密钥已隐藏]");
    }

    private java.lang.String httpError(int i, java.lang.String str) {
        java.lang.String strTrim = str == null ? "" : str.replaceAll("\\s+", " ").trim();
        if (strTrim.length() > 260) {
            strTrim = strTrim.substring(0, 260) + "…";
        }
        return "HTTP " + i + " · " + statusClass(i) + (strTrim.isEmpty() ? "" : "\n" + strTrim);
    }

    private static java.lang.String join(java.util.List<java.lang.String> list, java.lang.String str) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder();
        for (java.lang.String str2 : list) {
            if (sb.length() > 0) {
                sb.append(str);
            }
            sb.append(str2);
        }
        return sb.toString();
    }

    public InputAudit.Evidence preview(LineConfig config,String prompt){return InputAudit.freeze(config,prompt,payload(config,prompt)).evidence;}
}
