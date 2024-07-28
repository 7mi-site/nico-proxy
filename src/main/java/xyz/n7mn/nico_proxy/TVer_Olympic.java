package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVer_Olympic implements ShareService {

    private final Pattern matcher_SupportURL = Pattern.compile("https://tver\\.jp/olympic/");
    private final Pattern matcher_videoId = Pattern.compile("&quot;video_id&quot;:&quot;dx_(\\d+)_(\\d+)_(\\d+)&quot;,&quot;video_type&quot;:(\\d+),&quot;");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {

        //System.out.println(data.getURL());
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
