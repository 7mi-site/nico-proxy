package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import okhttp3.*;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;
import xyz.n7mn.nico_proxy.data.TokenJSON;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestMain {

    public static void main(String[] args) throws Exception {

        try{

            ResultVideoData video = new Twitter().getVideo(new RequestVideoData("https://twitter.com/Focke_3/status/1596496780572889088", null));
            System.out.println(video.getVideoURL());
            //System.out.println(new Abema().getTitle(new RequestVideoData("https://abema.tv/now-on-air/abema-news", null)));


        } catch (Exception e){
            e.printStackTrace();
        }


    }
}