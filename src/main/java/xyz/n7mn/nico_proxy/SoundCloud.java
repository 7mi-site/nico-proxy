package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import kotlin.Pair;
import okhttp3.*;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoundCloud implements ShareService{

    private final Pattern SupportURL_1 = Pattern.compile("https://soundcloud\\.com/");

    //private final Pattern appVersion = Pattern.compile("window\\.__sc_version=\"(\\d+)\"");
    private final Pattern clientId = Pattern.compile("client_id:\"(.+)\",env:\"production\"");
    private final Pattern jsonData = Pattern.compile("window\\.__sc_hydration = \\[(.+)\\];");
    private final Pattern CheckQuestion = Pattern.compile("\\?");

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    private final Gson gson = new Gson();

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://soundcloud.com/baron1_3/penguin3rd

        if (!SupportURL_1.matcher(data.getURL()).find() && data.getURL().split("/").length != 5){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        // https://soundcloud.com/wipecore-wipecore/frenchinwipecore-remix
        final Request request1 = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .addHeader("Connection", "keep-alive")
                .build();

        final Request request2 = new Request.Builder()
                .url("https://a-v2.sndcdn.com/assets/50-a0fa7b81.js")
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .addHeader("Connection", "keep-alive")
                .build();

        String result = "";
        try {
            Response response1 = client.newCall(request1).execute();
            result = response1.body().string();
            response1.close();
        } catch (Exception e){
            throw e;
        }

        //System.out.println(result);
        Matcher matcher1 = jsonData.matcher(result);
        JsonElement json = null;
        if (matcher1.find()){
            try {
                json = gson.fromJson("["+matcher1.group(1)+"]", JsonElement.class);
                //System.out.println(json);
            } catch (Exception e){
                throw new Exception("Not Support URL");
            }
        } else {
            throw new Exception("Not Support URL");
        }

        try {
            Response response2 = client.newCall(request2).execute();
            result = response2.body().string();
            response2.close();
        } catch (Exception e){
            throw e;
        }


        final String ClientId;
        Matcher matcher2 = clientId.matcher(result);
        if (matcher2.find()){
            ClientId = matcher2.group(1);
        } else {
            ClientId = null;
        }
        //System.out.println(ClientId);

        String TrackAuthorization = null;
        String BaseURL = null;
        for (int i = 0; i < json.getAsJsonArray().size(); i++) {
            if (json.getAsJsonArray().get(i).getAsJsonObject().get("hydratable").getAsString().equals("sound")){
                if (BaseURL == null){
                    BaseURL = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("media").getAsJsonObject().get("transcodings").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                }

                if (TrackAuthorization == null){
                    TrackAuthorization = json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("track_authorization").getAsString();
                }
            }

        }

        // https://api-v2.soundcloud.com/media/soundcloud:tracks:1954361827/d26da167-5823-4c08-af84-cebe1b3b82fa/stream/hls?client_id=8cs1BARLZuBIdVIVAHtl42iQVbg3RA71&track_authorization=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJnZW8iOiJKUCIsInN1YiI6IiIsInJpZCI6Ijk0MDIxNjEyLTRkYzUtNGIzYi04Y2NmLTZjYTQzZWM1MDk4YyIsImlhdCI6MTczMzEzMzk4M30.9dGti9Y_TzUnMSpIvroJK4wBGbHiv5cPlQXzuCJrgpk
        // https://api-v2.soundcloud.com/media/soundcloud:tracks:1954361827/d26da167-5823-4c08-af84-cebe1b3b82fa/stream/hls?client_id=8cs1BARLZuBIdVIVAHtl42iQVbg3RA71&track_authorization=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJnZW8iOiJKUCIsInN1YiI6IiIsInJpZCI6Ijk0MDIxNjEyLTRkYzUtNGIzYi04Y2NmLTZjYTQzZWM1MDk4YyIsImlhdCI6MTczMzEzNDEyMH0.5sgjjHynOINd926y_P-BFEfDRF31EPvLjMh9oLgbb-w
        String hlsUrl = BaseURL + "?client_id=" + ClientId + "&track_authorization=" + TrackAuthorization;
        //System.out.println(hlsUrl);
        final Request request3 = new Request.Builder()
                .url(hlsUrl)
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .addHeader("Connection", "keep-alive")
                .build();
        try {
            Response response3 = client.newCall(request3).execute();
            result = response3.body().string();
            response3.close();
        } catch (Exception e){
            throw e;
        }

        json = gson.fromJson(result, JsonElement.class);
        //System.out.println(json);

        if (json != null){
            return new ResultVideoData(null, json.getAsJsonObject().get("url").getAsString(), true, false, false, "");
        } else {

            //
            final Request request4 = new Request.Builder()
                    .url("https://api-v2.soundcloud.com/resolve?url="+URLEncoder.encode(data.getURL().split("\\?")[0], StandardCharsets.UTF_8)+"&client_id=3WIthHrmko3NUQ6wbfCSRvFcDexHgswc")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .addHeader("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .addHeader("Connection", "keep-alive")
                    .build();
            try {
                Response response4 = client.newCall(request4).execute();
                result = response4.body().string();
                //System.out.println(result);
                response4.close();
            } catch (Exception e){
                throw e;
            }
            json = gson.fromJson(result, JsonElement.class);

            hlsUrl = json.getAsJsonObject().get("media").getAsJsonObject().get("transcodings").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
            //System.out.println(hlsUrl);
            final Request request5 = CheckQuestion.matcher(hlsUrl).find() ? new Request.Builder()
                    .url(hlsUrl + "&client_id=3WIthHrmko3NUQ6wbfCSRvFcDexHgswc")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .addHeader("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .addHeader("Connection", "keep-alive")
                    .build() : new Request.Builder()
                    .url(hlsUrl + "?client_id=3WIthHrmko3NUQ6wbfCSRvFcDexHgswc")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .addHeader("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                    .addHeader("Connection", "keep-alive")
                    .build();
            try {
                Response response5 = client.newCall(request5).execute();
                result = response5.body().string();
                response5.close();
                //System.out.println(result);
            } catch (Exception e){
                throw e;
            }
            json = gson.fromJson(result, JsonElement.class);

            return new ResultVideoData(null, json.getAsJsonObject().get("url").getAsString(), true, false, false, "");
        }
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        // https://soundcloud.com/baron1_3/penguin3rd

        if (!SupportURL_1.matcher(data.getURL()).find() && data.getURL().split("/").length != 5){
            throw new Exception("Not Support URL");
        }

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        final Request request = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "ja,en;q=0.7,en-US;q=0.3")
                .addHeader("Connection", "keep-alive")
                .build();

        String result = "";
        try {
            Response response = client.newCall(request).execute();
            result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        Matcher matcher1 = jsonData.matcher(result);
        JsonElement json = null;
        if (matcher1.find()){
            try {
                json = gson.fromJson("["+matcher1.group(1)+"]", JsonElement.class);
                //System.out.println(json);
            } catch (Exception e){
                throw new Exception("Not Support URL");
            }
        } else {
            throw new Exception("Not Support URL");
        }

        for (int i = 0; i < json.getAsJsonArray().size(); i++) {
            if (json.getAsJsonArray().get(i).getAsJsonObject().get("hydratable").getAsString().equals("sound")){
                return json.getAsJsonArray().get(i).getAsJsonObject().get("data").getAsJsonObject().get("title").getAsString();
            }

        }

        return "";
    }

    @Override
    public String getServiceName() {
        return "SoundCloud";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }
}
