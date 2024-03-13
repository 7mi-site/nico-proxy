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

public class Gimy implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        Matcher matcher = Pattern.compile("https://gimy\\.ai/eps/(.+)\\.html").matcher(data.getURL());
        if (!matcher.find()){
            throw new Exception("gimy.ai Not Support URL");
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url(data.getURL())
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        //System.out.println(HtmlText);

        Matcher json_matcher = Pattern.compile("player_data=\\{(.+)\\}").matcher(HtmlText);
        if (!json_matcher.find()){
            throw new Exception("gimy.ai Not Found");
        }

        final String json = "{"+json_matcher.group(1)+"}";
        //System.out.println(json);
        JsonElement JsonData = new Gson().fromJson(json, JsonElement.class);
        if (JsonData.getAsJsonObject().has("url")){
            return new ResultVideoData(JsonData.getAsJsonObject().get("url").getAsString(), null, true, false, false, null);
        }

        throw new Exception("gimy.ai Not Support URL");
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        Matcher matcher = Pattern.compile("https://gimy\\.ai/eps/(.+)\\.html").matcher(data.getURL());
        if (!matcher.find()){
            throw new Exception("gimy.ai Not Support URL");
        }

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url(data.getURL())
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        //System.out.println(HtmlText);

        Matcher json_matcher = Pattern.compile("player_data=\\{(.+)\\}").matcher(HtmlText);
        if (!json_matcher.find()){
            throw new Exception("gimy.ai Not Found");
        }

        final String json = "{"+json_matcher.group(1)+"}";
        //System.out.println(json);
        JsonElement JsonData = new Gson().fromJson(json, JsonElement.class);
        if (JsonData.getAsJsonObject().get("vod_data").getAsJsonObject().has("vod_name")){
            return JsonData.getAsJsonObject().get("vod_data").getAsJsonObject().get("vod_name").getAsString();
        }
    }

    @Override
    public String getServiceName() {
        return "Gimy 劇迷";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
