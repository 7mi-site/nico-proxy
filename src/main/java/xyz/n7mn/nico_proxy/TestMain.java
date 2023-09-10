package xyz.n7mn.nico_proxy;

import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {

    public static void main(String[] args) throws Exception {

        try{

            ResultVideoData video = new Pornhub().getVideo(new RequestVideoData("https://jp.pornhub.com/view_video.php?viewkey=648be371d8cc2", null));

            String videoURL = video.getVideoURL();
            System.out.println("video url : "+videoURL);
            System.out.println("title : "+ new Pornhub().getTitle(new RequestVideoData("https://jp.pornhub.com/view_video.php?viewkey=648be371d8cc2", null)));


        } catch (Exception e){
            e.printStackTrace();
        }


    }
}