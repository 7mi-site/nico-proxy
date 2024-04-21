package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiliBiliCom_fromAPI implements ShareService{

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://www.bilibili.com/video/BV1do4y1T7cH/
        Matcher matcher1 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)/").matcher(data.getURL());
        Matcher matcher2 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)").matcher(data.getURL());

        String VideoID = "";
        if (matcher1.find()){
            VideoID = matcher1.group(1);
        } else if (matcher2.find()){
            VideoID = matcher2.group(1);
        } else {
            throw new Exception("Not Support URL");
        }

        // https://api.bilibili.com/x/web-interface/view?bvid=BV1do4y1T7cH
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        Request request_json = new Request.Builder()
                .url("https://api.bilibili.com/x/web-interface/view?bvid="+VideoID)
                .build();

        String JsonText = "";
        try {
            Response response = client.newCall(request_json).execute();
            if (response.body() != null){
                JsonText = response.body().string();
            } else {
                JsonText = "";
            }
            response.close();
        } catch (IOException e) {
            throw new Exception("bilibili.com " + e.getMessage() + (data.getProxy() == null ? "" : "(Use Proxy : "+data.getProxy().getProxyIP()+")"));
        }

        //System.out.println(JsonText);
        JsonElement json = new Gson().fromJson(JsonText, JsonElement.class);
        if (json.getAsJsonObject().get("code").getAsInt() == -400){
            throw new Exception("Video Not Found");
        }

        long cid = json.getAsJsonObject().get("data").getAsJsonObject().get("cid").getAsLong();

        // https://api.bilibili.com/x/player/playurl?bvid=BV1do4y1T7cH&cid=1144533664
        Request request_json2 = new Request.Builder()
                .url("https://api.bilibili.com/x/player/playurl?bvid="+VideoID+"&cid="+cid)
                .build();

        try {
            Response response = client.newCall(request_json2).execute();
            if (response.body() != null){
                JsonText = response.body().string();
            } else {
                JsonText = "";
            }
            response.close();
        } catch (IOException e) {
            throw new Exception("bilibili.com " + e.getMessage() + (data.getProxy() == null ? "" : "(Use Proxy : "+data.getProxy().getProxyIP()+")"));
        }

        // System.out.println(JsonText);

        json = new Gson().fromJson(JsonText, JsonElement.class);
        String MainURL = json.getAsJsonObject().get("data").getAsJsonObject().get("durl").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
        String SubURL = json.getAsJsonObject().get("data").getAsJsonObject().get("durl").getAsJsonArray().get(0).getAsJsonObject().get("backup_url").isJsonNull() ? null : json.getAsJsonObject().get("data").getAsJsonObject().get("durl").getAsJsonArray().get(0).getAsJsonObject().get("backup_url").getAsString();

        return new ResultVideoData(MainURL, "", false, false, false, SubURL);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        Matcher matcher1 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)/").matcher(data.getURL());
        Matcher matcher2 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)").matcher(data.getURL());

        String VideoID = "";
        if (matcher1.find()){
            VideoID = matcher1.group(1);
        } else if (matcher2.find()){
            VideoID = matcher2.group(1).split("\\?")[0];
        } else {
            throw new Exception("Not Support URL");
        }

        //https://api.bilibili.com/x/web-interface/view?bvid=
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        Request request_html = new Request.Builder()
                .url("https://api.bilibili.com/x/web-interface/view?bvid="+VideoID)
                .build();

        String JsonText = "";
        try {
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                JsonText = response.body().string();
            } else {
                JsonText = "";
            }
            response.close();
        } catch (IOException e) {
            throw new Exception("bilibili.com " + e.getMessage() + (data.getProxy() == null ? "" : "(Use Proxy : "+data.getProxy().getProxyIP()+")"));
        }

        //System.out.println(JsonText);
        JsonElement json = new Gson().fromJson(JsonText, JsonElement.class);
        if (json.getAsJsonObject().get("code").getAsInt() == -400){
            throw new Exception("Video Not Found");
        }

        return json.getAsJsonObject().get("data").getAsJsonObject().get("title").getAsString();
    }

    @Override
    public String getServiceName() {
        return "bilibili.com";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
