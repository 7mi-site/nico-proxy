package xyz.n7mn.nico_proxy;


import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {

    public static void main(String[] args) throws Exception {

        ResultVideoData live = new TVer().getVideo(new RequestVideoData("https://tver.jp/episodes/epq882oemn", null));

        //System.out.println(live.getVideoURL());
    }
}