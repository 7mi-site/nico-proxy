package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.SpankBangResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpankBang implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern matcher_json = Pattern.compile("var stream_data = \\{(.+)};");
    private final Pattern matcher_Title = Pattern.compile("<h1 class=\"main_content_title\" title=\"(.+)\">(.+)</h1>");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"*.spankbang.com"};
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

        Matcher matcher = matcher_json.matcher(text);
        if (!matcher.find()){
            //System.out.println(s);
            throw new FailedRetrieveException("取得に失敗しました。");
        }
        JsonElement json = Function.gson.fromJson("{" + matcher.group(1) + "}", JsonElement.class);

        SpankBangResult result = new SpankBangResult();
        Matcher matcher1 = matcher_Title.matcher(text);
        if (matcher1.find()){
            result.setTitle(matcher1.group(1));
        }

        String videoUrl = "";
        if (!json.getAsJsonObject().get("m3u8_4k").getAsJsonArray().isEmpty()){
            videoUrl = json.getAsJsonObject().get("m3u8_4k").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("m3u8_1080p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("m3u8_1080p").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("m3u8_720p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("m3u8_720p").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("m3u8_480p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("m3u8_480p").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("m3u8_320p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("m3u8_320p").getAsJsonArray().get(0).getAsString();
        }
        if (!json.getAsJsonObject().get("m3u8_240p").getAsJsonArray().isEmpty() && videoUrl.isEmpty()){
            videoUrl = json.getAsJsonObject().get("m3u8_240p").getAsJsonArray().get(0).getAsString();
        }
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

        result.setVideoURL(videoUrl);

        return Function.gson.toJson(result);

    }

    @Override
    public String getServiceName() {
        return "SpankBang";
    }

}
