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

public class Abema implements ShareService {


    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        Matcher matcher = Pattern.compile("https://abema.tv/video/episode/(.*)").matcher(data.getURL());

        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        Request request_html = new Request.Builder()
                .url(data.getURL())
                .build();
        try {
            Response response = client.newCall(request_html).execute();
            response.close();
        } catch (Exception e){
            throw e;
        }

        Request request_api = new Request.Builder()
                .url("https://api.p-c3-e.abema-tv.com/v1/video/programs/"+matcher.group(1)+"?division=0&include=tvod")
                .addHeader("Authorization","bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiI3YWQ5NjQ1Ni0zZjFmLTRiYTctOTQ1OC1jOTA0MzQyYTNiNDMiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOTRjeXh3UGR5OVdHcHcifQ.Muv9eT4Tmy4JsSOGTVexwxuGnf2ZkwL1RkBo6MrSZGg")
                .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0")
                .addHeader("Referer", "https://abema.tv/")
                .build();

       String api_result = "";
        try {
            Response response = client.newCall(request_api).execute();
            api_result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        //System.out.println(api_result);

        JsonElement json = new Gson().fromJson(api_result, JsonElement.class);
        String string = json.getAsJsonObject().getAsJsonObject("playback").get("hlsPreview").getAsString();

        return new ResultVideoData(string, "", true, false, false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        Matcher matcher = Pattern.compile("https://abema.tv/now-on-air/(.+)").matcher(data.getURL());

        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        Request request_api = new Request.Builder()
                .url("https://api.abema.io/v1/channels")
                .build();

        String api_result = "";
        try {
            Response response = client.newCall(request_api).execute();
            api_result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        JsonElement json = new Gson().fromJson(api_result, JsonElement.class);

        for (int i = 0; i < json.getAsJsonObject().getAsJsonArray("channels").size(); i++){
            JsonElement element = json.getAsJsonObject().getAsJsonArray("channels").get(i);

            if (matcher.group(1).split("\\?")[0].startsWith(element.getAsJsonObject().get("id").getAsString())){
                return new ResultVideoData(element.getAsJsonObject().getAsJsonObject("playback").get("hlsPreview").getAsString(), null, true, false, true, "");
            }
        }

        throw new Exception("Channel Not Found");
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        Matcher matcher = Pattern.compile("https://abema.tv/video/episode/(.*)").matcher(data.getURL());
        Matcher matcher2 = Pattern.compile("https://abema.tv/now-on-air/(.+)").matcher(data.getURL());

        boolean video = matcher.find();
        boolean live = matcher2.find();

        if (!video && !live){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        if (video){
            Request request_html = new Request.Builder()
                    .url(data.getURL())
                    .build();
            try {
                Response response = client.newCall(request_html).execute();
                response.close();
            } catch (Exception e){
                throw e;
            }

            Request request_api = new Request.Builder()
                    .url("https://api.p-c3-e.abema-tv.com/v1/video/programs/"+matcher.group(1)+"?division=0&include=tvod")
                    .addHeader("Authorization","bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiI3YWQ5NjQ1Ni0zZjFmLTRiYTctOTQ1OC1jOTA0MzQyYTNiNDMiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOTRjeXh3UGR5OVdHcHcifQ.Muv9eT4Tmy4JsSOGTVexwxuGnf2ZkwL1RkBo6MrSZGg")
                    .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0")
                    .addHeader("Referer", "https://abema.tv/")
                    .build();

            String api_result = "";
            try {
                Response response = client.newCall(request_api).execute();
                api_result = response.body().string();
                response.close();
            } catch (Exception e){
                throw e;
            }

            //System.out.println(api_result);

            JsonElement json = new Gson().fromJson(api_result, JsonElement.class);

            if (!json.getAsJsonObject().getAsJsonObject("series").get("title").isJsonNull()){
                return json.getAsJsonObject().getAsJsonObject("series").get("title").getAsString();
            }
        }

        if (live){
            Request request_api = new Request.Builder()
                    .url("https://api.abema.io/v1/channels")
                    .build();

            String api_result = "";
            try {
                Response response = client.newCall(request_api).execute();
                api_result = response.body().string();
                response.close();
            } catch (Exception e){
                throw e;
            }

            JsonElement json = new Gson().fromJson(api_result, JsonElement.class);

            for (int i = 0; i < json.getAsJsonObject().getAsJsonArray("channels").size(); i++){
                JsonElement element = json.getAsJsonObject().getAsJsonArray("channels").get(i);

                if (matcher2.group(1).split("\\?")[0].startsWith(element.getAsJsonObject().get("id").getAsString())){
                    return element.getAsJsonObject().get("name").getAsString();
                }
            }

        }

        return "";
    }

    @Override
    public String getServiceName() {
        return "Abema";
    }

    @Override
    public String getVersion() {
        return "20231116";
    }
}
