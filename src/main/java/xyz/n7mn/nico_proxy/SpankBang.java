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

public class SpankBang implements ShareService{

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    private final Pattern matcher_videoUrl = Pattern.compile("https://(.+)\\.spankbang\\.com/(.+)/video/(.+)");

    private final Pattern matcher_json = Pattern.compile("var stream_data = \\{(.+)};");

    private final Pattern matcher_Title = Pattern.compile("<h1 class=\"main_content_title\" title=\"(.+)\">(.+)</h1>");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        if (!matcher_videoUrl.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        String htmlText = "";

        Request request = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();
        Response response = client.newCall(request).execute();
        if (response.body() != null){
            htmlText = response.body().string();
        }

        Matcher matcher = matcher_json.matcher(htmlText);
        if (!matcher.find()){
            throw new Exception("Not Found");
        }

        String jsonText = "{" + matcher.group(1) + "}";

        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
        //System.out.println(json);

        String videoUrl = "";
        if (!json.getAsJsonObject().get("4k").getAsJsonArray().isEmpty()){
            videoUrl = json.getAsJsonObject().get("4k").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("1080p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("1080p").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("720p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("720p").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("480p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("480p").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("320p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("320p").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("240p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("240p").getAsJsonArray().get(0).getAsString();
        }

        return new ResultVideoData(videoUrl, null, false, false, false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }


    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        if (!matcher_videoUrl.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        String htmlText = "";

        Request request = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();
        Response response = client.newCall(request).execute();
        if (response.body() != null){
            htmlText = response.body().string();
        }

        Matcher matcher = matcher_Title.matcher(htmlText);
        if (!matcher.find()){
            throw new Exception("Not Found");
        }

        return matcher.group(1);
    }

    @Override
    public String getServiceName() {
        return "SpankBang";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
