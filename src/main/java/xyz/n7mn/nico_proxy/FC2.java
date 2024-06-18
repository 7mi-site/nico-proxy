package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FC2 implements ShareService {

    private final Pattern matcher_SupportURL1 = Pattern.compile("https://video\\.fc2\\.com/(.+)/content/(.+)");
    private final Pattern matcher_SupportURL2 = Pattern.compile("https://video\\.fc2\\.com/content/(.+)");
    private final Pattern matcher_SupportURL3 = Pattern.compile("https://live\\.fc2\\.com");
    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://video.fc2.com/ja/content/20240504hww75Nv3
        // https://video.fc2.com/content/20240504hww75Nv3
        Matcher matcher1 = matcher_SupportURL1.matcher(data.getURL());
        Matcher matcher2 = matcher_SupportURL2.matcher(data.getURL());

        final String id;
        if (!matcher1.find() && !matcher2.find()){
            throw new Exception("Not Support URL");
        } else if (matcher1.find()){
            id = matcher1.group(2).split("\\?")[0];
        } else{
            id = matcher2.group(1).split("\\?")[0];
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            // https://video.fc2.com/api/v3/videoplaylist/20240504hww75Nv3?sh=1&fs=0
            Request request_html = new Request.Builder()
                    .url("https://video.fc2.com/api/v3/videoplaylist/"+id+"?sh=1&fs=0")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();

        } catch (Exception e) {
            if (data.getProxy() != null) {
                throw new Exception("video.fc2.com " + e.getMessage() + " (Use Proxy : " + data.getProxy().getProxyIP() + ")");
            } else {
                throw new Exception("video.fc2.com " + e.getMessage());
            }
        }

        JsonElement json = new Gson().fromJson(HtmlText, JsonElement.class);
        //System.out.println(json);
        JsonElement json1 = json.getAsJsonObject().get("playlist");

        String uri = "";
        if (json1.getAsJsonObject().has("hq")){
            uri = json1.getAsJsonObject().get("hq").getAsString();
        } else if (json1.getAsJsonObject().has("nq")){
            uri = json1.getAsJsonObject().get("nq").getAsString();
        } else if (json1.getAsJsonObject().has("lq")){
            uri = json1.getAsJsonObject().get("lq").getAsString();
        } else {
            throw new Exception("Not Support URL");
        }


        // https://video.fc2.com/api/v3/videoplay/2yISIxC3xx33S2yFCF8FH-2Sa3x-3-y-S74TFjah/2?signature=WTI_11JRLSPRRN2WMPCLMVENL-0CC.AZJIX--6PJM403FQMS5P4F-8T&t=1718705999&0ks34t=0&pf=Win32&lg=ja&referrer=


        return new ResultVideoData("https://video.fc2.com"+uri, null, true,false,false,null);
    }

    private WebSocket webSocket = null;
    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        // https://live.fc2.com/adult/15233018/?afid=87602906
        // https://live.fc2.com/15233018/

        String id = "";
        for (String str : data.getURL().split("/")){
            try {
                if (Long.parseLong(str) >= 0){
                    id = str;
                }
            } catch (Exception e){

            }
        }

        // https://live.fc2.com/api/memberApi.php
        //
        //System.out.println("id : "+ id);
        //RequestBody body1 = RequestBody.create("channel=1\nprofile=1\nuser=1\nstreamid="+id, MediaType.get("text/plain; charset=utf-8"));
        RequestBody body1 = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("channel", null, RequestBody.create("1", MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("profile", null, RequestBody.create("1", MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("user", null, RequestBody.create("1", MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("streamid", null, RequestBody.create(id, MediaType.get("text/plain; charset=utf-8")))
                .build();

        Request request1 = new Request.Builder()
                .url("https://live.fc2.com/api/memberApi.php")
                .addHeader("User-agent", Constant.nico_proxy_UserAgent)
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Referer", "https://live.fc2.com/"+id)

                .post(body1)
                .build();
        Response response1 = client.newCall(request1).execute();
        String htmlText = "";
        if (response1.body() != null){
            htmlText = response1.body().string();
        }
        response1.close();

        //System.out.println(htmlText);

        JsonElement json = new Gson().fromJson(htmlText, JsonElement.class);
        //System.out.println(json);
        String version = "";
        if (json.getAsJsonObject().get("data").getAsJsonObject().has("channel_data")){
            version = json.getAsJsonObject().get("data").getAsJsonObject().get("channel_data").getAsJsonObject().get("version").getAsString();
        }

        String ipv4 = "";
        Request ip = new Request.Builder()
                .url("https://ipinfo.io/ip")
                .build();
        Response response = client.newCall(ip).execute();
        if (response.body() != null){
            ipv4 = response.body().string();
        }
        response.close();

        // https://live.fc2.com/api/getControlServer.php
        // channel_id=15233018&mode=play&orz=&channel_version=1Gr9lkVMn7hOn17f0-RCL&client_version=2.4.4++%5B1%5D&client_type=pc&client_app=browser_hls&ipv6=2400%3Aa300%3A12fe%3Ab500%3A60ac%3A4c7e%3A7af2%3Aa2d7&comment=2
        RequestBody body2 = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("channel_id", null, RequestBody.create(id, MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("mode", null, RequestBody.create("play", MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("orz", null, RequestBody.create("", MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("channel_version", null, RequestBody.create(version, MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("client_version", null, RequestBody.create("2.4.4++%5B1%5D", MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("client_type", null, RequestBody.create("pc", MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("client_app", null, RequestBody.create("browser_hls", MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("ipv4", null, RequestBody.create(ipv4, MediaType.get("text/plain; charset=utf-8")))
                .addFormDataPart("comment", null, RequestBody.create("2", MediaType.get("text/plain; charset=utf-8")))
                .build();

        Request request2 = new Request.Builder()
                .url("https://live.fc2.com/api/getControlServer.php")
                .addHeader("User-agent", Constant.nico_proxy_UserAgent)
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Referer", "https://live.fc2.com/"+id)
                .post(body2)
                .build();
        Response response2 = client.newCall(request2).execute();
        htmlText = "";
        if (response2.body() != null){
            htmlText = response2.body().string();
        }
        response2.close();
        json = new Gson().fromJson(htmlText, JsonElement.class);
        //System.out.println(json);

        // {"url":"wss:\/\/us-west-1-media-worker1083.live.fc2.com\/control\/channels\/15233018","orz":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJvcnoiOiI4NjUxYzEzNDQ5MjZlYWJlMjNmNDViYjFkMTU3MDdhYjdjMTcwNzY3In0.y3pjlCyfUYN1DYaIKNnuplxSMSaRgvwTEvTlwDr2C5I","orz_raw":"8651c1344926eabe23f45bb1d15707ab7c170767","control_token":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpZCI6IjNjOTU3NmIxYWZhZTgyZDkzZWQ5MDRlMjUyNjY3MTZlY2ViOTZkMzUuMjgyMjc2NzciLCJjaGFubmVsX2lkIjoiMTUyMzMwMTgiLCJ1c2VyX2lkIjoiM2M5NTc2YjFhZmFlODJkOTNlZDkwNGUyNTIiLCJzZXJ2aWNlX2lkIjowLCJvcnpfdG9rZW4iOiI4NjUxYzEzNDQ5MjZlYWJlMjNmNDViYjFkMTU3MDdhYjdjMTcwNzY3IiwicHJlbWl1bSI6MCwibW9kZSI6InBsYXkiLCJsYW5ndWFnZSI6ImphIiwiY2xpZW50X3R5cGUiOiJwYyIsImNsaWVudF9hcHAiOiJicm93c2VyX2hscyIsImNsaWVudF92ZXJzaW9uIjoiMi40LjQgIFsxXSIsImFwcF9pbnN0YWxsX2tleSI6IiIsImNoYW5uZWxfdmVyc2lvbiI6IjFHcjlsa1ZNbjdoT24xN2YwLVJDTCIsImlwIjoiMTIzLjAuNjUuMTIiLCJpcHY2IjoiMjQwMDphMzAwOjEyZmU6YjUwMDo2MGFjOjRjN2U6N2FmMjphMmQ3IiwiY29tbWVudGFibGUiOjEsInVzZXJfbmFtZSI6IiIsImFkdWx0X2FjY2VzcyI6MSwiYWdlbnRfaWQiOjAsImNvdW50cnlfY29kZSI6IkpQIiwicGF5X21vZGUiOjAsImV4cCI6MTcxODcwOTk5Nn0.V3ld4ijIy9TMMkFZYTrzzLKvlkuKymw7h1lwo8vM7LQ","status":0}

        String websocketURL = "";

        if (json.getAsJsonObject().has("url")){
            websocketURL = json.getAsJsonObject().get("url").getAsString() + "?control_token=" + json.getAsJsonObject().get("control_token").getAsString();
        }

        Request request = new Request.Builder()
                .url(websocketURL)
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        String[] temp = {"wait", ""};
        int[] temp2 = {2};
        boolean[] temp3 = {false};
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
                webSocket.cancel();
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                timer.cancel();
                webSocket.cancel();
                super.onClosing(webSocket, code, reason);
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
                timer.cancel();
                webSocket.cancel();
                super.onFailure(webSocket, t, response);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                //System.out.println("----");
                //System.out.println(text);

                if (text.startsWith("{\"name\":\"user_count\",\"arguments\":{")) {

                    if (!temp3[0]){
                        webSocket.send("{\"name\":\"get_hls_information\",\"arguments\":{},\"id\":1}");

                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {

                                webSocket.send("{\"name\":\"heartbeat\",\"arguments\":{},\"id\":"+temp2[0]+"}");
                                temp2[0]++;
                            }
                        }, 30000L, 30000L);

                        temp3[0] = true;
                    }

                }

                if (text.startsWith("{\"name\":\"_response_\",\"id\":1")) {

                    System.out.println(text);
                    JsonElement json = new Gson().fromJson(text, JsonElement.class);
                    if (json.getAsJsonObject().get("arguments").getAsJsonObject().has("playlists_high_latency")){
                        temp[0] = json.getAsJsonObject().get("arguments").getAsJsonObject().get("playlists_high_latency").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    } else if (json.getAsJsonObject().get("arguments").getAsJsonObject().has("playlists_middle_latency")){
                        temp[0] = json.getAsJsonObject().get("arguments").getAsJsonObject().get("playlists_middle_latency").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    } else if (json.getAsJsonObject().get("arguments").getAsJsonObject().has("playlists")){
                        temp[0] = json.getAsJsonObject().get("arguments").getAsJsonObject().get("playlists").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    } else {
                        temp[0] = "";
                    }

                }


                if (text.startsWith("{\"name\":\"control_disconnection\"")) {
                    timer.cancel();
                    webSocket.cancel();
                    temp[0] = "Error";
                }
                //System.out.println("----");
            }

            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                //System.out.println("websocket open");
                //webSocket.send("{\"type\":\"startWatching\",\"data\":{\"stream\":{\"quality\":\"abr\",\"protocol\":\"hls\",\"latency\":\"low\",\"chasePlay\":false},\"room\":{\"protocol\":\"webSocket\",\"commentable\":true},\"reconnect\":false}}");
            }
        });
        while (temp[0].startsWith("wait")){
            temp[1] = temp[0];
        }
        if (temp[0].startsWith("Error")){
            temp[0] = null;
        }

        // {"name":"user_count","arguments":{"pc_user_count":1161,"pc_total_count":12567,"mobile_user_count":738,"mobile_total_count":18582}}
        // --> {"name":"get_hls_information","arguments":{},"id":1}

        // https://us-west-1-media.live.fc2.com/a/stream/15233018/2/master_playlist?targets=10,20,30,40,90&c=d6fIhMeIM-P4NCj8gLgru&d=dHQX1uRv0NsxY8qlTi0pF7qiLmwWbv4wFtMDWW6jMq3FSs7OuSS8q9uAnqgEUi3NQ17RdY3nTIpmwKPrBQXEjhsk4xyrZn1db65IBPNZ8WFXLUc9fPgkRFsbAVxaWj5K
        return new ResultVideoData(temp[0], null, true, false, true, null);
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        // https://video.fc2.com/ja/content/20240504hww75Nv3
        // https://video.fc2.com/content/20240504hww75Nv3
        Matcher matcher1 = matcher_SupportURL1.matcher(data.getURL());
        Matcher matcher2 = matcher_SupportURL2.matcher(data.getURL());
        Matcher matcher3 = matcher_SupportURL3.matcher(data.getURL());

        final String id;
        boolean isVideo = false;

        if (!matcher1.find() && !matcher2.find() && !matcher3.find()){
            throw new Exception("Not Support URL");
        } else if (matcher1.find()){
            id = matcher1.group(2).split("\\?")[0];
            isVideo = true;
        } else if (matcher2.find()) {
            id = matcher2.group(1).split("\\?")[0];
            isVideo = true;
        } else {
            String tId = "";
            for (String str : data.getURL().split("/")){
                try {
                    if (Long.parseLong(str) >= 0){
                        tId = str;
                    }
                } catch (Exception e){

                }
            }
            id = tId;
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        if (isVideo){
            String HtmlText = "";
            try {

                Request request_html = new Request.Builder()
                        .url("https://video.fc2.com/api/v3/videoplayer/"+id+"?ddd27fa7fd3e1b8de9c210889e1a4fd0=1&tk=&fs=0")
                        .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                        .build();
                Response response = client.newCall(request_html).execute();
                if (response.body() != null){
                    HtmlText = response.body().string();
                }
                response.close();

            } catch (Exception e) {
                if (data.getProxy() != null) {
                    throw new Exception("video.fc2.com " + e.getMessage() + " (Use Proxy : " + data.getProxy().getProxyIP() + ")");
                } else {
                    throw new Exception("video.fc2.com " + e.getMessage());
                }
            }

            JsonElement json = new Gson().fromJson(HtmlText, JsonElement.class);
            //System.out.println(json);

            if (json.getAsJsonObject().has("title")){
                return json.getAsJsonObject().get("title").getAsString();
            }
        } else {

            RequestBody body1 = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("channel", null, RequestBody.create("1", MediaType.get("text/plain; charset=utf-8")))
                    .addFormDataPart("profile", null, RequestBody.create("1", MediaType.get("text/plain; charset=utf-8")))
                    .addFormDataPart("user", null, RequestBody.create("1", MediaType.get("text/plain; charset=utf-8")))
                    .addFormDataPart("streamid", null, RequestBody.create(id, MediaType.get("text/plain; charset=utf-8")))
                    .build();

            Request request1 = new Request.Builder()
                    .url("https://live.fc2.com/api/memberApi.php")
                    .addHeader("User-agent", Constant.nico_proxy_UserAgent)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .addHeader("Referer", "https://live.fc2.com/"+id)

                    .post(body1)
                    .build();
            Response response1 = client.newCall(request1).execute();
            String htmlText = "";
            if (response1.body() != null){
                htmlText = response1.body().string();
            }
            response1.close();

            JsonElement json = new Gson().fromJson(htmlText, JsonElement.class);

            //System.out.println(json);
            return json.getAsJsonObject().get("data").getAsJsonObject().get("channel_data").getAsJsonObject().get("title").getAsString();

        }

        return "";
    }

    @Override
    public String getServiceName() {
        return "FC2動画";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
