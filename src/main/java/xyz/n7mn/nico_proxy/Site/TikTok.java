package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.TikTokResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TikTok implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern matcher_DataJson = Pattern.compile("<script id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\" type=\"application/json\">\\{(.+)\\}</script>");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{"www.tiktok.com", "*.tiktok.com"};
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

        //System.out.println(Proxy);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .headers("User-Agent", Function.UserAgent)
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
        String text = send.body();
        if (send.statusCode() >= 400){
            request = null;
            throw new FailedRetrieveException("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")");
        }

        HashMap<String, String> cookieList = new HashMap<>();

        //send.headers().
        List<String> list = send.headers().allValues("Set-Cookie");

        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            //System.out.println(s);

            String s1 = s.split(";")[0];
            //System.out.println(s1);

            sb.append(s1).append("; ");
        }
        //System.out.println(sb.substring(0, sb.length() - 2));

        Matcher matcher = matcher_DataJson.matcher(text);
        String jsonText = "{}";
        if (matcher.find()){
            jsonText = "{" + matcher.group(1) + "}";
        }
        JsonElement json = Function.gson.fromJson(jsonText, JsonElement.class);

        if (json.isJsonObject() && json.getAsJsonObject().has("__DEFAULT_SCOPE__") && json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().has("webapp.video-detail") && json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().has("itemInfo")){

            TikTokResult result = new TikTokResult();
            result.setURL("https://" + json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.app-context").getAsJsonObject().get("host").getAsString() + "/@" + json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("author").getAsJsonObject().get("uniqueId").getAsString() + "/video/" + json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("id").getAsString());
            result.setDescription(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("desc").getAsString());
            result.setDiggCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("diggCount").getAsString()));
            result.setCommentCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("commentCount").getAsString()));
            result.setCollectCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("collectCount").getAsString()));
            result.setShareCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("shareCount").getAsString()));
            result.setPlayCount(Long.parseLong(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("statsV2").getAsJsonObject().get("playCount").getAsString()));
            result.setDuration(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("video").getAsJsonObject().get("duration").getAsLong());
            result.setVideoURL(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("video").getAsJsonObject().get("downloadAddr").getAsString());
            if (result.getVideoURL().isEmpty()){
                result.setVideoURL(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("video").getAsJsonObject().get("playAddr").getAsString());
            }
            result.setVideoAccessCookie(sb.substring(0, sb.length() - 2));
            //return json.toString();
            return Function.gson.toJson(result);
        } else {
            throw new FailedRetrieveException("存在しない動画です。");
        }

        //return "";

    }

    @Override
    public String getServiceName() {
        return "TikTok";
    }

}
