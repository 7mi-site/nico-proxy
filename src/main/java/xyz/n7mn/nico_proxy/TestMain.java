package xyz.n7mn.nico_proxy;

import xyz.n7mn.nico_proxy.data.ProxyData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

public class TestMain {

    public static void main(String[] args) throws Exception {

        new BiliBiliCom_fromAPI().getVideo(new RequestVideoData("https://www.bilibili.com/video/BV1Lx421279E/", null));

    }
}