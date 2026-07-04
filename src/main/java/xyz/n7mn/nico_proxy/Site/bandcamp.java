package xyz.n7mn.nico_proxy.Site;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.URLNotSupportException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.bandcampResult;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class bandcamp implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern matcher_json = Pattern.compile("data-tralbum=\"\\{(.+)}\" data-payment=");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"*.bandcamp.com"};
    }

    @Override
    public void setHttpClient(HttpClient client) {
        this.client = client;
    }

    @Override
    public void setURL(String URL) {
        this.url = URL;
    }

    @Override
    public void setProxy(ProxySetting proxy) {

    }

    @Override
    public void setToken(String[] token) {

    }


    @Override
    public String get() throws Exception {

        if (url  == null || url.isEmpty()){
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
        String result = send.body();
        //client.close();

        //System.out.println(result);

        Matcher matcher = matcher_json.matcher(result);
        if (!matcher.find()){
            throw new URLNotSupportException();
        }

        String s = "{" + matcher.group(1).replaceAll("&quot;", "\"") + "}";
        //System.out.println(s);

        JsonElement json = new Gson().fromJson(s, JsonElement.class);
        //System.out.println(json);

        JsonArray trackinfo = json.getAsJsonObject().get("trackinfo").getAsJsonArray();
        String[] audio = {"", ""};
        Map<String, JsonElement> file = trackinfo.get(0).getAsJsonObject().get("file").getAsJsonObject().asMap();
        file.forEach((name, value)->{
            if (audio[0].isEmpty()){
                audio[0] = value.getAsString();
                audio[1] = trackinfo.get(0).getAsJsonObject().get("title").getAsString();
            }
        });

        bandcampResult result1 = new bandcampResult();
        result1.setTitle(audio[1]);
        result1.setAudioURL(audio[0]);

        //client.close();
        return Function.gson.toJson(result1);

    }

    @Override
    public String getServiceName() {
        return "bandcamp";
    }
}
