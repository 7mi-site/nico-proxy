package xyz.n7mn.nico_proxy.Site;

import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.XvideoResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XVIDEOS implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern matcher_duration = Pattern.compile("<meta property=\"og:duration\" content=\"(\\d+)\" />");
    private final Pattern matcher_hlsURL = Pattern.compile("html5player\\.setVideoHLS\\('(.+)'\\)");
    private final Pattern matcher_Title = Pattern.compile("html5player\\.setVideoTitle\\('(.+)'\\)");
    private final Pattern matcher_ThumbUrl = Pattern.compile("html5player\\.setThumbUrl\\('(.+)'\\);");
    private final Pattern matcher_Description = Pattern.compile("\"description\": \"(.+)\",");
    private final Pattern matcher_playCount = Pattern.compile("\"userInteractionCount\": (\\d+)");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"xvideos.com", "www.xvideos.com"};
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
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
        String text = send.body();

        XvideoResult result = new XvideoResult();

        Matcher matcher1 = matcher_Title.matcher(text);
        Matcher matcher2 = matcher_Description.matcher(text);
        Matcher matcher3 = matcher_ThumbUrl.matcher(text);
        Matcher matcher4 = matcher_playCount.matcher(text);
        Matcher matcher5 = matcher_duration.matcher(text);
        Matcher matcher6 = matcher_hlsURL.matcher(text);

        if (matcher1.find()){
            result.setTitle(matcher1.group(1));
        }
        if (matcher2.find()){
            result.setDescription(matcher2.group(1));
        }
        if (matcher3.find()){
            result.setThumbUrl(matcher3.group(1));
        }
        if (matcher4.find()){
            result.setPlayCount(Long.parseLong(matcher4.group(1)));
        }
        if (matcher5.find()){
            result.setDuration(Long.parseLong(matcher5.group(1)));
        }
        if (matcher6.find()){
            result.setVideoURL(matcher6.group(1));
        } else {
            throw new FailedRetrieveException("取得に失敗しました。");
        }

        return Function.gson.toJson(result);

    }

    @Override
    public String getServiceName() {
        return "XVIDEOS.COM";
    }

}
