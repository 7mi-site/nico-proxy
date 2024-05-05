package xyz.n7mn.nico_proxy;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;



public class TestMain {

    public static void main(String[] args) throws Exception {

        ResultVideoData video = new TikTok().getVideo(new RequestVideoData("https://www.tiktok.com/@sobayu8055/video/7340965738779397394", null));
        System.out.println(video.getVideoURL());

        System.out.println("http://localhost:9999/?url="+video.getVideoURL()+"&cookiee="+video.getTokenJson());

    }
}