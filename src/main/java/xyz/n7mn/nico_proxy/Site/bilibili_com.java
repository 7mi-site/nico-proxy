package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Exception.URLNotSupportException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.bilibili;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class bilibili_com implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern Support_URL1 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)/");
    private final Pattern Support_URL2 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{
                "www.bilibili.com"
        };
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

        Matcher matcher1 = Support_URL1.matcher(url);
        Matcher matcher2 = Support_URL2.matcher(url);

        String VideoID = "";
        if (matcher1.find()){
            VideoID = matcher1.group(1);
        } else if (matcher2.find()){
            VideoID = matcher2.group(1);
        } else {
            throw new URLNotSupportException();
        }

        URI uri = new URI("https://api.bilibili.com/x/web-interface/view?bvid="+VideoID);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .headers("User-Agent", Function.UserAgent)
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());
        String jsonText = send.body();

        if (send.statusCode() >= 400){
            //client.close();
            request = null;
            uri = null;
            throw new FailedRetrieveException("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")");
        }
/*
            client.close();
            request = null;
            uri = null;
            client = null;
            return send.body();
*/
        /*
        *
        * private String URL;
        * private String Title;
        * private String Description;
        * private String Thumbnail;
        * private long ViewCount;
        * private long ReplyCount;
        * private long LikeCount;
        * private long CoinCount;
        * private long FavoriteCount;
        * private long Duration;

        * private String VideoURL;
        * private HashMap<String, String> VideoAccessCookie;
        *
         */

        JsonElement json = Function.gson.fromJson(jsonText, JsonElement.class);

        bilibili result = new bilibili();
        long cid = -1;
        if (json.isJsonObject() && json.getAsJsonObject().has("data")){
            result.setURL("https://www.bilibili.com/video/"+json.getAsJsonObject().get("data").getAsJsonObject().get("bvid").getAsString()+"/");
            result.setTitle(json.getAsJsonObject().get("data").getAsJsonObject().get("title").getAsString());
            result.setDescription(json.getAsJsonObject().get("data").getAsJsonObject().get("desc").getAsString());
            result.setThumbnail(json.getAsJsonObject().get("data").getAsJsonObject().get("pic").getAsString());
            result.setViewCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("view").getAsLong());
            result.setReplyCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("reply").getAsLong());
            result.setLikeCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("like").getAsLong());
            result.setCoinCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("coin").getAsLong());
            result.setFavoriteCount(json.getAsJsonObject().get("data").getAsJsonObject().get("stat").getAsJsonObject().get("favorite").getAsLong());
            result.setDuration(json.getAsJsonObject().get("data").getAsJsonObject().get("duration").getAsLong());

            cid = json.getAsJsonObject().get("data").getAsJsonObject().get("cid").getAsLong();
        }

        if (json.getAsJsonObject().has("code")){
            if (json.getAsJsonObject().get("code").getAsLong() == -400) {
                throw new FailedRetrieveException("動画が存在しません。");
            }
        }

        if (cid == -1) {
            throw new FailedRetrieveException("動画が存在しません。");
        }

        //System.out.println("cid : " + cid);

        uri = new URI("https://api.bilibili.com/x/player/playurl?bvid="+VideoID+"&cid="+cid);
        request = HttpRequest.newBuilder()
                .uri(uri)
                .headers("User-Agent", Function.UserAgent)
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .GET()
                .build();

        send = client.send(request, HttpResponse.BodyHandlers.ofString());
        jsonText = send.body();

        json = Function.gson.fromJson(jsonText, JsonElement.class);
        if (json.getAsJsonObject().has("data")){
            JsonArray elements = json.getAsJsonObject().get("data").getAsJsonObject().get("durl").getAsJsonArray();
            for (JsonElement element : elements) {
                if (result.getVideoURL() != null && !result.getVideoURL().isEmpty()){
                    break;
                }

                uri = new URI(element.getAsJsonObject().get("url").getAsString());
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Referer", url)
                        .HEAD()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (send.statusCode() < 400){
                    result.setVideoURL(element.getAsJsonObject().get("url").getAsString());
                    continue;
                }

                uri = new URI(element.getAsJsonObject().get("backup_url").getAsString());
                request = HttpRequest.newBuilder()
                        .uri(uri)
                        .headers("User-Agent", Function.UserAgent)
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .headers("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                        .headers("Referer", url)
                        .HEAD()
                        .build();

                send = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (send.statusCode() < 400){
                    result.setVideoURL(element.getAsJsonObject().get("url").getAsString());
                }
            }

        }

        //client.close();
        return Function.gson.toJson(result);

    }

    @Override
    public String getServiceName() {
        return "bilibili.com";
    }

}
