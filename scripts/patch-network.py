# 历史开发补丁，仅保留改动过程；不是构建步骤，不要对当前源码重复执行。
from pathlib import Path
b=Path(__file__).resolve().parents[1]/'app/src/main/java/app/pelican/check'
p=b/'ApiClient.java';s=p.read_text()
a=s.index('    public app.pelican.check.ApiClient.ModelResult fetchModels(');z=s.index('    private com.google.gson.JsonObject payload(',a)
s=s[:a]+'''    public app.pelican.check.ApiClient.ModelResult fetchModels(app.pelican.check.LineConfig config) {
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
                        result.message=prefix+"HTTP "+result.status+"\\n"+app.pelican.check.RequestInputs.safeError(body.length()>1200?body.substring(0,1200):body,config.apiKey);
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
        request.header("User-Agent","Pelican/2.3 Android");
    }

''' +s[z:]
s=s.replace('        public int status;','        public int status;\n        public String endpoint="";\n        public String apiBase="";')
# Recursively recognize documented container shapes, not arbitrary error/metadata keys.
old='''            com.google.gson.JsonObject asJsonObject = jsonElement.getAsJsonObject();
            java.lang.String[] strArr = {"id", "name", "model"};'''
new='''            com.google.gson.JsonObject asJsonObject = jsonElement.getAsJsonObject();
            for(String container:new String[]{"data","models","items","result"})if(asJsonObject.has(container)){
                collectModels(asJsonObject.get(container),linkedHashSet);return;
            }
            java.lang.String[] strArr = {"id", "name", "model"};'''
assert old in s;s=s.replace(old,new)
s=s.replace('int iMax = java.lang.Math.max(500, lineConfig.maxTokens);','int iMax = lineConfig.maxTokens;\n        if(iMax<1)throw new IllegalArgumentException("输出 Token 上限必须大于 0");')
old='''                if (iMax <= i + okio.Segment.SHARE_MINIMUM) {
                    jsonObject.addProperty("max_tokens", java.lang.Integer.valueOf(i + 4096));
                }'''
assert old in s;s=s.replace(old,'''                if (iMax <= i) throw new IllegalArgumentException("输出上限必须大于所选思考预算；不会自动提高输出上限或降低思考强度");''')
p.write_text(s)
p=b/'LineConfig.java';s=p.read_text();s=s.replace('    public Double inputTokenBaselineOverride = null;','    public int inputTokenExcess = 500;\n    public boolean tokenAuditEnabled = true;\n    public String cachedModelsBase="";\n    @Deprecated public Double inputTokenBaselineOverride = null;');s=s.replace('    public double inputTokenAlertRatio = 2.0;','    @Deprecated public double inputTokenAlertRatio = 2.0;');p.write_text(s)
print('Network handling updated: normalized credentials, correct root /v1 routing, no auth/redirect retries, explicit model errors; output cap no longer silently grows.')
