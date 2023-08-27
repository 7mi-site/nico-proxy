package xyz.n7mn.nico_proxy;


import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {
    public static void main(String[] args) throws Exception {

        try{
            Twitter twitter = new Twitter();
            ResultVideoData video = twitter.getVideo(new RequestVideoData("https://twitter.com/i/spaces/1MnxnpwrpnoGO", null));
            System.out.println(video.getVideoURL());
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}