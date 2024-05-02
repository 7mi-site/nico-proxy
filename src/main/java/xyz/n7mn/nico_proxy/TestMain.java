package xyz.n7mn.nico_proxy;


import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;



public class TestMain {

    public static void main(String[] args) throws Exception {
        ResultVideoData video = new Piapro().getVideo(new RequestVideoData("https://piapro.jp/t/KQn0", null));
        //System.out.println(video);
        System.out.println(new Piapro().getTitle(new RequestVideoData("https://piapro.jp/t/KQn0", null)));

    }
}