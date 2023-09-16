package xyz.n7mn.nico_proxy;

import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {

    public static void main(String[] args) throws Exception {

        try{

            Twicast twicast = new Twicast("1548673979992670209.3fa9fb392b85a7835a04cf6000e426624258e3c1a30f5674704252a23688d77f", "e909327aab8cc9e106e2bf2cbef03f652cfb074958ef159b5aa425300edc5629");
            // https://twitcasting.tv/twitcasting_jp/movie/776448195
            // https://twitcasting.tv/hmb_d
            ResultVideoData live = twicast.getLive(new RequestVideoData("https://twitcasting.tv/hmb_d/movie/776588096", null));

            // アーカイブの場合はURL取得できてもリファラが必要
            System.out.println(live.getVideoURL());

            String title = twicast.getTitle(new RequestVideoData("https://twitcasting.tv/hmb_d/movie/776588096", null));
            System.out.println(title);

        } catch (Exception e){
            e.printStackTrace();
        }


    }
}