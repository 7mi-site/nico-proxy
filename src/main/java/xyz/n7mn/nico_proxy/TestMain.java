package xyz.n7mn.nico_proxy;


import xyz.n7mn.nico_proxy.data.ProxyData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;



public class TestMain {

    public static void main(String[] args) throws Exception {

        ResultVideoData live = new Vimeo().getVideo(new RequestVideoData("https://vimeo.com/944027008?share=copy", null));
        //String title = new Vimeo().getTitle(new RequestVideoData("https://vimeo.com/944027008?share=copy", null));
        //System.out.println(title);
        System.out.println(live.getVideoURL());

    }
}