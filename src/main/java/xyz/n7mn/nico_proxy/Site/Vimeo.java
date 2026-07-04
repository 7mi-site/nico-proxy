package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.VimeoResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Vimeo implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern matcher_JsonData = Pattern.compile("<script id=\"microdata\" type=\"application/ld\\+json\">\n(.+)</script>");
    private final Pattern SupportURL = Pattern.compile("https://vimeo\\.com/(\\d+)");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"vimeo.com"};
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

        // https://soundcloud.com/kysn/5-hyperflip-67?access=ex_chrome
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .headers("User-Agent", Function.UserAgent)
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .headers("DNT", "1")
                .headers("Priority","u=0, i")
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
        String text = send.body();
        Matcher matcher1 = matcher_JsonData.matcher(text);
        if (matcher1.find()){

            Matcher matcher2 = SupportURL.matcher(url);
            String configUrl = "https://player.vimeo.com/video/"+(matcher2.find() ? matcher2.group(1) : "")+"/config?airplay=1&ask_ai=0&audio_tracks=1&autoplay=0&badge=1&byline=0&cc=1&chapters=1&chromecast=1&colors=000000%2C00adef%2Cffffff%2C000000&context=Vimeo%5CController%5CApi%5CResources%5CVideoController.&email=0&force_embed=1&fullscreen=1&h=eb35e2616e&like=1&muted=1&outro=beginning&pip=1&play_button_position=auto&playbar=1&portrait=0&preload=auto&quality_selector=1&share=1&skipping_forward=1&speed=1&title=0&transcript=0&transparent=0&vimeo_logo=1&volume=1&watch_later=1&s=9ec729bb4b1e85191cc9e6ee560da45bf53a1d9b_1783276808";
            try {

                request = HttpRequest.newBuilder()
                        .uri(new URI(configUrl))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("DNT", "1")
                        .headers("Priority","u=0, i")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString());
                text = send.body();

                //System.out.println(text);

                JsonElement json = Function.gson.fromJson(text, JsonElement.class);
                JsonElement element = json.getAsJsonObject().get("request").getAsJsonObject().get("files").getAsJsonObject().get("hls").getAsJsonObject().get("cdns");

                String hlsURL = "";
                if (element.getAsJsonObject().has("akfire_interconnect_quic")){
                    hlsURL = element.getAsJsonObject().get("akfire_interconnect_quic").getAsJsonObject().get("avc_url").getAsString();
                } else if (element.getAsJsonObject().has("fastly_skyfire")){
                    hlsURL = element.getAsJsonObject().get("fastly_skyfire").getAsJsonObject().get("avc_url").getAsString();
                } else {
                    hlsURL = null;
                }

/*

private String URL;
private String Title;
private Long Duration;
private String Thumbnail;

private String VideoURL;
*/

                VimeoResult result = new VimeoResult();
                result.setURL(json.getAsJsonObject().get("video").getAsJsonObject().get("share_url").getAsString());
                result.setTitle(json.getAsJsonObject().get("video").getAsJsonObject().get("title").getAsString());
                result.setDuration(json.getAsJsonObject().get("video").getAsJsonObject().get("duration").getAsLong());
                result.setThumbnail(json.getAsJsonObject().get("video").getAsJsonObject().get("thumbnail_url").getAsString());
                result.setVideoURL(hlsURL);

                //client.close();
                return Function.gson.toJson(result);

            } catch (Exception e){
                //client.close();
                throw new FailedRetrieveException("取得に失敗しました。");
            }

        } else {
            //client.close();
            throw new FailedRetrieveException("取得に失敗しました。");
        }

    }

    @Override
    public String getServiceName() {
        return "Vimeo";
    }

}
