package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import kotlin.Pair;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.n7mn.nico_proxy.data.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicoNicoVideo implements ShareService {

    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    private final String UserAgent = Constant.nico_proxy_UserAgent;

    private final Pattern matcher_Json = Pattern.compile("<meta name=\"server-response\" content=\"\\{(.+)}\" />");
    private final Pattern matcher_cookie = Pattern.compile("domand_bid=(.+); expires=");
    private final Pattern matcher_m3u8Audio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"(.+)\",NAME=\"(.+)\",DEFAULT=(.+),URI=\"(.+)\"");

    private final Pattern matcher_LdJsonVideo = Pattern.compile("<script type=\"application/ld\\+json\" class=\"LdJson\">\\{(.*)\\}");
    private final Pattern matcher_LdJsonLive = Pattern.compile("<script type=\"application/ld\\+json\">\\{(.*)\\}");

    /**
     * @param data ニコ動URL、接続プロキシ情報
     * @return String[0] 再生用動画URL String[1] ハートビートセッション文字列 String[2] ハートビート信号ID文字列
     * @throws Exception エラーメッセージ
     */
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        System.gc();

        // IDのみにする
        final String id = getId(data.getURL());

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        String nico_sid = "";
        try {
            Request request_html = new Request.Builder()
                    .url("https://www.nicovideo.jp/watch/" + id)
                    .addHeader("User-agent", UserAgent)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.header("X-niconico-sid") != null){
                nico_sid = response.header("X-niconico-sid");
            }
/*
            for (Pair<? extends String, ? extends String> header : response.headers()) {
                System.out.println(header.component1() + " : " + header.component2());
            }
*/

            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();

        } catch (Exception e) {
            if (data.getProxy() != null) {
                throw new Exception("www.nicovideo.jp" + e.getMessage() + " (Use Proxy : " + data.getProxy().getProxyIP() + ")");
            } else {
                throw new Exception("www.nicovideo.jp" + e.getMessage());
            }
        }

        //System.out.println("a : "+nico_sid);

        Matcher matcher = matcher_Json.matcher(HtmlText);
        if (!matcher.find()){
            throw new Exception("www.nicovideo.jp Not Found");
        }

        String jsonText = "{" + matcher.group(1).replaceAll("&quot;", "\"") + "}";
        //System.out.println(jsonText);

        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);

        String accessRightKey = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").get("accessRightKey").getAsString();
        String trackId = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("client").get("watchTrackId").getAsString();

        StringBuilder videoJson = new StringBuilder();

        for (JsonElement element : json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").getAsJsonArray("videos")) {
            //System.out.println(element);
            if (element.getAsJsonObject().get("isAvailable").getAsBoolean()) {
                videoJson.append("[\"").append(element.getAsJsonObject().get("id").getAsString()).append("\",\"").append(json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("response").getAsJsonObject("media").getAsJsonObject("domand").getAsJsonArray("audios").get(0).getAsJsonObject().get("id").getAsString()).append("\"],");
            }
        }

        // ["video-h264-360p","audio-aac-128kbps"]


        String sendJson = "{\"outputs\":["+videoJson.substring(0, videoJson.length() - 1)+"]}";
        //System.out.println(sendJson);
        //System.out.println(trackId);
        RequestBody body = RequestBody.create(sendJson, JSON);

        Request request_json2 = new Request.Builder()
                .url("https://nvapi.nicovideo.jp/v1/watch/"+id+"/access-rights/hls?actionTrackId="+trackId)
                .addHeader("Access-Control-Request-Headers", "content-type,x-access-right-key,x-frontend-id,x-frontend-version,x-niconico-language,x-request-with")
                .addHeader("User-Agent", UserAgent)
                .addHeader("X-Access-Right-Key", accessRightKey)
                .addHeader("X-Frontend-Id", "6")
                .addHeader("X-Frontend-Version", "0")
                .addHeader("X-Niconico-Language", "ja-jp")
                .addHeader("X-Request-With", "nicovideo")
                .addHeader("Cookie", "nicosid="+nico_sid)
                .post(body)
                .build();
        Response response2 = client.newCall(request_json2).execute();
        if (response2.body() != null){
            jsonText = response2.body().string();
        }

        String domand_bid = "";
        for (Pair<? extends String, ? extends String> header : response2.headers()) {
            if (header.component1().equals("set-cookie")){
                Matcher matcher1 = matcher_cookie.matcher(header.component2());
                if (matcher1.find()){
                    domand_bid = matcher1.group(1);
                }
            }
        }


        response2.close();

        //System.out.println(jsonText);

        json = new Gson().fromJson(jsonText, JsonElement.class);

        //System.out.println(json);

        // このままだと再生できないのでアクセス時にCookieを渡してあげる必要がある
        String contentUrl = json.getAsJsonObject().getAsJsonObject("data").get("contentUrl").getAsString();

        //System.out.println(contentUrl);

        //System.out.println("nicosid="+nico_sid+"; domand_bid="+domand_bid);
        Request request_m3u8 = new Request.Builder()
                .url(contentUrl)
                .addHeader("Cookie", "nicosid="+nico_sid+"; domand_bid="+domand_bid)
                .addHeader("User-Agent", UserAgent)
                .addHeader("Origin", "https://www.nicovideo.jp")
                .addHeader("Referer", "https://www.nicovideo.jp/")
                .build();
        Response response_m3u8 = client.newCall(request_m3u8).execute();

        String main_m3u8 = "";
        if (response_m3u8.body() != null){
            //System.out.println(response_m3u8.code());
            main_m3u8 = response_m3u8.body().string();

            //System.out.println(main_m3u8);
        }
        response_m3u8.close();

        String videoUrl = "";
        String audioUrl = "";
        for (String str : main_m3u8.split("\n")){
            Matcher matcher_m3u8 = matcher_m3u8Audio.matcher(str);
            if (matcher_m3u8.find()){
                audioUrl = matcher_m3u8.group(4);
                continue;
            }
            if (str.startsWith("#")){
                continue;
            }

            videoUrl = str;
            break;
        }

        NicoCookie nicoCookie = new NicoCookie();
        nicoCookie.setDomand_bid(domand_bid);
        nicoCookie.setNicosid(nico_sid);
        nicoCookie.setMainM3U8(main_m3u8);

        return new ResultVideoData(videoUrl, audioUrl, true, true, false, new Gson().toJson(nicoCookie));

    }


    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // 送られてきたURLを一旦IDだけにする
        final String id = getId(data.getURL());

        //System.out.println(id);
        // 無駄にアクセスしないようにすでに接続されてたらそれを返す
        ResultVideoData LiveURL = null;

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        String htmlText = "";

        return LiveURL;
    }




    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        String id = getId(data.getURL());
        String title = "";

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        if (!Pattern.compile("lv").matcher(data.getURL()).find()) {
            //System.out.println("video");
            try {
                Request request_html = new Request.Builder()
                        .url("https://www.nicovideo.jp/watch/" + id)
                        .addHeader("User-Agent", UserAgent)
                        .build();
                Response response = client.newCall(request_html).execute();
                if (response.body() != null) {
                    HtmlText = response.body().string();
                }
                response.close();

            } catch (Exception e) {
                return "";
            }
        } else {
            try {
                Request request_html = new Request.Builder()
                        .url("https://live.nicovideo.jp/watch/" + id)
                        .addHeader("User-Agent", UserAgent)
                        .build();
                Response response = client.newCall(request_html).execute();
                if (response.body() != null) {
                    HtmlText = response.body().string();
                }
                response.close();

            } catch (Exception e) {
                return "";
            }
        }

        //System.out.println(HtmlText);
        Matcher matcher = matcher_LdJsonVideo.matcher(HtmlText);

        if (Pattern.compile("lv").matcher(data.getURL()).find()){
            matcher = matcher_LdJsonLive.matcher(HtmlText);
        }

        if (!matcher.find()){
            return "";
        }
        //System.out.println("found");

        String jsonText = "{"+matcher.group(1)+"}";
        jsonText = jsonText.replaceAll("&quot;", "\"");
        //System.out.println(jsonText);

        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
        title = json.getAsJsonObject().get("name").getAsString();


        return title;
    }

    @Override
    public String getServiceName() {
        return "ニコニコ動画";
    }

    @Override
    public String getVersion() {
        return "20240805";
    }

    private String getId(String text){
        if (text.startsWith("sp.nicovideo.jp") || text.startsWith("nicovideo.jp") || text.startsWith("www.nicovideo.jp") || text.startsWith("nico.ms") || text.startsWith("live.nicovideo.jp") || text.startsWith("sp.live.nicovideo.jp")){
            text = "https://"+text;
        }

        // 余計なものは削除
        text = text.replaceAll("http://sp.nicovideo.jp/watch/","").replaceAll("https://sp.nicovideo.jp/watch/","").replaceAll("http://nicovideo.jp/watch/","").replaceAll("https://nicovideo.jp/watch/","").replaceAll("http://www.nicovideo.jp/watch/","").replaceAll("https://www.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","").replaceAll("http://sp.live.nicovideo.jp/watch/","").replaceAll("https://sp.live.nicovideo.jp/watch/","").replaceAll("http://live.nicovideo.jp/watch/","").replaceAll("https://live.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","").split("\\?")[0];

        return text;
    }
}
