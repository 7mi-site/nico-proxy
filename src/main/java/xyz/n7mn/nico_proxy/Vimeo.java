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

public class Vimeo implements ShareService{

    private final Pattern SupportURL = Pattern.compile("https://vimeo\\.com/(.+)");
    private final Pattern matcher_JsonData = Pattern.compile("window\\.vimeo\\.clip_page_config = \\{(.+)}");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        if (!SupportURL.matcher(data.getURL()).find()){
            throw new Exception("Not SupportURL");
        }
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        Request request_html = new Request.Builder()
                .url(data.getURL())
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/png,image/svg+xml,*/*;q=0.8")
                //.addHeader("Accept-Encoding", "gzip")
                .addHeader("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .addHeader("Connection", "keep-alive")
                .addHeader("DNT", "1")
                .addHeader("Priority","u=0, i")
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();
        Response response = client.newCall(request_html).execute();
        if (response.body() != null){
            HtmlText = response.body().string();
        }
        //System.out.println(HtmlText);
        response.close();

        final Matcher matcher = matcher_JsonData.matcher(HtmlText);
        String jsonText = "{}";
        if (matcher.find()){
            jsonText = "{" + matcher.group(1) + "}";
        }
        //System.out.println(jsonText);
        JsonElement json;
        try {
            json = new Gson().fromJson(jsonText, JsonElement.class);
        } catch (Exception e){
            throw new Exception("Not Support Video");
        }

        if (!json.getAsJsonObject().has("player")){
            throw new Exception("Not Support Video");
        }
        if (!json.getAsJsonObject().get("player").getAsJsonObject().has("config_url")){
            throw new Exception("Not Support Video");
        }

        String configUrl = json.getAsJsonObject().get("player").getAsJsonObject().get("config_url").getAsString();
        Request request = new Request.Builder()
                .url(configUrl)
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();
        Response response1 = client.newCall(request).execute();
        if (response1.body() != null){
            HtmlText = response1.body().string();
        }
        response1.close();
        json = new Gson().fromJson(HtmlText, JsonElement.class);

        JsonElement element = json.getAsJsonObject().get("request").getAsJsonObject().get("files").getAsJsonObject().get("hls").getAsJsonObject().get("cdns");

        String hlsURL = "";

        if (element.getAsJsonObject().has("akfire_interconnect_quic")){
            hlsURL = element.getAsJsonObject().get("akfire_interconnect_quic").getAsJsonObject().get("avc_url").getAsString();
        } else if (element.getAsJsonObject().has("fastly_skyfire")){
            hlsURL = element.getAsJsonObject().get("fastly_skyfire").getAsJsonObject().get("avc_url").getAsString();
        } else {
            throw new Exception("Not Support URL");
        }

        return new ResultVideoData(hlsURL, null, true, false, false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        if (!SupportURL.matcher(data.getURL()).find()){
            throw new Exception("Not SupportURL");
        }
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        Request request_html = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();
        Response response = client.newCall(request_html).execute();
        if (response.body() != null){
            HtmlText = response.body().string();
        }
        response.close();

        final Matcher matcher = matcher_JsonData.matcher(HtmlText);
        String jsonText = "";
        if (matcher.find()){
            jsonText = "{" + matcher.group(1) + "}";
        }
        //System.out.println(jsonText);
        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
        if (json.getAsJsonObject().get("clip").getAsJsonObject().has("title")){
            return json.getAsJsonObject().get("clip").getAsJsonObject().get("title").getAsString();
        }

        return "";
    }

    @Override
    public String getServiceName() {
        return "vimeo";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
