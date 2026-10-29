# 历史开发补丁，仅保留改动过程；不是构建步骤，不要对当前源码重复执行。
from pathlib import Path
p=Path(__file__).resolve().parents[1]/'app/src/main/java/app/pelican/check/ApiClient.java';s=p.read_text()
s=s.replace('        int rawInputTokens;','        InputAudit.Evidence inputAudit;\n        int rawInputTokens;',1)
s=s.replace('            attemptData3.durationMs = System.currentTimeMillis() - attemptData3.startedAt;','            attemptData3.inputAudit = genResultPerformOnce.inputAudit;\n            attemptData3.durationMs = System.currentTimeMillis() - attemptData3.startedAt;',1)
s=s.replace('            sampleData.finalAttemptMs = genResultPerformOnce.totalMs;','            sampleData.inputAudit = genResultPerformOnce.inputAudit;\n            sampleData.finalAttemptMs = genResultPerformOnce.totalMs;',1)
old='''            okhttp3.Request.Builder builderPost = new okhttp3.Request.Builder().url(endpoints(lineConfig.baseUrl, lineConfig.protocol, "anthropic".equals(lineConfig.protocol) ? "/messages" : "/chat/completions").get(0)).post(okhttp3.RequestBody.create(app.pelican.check.Json.gson().toJson((com.google.gson.JsonElement) payload(lineConfig, str)), JSON));'''
assert old in s;s=s.replace(old,'''            final InputAudit.Frozen frozen=InputAudit.freeze(lineConfig,str,payload(lineConfig,str));
            genResult.inputAudit=frozen.evidence;
            prepareTokenCount(lineConfig,frozen);
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
            okhttp3.Request.Builder builderPost = new okhttp3.Request.Builder().url(endpoints(lineConfig.baseUrl,lineConfig.protocol,route).get(0)).post(requestBody);''')
import re
s,n=re.subn('(public void requestBodyEnd\\(okhttp3.Call call, long j\\) \\{\\s*)stage\\("等待首字"\\);','\\1genResult.inputAudit.wireBytesVerified=j==genResult.inputAudit.bodyBytes;\\n                    stage("等待首字");',s)
assert n==1,("body listener",n)
old='''        if ("anthropic".equals(lineConfig.protocol)) {
            jsonObject.addProperty("max_tokens", java.lang.Integer.valueOf(iMax));''';assert old in s;s=s.replace(old,'''        if ("responses".equals(lineConfig.protocol)) {
            jsonObject.remove("messages");jsonObject.add("input",jsonArray);
            jsonObject.addProperty("max_output_tokens",iMax);
            if(!"auto".equals(lineConfig.effort)){
                com.google.gson.JsonObject reasoning=new com.google.gson.JsonObject();reasoning.addProperty("effort",lineConfig.effort);jsonObject.add("reasoning",reasoning);
            }
            return jsonObject;
        }
        if ("anthropic".equals(lineConfig.protocol)) {
            jsonObject.addProperty("max_tokens", java.lang.Integer.valueOf(iMax));''')
old='''        int length = streamState.text.length();'''
a=s.index('    private void consumeData(');index=s.index(old,a)
s=s[:index]+'''        if(str.startsWith("response.")){
            com.google.gson.JsonObject completed=obj(asJsonObject,"response");
            if(completed!=null){
                streamState.id=str(completed,"id");streamState.model=str(completed,"model");
                if(obj(completed,"usage")!=null)mergeUsage(streamState,obj(completed,"usage"));
                if("response.failed".equals(str)||"response.incomplete".equals(str)){
                    genResult.error="Responses 未完整完成："+completed.toString();genResult.errorClass="接口返回异常";genResult.retryable=false;
                }
            }
            if("response.completed".equals(str)){streamState.done=true;streamState.finish=true;}
        }
        int length = streamState.text.length();
        if("response.output_text.delta".equals(str))streamState.text.append(str(asJsonObject,"delta"));''' +s[index+len(old):]
old='''            } else if (asJsonObject.has("content")) {
                appendContent(sb, asJsonObject.get("content"));
            }''';assert old in s;s=s.replace(old,'''            } else if (asJsonObject.has("output")&&asJsonObject.get("output").isJsonArray()){
                for(com.google.gson.JsonElement output:asJsonObject.getAsJsonArray("output"))if(output.isJsonObject())appendContent(sb,output.getAsJsonObject().get("content"));
            } else if (asJsonObject.has("content")) {
                appendContent(sb, asJsonObject.get("content"));
            }''')
idx=s.rfind('}');s=s[:idx]+'''
    private void prepareTokenCount(LineConfig config,InputAudit.Frozen frozen){
        InputAudit.Evidence e=frozen.evidence;
        if(!config.tokenAuditEnabled||!"responses".equals(config.protocol))return;
        com.google.gson.JsonObject countBody=com.google.gson.JsonParser.parseString(e.bodyJson).getAsJsonObject();
        countBody.remove("stream");countBody.remove("max_output_tokens");
        String url=endpoints(config.baseUrl,config.protocol,"/responses/input_tokens").get(0);
        okhttp3.Request.Builder request=new okhttp3.Request.Builder().url(url).post(okhttp3.RequestBody.create(Json.gson().toJson(countBody),JSON));
        authHeaders(request,config);
        try(okhttp3.Response response=http.newBuilder().followRedirects(false).followSslRedirects(false)
                .callTimeout(12,java.util.concurrent.TimeUnit.SECONDS).build().newCall(request.build()).execute()){
            if(!response.isSuccessful()){e.countNote="计数接口 HTTP "+response.code()+"，仅保留本地估算，不触发疑似注入";return;}
            com.google.gson.JsonObject body=com.google.gson.JsonParser.parseString(response.body().string()).getAsJsonObject();
            if(!"response.input_tokens".equals(str(body,"object"))||!body.has("input_tokens")){
                e.countNote="计数响应缺少标准格式，未当作准确参考";return;
            }
            long n=body.get("input_tokens").getAsLong();
            if(n<1||n>Integer.MAX_VALUE){e.countNote="计数接口返回无效数量，已跳过告警";return;}
            e.expectedInputTokens=(int)n;e.countReliable=true;e.countSource="同站 Responses 输入计数接口";
            e.countNote="计数与生成复用同一份已校验输入；同站结果仍不是独立于中转站的证明";
        }catch(Exception ex){e.countNote="计数接口不可用，已回退本地估算，不触发疑似注入";}
    }
''' +s[idx:]
p.write_text(s);print('Network audit / Responses parser integration complete.')
