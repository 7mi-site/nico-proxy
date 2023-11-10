package xyz.n7mn.nico_proxy;


import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {

    public static void main(String[] args) throws Exception {

        ResultVideoData video = new Youtube().getVideo(new RequestVideoData("https://www.youtube.com/watch?v=GxrLU_wcguY", null));

        System.out.println("VideoURL : "+video.getVideoURL());
        System.out.println("AudioURL : "+video.getAudioURL());
        System.out.println("字幕Json :" + video.getCaptionData());

    }
}