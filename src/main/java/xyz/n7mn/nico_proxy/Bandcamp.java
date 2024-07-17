package xyz.n7mn.nico_proxy;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bandcamp implements ShareService {

    private final Pattern matcher_URL1 = Pattern.compile("https://(.+)\\.bandcamp\\.com/album/(.+)");
    private final Pattern matcher_URL2 = Pattern.compile("https://(.+)\\.bandcamp\\.com/track/(.+)");
    private final Pattern matcher_json = Pattern.compile("data-tralbum=\"\\{(.+)}\" data-payment=");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        if (!matcher_URL1.matcher(data.getURL()).find() && !matcher_URL2.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        final HttpClient client;
        if (data.getProxy() != null){
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .proxy(ProxySelector.of(new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort())))
                    .build();
        } else {
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(data.getURL()))
                .headers("User-Agent", Constant.nico_proxy_UserAgent)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        //client.close();

        //System.out.println(result);

        Matcher matcher = matcher_json.matcher(result);
        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        String s = "{" + matcher.group(1).replaceAll("&quot;", "\"") + "}";
        //System.out.println(s);

        JsonElement json = new Gson().fromJson(s, JsonElement.class);
        //System.out.println(json);

        JsonArray trackinfo = json.getAsJsonObject().get("trackinfo").getAsJsonArray();
        String[] audioUrl = {""};
        Map<String, JsonElement> file = trackinfo.get(0).getAsJsonObject().get("file").getAsJsonObject().asMap();
        int[] temp = {0};
        file.forEach((name, value)->{
            if (temp[0] == 0){
                audioUrl[0] = value.getAsString();
            }
            temp[0]++;
        });
        return new ResultVideoData(null, audioUrl[0], false, false, false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        if (!matcher_URL1.matcher(data.getURL()).find() && !matcher_URL2.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        final HttpClient client;
        if (data.getProxy() != null){
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .proxy(ProxySelector.of(new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort())))
                    .build();
        } else {
            client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(data.getURL()))
                .headers("User-Agent", Constant.nico_proxy_UserAgent)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String result = response.body();
        //client.close();

        //System.out.println(result);

        Matcher matcher = matcher_json.matcher(result);
        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        String s = "{" + matcher.group(1).replaceAll("&quot;", "\"") + "}";
        //System.out.println(s);

        JsonElement json = new Gson().fromJson(s, JsonElement.class);
        //System.out.println(json);

        return json.getAsJsonObject().get("current").getAsJsonObject().get("title").getAsString();
    }

    @Override
    public String getServiceName() {
        return "bandcamp";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
