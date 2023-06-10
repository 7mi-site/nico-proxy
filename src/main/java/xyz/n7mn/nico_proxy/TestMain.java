package xyz.n7mn.nico_proxy;

import xyz.n7mn.nico_proxy.data.RequestVideoData;

public class TestMain {
    public static void main(String[] args) {

        ShareService video = new NicoNicoVideo();
        String URL;
        try {
            URL = video.getVideo(new RequestVideoData("https://www.nicovideo.jp/watch/so42042971", null)).getVideoURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(URL);

    }
}
