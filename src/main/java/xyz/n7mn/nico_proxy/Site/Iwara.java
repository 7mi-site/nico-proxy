package xyz.n7mn.nico_proxy.Site;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Exception.URLNotSupportException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.IwaraResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class Iwara implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;


    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"www.iwara.tv"};
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

        String[] split = url.split("/");

        if (split.length < 5){
            throw new URLNotSupportException();
        }

        // https://apiq.iwara.tv/video/4lbqvFBO4n98ZN
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://api.iwara.tv/video/" + split[4]))
                .headers("Accept", "application/json")
                .headers("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                .headers("Connection", "keep-alive")
                .headers("Content-Type", "application/json")
                .headers("Host", "api.iwara.tv")
                .headers("Origin", "https://www.iwara.tv")
                .headers("Priority", "u=4")
                .headers("Referer", "https://www.iwara.tv/")
                .headers("Sec-Fetch-Dest", "empty")
                .headers("Sec-Fetch-Mode", "cors")
                .headers("Sec-Fetch-Site", "same-site")
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
        String jsonText = send.body();

        System.out.println(jsonText);
        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);

        IwaraResult result = new IwaraResult();
        result.setTitle(json.getAsJsonObject().get("title").getAsString());
        result.setDescription(json.getAsJsonObject().get("body").getAsString());
        result.setLikeCount(json.getAsJsonObject().get("numLikes").getAsLong());
        result.setViewCount(json.getAsJsonObject().get("numViews").getAsLong());

        String baseUrl = json.getAsJsonObject().get("fileUrl").getAsString();

        //System.out.println(baseUrl + "&download="+URLEncoder.encode("Iwara - "+result.getTitle()+" ["+json.getAsJsonObject().get("id").getAsString()+"].mp4", StandardCharsets.UTF_8));
        request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "&download="+URLEncoder.encode("Iwara - "+result.getTitle()+" ["+json.getAsJsonObject().get("id").getAsString()+"].mp4", StandardCharsets.UTF_8)))
                .headers("User-Agent", Function.UserAgent)
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .headers("Accept-Encoding", "gzip, br")
                // いつかこのX-Versionを取れるようにする
                // .headers("X-Version","3f8ce8c9518993ed46b9f388988b4ad0781eff7d")
                .GET()
                .build();
        send = client.send(request, HttpResponse.BodyHandlers.ofString());
        jsonText = send.body();
        json = new Gson().fromJson(jsonText, JsonElement.class);

        result.setVideoURL("https:"+json.getAsJsonArray().get(0).getAsJsonObject().get("src").getAsJsonObject().get("view").getAsString());

        return Function.gson.toJson(result);
    }

    @Override
    public String getServiceName() {
        return "iwara.tv";
    }

}
