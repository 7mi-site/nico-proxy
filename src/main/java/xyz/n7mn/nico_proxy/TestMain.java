package xyz.n7mn.nico_proxy;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.*;
import xyz.n7mn.nico_proxy.data.ProxyData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TestMain {

    public static void main(String[] args) throws Exception {
        ResultVideoData video = new TVer().getVideo(new RequestVideoData("https://tver.jp/episodes/ep8gyppoa4?utm_campaign=article_153182&utm_medium=referral&utm_source=tver_plus", null));
        System.out.println(video.getVideoURL());
        System.out.println(video.getAudioURL());
    }
}