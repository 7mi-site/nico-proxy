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
        String video = new Abema().getTitle(new RequestVideoData("https://abema.tv/channels/fighting-sports/slots/EEPqpkB2qEdYgX", null));
        System.out.println(video);

    }
}