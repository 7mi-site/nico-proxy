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

    private WebSocket webSocket = null;

    private final Pattern matcher_WebsocketURL = Pattern.compile("webSocketUrl&quot;:&quot;wss://(.*)&quot;,&quot;csrfToken");
    private final Pattern matcher_WebsocketData1 = Pattern.compile("\\{\"type\":\"stream\",\"data\":\\{\"uri\":\"https://");
    private final Pattern matcher_WebsocketData2 = Pattern.compile("\"uri\":\"(.*)\",\"syncUri\":\"");

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
        String title = "";



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
