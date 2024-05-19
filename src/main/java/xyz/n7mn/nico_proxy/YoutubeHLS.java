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

public class YoutubeHLS implements ShareService{

    private final Pattern SupportURL = Pattern.compile("(youtu\\.be/|youtube\\.com/watch\\?v=)(.+)");
    private final Pattern matcher_rate = Pattern.compile("#EXT-X-STREAM-INF:BANDWIDTH=(\\d+),CODECS=\"(.*)\",RESOLUTION=(.*),FRAME-RATE=(\\d+),VIDEO-RANGE=(.*),AUDIO=\"(\\d+)\",CLOSED-CAPTIONS=");
    private final Pattern matcher_rate2 = Pattern.compile("#EXT-X-MEDIA:URI=\"(.*)\",TYPE=AUDIO,GROUP-ID=\"(\\d+)\",NAME=\"(.*)\",DEFAULT=(.*),AUTOSELECT=");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        String url = data.getURL().split("&")[0];
        Matcher matcher = SupportURL.matcher(url);

        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        String videoID = matcher.group(2);

        final OkHttpClient client = data.getProxy() != null ? new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();


        String SendJson = "{\"context\": {\"client\": {\"clientName\": \"IOS\", \"clientVersion\": \"19.09.3\", \"deviceModel\": \"iPhone14,3\", \"userAgent\": \"com.google.ios.youtube/19.09.3 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)\", \"hl\": \"en\", \"timeZone\": \"UTC\", \"utcOffsetMinutes\": 0}}, \"videoId\": \""+ videoID +"\", \"playbackContext\": {\"contentPlaybackContext\": {\"html5Preference\": \"HTML5_PREF_WANTS\"}}, \"contentCheckOk\": true, \"racyCheckOk\": true}";

        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(SendJson, JSON);

        Request request_iosAPI = new Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?key=AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc&prettyPrint=false")
                .addHeader("Cookie", "GPS=1; PREF=hl=en&tz=UTC; SOCS=CAI; VISITOR_INFO1_LIVE=9tH5MTNfq14; VISITOR_PRIVACY_METADATA=CgJKUBIEGgAgJg%3D%3D; YSC=GwL8drMhsgc")
                .addHeader("User-Agent", "com.google.ios.youtube/19.09.3 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "en-us,en;q=0.5")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Youtube-Client-Name", "5")
                .addHeader("X-Youtube-Client-Version", "19.09.3")
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
        String VideoURL = "";
        String AudioURL = "";

        JsonElement json = new Gson().fromJson(ResponseJson, JsonElement.class);
        //System.out.println(json.getAsJsonObject().getAsJsonObject("playabilityStatus"));

        String hls = json.getAsJsonObject().getAsJsonObject("streamingData").get("hlsManifestUrl").getAsString();

        Request request2 = new Request.Builder()
                .url(hls)
                .addHeader("Origin", "https://www.youtube.com")
                .addHeader("Accept-Encoding", "gzip")
                .post(body)
                .build();

        Response response2 = client.newCall(request2).execute();

        if (response2.body() != null){
            String string = "";
            ByteArrayOutputStream decompressBaos = new ByteArrayOutputStream();
            try (InputStream gzip = new GZIPInputStream(new ByteArrayInputStream(response2.body().bytes()))) {
                int b;
                while ((b = gzip.read()) != -1) {
                    decompressBaos.write(b);
                }
            }
            byte[] byteArray = decompressBaos.toByteArray();
            string = new String(byteArray, StandardCharsets.UTF_8);

            //System.out.println(string);
            long bitrate = 0;
            int groupId = 0;

            boolean isAdd = false;
            for (String str : string.split("\n")){
                Matcher matcher1 = matcher_rate.matcher(str);

                if (matcher1.find()){
                    long l = Long.parseLong(matcher1.group(1));
                    int g = Integer.parseInt(matcher1.group(6));
                    if (bitrate < l){
                        bitrate = l;
                        groupId = g;
                        isAdd = true;
                        continue;
                    }
                }

                if (isAdd){
                    VideoURL = str;
                    isAdd = false;
                }
            }

            //System.out.println(VideoURL);
            //System.out.println(groupId);

            for (String str : string.split("\n")){
                Matcher matcher1 = matcher_rate2.matcher(str);

                if (matcher1.find()){
                    int g = Integer.parseInt(matcher1.group(2));
                    if (g == groupId){
                        AudioURL = matcher1.group(1);
                        break;
                    }
                }
            }
        }

        response2.close();

        String caption = null;
        try {
            if (!json.getAsJsonObject().getAsJsonObject("captions").isJsonNull()){
                caption = json.getAsJsonObject().getAsJsonObject("captions").toString();
            }
        } catch (Exception e){
            //e.printStackTrace();
        }

        return new ResultVideoData(VideoURL, AudioURL, true, false, json.getAsJsonObject().getAsJsonObject("playabilityStatus").get("liveStreamability") != null, "", caption);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return getVideo(data);
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        String url = data.getURL().split("&")[0];
        Matcher matcher = SupportURL.matcher(url);

        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        String videoID = matcher.group(2);

        final OkHttpClient client = data.getProxy() != null ? new OkHttpClient.Builder().proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();


        String SendJson = "{\"context\": {\"client\": {\"clientName\": \"IOS\", \"clientVersion\": \"19.09.3\", \"deviceModel\": \"iPhone14,3\", \"userAgent\": \"com.google.ios.youtube/19.09.3 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)\", \"hl\": \"en\", \"timeZone\": \"UTC\", \"utcOffsetMinutes\": 0}}, \"videoId\": \""+ videoID +"\", \"playbackContext\": {\"contentPlaybackContext\": {\"html5Preference\": \"HTML5_PREF_WANTS\"}}, \"contentCheckOk\": true, \"racyCheckOk\": true}";

        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(SendJson, JSON);

        Request request_iosAPI = new Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?key=AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc&prettyPrint=false")
                .addHeader("Cookie", "GPS=1; PREF=hl=en&tz=UTC; SOCS=CAI; VISITOR_INFO1_LIVE=9tH5MTNfq14; VISITOR_PRIVACY_METADATA=CgJKUBIEGgAgJg%3D%3D; YSC=GwL8drMhsgc")
                .addHeader("User-Agent", "com.google.ios.youtube/19.09.3 (iPhone14,3; U; CPU iOS 15_6 like Mac OS X)")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "en-us,en;q=0.5")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Youtube-Client-Name", "5")
                .addHeader("X-Youtube-Client-Version", "19.09.3")
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

        JsonElement json = new Gson().fromJson(ResponseJson, JsonElement.class);

        return json.getAsJsonObject().getAsJsonObject("videoDetails").get("title").getAsString();
    }

    @Override
    public String getServiceName() {
        return "Youtube";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
