package xyz.n7mn.nico_proxy;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;


public class TestMain {

    public static void main(String[] args) throws Exception {
        ResultVideoData video = new Gimy().getVideo(new RequestVideoData("https://gimy.ai/eps/255446-5-1.html", null));
        System.out.println(video.getVideoURL());

    }
}