package xyz.n7mn.nico_proxy.Site;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import xyz.n7mn.nico_proxy.Exception.FailedRetrieveException;
import xyz.n7mn.nico_proxy.Exception.URLNotFoundException;
import xyz.n7mn.nico_proxy.Exception.URLNotSupportException;
import xyz.n7mn.nico_proxy.Function;
import xyz.n7mn.nico_proxy.ProxySetting;
import xyz.n7mn.nico_proxy.Site.SiteResult.bilibili;
import xyz.n7mn.nico_proxy.TestMain;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class bilibili_com implements ServiceAPI {

    private String url = null;
    private HttpClient client = null;

    private final Pattern Support_URL1 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)/");
    private final Pattern Support_URL2 = Pattern.compile("https://www\\.bilibili\\.com/video/(.+)");
    private final Pattern Support_URL3 = Pattern.compile("b23\\.tv");

    private final Pattern matcher_json = Pattern.compile("<script>window\\.__INITIAL_STATE__=\\{(.+)\\};");

    @Override
    public String[] getCorrespondingURL() {
        return new String[]{
                "www.bilibili.com",
                "b23.tv"
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
        Matcher matcher3 = Support_URL3.matcher(url);

        String VideoID = "";
        if (matcher1.find()){
            VideoID = matcher1.group(1);
        } else if (matcher2.find()) {
            VideoID = matcher2.group(1);
        } else if (matcher3.find()) {
            try  {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .headers("User-Agent", Function.UserAgent)
                        .GET()
                        .build();

                HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                url = send.uri().toURL().toString();

                Matcher matcher = Support_URL1.matcher(url);
                if (matcher.find()) {
                    VideoID = matcher.group(1);
                }
            } catch (Exception e){
                throw new URLNotSupportException();
            }
        } else {
            throw new URLNotSupportException();
        }

        URI uri = new URI("https://www.bilibili.com/video/"+VideoID+"/");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .headers("User-Agent", Function.UserAgent)
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip")
                .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                .GET()
                .build();

        HttpResponse<byte[]> send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (send.statusCode() >= 400){
            //client.close();
            request = null;
            uri = null;
            throw new FailedRetrieveException("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")");
        }

        String html = new String(Function.decompressByte(send.body(), send.headers().firstValue("content-encoding").get()), StandardCharsets.UTF_8);
        Matcher matcher = matcher_json.matcher(html);
        String jsonText = "{}";
        if (matcher.find()){
            jsonText = "{"+matcher.group(1)+"}";
        }

        JsonElement json = Function.gson.fromJson(jsonText, JsonElement.class);

        String avid = "";
        String bvid = "";
        String cid = "";

        try {
            avid = json.getAsJsonObject().get("aid").getAsString();
            bvid = json.getAsJsonObject().get("bvid").getAsString();
            cid = json.getAsJsonObject().get("cid").getAsString();
        } catch (Exception e){
            throw new FailedRetrieveException("動画の取得に失敗しました。" + e.getMessage());
        }

        bilibili result = new bilibili();

        result.setURL("https://www.bilibili.com/video/"+bvid+"/");
        result.setTitle(json.getAsJsonObject().get("videoData").getAsJsonObject().get("title").getAsString());
        result.setDescription(json.getAsJsonObject().get("videoData").getAsJsonObject().get("desc").getAsString());
        result.setThumbnail(json.getAsJsonObject().get("videoData").getAsJsonObject().get("pic").getAsString());
        result.setViewCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("view").getAsInt());
        result.setReplyCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("reply").getAsInt());
        result.setLikeCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("like").getAsInt());
        result.setCoinCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("coin").getAsInt());
        result.setFavoriteCount(json.getAsJsonObject().get("videoData").getAsJsonObject().get("stat").getAsJsonObject().get("favorite").getAsInt());
        result.setDuration(json.getAsJsonObject().get("videoData").getAsJsonObject().get("duration").getAsInt());

        uri = new URI("https://api.bilibili.com/x/player/wbi/playurl?avid="+avid+"&bvid="+bvid+"&cid="+cid+"&qn=0&fnver=0&fnval=4048&fourk=1&gaia_source=&from_client=BROWSER&is_main_page=true&need_fragment=false&isGaiaAvoided=false&client_attr=0&version_name=4.10.4&app_id=100&session=bea6a57fe31194bf5fce97ee4f0dc942&web_location=1315873&dm_img_list=[]&dm_img_str=V2ViR0wgMS&dm_cover_img_str=QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDk4MCBEaXJlY3QzRDExIHZzXzVfMCBwc181XzApLCBvciBzaW1pbGFyR29vZ2xlIEluYy4gKE5WSURJQS&dm_img_inter=%7B%22ds%22:[],%22wh%22:[5773,6976,105],%22of%22:[331,662,331]%7D&x-bili-device-req-json=%7B%22platform%22:%22web%22,%22device%22:%22pc%22,%22mobi_app%22:%22web_cn%22%7D&x-bili-locale-json=%7B%22c_locale%22:%7B%22language%22:%22zh%22,%22script%22:%22Hans%22%7D,%22always_translate%22:false%7D&w_rid=77dde55e1e434e02143c8084dba6ad41&wts=1791025255");
        request = HttpRequest.newBuilder()
                .uri(uri)
                .headers("User-Agent", Function.UserAgent)
                .header("Accept", "*/*")
                .header("Accept-Encoding", "gzip")
                .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                .GET()
                .build();

        send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (send.statusCode() >= 400){
            //client.close();
            request = null;
            uri = null;
            throw new FailedRetrieveException("取得に失敗しました。(HTTPエラーコード : "+send.statusCode()+")");
        }
        jsonText = new String(Function.decompressByte(send.body(), send.headers().firstValue("content-encoding").get()), StandardCharsets.UTF_8);

        json = Function.gson.fromJson(jsonText, JsonElement.class);

        JsonArray arrayVideo = json.getAsJsonObject().get("data").getAsJsonObject().get("dash").getAsJsonObject().get("video").getAsJsonArray();
        JsonArray arrayAudio = json.getAsJsonObject().get("data").getAsJsonObject().get("dash").getAsJsonObject().get("audio").getAsJsonArray();

        String videoUrl = null;
        long maxVideoBitrate = -1;
        String audioUrl = null;
        long maxAudioBitrate = -1;

        for (JsonElement jsonElement : arrayVideo) {

            String tempURL = jsonElement.getAsJsonObject().get("baseUrl").getAsString();
            uri = new URI(tempURL);
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "gzip")
                    .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                    .header("Referer", "https://www.bilibili.com/")
                    .header("Range", "bytes=0-2775")
                    .GET()
                    .build();
            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() >= 400){
                tempURL = jsonElement.getAsJsonObject().get("backupUrl").getAsJsonArray().get(0).getAsString();
            }

            if (maxVideoBitrate <= jsonElement.getAsJsonObject().get("bandwidth").getAsInt()) {
                videoUrl = tempURL;
                maxVideoBitrate = jsonElement.getAsJsonObject().get("bandwidth").getAsInt();
            }

        }

        for (JsonElement jsonElement : arrayAudio) {

            String tempURL = jsonElement.getAsJsonObject().get("baseUrl").getAsString();
            uri = new URI(tempURL);
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .headers("User-Agent", Function.UserAgent)
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "gzip")
                    .header("Accept-Language", "ja,en;q=0.9,en-US;q=0.8")
                    .header("Referer", "https://www.bilibili.com/")
                    .header("Range", "bytes=0-2775")
                    .GET()
                    .build();
            send = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() >= 400){
                tempURL = jsonElement.getAsJsonObject().get("backupUrl").getAsJsonArray().get(0).getAsString();
            }

            if (maxAudioBitrate <= jsonElement.getAsJsonObject().get("bandwidth").getAsInt()) {
                audioUrl = tempURL;
                maxAudioBitrate = jsonElement.getAsJsonObject().get("bandwidth").getAsInt();
            }

        }

        result.setVideoURL(videoUrl);
        result.setAudioURL(audioUrl);

        return Function.gson.toJson(result);
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
/*
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
                .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,* /*;q=0.8")
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
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,* /*;q=0.8")
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
                        .headers("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,* /*;q=0.8")
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
*/
    }

    @Override
    public String getServiceName() {
        return "bilibili.com";
    }

}
