package xyz.n7mn.nico_proxy;


import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {

    public static void main(String[] args) throws Exception {

        ResultVideoData video = new Youtube().getVideo(new RequestVideoData("https://www.youtube.com/watch?v=2IZbzRj3CJk", null));
/*
        System.out.println("VideoURL : "+video.getVideoURL());
        System.out.println("AudioURL : "+video.getAudioURL());
        System.out.println("字幕Json :" + video.getCaptionData());

         */

        //ResultVideoData video = new NicoNicoVideo().getVideo(new RequestVideoData("https://www.nicovideo.jp/watch/so43000784", null));
        System.out.println(video.getVideoURL());
    }
}