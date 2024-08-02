package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.bilibili.Video;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVer_Olympic implements ShareService {

    private final Pattern matcher_SupportURL = Pattern.compile("https://tver\\.jp/olympic/");
    private final Pattern matcher_Video = Pattern.compile("video");
    private final Pattern matcher_VideoURL = Pattern.compile("https://tver\\.jp/olympic/(.+)/video/(\\d+)/");
    private final Pattern matcher_configId = Pattern.compile("autoplay:\"\",deliveryConfigId:\"(.+)\",css:\\{controlBarColor");
    private final Pattern matcher_pk = Pattern.compile("policyKey:\"(.+)\"}},\\{name:\"playlist\",autoInit:false");
    private final Pattern matcher_videoId = Pattern.compile("&quot;video_id&quot;:&quot;dx_(\\d+)_(\\d+)_(\\d+)&quot;,&quot;video_type&quot;:(\\d+),&quot;");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        if (!matcher_SupportURL.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        Matcher matcher = matcher_VideoURL.matcher(data.getURL());

        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        String AccountId;
        String PlayerId;
        String VideoId = matcher.group(2);
        String ConfigId;
        String pk;

        String jsonText = "";


        Request request1 = new Request.Builder()
                .url("https://tver.jp/olympic/paris2024/req/api/hook?q=https%3A%2F%2Folympic-assets.tver.jp%2Fweb-static%2Fjson%2Fconfig.json&d=")
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        Response response1 = client.newCall(request1).execute();
        if (response1.body() != null){
            jsonText = response1.body().string();
        }
        response1.close();

        //System.out.println(jsonText);
        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);

        AccountId = json.getAsJsonObject().get("content").getAsJsonObject().get("brightcove").getAsJsonObject().get("E200").getAsJsonObject().get("pro").getAsJsonObject().get("pc").getAsJsonObject().get("account_id").getAsString();
        PlayerId = json.getAsJsonObject().get("content").getAsJsonObject().get("brightcove").getAsJsonObject().get("E200").getAsJsonObject().get("pro").getAsJsonObject().get("pc").getAsJsonObject().get("player_id").getAsString();


        //System.out.println("https://players.brightcove.net/"+AccountId+"/"+PlayerId+"_default/index.min.js");
        Request request2 = new Request.Builder()
                .url("https://players.brightcove.net/"+AccountId+"/"+PlayerId+"_default/index.min.js")
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        Response response2 = client.newCall(request2).execute();
        if (response2.body() != null){
            jsonText = response2.body().string();
        }
        response2.close();

        Matcher matcher1 = matcher_configId.matcher(jsonText);
        if (matcher1.find()){
            ConfigId = matcher1.group(1);
        } else {
            ConfigId = "";
        }

        Matcher matcher2 = matcher_pk.matcher(jsonText);
        if (matcher2.find()){
            pk = matcher2.group(1);
        } else {
            pk = "";
        }

        // https://edge.api.brightcove.com/playback/v1/accounts/4774017240001/videos/6359770559112?config_id=a14c3f85-bd8f-41bd-89e6-1d2bad30112b
        //System.out.println("https://edge.api.brightcove.com/playback/v1/accounts/"+AccountId+"/videos/"+ VideoId +"?config_id="+ConfigId);
        Request request3 = new Request.Builder()
                .url("https://edge.api.brightcove.com/playback/v1/accounts/"+AccountId+"/videos/"+ VideoId +"?config_id="+ConfigId)
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .addHeader("Accept", "application/json;pk="+pk) // Accept application/json;pk=BCpkADawqM0VGV-a4pSbjbhxf-g-gO2-ODImYMujqzBqIAKq5E1c2540NLAp6JVU8FAtIa_jvVQr9t-ubsFT4HPZ6iu1-RGrClZ9lWHpb9D9-hvRN4sg0lp-nkMB-FUoK7Oi6ZCZzqIfW1vA
                .build();

        Response response3 = client.newCall(request3).execute();
        if (response3.body() != null){
            jsonText = response3.body().string();
        }
        response3.close();

        //System.out.println(jsonText);
        json = new Gson().fromJson(jsonText, JsonElement.class);

        return new ResultVideoData(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString(), null, true, false,false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {

        //System.out.println(data.getURL());
        if (!matcher_SupportURL.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        if (matcher_Video.matcher(data.getURL()).find()){
            return getVideo(data);
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String htmlText = "";

        Request request0 = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        Response response0 = client.newCall(request0).execute();
        if (response0.body() != null){
            htmlText = response0.body().string();
        }
        response0.close();
        Matcher matcher = matcher_videoId.matcher(htmlText);
        if (!matcher.find()){
            throw new Exception("ID Not Found");
        }
        // dx_(\d+)_(\d+)_(\d+)
        String videoId = "dx_"+matcher.group(1)+"_"+matcher.group(2)+"_"+matcher.group(3);

        Request request1 = new Request.Builder()
                .url("https://platform-api.tver.jp/v2/api/public/current_time")
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        Response response1 = client.newCall(request1).execute();
        response1.close();

        String jsonText = "";
        String X_Streaks_Api_Key = "";

        Request request2 = new Request.Builder()
                .url("https://olympic-assets.tver.jp/web-static/json/config.json")
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        Response response2 = client.newCall(request2).execute();
        if (response2.body() != null){
            jsonText = response2.body().string();
        }
        response2.close();

        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
        X_Streaks_Api_Key = json.getAsJsonObject().get("streaksplayer").getAsJsonObject().get("D400").getAsJsonObject().get("pro").getAsJsonObject().get("api_key").getAsString();

        //System.out.println("https://playback.api.streaks.jp/v1/projects/tver-olympic/medias/ref:"+videoId);
        //System.out.println(X_Streaks_Api_Key);
        Request request3 = new Request.Builder()
                .url("https://playback.api.streaks.jp/v1/projects/tver-olympic/medias/ref:"+videoId)
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .addHeader("X-Streaks-Api-Key", X_Streaks_Api_Key)
                .build();

        Response response3 = client.newCall(request3).execute();
        if (response3.body() != null){
            jsonText = response3.body().string();
        }
        response3.close();

        //System.out.println(jsonText);
        json = new Gson().fromJson(jsonText, JsonElement.class);

        return new ResultVideoData(json.getAsJsonObject().get("sources").getAsJsonArray().get(0).getAsJsonObject().get("src").getAsString(), null, true, false,true, null);
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        if (!matcher_SupportURL.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String htmlText = "";

        Request request0 = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        Response response0 = client.newCall(request0).execute();
        if (response0.body() != null){
            htmlText = response0.body().string();
        }
        response0.close();
        Matcher matcher = matcher_videoId.matcher(htmlText);

        JsonElement json;
        if (matcher.find()){
            // dx_(\d+)_(\d+)_(\d+)
            String videoId = "dx_"+matcher.group(1)+"_"+matcher.group(2)+"_"+matcher.group(3);

            Request request1 = new Request.Builder()
                    .url("https://platform-api.tver.jp/v2/api/public/current_time")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();

            Response response1 = client.newCall(request1).execute();
            response1.close();

            String jsonText = "";
            String X_Streaks_Api_Key = "";

            Request request2 = new Request.Builder()
                    .url("https://olympic-assets.tver.jp/web-static/json/config.json")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();

            Response response2 = client.newCall(request2).execute();
            if (response2.body() != null){
                jsonText = response2.body().string();
            }
            response2.close();

            json = new Gson().fromJson(jsonText, JsonElement.class);
            X_Streaks_Api_Key = json.getAsJsonObject().get("streaksplayer").getAsJsonObject().get("D400").getAsJsonObject().get("pro").getAsJsonObject().get("api_key").getAsString();

            //System.out.println("https://playback.api.streaks.jp/v1/projects/tver-olympic/medias/ref:"+videoId);
            //System.out.println(X_Streaks_Api_Key);
            Request request3 = new Request.Builder()
                    .url("https://playback.api.streaks.jp/v1/projects/tver-olympic/medias/ref:"+videoId)
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .addHeader("X-Streaks-Api-Key", X_Streaks_Api_Key)
                    .build();

            Response response3 = client.newCall(request3).execute();
            if (response3.body() != null){
                jsonText = response3.body().string();
            }
            response3.close();

            //System.out.println(jsonText);
            json = new Gson().fromJson(jsonText, JsonElement.class);

        } else {

            matcher = matcher_VideoURL.matcher(data.getURL());
            if (!matcher.find()){
                return "";
            }

            String AccountId;
            String PlayerId;
            String VideoId = matcher.group(2);
            String ConfigId;
            String pk;

            String jsonText = "";


            Request request1 = new Request.Builder()
                    .url("https://tver.jp/olympic/paris2024/req/api/hook?q=https%3A%2F%2Folympic-assets.tver.jp%2Fweb-static%2Fjson%2Fconfig.json&d=")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();

            Response response1 = client.newCall(request1).execute();
            if (response1.body() != null){
                jsonText = response1.body().string();
            }
            response1.close();

            //System.out.println(jsonText);
            json = new Gson().fromJson(jsonText, JsonElement.class);

            AccountId = json.getAsJsonObject().get("content").getAsJsonObject().get("brightcove").getAsJsonObject().get("E200").getAsJsonObject().get("pro").getAsJsonObject().get("pc").getAsJsonObject().get("account_id").getAsString();
            PlayerId = json.getAsJsonObject().get("content").getAsJsonObject().get("brightcove").getAsJsonObject().get("E200").getAsJsonObject().get("pro").getAsJsonObject().get("pc").getAsJsonObject().get("player_id").getAsString();


            //System.out.println("https://players.brightcove.net/"+AccountId+"/"+PlayerId+"_default/index.min.js");
            Request request2 = new Request.Builder()
                    .url("https://players.brightcove.net/"+AccountId+"/"+PlayerId+"_default/index.min.js")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();

            Response response2 = client.newCall(request2).execute();
            if (response2.body() != null){
                jsonText = response2.body().string();
            }
            response2.close();

            Matcher matcher1 = matcher_configId.matcher(jsonText);
            if (matcher1.find()){
                ConfigId = matcher1.group(1);
            } else {
                ConfigId = "";
            }

            Matcher matcher2 = matcher_pk.matcher(jsonText);
            if (matcher2.find()){
                pk = matcher2.group(1);
            } else {
                pk = "";
            }

            // https://edge.api.brightcove.com/playback/v1/accounts/4774017240001/videos/6359770559112?config_id=a14c3f85-bd8f-41bd-89e6-1d2bad30112b
            //System.out.println("https://edge.api.brightcove.com/playback/v1/accounts/"+AccountId+"/videos/"+ VideoId +"?config_id="+ConfigId);
            Request request3 = new Request.Builder()
                    .url("https://edge.api.brightcove.com/playback/v1/accounts/"+AccountId+"/videos/"+ VideoId +"?config_id="+ConfigId)
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .addHeader("Accept", "application/json;pk="+pk) // Accept application/json;pk=BCpkADawqM0VGV-a4pSbjbhxf-g-gO2-ODImYMujqzBqIAKq5E1c2540NLAp6JVU8FAtIa_jvVQr9t-ubsFT4HPZ6iu1-RGrClZ9lWHpb9D9-hvRN4sg0lp-nkMB-FUoK7Oi6ZCZzqIfW1vA
                    .build();

            Response response3 = client.newCall(request3).execute();
            if (response3.body() != null){
                jsonText = response3.body().string();
            }
            response3.close();

            //System.out.println(jsonText);
            json = new Gson().fromJson(jsonText, JsonElement.class);
        }


        return json.getAsJsonObject().get("name").getAsString();

    }

    @Override
    public String getServiceName() {
        return "TVer (オリンピック)";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
