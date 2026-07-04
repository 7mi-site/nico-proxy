package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.SoundCloudResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoundCloud implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern clientId = Pattern.compile("client_id:\"(.+)\",env:\"production\"");
    private final Pattern jsonData = Pattern.compile("window\\.__sc_hydration = \\[(.+)\\];");
    private final Pattern CheckQuestion = Pattern.compile("\\?");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"soundcloud.com"};
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

                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
        String text = send.body();
        Matcher matcher1 = jsonData.matcher(text);

        JsonElement json = null;
        if (matcher1.find()){
            try {
                json = Function.gson.fromJson("["+matcher1.group(1)+"]", JsonElement.class);
            } catch (Exception e){
                //client.close();
                throw new FailedRetrieveException("対応していないURLです。");
            }
        }

        if (json == null){
            //client.close();
            throw new FailedRetrieveException("対応していないURLです。");
        }

        request = HttpRequest.newBuilder()
                .uri(new URI("https://a-v2.sndcdn.com/assets/50-a0fa7b81.js"))
                .headers("User-Agent", Function.UserAgent)
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .GET()
                .build();

        send = client.send(request, HttpResponse.BodyHandlers.ofString());
        text = send.body();
        final String ClientId;
        Matcher matcher2 = clientId.matcher(text);
        if (matcher2.find()){
            ClientId = matcher2.group(1);
        } else {
            ClientId = null;
        }

        String TrackAuthorization = null;
        String BaseURL = null;

        String permalink_url = null;
        String title = null;
        Long duration = null;
        String description = null;

        for (int i = 0; i < json.getAsJsonArray().size(); i++) {
            if (json.getAsJsonArray().get(i).getAsJsonObject().get("hydratable").getAsString().equals("sound")){
                if (BaseURL == null){
                    BaseURL = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("media").getAsJsonObject().get("transcodings").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                }

                if (TrackAuthorization == null){
                    TrackAuthorization = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("track_authorization").getAsString();
                }

                if (permalink_url == null){
                    permalink_url = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("permalink_url").getAsString();
                }
                if (title == null){
                    title = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("title").getAsString();
                }
                if (duration == null){
                    duration = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("full_duration").getAsLong();
                }
                if (description == null){
                    description = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("description").getAsString();
                }
            }

        }

        SoundCloudResult result = new SoundCloudResult();
        result.setURL(permalink_url);
        result.setTitle(title);
        result.setDescription(description);
        result.setDuration(duration);

        String hlsUrl = BaseURL + "?client_id=" + ClientId + "&track_authorization=" + TrackAuthorization;

        request = HttpRequest.newBuilder()
                .uri(new URI(hlsUrl))
                .headers("User-Agent", Function.UserAgent)
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")

                .GET()
                .build();

        send = client.send(request, HttpResponse.BodyHandlers.ofString());
        text = send.body();
        json = Function.gson.fromJson(text, JsonElement.class);

        if (json != null){
            result.setAudioURL(json.getAsJsonObject().get("url").getAsString());
        } else {
            String ClientID = "3WIthHrmko3NUQ6wbfCSRvFcDexHgswc";
            request = HttpRequest.newBuilder()
                    .uri(new URI("https://api-v2.soundcloud.com/resolve?url="+ URLEncoder.encode(url, StandardCharsets.UTF_8)+"&client_id="+ClientID))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .GET()
                    .build();

            send = client.send(request, HttpResponse.BodyHandlers.ofString());
            text = send.body();
            json = Function.gson.fromJson(text, JsonElement.class);

            if (json == null){
                ClientID = "YHtBnq6bxM7DhJkIfzrGq3gYrueyLDMM";
                request = HttpRequest.newBuilder()
                        .uri(new URI("https://api-v2.soundcloud.com/resolve?url="+ URLEncoder.encode(url, StandardCharsets.UTF_8)+"&client_id="+ClientID))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .GET()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString());
                text = send.body();
                json = Function.gson.fromJson(text, JsonElement.class);

                hlsUrl = json.getAsJsonObject().get("media").getAsJsonObject().get("transcodings").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                //System.out.println(hlsUrl);
                request = CheckQuestion.matcher(hlsUrl).find() ? HttpRequest.newBuilder()
                        .uri(new URI(hlsUrl + "&client_id="+ClientID))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")

                        .GET()
                        .build() : HttpRequest.newBuilder()
                        .uri(new URI(hlsUrl + "?client_id="+ClientID))
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")

                        .GET()
                        .build();
                send = client.send(request, HttpResponse.BodyHandlers.ofString());
                text = send.body();
                json = Function.gson.fromJson(text, JsonElement.class);

                result.setAudioURL(json.getAsJsonObject().get("url").getAsString());
            } else {
                result.setAudioURL(json.getAsJsonObject().get("url").getAsString());
            }
        }

        //client.close();
        return Function.gson.toJson(result);

    }

    @Override
    public String getServiceName() {
        return "SoundCloud";
    }

}
