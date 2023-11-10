package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import okhttp3.*;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class Youtube implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        String url = data.getURL();
        Matcher matcher = Pattern.compile("(youtu\\.be/|youtube.com/watch\\?v=)(.*)").matcher(url);

        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        String videoID = matcher.group(2);

        final OkHttpClient client = data.getProxy() != null ? new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();


        String SendJson = "{\"context\": {\"client\": {\"clientName\": \"IOS\", \"clientVersion\": \"17.33.2\", \"deviceModel\": \"iPhone14,3\", \"userAgent\": \"com.google.ios.youtube/17.33.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)\", \"hl\": \"ja\", \"timeZone\": \"UTC\", \"utcOffsetMinutes\": 0}}, \"videoId\": \""+videoID+"\", \"playbackContext\": {\"contentPlaybackContext\": {\"html5Preference\": \"HTML5_PREF_WANTS\"}}, \"contentCheckOk\": true, \"racyCheckOk\": true}";

        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(SendJson, JSON);

        Request request_iosAPI = new Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?key=AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc&prettyPrint=false")
                .addHeader("Cookie", "PREF=hl=en&tz=UTC; SOCS=CAI; GPS=1; YSC=cfiTHKBLEmo; VISITOR_INFO1_LIVE=P3aYDmsojgI; VISITOR_PRIVACY_METADATA=CgJKUBICGgA%3D")
                .addHeader("User-Agent", "com.google.ios.youtube/17.33.2 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "en-us,en;q=0.5")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Youtube-Client-Name", "5")
                .addHeader("X-Youtube-Client-Version", "17.33.2")
                .addHeader("Origin", "https://www.youtube.com")
                .addHeader("Accept-Encoding", "gzip")
                .addHeader("Connection", "close")
                .post(body)
                .build();

        Response response = client.newCall(request_iosAPI).execute();

        String ResponseJson = "";
        if (response.body() != null){

            //Headers headers = response.headers();
            //System.out.println(response.code());
            //for (String name : headers.names()) {
            //    System.out.println(name + " : " + headers.get(name));
            //}

            ByteArrayOutputStream decompressBaos = new ByteArrayOutputStream();
            try (InputStream gzip = new GZIPInputStream(new ByteArrayInputStream(response.body().bytes()))) {
                int b;
                while ((b = gzip.read()) != -1) {
                    decompressBaos.write(b);
                }
            }
            byte[] byteArray = decompressBaos.toByteArray();
            ResponseJson = new String(byteArray, StandardCharsets.UTF_8);
            //System.out.println(ResponseJson);

            response.close();
        }

        //System.out.println(ResponseJson);
        String[] VideoURL = {""};
        long[] VideoBitrate = {0};
        String[] AudioURL = {""};
        long[] AudioBitrate = {0};

        JsonElement json = new Gson().fromJson(ResponseJson, JsonElement.class);
        //System.out.println(json.getAsJsonObject().getAsJsonObject("playabilityStatus"));
        JsonArray jsonArray = json.getAsJsonObject().getAsJsonObject("streamingData").getAsJsonArray("adaptiveFormats");
        jsonArray.forEach((s)->{
            long bitrate = s.getAsJsonObject().get("bitrate").getAsLong();
            if (s.getAsJsonObject().get("mimeType").getAsString().startsWith("video/mp4")){
                if (VideoBitrate[0] >= bitrate){
                    return;
                }
                VideoURL[0] = s.getAsJsonObject().get("url").getAsString();
                VideoBitrate[0] = bitrate;
            } else {
                if (AudioBitrate[0] >= bitrate){
                    return;
                }
                AudioURL[0] = s.getAsJsonObject().get("url").getAsString();
                AudioBitrate[0] = bitrate;
            }
        });

        String caption = null;
        try {
            if (!json.getAsJsonObject().getAsJsonObject("captions").isJsonNull()){
                caption = json.getAsJsonObject().getAsJsonObject("captions").toString();
            }
        } catch (Exception e){
            //e.printStackTrace();
        }

        return new ResultVideoData(VideoURL[0], AudioURL[0], false, false, false, "", caption);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }
}
