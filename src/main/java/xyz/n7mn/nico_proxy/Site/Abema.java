package xyz.n7mn.nico_proxy.Site;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.URLNotSupportException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.AbemaResult;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Abema implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Gson gson = Function.gson;

    private final Pattern SupportURL_Video1 = Pattern.compile("https://abema\\.tv/video/episode/(.+)");
    private final Pattern SupportURL_Video2 = Pattern.compile("https://abema\\.tv/channels/(.+)/slots/(.+)");
    private final Pattern SupportURL_Live1 = Pattern.compile("https://abema\\.tv/now-on-air/(.+)");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"abema.tv", "abema.app", "abema.go.link"};
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

        if (url.startsWith("https://abema.app") || url.startsWith("https://abema.go.link")){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .headers("User-Agent", Function.UserAgent)
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            url = send.uri().toURL().toString();
        }


        Matcher matcher = SupportURL_Video1.matcher(url);
        Matcher matcher1 = SupportURL_Video2.matcher(url);
        Matcher matcher2 = SupportURL_Live1.matcher(url);

        boolean video = matcher.find();
        boolean archive = matcher1.find();
        boolean live = matcher2.find();

        if (!video && !archive && !live){
            //System.out.println(url);
            throw new URLNotSupportException();
        }

        if (video){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.p-c3-e.abema-tv.com/v1/video/programs/"+matcher.group(1)+"?division=0&include=tvod"))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Authorization","bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiI3YWQ5NjQ1Ni0zZjFmLTRiYTctOTQ1OC1jOTA0MzQyYTNiNDMiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOTRjeXh3UGR5OVdHcHcifQ.Muv9eT4Tmy4JsSOGTVexwxuGnf2ZkwL1RkBo6MrSZGg")
                    .headers("Referer", "https://abema.tv/")
                    .GET()
                    .build();

            //HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
            String jsonText = send.body();
            //System.out.println(send.body());

            JsonElement json;
            try {
                json = gson.fromJson(jsonText, JsonElement.class);
            } catch (Exception e){
                throw new URLNotSupportException();
            }

            if (!json.getAsJsonObject().has("playback")){
                throw new FailedRetrieveException("対応していない動画です");
            }

            /*
private String URL;
private String Title;
private String Content;
private String Thumbnail;

private String VideoURL;
private String LiveURL;
             */

            AbemaResult result = new AbemaResult();
            result.setURL(url);
            StringBuilder sb = new StringBuilder();
            if (json.getAsJsonObject().has("series") && json.getAsJsonObject().get("series").getAsJsonObject().has("title")){
                sb.append(json.getAsJsonObject().get("series").getAsJsonObject().get("title").getAsString());
            }
            if (json.getAsJsonObject().has("episode") && json.getAsJsonObject().get("episode").getAsJsonObject().has("title")){
                sb.append(" ").append(json.getAsJsonObject().get("episode").getAsJsonObject().get("title").getAsString());
            }
            result.setTitle(sb.toString());
            sb.setLength(0);
            if (json.getAsJsonObject().has("episode") && json.getAsJsonObject().get("episode").getAsJsonObject().has("content")){
                result.setContent(json.getAsJsonObject().get("episode").getAsJsonObject().get("content").getAsString());
            }
            if (json.getAsJsonObject().has("series") && json.getAsJsonObject().get("series").getAsJsonObject().has("thumbComponent")){
                sb.append(json.getAsJsonObject().get("series").getAsJsonObject().get("thumbComponent").getAsJsonObject().get("urlPrefix").getAsString()).append("/");
                sb.append(json.getAsJsonObject().get("series").getAsJsonObject().get("thumbComponent").getAsJsonObject().get("filename").getAsString()).append("?");
                sb.append(json.getAsJsonObject().get("series").getAsJsonObject().get("thumbComponent").getAsJsonObject().get("query").getAsString());
            }
            if (!sb.isEmpty()){
                result.setThumbnail(sb.toString());
            }
            sb.setLength(0);
            sb = null;

            if (json.getAsJsonObject().getAsJsonObject("playback").has("hlsPreview")){
                result.setVideoURL(json.getAsJsonObject().getAsJsonObject("playback").get("hlsPreview").getAsString());
            }

            return gson.toJson(result);

        }

        if (archive){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.p-c3-e.abema-tv.com/v1/media/slots/"+matcher1.group(2)+"?include=payperview"))
                    .headers("User-Agent", Function.UserAgent)
                    .headers("Authorization","bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkZXYiOiI3YWQ5NjQ1Ni0zZjFmLTRiYTctOTQ1OC1jOTA0MzQyYTNiNDMiLCJleHAiOjIxNDc0ODM2NDcsImlzcyI6ImFiZW1hLmlvL3YxIiwic3ViIjoiOTRjeXh3UGR5OVdHcHcifQ.Muv9eT4Tmy4JsSOGTVexwxuGnf2ZkwL1RkBo6MrSZGg")
                    .headers("Referer", "https://abema.tv/")
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
            String jsonText = send.body();

            JsonElement json;
            try {
                json = gson.fromJson(jsonText, JsonElement.class);
            } catch (Exception e){
                throw new URLNotSupportException();
            }

            if (!json.getAsJsonObject().has("slot")){
                throw new FailedRetrieveException("対応していない配信アーカイブです");
            }

            AbemaResult result = new AbemaResult();
            result.setURL(url);
            if (json.getAsJsonObject().has("slot") && json.getAsJsonObject().get("slot").getAsJsonObject().has("title")){
                result.setTitle(json.getAsJsonObject().get("slot").getAsJsonObject().get("title").getAsString());
            }
            if (json.getAsJsonObject().has("slot") && json.getAsJsonObject().get("slot").getAsJsonObject().has("content")){
                result.setContent(json.getAsJsonObject().get("slot").getAsJsonObject().get("content").getAsString());
            }

            if (json.getAsJsonObject().get("slot").getAsJsonObject().has("playback") && json.getAsJsonObject().get("slot").getAsJsonObject().get("playback").getAsJsonObject().has("hlsPreview")){
                result.setVideoURL(json.getAsJsonObject().get("slot").getAsJsonObject().get("playback").getAsJsonObject().get("hlsPreview").getAsString());
            }
            return gson.toJson(result);
        }

        if (live){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.abema.io/v1/channels"))
                    .headers("User-Agent", Function.UserAgent)
                    .GET()
                    .build();

            HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
            String jsonText = send.body();

            JsonElement json;
            try {
                json = gson.fromJson(jsonText, JsonElement.class);
            } catch (Exception e){
                throw new URLNotSupportException();
            }

            if (json.getAsJsonObject().has("channels")){

                AbemaResult result = new AbemaResult();

                String id = matcher2.group(1);
                for (JsonElement element : json.getAsJsonObject().get("channels").getAsJsonArray()) {
                    if (element.getAsJsonObject().get("id").getAsString().equals(id)){
                        result.setURL("https://abema.tv/now-on-air/"+id);
                        result.setTitle(element.getAsJsonObject().get("name").getAsString());
                        result.setLiveURL(element.getAsJsonObject().get("playback").getAsJsonObject().get("hlsPreview").getAsString());
                        return gson.toJson(result);
                    }
                }

            }

            throw new FailedRetrieveException("存在しないチャンネルです");
        }

        throw new URLNotSupportException();

    }

    @Override
    public String getServiceName() {
        return "Abema";
    }
}
