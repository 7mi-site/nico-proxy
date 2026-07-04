package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Exception.URLNotSupportException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.PornhubResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pornhub implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern Support_URL = Pattern.compile("https://(.+)\\.pornhub\\.com/view_video\\.php\\?viewkey=(.+)");
    private final Pattern matcher_json = Pattern.compile("var flashvars_(\\d+) = \\{(.*)\\}\\;");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"*.pornhub.com"};
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
        if (url  == null || url.isEmpty()){
            throw new URLNotFoundException();
        }
        Matcher matcher = Support_URL.matcher(url);

        if (!matcher.find()){
            throw new URLNotSupportException();
        }

        String id = matcher.group(2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://jp.pornhub.com/view_video.php?viewkey="+id))
                .headers("User-Agent", Function.UserAgent)
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
        String text = send.body();
        Matcher matcher1 = matcher_json.matcher(text);
        if (!matcher1.find()){
            throw new FailedRetrieveException("取得に失敗しました。");
        }
        String s = "{" + matcher1.group(2) + "}";
        JsonElement json = Function.gson.fromJson(s, JsonElement.class);

        PornhubResult result = new PornhubResult();

        result.setTitle(json.getAsJsonObject().get("video_title").getAsString());
        result.setThumbnail(json.getAsJsonObject().get("image_url").getAsString());
        result.setDuration(json.getAsJsonObject().get("video_duration").getAsLong());

        String hlsURL = "";
        int width = -1;
        int height = -1;
        for (JsonElement element : json.getAsJsonObject().get("mediaDefinitions").getAsJsonArray()) {

            if (element.getAsJsonObject().get("format").getAsString().equals("hls")){
                int height1 = element.getAsJsonObject().get("height").getAsInt();
                int width1 = element.getAsJsonObject().get("width").getAsInt();

                if (width1 >= width || height1 >= height){
                    width = width1;
                    height = height1;

                    hlsURL = element.getAsJsonObject().get("videoUrl").getAsString();
                }
            }
        }

        result.setVideoURL(hlsURL);

        return Function.gson.toJson(result);

    }

    @Override
    public String getServiceName() {
        return "Pornhub";
    }

}
