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

    private Pattern matcher_json = Pattern.compile("window\\.__playinfo__=\\{(.*)\\}</script><script>window\\.__INITIAL_STATE__=\\{");
    private Pattern matcher_cid = Pattern.compile("\"cid\":(\\d+)");
    private Pattern matcher_title = Pattern.compile("<h1 title=\"(.*)\" class=\"video-title\"");

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
        Matcher matcher3 = matcher_json.matcher(HtmlText);
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

        Matcher matcher2 = matcher_cid.matcher(responseText);

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

        final boolean is403;
        boolean is40;

        Request request_video = new Request.Builder()
                .url(array.get(0).getAsJsonObject().get("baseUrl").getAsString())
                .addHeader("Referer","https://www.bilibili.com/")
                .build();

        try {
            Response response = client.newCall(request_video).execute();
            if (response.code() == 200){
                videoUrl.add(array.get(0).getAsJsonObject().get("baseUrl").getAsString());
                is40 = false;
            } else {
                videoUrl.add(array.get(0).getAsJsonObject().get("backupUrl").getAsString());
                is40 = true;
            }
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
            is40 = true;
        }
        is403 = is40;

        int[] i = {0};
        array.forEach((a)->{
            if (i[0] == 0){
                i[0]++;
            } else {
                if (!is403){
                    videoUrl.add(a.getAsJsonObject().get("baseUrl").getAsString());
                } else {
                    videoUrl.add(a.getAsJsonObject().get("backupUrl").getAsString());
                }

                i[0]++;
            }
        });

        JsonArray array2 = result.getAsJsonObject("data").getAsJsonObject("dash").getAsJsonArray("audio");

        List<String> audioUrl = new ArrayList<>();
        array2.forEach((a)->{
            // a.getAsJsonObject().get("baseUrl").getAsString()
            if (!is403){
                audioUrl.add(a.getAsJsonObject().get("baseUrl").getAsString());
            } else {
                audioUrl.add(a.getAsJsonObject().get("backupUrl").getAsString());
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

        Matcher matcher = matcher_title.matcher(HtmlText);
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
