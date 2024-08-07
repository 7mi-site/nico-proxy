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

    private final Pattern SupportURL_Video1 = Pattern.compile("https://abema\\.tv/video/episode/(.+)");
    private final Pattern SupportURL_Video2 = Pattern.compile("https://abema\\.tv/channels/(.+)/slots/(.+)");
    private final Pattern SupportURL_Live1 = Pattern.compile("https://abema\\.tv/now-on-air/(.+)");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        Matcher matcher = SupportURL_Video1.matcher(data.getURL());
        Matcher matcher1 = SupportURL_Video2.matcher(data.getURL());

        boolean video = matcher.find();
        boolean archive = matcher1.find();

        if (!video && !archive){
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

        if (video){
            Request request_api = new Request.Builder()
                    .url("https://api.p-c3-e.abema-tv.com/v1/video/programs/"+matcher.group(1)+"?division=0&include=tvod")
                    .addHeader("Authorization","bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiI3YWQ5NjQ1Ni0zZjFmLTRiYTctOTQ1OC1jOTA0MzQyYTNiNDMiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOTRjeXh3UGR5OVdHcHcifQ.Muv9eT4Tmy4JsSOGTVexwxuGnf2ZkwL1RkBo6MrSZGg")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
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

            JsonElement json;
            try {
                json = new Gson().fromJson(api_result, JsonElement.class);
            } catch (Exception e){
                throw new Exception("Not Support Video");
            }

            if (!json.getAsJsonObject().has("playback")){
                throw new Exception("Not Support Video");
            }

            if (!json.getAsJsonObject().getAsJsonObject("playback").has("hlsPreview")){
                throw new Exception("Not Support Video");
            }
            String string = json.getAsJsonObject().getAsJsonObject("playback").get("hlsPreview").getAsString();

            return new ResultVideoData(string, "", true, false, false, null);
        }
        if (archive){
            Request request_api = new Request.Builder()
                    .url("https://api.p-c3-e.abema-tv.com/v1/media/slots/"+matcher1.group(2)+"?include=payperview")
                    .addHeader("Authorization","bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiI3YWQ5NjQ1Ni0zZjFmLTRiYTctOTQ1OC1jOTA0MzQyYTNiNDMiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOTRjeXh3UGR5OVdHcHcifQ.Muv9eT4Tmy4JsSOGTVexwxuGnf2ZkwL1RkBo6MrSZGg")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
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

            JsonElement json;
            try {
                json = new Gson().fromJson(api_result, JsonElement.class);
            } catch (Exception e){
                throw new Exception("Not Support Archive");
            }

            if (!json.getAsJsonObject().has("slot")){
                throw new Exception("Not Support Video");
            }

            if (!json.getAsJsonObject().getAsJsonObject("slot").has("playback")){
                throw new Exception("Not Support Video");
            }

            if (!json.getAsJsonObject().getAsJsonObject("slot").getAsJsonObject("playback").has("hlsPreview")){
                throw new Exception("Not Support Archive");
            }
            String string = json.getAsJsonObject().getAsJsonObject("slot").getAsJsonObject("playback").get("hlsPreview").getAsString();

            return new ResultVideoData(string, "", true, false, false, null);
        }

        return null;

    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        Matcher matcher = SupportURL_Live1.matcher(data.getURL());

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

            if (matcher.group(1).split("\\?")[0].equals(element.getAsJsonObject().get("id").getAsString())){
                return new ResultVideoData(element.getAsJsonObject().getAsJsonObject("playback").get("hlsPreview").getAsString(), null, true, false, true, "");
            }
        }

        throw new Exception("Channel Not Found");
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        Matcher matcher = SupportURL_Video1.matcher(data.getURL());
        Matcher matcher1 = SupportURL_Video2.matcher(data.getURL());
        Matcher matcher2 = SupportURL_Live1.matcher(data.getURL());

        boolean video = matcher.find();
        boolean archive = matcher1.find();
        boolean live = matcher2.find();

        if (!video && !archive && !live){
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
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
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

        if (archive){
            Request request_api = new Request.Builder()
                    .url("https://api.p-c3-e.abema-tv.com/v1/media/slots/"+matcher1.group(2)+"?include=payperview")
                    .addHeader("Authorization","bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiI3YWQ5NjQ1Ni0zZjFmLTRiYTctOTQ1OC1jOTA0MzQyYTNiNDMiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOTRjeXh3UGR5OVdHcHcifQ.Muv9eT4Tmy4JsSOGTVexwxuGnf2ZkwL1RkBo6MrSZGg")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
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
            if (json.getAsJsonObject().getAsJsonObject("slot").has("title")){
                return json.getAsJsonObject().getAsJsonObject("slot").get("title").getAsString();
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
                if (matcher2.group(1).split("\\?")[0].equals(element.getAsJsonObject().get("id").getAsString())){
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
        return "20240502";
    }
}
