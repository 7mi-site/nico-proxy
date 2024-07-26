package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import kotlin.Pair;
import okhttp3.*;
import xyz.n7mn.nico_proxy.data.NicoCookie;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class NicoNicoVideoRekari implements ShareService {

    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    private final String UserAgent = Constant.nico_proxy_UserAgent;

    //private final Pattern matcher_VideoTime = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">");
    private final Pattern matcher_Json = Pattern.compile("<div style=\"display:none\" id=\"niconico-tmp-context\" data-niconico-tmp-context=\"\\{(.+)}\"></div>");
    private final Pattern matcher_cookie = Pattern.compile("domand_bid=(.+); expires=(.+); Max-Age=(\\d+); path=(.+); domain=(.+); priority=(.+); secure; HttpOnly");
    private final Pattern matcher_m3u8Audio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"(.+)\",NAME=\"(.+)\",DEFAULT=(.+),URI=\"(.+)\"");

    private final Pattern matcher_live = Pattern.compile("<source type=\"application/x-mpegURL; codecs=&quot;avc1&quot;\" src=\"(.+)\"/></video>");


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
                    .url("https://www.nicovideo.jp/watch_tmp/" + id)
                    .addHeader("User-Agent", UserAgent)
                    .build();
            Response response = client.newCall(request_html).execute();
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

        //System.out.println(HtmlText);

        String json_text = "";
        Matcher matcher_json = matcher_Json.matcher(HtmlText);
        if (matcher_json.find()){
            json_text = "{" + matcher_json.group(1) + "}";
            json_text = json_text.replaceAll("&quot;", "\"").replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">");

            //System.out.println(json_text);

        }

        JsonElement json = null;
        try {
            json = new Gson().fromJson(json_text, JsonElement.class);
        } catch (Exception e){
            //e.printStackTrace();
            //return null;
        }

        if (json_text.isEmpty() || json == null){
            throw new Exception("www.nicovideo.jp Not Found");
        }

        //System.out.println(id);
        final JsonElement firstJson = json;

        String domand_bid = "";

        // https://www.nicovideo.jp/api/watch/tmp/sm982882?_frontendId=6&_frontendVersion=0.0.0
        Request request_api1 = new Request.Builder()
                .url("https://www.nicovideo.jp/api/watch/tmp/"+id+"?_frontendId=6&_frontendVersion=0.0.0")
                .addHeader("User-Agent", UserAgent)
                .addHeader("Host", "www.nicovideo.jp")
                .addHeader("Priority","u=4")
                .addHeader("Referer", "https://www.nicovideo.jp/watch_tmp/" + id)
                .build();
        Response response = client.newCall(request_api1).execute();
        if (response.body() != null){
            HtmlText = response.body().string();
        }
        Headers headers = response.headers();
        for (String value : headers.values("Set-Cookie")) {
            //System.out.println(value);
        }

        response.close();

        json = new Gson().fromJson(HtmlText, JsonElement.class);

        nico_sid = json.getAsJsonObject().get("data").getAsJsonObject().get("client").getAsJsonObject().get("nicosid").getAsString();
        String trackId = json.getAsJsonObject().get("data").getAsJsonObject().get("client").getAsJsonObject().get("watchTrackId").getAsString();

        // {"outputs":[["video-h264-360p","audio-aac-128kbps"],["video-h264-360p-lowest","audio-aac-128kbps"]]}
        // {"outputs":[["video-h264-360p","audio-aac-128kbps"],["video-h264-360p-lowest","audio-aac-128kbps"]]}
        StringBuilder postJson = new StringBuilder("{\"outputs\":[[");

        JsonArray array = json.getAsJsonObject().get("data").getAsJsonObject().get("media").getAsJsonObject().get("domand").getAsJsonObject().get("videos").getAsJsonArray();
        JsonArray array2 = json.getAsJsonObject().get("data").getAsJsonObject().get("media").getAsJsonObject().get("domand").getAsJsonObject().get("audios").getAsJsonArray();
        for (JsonElement element : array) {
            //System.out.println(element.getAsJsonObject().get("id").getAsString());
            if (!element.getAsJsonObject().get("id").getAsString().equals("video-h264-1080p")){
                postJson.append(element.getAsJsonObject().get("id")).append(",").append(array2.get(0).getAsJsonObject().get("id")).append("],[");
            }
        }
        String post = postJson.substring(0, postJson.length() - 2) + "]}";

        //System.out.println(post);

        RequestBody body = RequestBody.create(post, JSON);
        Request request_api2 = new Request.Builder()
                .url("https://nvapi.nicovideo.jp/v1/tmp/watch/"+id+"/access-rights/hls?actionTrackId="+trackId+"&_frontendId=6&_frontendVersion=0.0.0")
                .addHeader("User-Agent", UserAgent)
                .addHeader("Cookie", "nico_sid="+nico_sid)
                .addHeader("X-Request-With","https://www.nicovideo.jp")
                .addHeader("X-Access-Right-Key", json.getAsJsonObject().get("data").getAsJsonObject().get("media").getAsJsonObject().get("domand").getAsJsonObject().get("accessRightKey").getAsString())
                .post(body)
                .build();
        Response response2 = client.newCall(request_api2).execute();
        if (response2.body() != null){
            HtmlText = response2.body().string();
        }
        for (Pair<? extends String, ? extends String> header : response2.headers()) {
            //System.out.println(header.component1() + " : " + header.component2());
            Matcher matcher1 = matcher_cookie.matcher(header.component2());
            if (matcher1.find()){
                //System.out.println(header.component1() + " : " + header.component2());
                domand_bid = matcher1.group(1);
                break;
            }
        }
        response2.close();

        //System.out.println(HtmlText);
        json = new Gson().fromJson(HtmlText, JsonElement.class);

        // このままだと再生できないのでアクセス時にCookieを渡してあげる必要がある
        String contentUrl = json.getAsJsonObject().getAsJsonObject("data").get("contentUrl").getAsString();

        //System.out.println(contentUrl);
        Request request_m3u8 = new Request.Builder()
                .url(contentUrl)
                .addHeader("Cookie", "nicosid="+nico_sid+"; domand_bid=\"" + domand_bid+"\"")
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

    private WebSocket webSocket = null;

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        // https://live.nicovideo.jp/rekari/kl1
        Request request = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", UserAgent)
                .build();
        Response response = client.newCall(request).execute();

        ResultVideoData videoData = null;
        if (response.body() != null){

            Matcher matcher1 = matcher_live.matcher(response.body().string());
            if (matcher1.find()){
                videoData = new ResultVideoData(matcher1.group(1), null, true, false, true, null);
            } else {
                response.close();
                throw new Exception("Not Found");
            }

        }
        response.close();

        return videoData;
    }



    private final Pattern matcher_videoURL = Pattern.compile("https://www\\.nicovideo\\.jp/watch_tmp/(.+)");
    private final Pattern matcher_liveURL = Pattern.compile("https://live\\.nicovideo\\.jp/rekari/(.+)");

    private final Pattern matcher_liveTitle = Pattern.compile("<meta property=\"og:title\" content=\"(.+)\"/>");

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        String id = getId(data.getURL());
        String title = "";

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        if (matcher_videoURL.matcher(data.getURL()).find()){

            String HtmlText = "";
            try {
                Request request_html = new Request.Builder()
                        .url("https://www.nicovideo.jp/watch_tmp/" + id)
                        .addHeader("User-Agent", UserAgent)
                        .build();
                Response response = client.newCall(request_html).execute();
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

            //System.out.println(HtmlText);

            String json_text = "";
            Matcher matcher_json = matcher_Json.matcher(HtmlText);
            if (matcher_json.find()){
                json_text = "{" + matcher_json.group(1) + "}";
                json_text = json_text.replaceAll("&quot;", "\"").replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">");

                //System.out.println(json_text);

            }

            JsonElement json = null;
            try {
                json = new Gson().fromJson(json_text, JsonElement.class);
            } catch (Exception e){
                //e.printStackTrace();
                //return null;
            }

            if (json_text.isEmpty() || json == null){
                throw new Exception("www.nicovideo.jp Not Found");
            }

            //System.out.println(id);

            // https://www.nicovideo.jp/api/watch/tmp/sm982882?_frontendId=6&_frontendVersion=0.0.0
            Request request_api1 = new Request.Builder()
                    .url("https://www.nicovideo.jp/api/watch/tmp/"+id+"?_frontendId=6&_frontendVersion=0.0.0")
                    .addHeader("User-Agent", UserAgent)
                    .addHeader("Host", "www.nicovideo.jp")
                    .addHeader("Priority","u=4")
                    .addHeader("Referer", "https://www.nicovideo.jp/watch_tmp/" + id)
                    .build();
            Response response = client.newCall(request_api1).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }

            json = new Gson().fromJson(HtmlText, JsonElement.class);

            title = json.getAsJsonObject().get("data").getAsJsonObject().get("video").getAsJsonObject().get("title").getAsString();

        } else if (matcher_liveURL.matcher(data.getURL()).find()) {

            Request request = new Request.Builder()
                    .url(data.getURL())
                    .addHeader("User-Agent", UserAgent)
                    .build();
            Response response = client.newCall(request).execute();

            if (response.body() != null){

                Matcher matcher1 = matcher_liveTitle.matcher(response.body().string());
                if (matcher1.find()){
                    title = matcher1.group(1);
                } else {
                    response.close();
                    throw new Exception("Not Found");
                }

            }

        }


        return title;
    }

    @Override
    public String getServiceName() {
        return "ニコニコ動画(Re:仮)";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    private String getId(String text){
        if (text.startsWith("sp.nicovideo.jp") || text.startsWith("nicovideo.jp") || text.startsWith("www.nicovideo.jp") || text.startsWith("nico.ms") || text.startsWith("live.nicovideo.jp") || text.startsWith("sp.live.nicovideo.jp")){
            text = "https://"+text;
        }

        Matcher matcher1 = matcher_videoURL.matcher(text);

        if (matcher1.find()){
            text = matcher1.group(1).split("\\?")[0];
        }

        return text;
    }
}
