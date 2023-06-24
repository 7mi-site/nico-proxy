package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.n7mn.nico_proxy.data.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
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
        try {
            Request request_html = new Request.Builder()
                    .url("https://www.nicovideo.jp/watch/" + id)
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

        Matcher matcher = Pattern.compile("<meta property=\"video:duration\" content=\"(\\d+)\">").matcher(HtmlText);
        if (!matcher.find()) {
            throw new Exception("www.nicovideo.jp Not Found");
        }

        String SessionId = null;
        String Token = null;
        String Signature = null;

        // セッションID
        Matcher matcher1 = Pattern.compile("player_id\\\\&quot;:\\\\&quot;nicovideo-(.*)\\\\&quot;,\\\\&quot;recipe_id").matcher(HtmlText);

        if (matcher1.find()) {
            SessionId = matcher1.group(1);
            //System.out.println("[Debug] セッションID : "+SessionId+" "+sdf.format(new Date()));
        }

        // Tokenデータ
        Matcher matcher2 = Pattern.compile("\\{\\\\&quot;service_id\\\\&quot;:\\\\&quot;nicovideo\\\\&quot;(.*)\\\\&quot;transfer_presets\\\\&quot;:\\[\\]\\}").matcher(HtmlText);
        if (matcher2.find()) {
            Token = matcher2.group().replaceAll("\\\\", "").replaceAll("&quot;", "\"").replaceAll("\"", "\\\\\"");
            //System.out.println("[Debug] TokenData : \n"+Token+"\n"+ sdf.format(new Date()));
        }

        // signature
        Matcher matcher3 = Pattern.compile("signature&quot;:&quot;(.*)&quot;,&quot;contentId").matcher(HtmlText);
        if (matcher3.find()) {
            Signature = matcher3.group(1);
            //System.out.println("[Debug] signature : "+Signature+" "+ sdf.format(new Date()));
        }

        if (SessionId == null && Token == null && Signature == null) {
            throw new Exception("www.nicovideo.jp Not Found SessionData");
        }


        Matcher matcher_video = Pattern.compile("&quot;,&quot;videos&quot;:\\[(.*)\\],&quot;audios").matcher(HtmlText);
        Matcher matcher_audio = Pattern.compile("&quot;audios\\\\&quot;:\\[(.*)\\],\\\\&quot;movies").matcher(HtmlText);
        String video_src = "";
        String audio_src = "";
        if (matcher_video.find()) {
            video_src = matcher_video.group(1).replaceAll("&quot;", "\"");
        }
        if (matcher_audio.find()) {
            audio_src = matcher_audio.group(1).replaceAll("&quot;", "\"").replaceAll("\\\\", "");
        }

        Matcher matcher_hls1 = Pattern.compile("hls_encrypted_key\\\\&quot;:\\\\&quot;(.*)\\\\&quot;}&quot;,&quot;signature").matcher(HtmlText);
        Matcher matcher_hls2 = Pattern.compile("&quot;keyUri&quot;:&quot;(.*)&quot;},&quot;movie").matcher(HtmlText);
        Matcher matcher_hls3 = Pattern.compile(",&quot;token&quot;:&quot;(.*)&quot;,&quot;signature&quot;:&quot;").matcher(HtmlText);
        Matcher matcher_hls4 = Pattern.compile(",&quot;trackingId&quot;:&quot;(.*)&quot;},&quot;deliveryLegac").matcher(HtmlText);

        if (matcher_hls4.find()) {
            String key = matcher_hls4.group(1).replaceAll("\\\\","");
            //System.out.println(key);

            Request request_hls = new Request.Builder()
                    .url("https://nvapi.nicovideo.jp/v1/2ab0cbaa/watch?t=" + key)
                    .addHeader("X-Frontend-Id", "6")
                    .addHeader("X-Frontend-Version", "0")
                    .addHeader("X-Request-With", "https://www.nicovideo.jp")
                    .addHeader("Access-Control-Request-Headers", "x-frontend-id,x-frontend-version")
                    .addHeader("Access-Control-Request-Method", "GET")
                    .addHeader("Origin", "https://www.nicovideo.jp")
                    .addHeader("Referer", "https://www.nicovideo.jp/watch/"+id)
                    .build();
            Response response_hls = client.newCall(request_hls).execute();
            //System.out.println(response_hls.body().string());
            response_hls.close();

        }

        String hls_encrypted_key = null;
        if (matcher_hls1.find()){
            hls_encrypted_key = matcher_hls1.group(1).replaceAll("\\\\","");
        }
        String keyUri = "";
        if (matcher_hls2.find()){
            keyUri = matcher_hls2.group(1).replaceAll("\\\\","").replaceAll("&amp;","&");
        }
        //System.out.println(keyUri);

        if (matcher_hls3.find()) {
            Token = matcher_hls3.group(1).replaceAll("&quot;", "\"");
        }

        String[] split1 = video_src.split(",");
        String[] split2 = audio_src.split(",");

        StringBuilder temp = new StringBuilder();
        for (String s : split1) {
            if (s.startsWith("\"archive_h264_1080p\"") || s.startsWith("\"archive_h264_720p\"")){
                continue;
            }

            temp.append(s);
            temp.append(",");
        }
        String video_src2 = temp.substring(0, temp.length() - 1);
        //System.out.println("video_src3 : " + video_src3);

        String SendJson = "{\"session\":{\"recipe_id\":\"nicovideo-"+id+"\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":["+video_src+"],\"audio_src_ids\":["+split2[0]+"]}},{\"src_id_to_mux\":{\"video_src_ids\":["+split1[split1.length - 1]+"],\"audio_src_ids\":["+split2[0]+"]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"hls_parameters\":{\"use_well_known_port\":\"yes\",\"use_ssl\":\"yes\",\"transfer_preset\":\"\",\"segment_duration\":6000,\"encryption\":{\"hls_encryption_v1\":{\"encrypted_key\":\""+hls_encrypted_key+"\",\"key_uri\":\""+keyUri+"\"}}}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\""+Token+"\",\"signature\":\""+Signature+"\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\""+SessionId+"\"},\"client_info\":{\"player_id\":\"nicovideo-"+SessionId+"\"},\"priority\":0.2}}";
        if (hls_encrypted_key == null) {
            SendJson = "{\"session\":{\"recipe_id\":\"nicovideo-" + id + "\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":[" + video_src + "],\"audio_src_ids\":[" + split2[0] + "]}},{\"src_id_to_mux\":{\"video_src_ids\":[" + (id.startsWith("so") ? video_src2 : split1[split1.length - 1]) + "],\"audio_src_ids\":[" + split2[0] + "]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"hls_parameters\":{\"use_well_known_port\":\"yes\",\"use_ssl\":\"yes\",\"transfer_preset\":\"\",\"segment_duration\":6000}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\"" + Token + "\",\"signature\":\"" + Signature + "\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\"" + SessionId + "\"},\"client_info\":{\"player_id\":\"nicovideo-" + SessionId + "\"},\"priority\":" + (id.startsWith("so") ? "0.2" : "0") + "}}";
            // {"session":{"recipe_id":"nicovideo-sm*****","content_id":"out1","content_type":"movie","content_src_id_sets":[{"content_src_ids":[{"src_id_to_mux":{"video_src_ids":["archive_h264_360p","archive_h264_360p_low"],"audio_src_ids":["archive_aac_64kbps"]}},{"src_id_to_mux":{"video_src_ids":["archive_h264_360p_low"],"audio_src_ids":["archive_aac_64kbps"]}}]}],"timing_constraint":"unlimited","keep_method":{"heartbeat":{"lifetime":120000}},"protocol":{"name":"http","parameters":{"http_parameters":{"parameters":{"hls_parameters":{"use_well_known_port":"yes","use_ssl":"yes","transfer_preset":"","segment_duration":6000}}}}},"content_uri":"","session_operation_auth":{"session_operation_auth_by_signature":{"token":"{\"service_id\":\"nicovideo\",\"player_id\":\"nicovideo-6-h9V9x02JtS_1685101570190\",\"recipe_id\":\"nicovideo-sm500873\",\"service_user_id\":\"6-h9V9x02JtS_1685101570190\",\"protocols\":[{\"name\":\"http\",\"auth_type\":\"ht2\"},{\"name\":\"hls\",\"auth_type\":\"ht2\"}],\"videos\":[\"archive_h264_360p\",\"archive_h264_360p_low\"],\"audios\":[\"archive_aac_64kbps\"],\"movies\":[],\"created_time\":1685101570000,\"expire_time\":1685187970000,\"content_ids\":[\"out1\"],\"heartbeat_lifetime\":120000,\"content_key_timeout\":600000,\"priority\":0,\"transfer_presets\":[]}","signature":"491ecff65d053d7a46976f17c85c291e43a3d845f3c2d59b277712edf25953af"}},"content_auth":{"auth_type":"ht2","content_key_timeout":600000,"service_id":"nicovideo","service_user_id":"6-h9V9x02JtS_1685101570190"},"client_info":{"player_id":"nicovideo-6-h9V9x02JtS_1685101570190"},"priority":0}}
            // {"session":{"recipe_id":"nicovideo-so*****","content_id":"out1","content_type":"movie","content_src_id_sets":[{"content_src_ids":[{"src_id_to_mux":{"video_src_ids":["archive_h264_720p","archive_h264_480p","archive_h264_360p","archive_h264_360p_low"],"audio_src_ids":["archive_aac_192kbps"]}},{"src_id_to_mux":{"video_src_ids":["archive_h264_480p","archive_h264_360p","archive_h264_360p_low"],"audio_src_ids":["archive_aac_192kbps"]}},{"src_id_to_mux":{"video_src_ids":["archive_h264_360p","archive_h264_360p_low"],"audio_src_ids":["archive_aac_192kbps"]}},{"src_id_to_mux":{"video_src_ids":["archive_h264_360p_low"],"audio_src_ids":["archive_aac_192kbps"]}}]}],"timing_constraint":"unlimited","keep_method":{"heartbeat":{"lifetime":120000}},"protocol":{"name":"http","parameters":{"http_parameters":{"parameters":{"hls_parameters":{"use_well_known_port":"yes","use_ssl":"yes","transfer_preset":"","segment_duration":6000}}}}},"content_uri":"","session_operation_auth":{"session_operation_auth_by_signature":{"token":"{\"service_id\":\"nicovideo\",\"player_id\":\"nicovideo-6-1V9qneiCAS_1685678390581\",\"recipe_id\":\"nicovideo-so41603987\",\"service_user_id\":\"6-1V9qneiCAS_1685678390581\",\"protocols\":[{\"name\":\"http\",\"auth_type\":\"ht2\"},{\"name\":\"hls\",\"auth_type\":\"ht2\"}],\"videos\":[\"archive_h264_360p\",\"archive_h264_360p_low\",\"archive_h264_480p\",\"archive_h264_720p\"],\"audios\":[\"archive_aac_192kbps\",\"archive_aac_64kbps\"],\"movies\":[],\"created_time\":1685678390000,\"expire_time\":1685764790000,\"content_ids\":[\"out1\"],\"heartbeat_lifetime\":120000,\"content_key_timeout\":600000,\"priority\":0.200000000000000011102230246251565404236316680908203125,\"transfer_presets\":[]}","signature":"91dadc832a556c382408f79620455c3a143e45fb1366766afd38f6922646a049"}},"content_auth":{"auth_type":"ht2","content_key_timeout":600000,"service_id":"nicovideo","service_user_id":"6-1V9qneiCAS_1685678390581"},"client_info":{"player_id":"nicovideo-6-1V9qneiCAS_1685678390581"},"priority":0.2}}
        }
        //System.out.println(SendJson);
        //System.out.println(keyUri);

        String ResponseJson = "";
        RequestBody body = RequestBody.create(SendJson, JSON);

        Request request2 = new Request.Builder()
                .url("https://api.dmc.nico/api/sessions?_format=json")
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
    public String getServiceName() {
        return "ニコニコ動画";
    }

    @Override
    public String getVersion() {
        return "1.0-20230602";
    }

    private String getId(String text){
        if (text.startsWith("sp.nicovideo.jp") || text.startsWith("nicovideo.jp") || text.startsWith("www.nicovideo.jp") || text.startsWith("nico.ms") || text.startsWith("live.nicovideo.jp") || text.startsWith("sp.live.nicovideo.jp")){
            text = "https://"+text;
        }

        // 余計なものは削除
        text = text.replaceAll("http://nextnex.com/\\?url=","").replaceAll("https://nextnex.com/\\?url=","").replaceAll("nextnex.com/\\?url=","");
        text = text.replaceAll("http://nico.7mi.site/proxy/\\?","").replaceAll("https://nico.7mi.site/proxy/\\?","").replaceAll("nico.7mi.site/proxy/\\?","");
        text = text.replaceAll("http://sp.nicovideo.jp/watch/","").replaceAll("https://sp.nicovideo.jp/watch/","").replaceAll("http://nicovideo.jp/watch/","").replaceAll("https://nicovideo.jp/watch/","").replaceAll("http://www.nicovideo.jp/watch/","").replaceAll("https://www.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","").replaceAll("http://sp.live.nicovideo.jp/watch/","").replaceAll("https://sp.live.nicovideo.jp/watch/","").replaceAll("http://live.nicovideo.jp/watch/","").replaceAll("https://live.nicovideo.jp/watch/","").replaceAll("http://nico.ms/","").replaceAll("https://nico.ms/","").split("\\?")[0];

        return text;
    }
}
