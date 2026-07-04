package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.mellow_fan_Result;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class mellow_fan implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"www.mellow-fan.com"};
    }

    @Override
    public void setURL(String URL) {
        this.url = URL;
    }

    @Override
    public void setHttpClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public void setToken(String[] token) {

    }

    @Override
    public void setProxy(ProxySetting proxy) {

    }

    @Override
    public String get() throws Exception {
        if (url == null || url.isEmpty()){
            throw new URLNotFoundException();
        }

        String[] split = url.split("/");
        String id = split[split.length - 1];

        URI uri = new URI("https://public.mellow-fan.com/external/api/v5/movies/"+id);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .headers("User-Agent", Function.UserAgent)
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
        String jsonText = send.body();
        JsonElement json = Function.gson.fromJson(jsonText, JsonElement.class);
        jsonText = null;
        mellow_fan_Result result = new mellow_fan_Result();
        if (json.getAsJsonObject().has("id")){
            if (json.getAsJsonObject().get("is_live").getAsBoolean()){
                result.setURL("https://www.mellow-fan.com/live/"+id);
            } else {
                result.setURL("https://www.mellow-fan.com/movie/"+id);
            }
            result.setTitle(json.getAsJsonObject().get("title").getAsString());
            result.setIntroduction(json.getAsJsonObject().get("introduction").getAsString());
            result.setThumbnail(json.getAsJsonObject().get("l_thumbnail_url").getAsString());
            if (json.getAsJsonObject().get("is_live").getAsBoolean()){
                result.setLiveViews(json.getAsJsonObject().get("live_views").getAsLong());
                result.setTotalViews(json.getAsJsonObject().get("total_views").getAsLong());
            }
            result.setLive(json.getAsJsonObject().get("is_live").getAsBoolean());
            if (result.isLive()){
                result.setLiveURL(json.getAsJsonObject().get("media").getAsJsonObject().get("url").getAsString());
            } else {
                result.setVideoURL(json.getAsJsonObject().get("media").getAsJsonObject().get("url").getAsString());
            }
        } else {
            //client.close();
            throw new FailedRetrieveException("存在しない 配信 または 動画 です");
        }

        //client.close();
        return Function.gson.toJson(result);
    }

    @Override
    public String getServiceName() {
        return "mellow-fan";
    }

}
