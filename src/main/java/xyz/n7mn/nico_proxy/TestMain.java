package xyz.n7mn.nico_proxy;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.ProxyData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.io.IOException;


public class TestMain {

    public static void main(String[] args) throws Exception {
        final OkHttpClient client = new OkHttpClient();

        ResultVideoData video = new Iwara().getVideo(new RequestVideoData("https://www.iwara.tv/video/vwvOcGMRQyvlwD/56-iochi-mari", null));
        System.out.println(video.getVideoURL());

    }
}