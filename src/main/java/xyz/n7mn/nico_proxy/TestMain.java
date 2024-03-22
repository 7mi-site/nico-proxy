package xyz.n7mn.nico_proxy;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;


public class TestMain {

    public static void main(String[] args) throws Exception {
        final OkHttpClient client = new OkHttpClient();

        ResultVideoData video = new Abema().getVideo(new RequestVideoData("https://abema.tv/channels/animejapan-green/slots/A21BVc7qNXhtRd", null));
        System.out.println(video.getVideoURL());
        /*//System.out.println(video.getTokenJson());

        JsonElement json = new Gson().fromJson(video.getTokenJson(), JsonElement.class);

        String nicosid = json.getAsJsonObject().get("nicosid").getAsString();
        String domand_bid = json.getAsJsonObject().get("domand_bid").getAsString();
        String m3u8 = json.getAsJsonObject().get("MainM3U8").getAsString();
        Request request_audio_m3u8 = new Request.Builder()
                .url(video.getVideoURL())
                .addHeader("Cookie", "nicosid="+nicosid+"; domand_bid=" + domand_bid)
                .build();


        Response response2 = client.newCall(request_audio_m3u8).execute();

        if (response2.body() != null){
            System.out.println(response2.body().string());
        }

        response2.close();*/
        /*final OkHttpClient client = new OkHttpClient();
        Request build = new Request.Builder()
                .url(video.getVideoURL())
                .addHeader("Referer", "https://www.bilibili.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0")
                .build();

        Response response = client.newCall(build).execute();
        if (response.body() != null){
            System.out.println(response.code());
            System.out.println(response.body().contentType());
            //System.out.println(response.body().string());
        }
        response.close();*/
    }
}