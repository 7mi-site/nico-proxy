package xyz.n7mn.nico_proxy;

import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {

    public static void main(String[] args) throws Exception {

        ResultVideoData video = new Sonicbowl().getVideo(new RequestVideoData("https://player.sonicbowl.cloud/episode/f338b118-b13c-47b3-b26b-d1966ca19cb8/", null));
        System.out.println(video.getAudioURL());

        String title = new Sonicbowl().getTitle(new RequestVideoData("https://player.sonicbowl.cloud/episode/f338b118-b13c-47b3-b26b-d1966ca19cb8/", null));
        System.out.println(title);

    }
}