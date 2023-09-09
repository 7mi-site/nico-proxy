package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BilibiliCom implements ShareService{

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        String s = data.getURL().split("\\?")[0];
        String[] strings = s.split("/");
        String id = strings[strings.length - 1];
        if (id.isEmpty() || id.startsWith("?")){
            id = strings[strings.length - 2];
        }

        //System.out.println("debug id : "+id);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        final String HtmlText;

        Request request_html = new Request.Builder()
                .url("https://www.bilibili.com/video/"+id+"/")
                .build();

        try {
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            } else {
                HtmlText = "";
            }
            response.close();
        } catch (IOException e) {
            throw new Exception("bilibili.com " + e.getMessage() + (data.getProxy() == null ? "" : "(Use Proxy : "+data.getProxy().getProxyIP()+")"));
        }

        //System.out.println(HtmlText);
        Matcher matcher3 = Pattern.compile("window.__playinfo__=\\{(.*)\\}</script><script>window.__INITIAL_STATE__=\\{").matcher(HtmlText);
        if (!matcher3.find()){
            throw new Exception("bilibili.com Not Found");
        }

        Matcher matcher = Pattern.compile("\"aid\":(\\d+)").matcher(HtmlText);
        if (!matcher.find()){
            throw new Exception("bilibili.com Not aid");
        }

        final String aid = matcher.group(1);

        final String responseText;
        Request request_api1 = new Request.Builder()
                .url("https://api.bilibili.com/x/player/pagelist?bvid="+id+"&jsonp=jsonp")
                .build();

        try {
            Response response = client.newCall(request_api1).execute();
            if (response.body() != null){
                responseText = response.body().string();
            } else {
                responseText = "";
            }
            response.close();
        } catch (IOException e) {
            throw new Exception("api.bilibili.com/x/player/pagelist " + e.getMessage() + (data.getProxy() == null ? "" : "(Use Proxy : "+data.getProxy().getProxyIP()+")"));
        }

        //System.out.println(responseText);

        Matcher matcher2 = Pattern.compile("\"cid\":(\\d+)").matcher(responseText);

        if (!matcher2.find()){
            throw new Exception("bilibili.com Not cid");
        }

        Request request_api2 = new Request.Builder()
                .url("https://api.bilibili.com/x/player/v2?aid="+aid+"&cid="+matcher2.group(1))
                .build();

        final String response2Text;
        try {
            Response response = client.newCall(request_api2).execute();
            if (response.body() != null){
                response2Text = response.body().string();
            } else {
                response2Text = "";
            }
            response.close();
        } catch (IOException e) {
            throw new Exception("api.bilibili.com/x/player/v2 " + e.getMessage() + (data.getProxy() == null ? "" : "(Use Proxy : "+data.getProxy().getProxyIP()+")"));
        }

        //System.out.println(response2Text);

        String json = "{" + matcher3.group(1) + "}";
        //System.out.println(json);

        JsonObject result = new Gson().fromJson(json, JsonElement.class).getAsJsonObject();
        JsonArray array = result.getAsJsonObject("data").getAsJsonObject("dash").getAsJsonArray("video");

        List<String> videoUrl = new ArrayList<>();
        array.forEach((a)->{
            // a.getAsJsonObject().get("baseUrl").getAsString()
            Request request_video = new Request.Builder()
                    .url(a.getAsJsonObject().get("baseUrl").getAsString())
                    .addHeader("Referer","https://www.bilibili.com/")
                    .build();

            boolean is403 = false;
            try {
                Response response = client.newCall(request_video).execute();
                if (response.code() == 200){
                    videoUrl.add(a.getAsJsonObject().get("baseUrl").getAsString());
                } else {
                    is403 = true;
                }
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (is403){
                request_video = new Request.Builder()
                        .url(a.getAsJsonObject().get("backupUrl").getAsString())
                        .addHeader("Referer","https://www.bilibili.com/")
                        .build();

                try {
                    Response response = client.newCall(request_video).execute();
                    //System.out.println(response.code());
                    if (response.code() == 200){
                        videoUrl.add(a.getAsJsonObject().get("backupUrl").getAsString());
                    }
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        JsonArray array2 = result.getAsJsonObject("data").getAsJsonObject("dash").getAsJsonArray("audio");

        List<String> audioUrl = new ArrayList<>();
        array2.forEach((a)->{
            // a.getAsJsonObject().get("baseUrl").getAsString()
            Request request_audio = new Request.Builder()
                    .url(a.getAsJsonObject().get("baseUrl").getAsString())
                    .addHeader("Referer","https://www.bilibili.com/")
                    .build();

            boolean is403 = false;
            try {
                Response response = client.newCall(request_audio).execute();
                if (response.code() == 200){
                    audioUrl.add(a.getAsJsonObject().get("baseUrl").getAsString());
                } else {
                    is403 = true;
                }
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (is403){
                request_audio = new Request.Builder()
                        .url(a.getAsJsonObject().get("backupUrl").getAsString())
                        .addHeader("Referer","https://www.bilibili.com/")
                        .build();

                try {
                    Response response = client.newCall(request_audio).execute();
                    //System.out.println(response.code());
                    if (response.code() == 200){
                        audioUrl.add(a.getAsJsonObject().get("backupUrl").getAsString());
                    }
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        return new ResultVideoData(videoUrl.get(videoUrl.size() - 1), audioUrl.get(audioUrl.size() - 1), false, false, false, null);
    }


    @Override
    public ResultVideoData getLive(RequestVideoData data) {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) {
        String title = "";

        String s = data.getURL().split("\\?")[0];
        String[] strings = s.split("/");
        String id = strings[strings.length - 1];
        if (id.isEmpty() || id.startsWith("?")){
            id = strings[strings.length - 2];
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        final String HtmlText;

        Request request_html = new Request.Builder()
                .url("https://www.bilibili.com/video/"+id+"/")
                .build();

        try {
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            } else {
                HtmlText = "";
            }
            response.close();
        } catch (IOException e) {
            return "";
        }

        Matcher matcher = Pattern.compile("<h1 title=\"(.*)\" class=\"video-title\"").matcher(HtmlText);
        if (matcher.find()){
            title = matcher.group(1);
        }

        return title;
    }

    @Override
    public String getServiceName() {
        return "bilibili.com";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }
}
