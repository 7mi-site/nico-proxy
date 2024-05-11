package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicoNicoVideo implements ShareService {

    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    private final String UserAgent = Constant.nico_proxy_UserAgent;

    private final Pattern matcher_VideoTime = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">");
    private final Pattern matcher_Json = Pattern.compile("<div id=\"js-initial-watch-data\" data-api-data=\"\\{(.*)\\}\" data-environment=\"\\{");
    private final Pattern matcher_cookie = Pattern.compile("domand_bid=(.+); expires=(.+); Max-Age=(\\d+); path=(.+); domain=(.+); priority=(.+); secure; HttpOnly");
    private final Pattern matcher_m3u8Audio = Pattern.compile("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"(.+)\",NAME=\"(.+)\",DEFAULT=(.+),URI=\"(.+)\"");
    private final Pattern matcher_VideoURL = Pattern.compile("\"content_uri\":\"(.*)\",\"session_operation_auth");
    private final Pattern matcher_Session = Pattern.compile("\\{\"meta\":\\{\"status\":201,\"message\":\"created\"},\"data\":\\{(.*)\\}");
    private final Pattern matcher_SessionID = Pattern.compile("\"data\":\\{\"session\":\\{\"id\":\"(.*)\",\"recipe_id\"");

    private final Pattern matcher_WebsocketURL = Pattern.compile("webSocketUrl&quot;:&quot;wss://(.*)&quot;,&quot;csrfToken");
    private final Pattern matcher_WebsocketData1 = Pattern.compile("\\{\"type\":\"stream\",\"data\":\\{\"uri\":\"https://");
    private final Pattern matcher_WebsocketData2 = Pattern.compile("\"uri\":\"(.*)\",\"syncUri\":\"");

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

        Matcher matcher = matcher_VideoTime.matcher(HtmlText);
        if (!matcher.find()) {
            throw new Exception("www.nicovideo.jp Not Found");
        }

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

        final JsonElement firstJson = json;

        String video = "";
        String audio = "";

        for (JsonElement videos : json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("domand").getAsJsonArray("videos")) {
            if (videos.getAsJsonObject().get("isAvailable").getAsBoolean()){
                video = videos.getAsJsonObject().get("id").getAsString();
                break;
            }
        }

        for (JsonElement audios : json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("domand").getAsJsonArray("audios")) {
            if (audios.getAsJsonObject().get("isAvailable").getAsBoolean()){
                audio = audios.getAsJsonObject().get("id").getAsString();
                break;
            }
        }

        RequestBody body2 = RequestBody.create("{\"outputs\":[[\""+video+"\",\""+audio+"\"]]}", JSON);
        //System.out.println("{\"outputs\":[[\""+video+"\",\""+audio+"\"]]}");
        Request request3 = new Request.Builder()
                .url("https://nvapi.nicovideo.jp/v1/watch/"+id+"/access-rights/hls?actionTrackId="+json.getAsJsonObject().getAsJsonObject("client").get("watchTrackId").getAsString())
                .addHeader("User-agent", UserAgent)
                .addHeader("X-Access-Right-Key", json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("domand").get("accessRightKey").getAsString())
                .addHeader("X-Frontend-Version", "0")
                .addHeader("X-Frontend-Id", "6")
                .addHeader("X-Request-With", "https://www.nicovideo.jp")
                .post(body2)
                .build();
        Response response3 = client.newCall(request3).execute();

        String domand_bid = "";
        if (response3.body() != null){
            for (Pair<? extends String, ? extends String> header : response3.headers()) {
                //System.out.println(header.component1() + " : " + header.component2());
                Matcher matcher1 = matcher_cookie.matcher(header.component2());
                if (matcher1.find()){
                    //System.out.println(header.component1() + " : " + header.component2());
                    domand_bid = matcher1.group(1);
                    break;
                }
            }

            //System.out.println(domand_bid);
            json_text = response3.body().string();
        }
        response3.close();

        //System.out.println(json_text);
        json = new Gson().fromJson(json_text, JsonElement.class);

        // このままだと再生できないのでアクセス時にCookieを渡してあげる必要がある
        String contentUrl = json.getAsJsonObject().getAsJsonObject("data").get("contentUrl").getAsString();

        //System.out.println(contentUrl);
        Request request_m3u8 = new Request.Builder()
                .url(contentUrl)
                .addHeader("Cookie", "nicosid="+nico_sid+"; domand_bid=" + domand_bid)
                .addHeader("User-Agent", UserAgent)
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

        ResultVideoData result = new ResultVideoData(videoUrl, audioUrl, true, true, false, new Gson().toJson(nicoCookie));
        return result;

    }

    private WebSocket webSocket = null;

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // 送られてきたURLを一旦IDだけにする
        final String id = getId(data.getURL());

        //System.out.println(id);
        // 無駄にアクセスしないようにすでに接続されてたらそれを返す
        ResultVideoData LiveURL;

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        String htmlText = "";
        try {
            Request request = new Request.Builder()
                    .url("https://live.nicovideo.jp/watch/"+id)
                    .addHeader("User-Agent", UserAgent)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.body() != null){
                htmlText = response.body().string();
            }
            response.close();

        } catch (Exception e) {
            if (data.getProxy() != null){
                throw new Exception("live.nicovideo.jp " + e.getMessage() + " (Use Proxy : " + data.getProxy().getProxyIP()+")");
            } else {
                throw new Exception("live.nicovideo.jp " + e.getMessage());
            }
        }

        Matcher matcher  = matcher_WebsocketURL.matcher(htmlText);

        if (!matcher.find()){
            throw new Exception("live.nicovideo.jp No WebSocket Found");
        }

        String websocketURL = "wss://"+matcher.group(1);
        Request request = new Request.Builder()
                .url(websocketURL)
                .addHeader("User-Agent", UserAgent)
                .build();

        String[] temp = new String[]{"wait", ""};
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            private final Timer timer = new Timer();
            private String liveUrl = "";

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                /*
                super.onClosed(webSocket, code, reason);
                System.out.println("---- reason text ----");
                System.out.println(reason);
                System.out.println("---- reason text ----");
                 */
                timer.cancel();
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                timer.cancel();
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                timer.cancel();
                super.onFailure(webSocket, t, response);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                //System.out.println("----");
                //System.out.println(text);

                if (text.startsWith("{\"type\":\"serverTime\",\"data\":{")) {
                    webSocket.send("{\"type\":\"getEventState\",\"data\":{}}");
                    //System.out.println("{\"type\":\"getEventState\",\"data\":{}}");
                }
                if (text.startsWith("{\"type\":\"eventState\",\"data\":{\"commentState\":{\"locked\":false,\"layout\":\"normal\"}}}")) {
                    webSocket.send("{\"type\":\"getAkashic\",\"data\":{\"chasePlay\":false}}");
                    //System.out.println("{\"type\":\"getAkashic\",\"data\":{\"chasePlay\":false}}");
                }

                if (text.equals("{\"type\":\"ping\"}")) {
                    webSocket.send("{\"type\":\"pong\"}");
                }

                if (text.startsWith("{\"type\":\"seat\",\"data\":{\"keepIntervalSec\":30}}")) {

                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            webSocket.send("{\"type\":\"keepSeat\"}");
                            //System.out.println("{\"type\":\"keepSeat\"}");
                            Request request = new Request.Builder()
                                    .url(liveUrl)
                                    .addHeader("User-Agent", UserAgent)
                                    .build();
                            try {
                                Response response = client.newCall(request).execute();
                                if (response.code() == 403 || response.code() == 404){
                                    timer.cancel();
                                    webSocket.cancel();
                                    response.close();
                                }
                                response.close();
                            } catch (Exception e){
                                timer.cancel();
                                webSocket.cancel();
                                //e.printStackTrace();
                            }
                        }
                    }, 30000L, 30000L);
                    //System.out.println("{\"type\":\"keepSeat\"}");
                }

                if (text.startsWith("{\"type\":\"disconnect\"")) {
                    timer.cancel();
                    webSocket.cancel();
                }

                Matcher matcherData = matcher_WebsocketData1.matcher(text);

                if (matcherData.find()) {
                    Matcher matcher = matcher_WebsocketData2.matcher(text);
                    //System.out.println("url get");
                    if (matcher.find()) {
                        //System.out.println("url get ok");
                        temp[0] = matcher.group(1);
                        liveUrl = temp[0];
                    } else {
                        temp[0] = "Error";
                        webSocket.cancel();
                    }

                    //System.out.println(temp[0]);
                }

                //System.out.println("----");
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {

            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                //System.out.println("websocket open");
                webSocket.send("{\"type\":\"startWatching\",\"data\":{\"stream\":{\"quality\":\"abr\",\"protocol\":\"hls\",\"latency\":\"low\",\"chasePlay\":false},\"room\":{\"protocol\":\"webSocket\",\"commentable\":true},\"reconnect\":false}}");
            }
        });
        while (temp[0].startsWith("wait")){
            temp[1] = temp[0];
        }

        LiveURL = new ResultVideoData(temp[0], null, true, false, true, null);

        //System.out.println("t : "+temp[0]);
        //System.out.println("l : "+LiveURL);

        if (temp[0].equals("Error")){
            throw new Exception("live.nicovideo.jp Not m3u8 URL Found");
        }

        return LiveURL;
    }

    public void cancelWebSocket() {
        if (webSocket != null){
            webSocket.cancel();
        }
    }

    private boolean SendHeartBeatVideo(String HeartBeatSession, String HeartBeatSessionId, ProxyData proxy){
        System.gc();
        final OkHttpClient client = proxy != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getProxyIP(), proxy.getPort()))).build() : new OkHttpClient();

        RequestBody body = RequestBody.create(HeartBeatSession, JSON);
        Request request = new Request.Builder()
                .url("https://api.dmc.nico/api/sessions/" + HeartBeatSessionId + "?_format=json&_method=PUT")
                .post(body)
                .addHeader("User-Agent", UserAgent)
                .build();
        try {
            Response response = client.newCall(request).execute();
            //System.out.println(response.body().string());
            response.close();

            return true;
        } catch (IOException e) {
            // e.printStackTrace();
            return false;
        }
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
        return "20240511";
    }

    private String getId(String text){
        if (text.startsWith("sp.nicovideo.jp") || text.startsWith("nicovideo.jp") || text.startsWith("www.nicovideo.jp") || text.startsWith("nico.ms") || text.startsWith("live.nicovideo.jp") || text.startsWith("sp.live.nicovideo.jp")){
            text = "https://"+text;
        }

        // 余計なものは削除
        text = text.replaceAll("http://nextnex.com/\\?url=","").replaceAll("https://nextnex.com/\\?url=","").replaceAll("nextnex.com/\\?url=","");
        text = text.replaceAll("http://nico.7mi.site/proxy/\\?","").replaceAll("https://nico.7mi.site/proxy/\\?","").replaceAll("nico.7mi.site/proxy/\\?","");
        text = text.replaceAll("http://nicovrc.net/proxy/\\?","").replaceAll("https://nicovrc.net/proxy/\\?","").replaceAll("nicovrc.net/proxy/\\?","");
        text = text.replaceAll("http://sp.nicovideo.jp/watch/","").replaceAll("https://sp.nicovideo.jp/watch/","").replaceAll("http://nicovideo.jp/watch/","").replaceAll("https://nicovideo.jp/watch/","").replaceAll("http://www.nicovideo.jp/watch/","").replaceAll("https://www.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","").replaceAll("http://sp.live.nicovideo.jp/watch/","").replaceAll("https://sp.live.nicovideo.jp/watch/","").replaceAll("http://live.nicovideo.jp/watch/","").replaceAll("https://live.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","").split("\\?")[0];

        return text;
    }
}
