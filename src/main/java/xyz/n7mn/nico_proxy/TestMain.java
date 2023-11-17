package xyz.n7mn.nico_proxy;


import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {

    public static void main(String[] args) throws Exception {

        ResultVideoData live = new TVer().getLive(new RequestVideoData("https://tver.jp/live/ntv", null));

        System.out.println(live.getVideoURL());
        String title = new TVer().getTitle(new RequestVideoData("https://tver.jp/live/simul/let3phmt6v", null));
        System.out.println(title);

    }
}