package xyz.n7mn.nico_proxy;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;



public class TestMain {

    public static void main(String[] args) throws Exception {
        /*
        ResultVideoData video = new TikTok().getVideo(new RequestVideoData("https://www.tiktok.com/@sobayu8055/video/7340965738779397394", null));
        System.out.println(video.getVideoURL());

        OkHttpClient client = new OkHttpClient();
        Request request_html = new Request.Builder()
                .url(video.getVideoURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .addHeader("Cookie", video.getTokenJson())
                .addHeader("Origin", "https://www.tiktok.com")
                .addHeader("Referer", "https://www.tiktok.com/")
                .build();
        Response response = client.newCall(request_html).execute();
        System.out.println(response.code());
        System.out.println(response.body().string());
        response.close();
         */
    }
}