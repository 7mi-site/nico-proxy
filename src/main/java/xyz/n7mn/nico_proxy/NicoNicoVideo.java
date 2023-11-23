package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kotlin.Pair;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.n7mn.nico_proxy.data.*;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NicoNicoVideo implements ShareService {

    private final Map<String, ResultVideoData> QueueList = new HashMap<>();
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

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

        // 無駄にアクセスしないようにすでに接続されてたらそれを返す
        ResultVideoData videoData = QueueList.get(id);
        if (videoData != null) {
            return videoData;
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        String nico_sid = "";
        try {
            Request request_html = new Request.Builder()
                    .url("https://www.nicovideo.jp/watch/" + id)
                    .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0 nico-proxy/1.0")
                    //.addHeader("User-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0.1 Mobile/15E148 Safari/604.1 nico-proxy/1.0")
                    //.addHeader("Cookie", "usepc=1;")
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

        Matcher matcher = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">").matcher(HtmlText);
        if (!matcher.find()) {
            throw new Exception("www.nicovideo.jp Not Found");
        }

        String SessionId = null;
        String Token = null;
        String Signature = null;

        String json_text = "";
        Matcher matcher_json = Pattern.compile("<div id=\"js-initial-watch-data\" data-api-data=\"\\{(.*)\\}\" data-environment=\"\\{").matcher(HtmlText);
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

        // domand鯖へアクセスするためのデバッグ用
        /*Request request_html = new Request.Builder()
                .url("https://www.nicovideo.jp/api/watch/v3_guest/"+id+"?_frontendId=3&_frontendVersion=0&actionTrackId="+json.getAsJsonObject().getAsJsonObject("client").get("watchTrackId").getAsString())
                .build();
        Response response = client.newCall(request_html).execute();
        if (response.body() != null){
            json = new Gson().fromJson(response.body().string(), JsonElement.class).getAsJsonObject().getAsJsonObject("data");
        }
        response.close();*/

        if (json_text.isEmpty() || json == null){
            throw new Exception("www.nicovideo.jp Not Found");
        }

        if (json.getAsJsonObject().getAsJsonObject("media").get("domand") != null && !json.getAsJsonObject().getAsJsonObject("media").get("domand").isJsonNull()){
            // domand

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

            RequestBody body = RequestBody.create("{\"outputs\":[[\""+video+"\",\""+audio+"\"]]}", JSON);
            Request request2 = new Request.Builder()
                    .url("https://nvapi.nicovideo.jp/v1/watch/"+id+"/access-rights/hls?actionTrackId="+json.getAsJsonObject().getAsJsonObject("client").get("watchTrackId").getAsString())
                    .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0 nico-proxy/1.0")
                    .addHeader("X-Access-Right-Key", json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("domand").get("accessRightKey").getAsString())
                    .addHeader("X-Frontend-Version", "0")
                    .addHeader("X-Frontend-Id", "3") // 今は暫定でスマホ側の3
                    .addHeader("X-Request-With", "https://www.nicovideo.jp")
                    .post(body)
                    .build();
            Response response2 = client.newCall(request2).execute();

            String policy = "";
            String signature = "";
            String KeyPairId = "";
            if (response2.body() != null){
                for (Pair<? extends String, ? extends String> header : response2.headers()) {
                    //System.out.println(header.component1() + " : " + header.component2());
                    Matcher matcher1 = Pattern.compile("CloudFront-Policy=(.+); expires=(.+); Max-Age=(\\d+); path=(.+); domain=nicovideo\\.jp; priority=Low; secure; HttpOnly").matcher(header.component2());
                    Matcher matcher2 = Pattern.compile("CloudFront-Signature=(.+); expires=(.+); Max-Age=(\\d+); path=(.+); domain=nicovideo\\.jp; priority=Low; secure; HttpOnly").matcher(header.component2());
                    Matcher matcher3 = Pattern.compile("CloudFront-Key-Pair-Id=(.+); expires=(.+); Max-Age=(\\d+); path=(.+); domain=nicovideo\\.jp; priority=Low; secure; HttpOnly").matcher(header.component2());
                    if (matcher1.find()){
                        policy = matcher1.group(1);
                    }
                    if (matcher2.find()){
                        signature = matcher2.group(1);
                    }
                    if (matcher3.find()){
                        KeyPairId = matcher3.group(1);
                    }
                    if (!policy.isEmpty() && !signature.isEmpty() && !KeyPairId.isEmpty()){
                        break;
                    }
                }

                json_text = response2.body().string();
            }
            response2.close();

            //System.out.println(json_text);
            json = new Gson().fromJson(json_text, JsonElement.class);

            // このままだと再生できないのでアクセス時にCookieを渡してあげる必要がある
            String contentUrl = json.getAsJsonObject().getAsJsonObject("data").get("contentUrl").getAsString();
            NicoCookie nicoCookie = new NicoCookie();
            nicoCookie.setCloudFront_Policy(policy);
            nicoCookie.setCloudFront_Signature(signature);
            nicoCookie.setCloudFront_Key_Pair_Id(KeyPairId);
            return new ResultVideoData(contentUrl, "", true, false, false, new Gson().toJson(nicoCookie));
        } else {
            // dmc.nico
            // Tokenデータ
            try {
                Token = json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("delivery").getAsJsonObject("movie").getAsJsonObject("session").get("token").getAsString();
            } catch (Exception e){
                //e.printStackTrace();
                //return null;
            }

            //System.out.println(Token);

            // セッションID
            try {
                JsonElement json1 = new Gson().fromJson(Token, JsonElement.class);
                String player_id = json1.getAsJsonObject().get("player_id").getAsString();
                SessionId = player_id.replaceAll("nicovideo-", "");
            } catch (Exception e){
                //e.printStackTrace();
                //return null;
            }

            // signature
            try {
                Signature = json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("delivery").getAsJsonObject("movie").getAsJsonObject("session").get("signature").getAsString();
            } catch (Exception e){
                //e.printStackTrace();
                //return null;
            }

            StringBuilder video_src = new StringBuilder();
            StringBuilder audio_src = new StringBuilder();
            try {
                JsonArray temp = json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("delivery").getAsJsonObject("movie").getAsJsonObject("session").getAsJsonArray("videos");
                for (int i = 0; i < temp.size(); i++){
                    video_src.append("\"").append(temp.get(i).getAsString()).append("\"");

                    if (i + 1 < temp.size()){
                        video_src.append(",");
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }
            try {
                JsonArray temp = json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("delivery").getAsJsonObject("movie").getAsJsonObject("session").getAsJsonArray("audios");
                for (int i = 0; i < temp.size(); i++){
                    audio_src.append("\"").append(temp.get(i).getAsString()).append("\"");
                    if (i + 1 < temp.size()){
                        audio_src.append(",");
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
                return null;
            }

            //System.out.println(video_src);
            //System.out.println(audio_src);

            boolean isEncrypt = !json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("delivery").get("encryption").isJsonNull();
            if (isEncrypt) {
                //System.out.println("null?");
                String key = json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("delivery").get("trackingId").getAsString();
                //System.out.println(key);
                //System.out.println("https://nvapi.nicovideo.jp/v1/2ab0cbaa/watch?t=" + URLEncoder.encode(key, StandardCharsets.UTF_8));
                Request request_hls = new Request.Builder()
                        .url("https://nvapi.nicovideo.jp/v1/2ab0cbaa/watch?t=" + URLEncoder.encode(key, StandardCharsets.UTF_8))
                        .addHeader("X-Frontend-Id", "6")
                        .addHeader("X-Frontend-Version", "0")
                        .addHeader("Cookie", "nicosid="+nico_sid)
                        .addHeader("X-Niconico-Language", "en-us")
                        .addHeader("X-Request-With", "https://www.nicovideo.jp")
                        .addHeader("Origin", "https://www.nicovideo.jp")
                        .addHeader("Referer", "https://www.nicovideo.jp/")
                        .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0 nico-proxy/1.0")
                        .build();
                Response response_hls = client.newCall(request_hls).execute();
                //System.out.println(response_hls.body().string());
                response_hls.close();
                //System.out.println("Encrypted!");
            }

            String hls_encrypted_key = null;
            String keyUri = "";
            if (isEncrypt){
                try {
                    hls_encrypted_key = json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("delivery").getAsJsonObject("encryption").get("encryptedKey").getAsString();
                    //System.out.println(hls_encrypted_key);
                } catch (Exception e){
                    e.printStackTrace();
                }
                try {
                    keyUri = json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject("delivery").getAsJsonObject("encryption").get("keyUri").getAsString();
                } catch (Exception e){
                    //e.printStackTrace();
                }
            }
            //System.out.println(keyUri);

            String[] split1 = video_src.toString().split(",");
            String[] split2 = audio_src.toString().split(",");

            StringBuilder temp = new StringBuilder();
            for (String s : split1) {
                if (s.startsWith("\"archive_h264_1080p\"") || s.startsWith("\"archive_h264_720p\"")){
                    continue;
                }

                temp.append(s);
                temp.append(",");
            }

            String SendJson = "{\"session\":{\"recipe_id\":\"nicovideo-" + id + "\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":[" + video_src + "],\"audio_src_ids\":[" + split2[0] + "]}},{\"src_id_to_mux\":{\"video_src_ids\":[" + split1[split1.length - 1] + "],\"audio_src_ids\":[" + split2[0] + "]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"hls_parameters\":{\"use_well_known_port\":\"yes\",\"use_ssl\":\"yes\",\"transfer_preset\":\"\",\"segment_duration\":6000,\"encryption\":{\"hls_encryption_v1\":{\"encrypted_key\":\"" + hls_encrypted_key + "\",\"key_uri\":\"" + keyUri + "\"}}}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\"" + Token.replaceAll("\n","").replaceAll("\"","\\\\\"") + "\",\"signature\":\"" + Signature + "\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\"" + SessionId + "\"},\"client_info\":{\"player_id\":\"nicovideo-" + SessionId + "\"},\"priority\":0.2}}";
            if (hls_encrypted_key == null) {
                SendJson = "{\"session\":{\"recipe_id\":\"nicovideo-" + id + "\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":[" + video_src + "],\"audio_src_ids\":[" + split2[0] + "]}},{\"src_id_to_mux\":{\"video_src_ids\":[" + split1[split1.length - 1] + "],\"audio_src_ids\":[" + split2[0] + "]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"hls_parameters\":{\"use_well_known_port\":\"yes\",\"use_ssl\":\"yes\",\"transfer_preset\":\"\",\"segment_duration\":6000}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\"" + Token.replaceAll("\n","").replaceAll("\"","\\\\\"") + "\",\"signature\":\"" + Signature + "\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\"" + SessionId + "\"},\"client_info\":{\"player_id\":\"nicovideo-" + SessionId + "\"},\"priority\":" + (id.startsWith("so") ? "0.2" : "0") + "}}";
            }
            //System.out.println(SendJson);
            //System.out.println(keyUri);

            String ResponseJson = "";
            RequestBody body = RequestBody.create(SendJson, JSON);

            Request request2 = new Request.Builder()
                    .url("https://api.dmc.nico/api/sessions?_format=json")
                    .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0 nico-proxy/1.0")
                    .post(body)
                    .build();


            try {
                Response response2 = client.newCall(request2).execute();
                if (response2.body() != null){
                    ResponseJson = response2.body().string();
                }
                //System.out.println(ResponseJson);
                response2.close();
            } catch (IOException e) {
                if (data.getProxy() != null){
                    throw new Exception("api.dmc.nico" + e.getMessage() + " (Use Proxy : "+data.getProxy().getProxyIP()+")");
                } else {
                    throw new Exception("api.dmc.nico" + e.getMessage());
                }
            }

            // 送られてきたJSONから動画ファイルのURLとハートビート信号用のセッションを取得する
            final String HeartBeatSession;
            final String HeartBeatSessionId;

            // 動画URL
            String VideoURL = null;
            Matcher video_matcher = Pattern.compile("\"content_uri\":\"(.*)\",\"session_operation_auth").matcher(ResponseJson);
            if (video_matcher.find()){
                VideoURL = video_matcher.group(1).replaceAll("\\\\","");
                QueueList.put(id, new ResultVideoData(VideoURL, null, true, hls_encrypted_key != null, false, null));
            }

            // ハートビート信号用 セッション
            Matcher heart_session_matcher = Pattern.compile("\\{\"meta\":\\{\"status\":201,\"message\":\"created\"},\"data\":\\{(.*)\\}").matcher(ResponseJson);
            if (heart_session_matcher.find()){
                HeartBeatSession = "{"+heart_session_matcher.group(1); //.replaceAll("\\\\","");
            } else {
                HeartBeatSession = null;
            }
            // ハートビート信号用ID
            Matcher heart_session_matcher2 = Pattern.compile("\"data\":\\{\"session\":\\{\"id\":\"(.*)\",\"recipe_id\"").matcher(ResponseJson);
            if (heart_session_matcher2.find()){
                HeartBeatSessionId = heart_session_matcher2.group(1).replaceAll("\\\\","");
            } else {
                HeartBeatSessionId = null;
            }

            if (VideoURL == null || HeartBeatSession == null || HeartBeatSessionId == null){
                throw new Exception("api.dmc.nico PostData Error");
            }

            ResultVideoData result;
            if (hls_encrypted_key != null){
                result = new ResultVideoData(VideoURL, null, true, true, false, new Gson().toJson(new EncryptedTokenJSON(keyUri, "https://api.dmc.nico/api/sessions/" + HeartBeatSessionId + "?_format=json&_method=PUT", HeartBeatSession)));
            } else {
                result = new ResultVideoData(VideoURL, null, true, false, false, new Gson().toJson(new TokenJSON("https://api.dmc.nico/api/sessions/" + HeartBeatSessionId + "?_format=json&_method=PUT", HeartBeatSession)));

            }
            QueueList.remove(id);
            QueueList.put(id, result);

            return result;
        }

    }

    private WebSocket webSocket = null;

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // 送られてきたURLを一旦IDだけにする
        final String id = getId(data.getURL());

        //System.out.println(id);
        // 無駄にアクセスしないようにすでに接続されてたらそれを返す
        ResultVideoData LiveURL = QueueList.get(id);
        if (LiveURL != null){
            return LiveURL;
        }
        //System.out.println("a");

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        String htmlText = "";
        try {
            Request request = new Request.Builder()
                    .url("https://live.nicovideo.jp/watch/"+id)
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

        Matcher matcher  = Pattern.compile("webSocketUrl&quot;:&quot;wss://(.*)&quot;,&quot;csrfToken").matcher(htmlText);

        if (!matcher.find()){
            throw new Exception("live.nicovideo.jp No WebSocket Found");
        }

        String websocketURL = "wss://"+matcher.group(1);
        Request request = new Request.Builder()
                .url(websocketURL)
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
                QueueList.remove(id);
                timer.cancel();
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
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
                                    .build();
                            try {
                                Response response = client.newCall(request).execute();
                                if (response.code() == 403 || response.code() == 404){
                                    QueueList.remove(id);
                                    timer.cancel();
                                    webSocket.cancel();
                                    response.close();
                                }
                                response.close();
                            } catch (Exception e){
                                QueueList.remove(id);
                                timer.cancel();
                                webSocket.cancel();
                                //e.printStackTrace();
                            }
                        }
                    }, 30000L, 30000L);
                    //System.out.println("{\"type\":\"keepSeat\"}");
                }

                if (text.startsWith("{\"type\":\"disconnect\"")) {
                    QueueList.remove(id);
                    timer.cancel();
                    webSocket.cancel();
                }

                Matcher matcherData = Pattern.compile("\\{\"type\":\"stream\",\"data\":\\{\"uri\":\"https://").matcher(text);

                if (matcherData.find()) {
                    Matcher matcher = Pattern.compile("\"uri\":\"(.*)\",\"syncUri\":\"").matcher(text);
                    //System.out.println("url get");
                    if (matcher.find()) {
                        //System.out.println("url get ok");
                        temp[0] = matcher.group(1);
                        QueueList.put(id, new ResultVideoData(temp[0], null, true, false, true, null));
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
        Matcher matcher = Pattern.compile("<script type=\"application/ld\\+json\" class=\"LdJson\">\\{(.*)\\}").matcher(HtmlText);

        if (Pattern.compile("lv").matcher(data.getURL()).find()){
            matcher = Pattern.compile("<script type=\"application/ld\\+json\">\\{(.*)\\}").matcher(HtmlText);
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
        return "1.0-20230918";
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
