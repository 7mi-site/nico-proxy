package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.SonicbowlResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sonicbowl implements ServiceAPI {

    private String proxy = null;
    private String url = null;
    private HttpClient client = null;

    private final Pattern matcher_Audio = Pattern.compile("<meta property=\"og:audio\" content=\"(.+)\">");
    private final Pattern matcher_Title = Pattern.compile("<title>(.+)</title>");
    private final Pattern matcher_Description = Pattern.compile("<meta name=\"description\" content=\"(.+)\">");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"player.sonicbowl.cloud"};
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

        SonicbowlResult result = new SonicbowlResult();

        Matcher matcher1 = matcher_Title.matcher(text);
        if (matcher1.find()){
            result.setTitle(matcher1.group(1));
        }
        Matcher matcher2 = matcher_Description.matcher(text);
        if (matcher2.find()){
            result.setDescription(matcher2.group(1));
        }
        Matcher matcher3 = matcher_Audio.matcher(text);
        if (matcher3.find()){
            result.setAudioURL(matcher3.group(1));
        }

        //client.close();
        return Function.gson.toJson(result);

    }

    @Override
    public String getServiceName() {
        return "Sonicbowl";
    }

}
