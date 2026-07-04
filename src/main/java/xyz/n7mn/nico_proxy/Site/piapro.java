package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Exception.URLNotSupportException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.piaproResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class piapro implements ServiceAPI {
    private String url = null;
    private HttpClient client = null;

    private final Pattern matcher_url1 = Pattern.compile("https://piapro\\.jp/t/(.+)");
    private final Pattern matcher_url = Pattern.compile("\"url\": \"(.+)\",");
    private final Pattern matcher_title = Pattern.compile("<h1 class=\"contents_title\">(.+)</h1>");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"piapro.jp"};
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

        if (!matcher_url1.matcher(url).find()){
            throw new URLNotSupportException();
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

        Matcher matcher1 = matcher_url.matcher(text);
        Matcher matcher2 = matcher_title.matcher(text);
        String Title = "";
        String url = "";
        if (matcher1.find()){
            url = matcher1.group(1);
        } else {
            //client.close();
            throw new FailedRetrieveException("取得に失敗しました。");
        }
        if (matcher2.find()){
            Title = matcher2.group(1);
        }

        //client.close();

        piaproResult result = new piaproResult();
        result.setTitle(Title);
        result.setAudioURL(url);

        return Function.gson.toJson(result);
    }

    @Override
    public String getServiceName() {
        return "piapro";
    }

}
