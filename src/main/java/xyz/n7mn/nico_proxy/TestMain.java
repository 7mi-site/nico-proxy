package xyz.n7mn.nico_proxy;

import xyz.n7mn.nico_proxy.data.ProxyData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {

    public static void main(String[] args) throws Exception {

        ResultVideoData video = new TVer().getLive(new RequestVideoData("https://tver.jp/live/cx", null));
        System.out.println(video.getVideoURL());

    }
}