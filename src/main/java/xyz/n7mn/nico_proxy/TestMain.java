package xyz.n7mn.nico_proxy;


import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {
    public static void main(String[] args)  {

        TikTok tikTok = new TikTok();

        try {
            ResultVideoData video = tikTok.getVideo(new RequestVideoData("https://www.tiktok.com/@komedascoffee/video/7258220227773746433", null));
            System.out.println(video.getVideoURL());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}