package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import okhttp3.*;
import xyz.n7mn.nico_proxy.data.EncryptedTokenJSON;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TestMain {
    public static void main(String[] args)  {

        ShareService service = new NicoNicoVideo();
        ResultVideoData video;
        String URL = null;
        try {
            video = service.getVideo(new RequestVideoData("https://www.nicovideo.jp/watch/so38016254", null));
            if (video == null){
                return;
            }
            URL = video.getVideoURL();

            EncryptedTokenJSON json = new Gson().fromJson(video.getTokenJson(), EncryptedTokenJSON.class);

            System.out.println(URL);

            if (video.isEncrypted()){
                final OkHttpClient client = new OkHttpClient();

                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {

                        RequestBody body = RequestBody.create(json.getTokenValue(), MediaType.get("application/json; charset=utf-8"));
                        Request request1 = new Request.Builder()
                                .url(json.getTokenSendURL())
                                .post(body)
                                .build();
                        try {
                            Response response1 = client.newCall(request1).execute();
                            //System.out.println(response.body().string());
                            response1.close();
                        } catch (IOException e) {
                            // e.printStackTrace();
                            return;
                        }
                    }
                }, 0L, 40000L);

                Request request_hls = new Request.Builder()
                        .url(URL)
                        .build();


                Matcher matcher = Pattern.compile("(.*)master.m3u8").matcher(URL);
                String baseURL = "";
                if (matcher.find()){
                    baseURL = matcher.group(1);
                }

                Response response_hls = client.newCall(request_hls).execute();

                String MasterHls = null;
                if (response_hls.body() != null){
                    MasterHls = response_hls.body().string();
                }
                response_hls.close();

                String videoHls = null;
                if (MasterHls != null){
                    String[] split = MasterHls.split("\n");
                    for (String str : split){
                        if (!str.startsWith("#")){
                            videoHls = baseURL+str;
                            break;
                        }
                    }
                }

                StringBuilder hls = new StringBuilder();
                String ivText = "";
                String keyURL = "";
                String baseTsUrl = null;

                if (videoHls != null){
                    System.out.println(videoHls);
                    Matcher matcher2 = Pattern.compile("(.*)playlist.m3u8").matcher(videoHls);

                    if (matcher2.find()){
                        baseTsUrl = matcher2.group(1);
                    }
                    if (baseTsUrl == null){
                        return;
                    }
                    Request request_hls2 = new Request.Builder()
                            .url(videoHls)
                            .build();
                    Response response_hls2 = client.newCall(request_hls2).execute();
                    if (response_hls2.body() != null){
                        String s = response_hls2.body().string();
                        System.out.println(s);
                        String[] split = s.split("\n");
                        for (String str : split){
                            if (str.startsWith("#EXT-X-KEY")){
                                Matcher matcher_key = Pattern.compile("URI=\"(.*)\",IV=").matcher(str);
                                Matcher matcher_iv = Pattern.compile("IV=([a-z0-9A-Z]+)").matcher(str);
                                if (matcher_iv.find()){
                                    ivText = matcher_iv.group(1);
                                }
                                if (matcher_key.find()){
                                    keyURL = matcher_key.group(1);
                                }
                                continue;
                            }

                            hls.append(str);
                            hls.append("\n");
                        }
                    }
                    response_hls2.close();
                }

                Request request_hls3 = new Request.Builder()
                        .url(keyURL)
                        .addHeader("X-Frontend-Id", "6")
                        .addHeader("X-Frontend-Version", "0")
                        .addHeader("Access-Control-Request-Headers", "x-frontend-id,x-frontend-version")
                        .addHeader("Access-Control-Request-Method", "GET")
                        .addHeader("Origin", "https://www.nicovideo.jp")
                        .addHeader("Referer", "https://www.nicovideo.jp/")
                        .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 nico-proxy/1.0 Safari/537.3")
                        .build();
                Response response_hls3 = client.newCall(request_hls3).execute();

                System.out.println(ivText);
                String s = ivText.startsWith("0x") ? ivText.substring(2) : ivText;
                byte[] iv = new BigInteger(s, 16).toByteArray();
                byte[] ivDataEncoded = new byte[16];
                int offset = iv.length > 16 ? iv.length - 16 : 0;
                System.arraycopy(
                        iv,
                        offset,
                        ivDataEncoded,
                        ivDataEncoded.length - iv.length + offset,
                        iv.length - offset);

                Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(response_hls3.body().bytes(), "AES"), new IvParameterSpec(ivDataEncoded));

                String tempFolder = "temp_"+new Date().getTime();
                if (new File("./"+tempFolder).mkdir()){
                    byte[] bytes = hls.toString().getBytes(StandardCharsets.UTF_8);
                    FileOutputStream stream = new FileOutputStream("./" + tempFolder + "/play.m3u8");
                    stream.write(bytes);
                    stream.close();
                    String[] split = hls.toString().split("\n");
                    hls = new StringBuilder();

                    int i = 1;
                    for (String str : split){
                        hls.append(str.split("\n")[0]);
                        hls.append("\n");

                        if (str.startsWith("#")){
                            continue;
                        }
                        if (str.length() == 0){
                            continue;
                        }

                        Request request_hls4 = new Request.Builder()
                                .url(baseTsUrl + str)
                                .addHeader("X-Frontend-Id", "6")
                                .addHeader("X-Frontend-Version", "0")
                                .addHeader("Access-Control-Request-Headers", "x-frontend-id,x-frontend-version")
                                .addHeader("Access-Control-Request-Method", "GET")
                                .addHeader("Origin", "https://www.nicovideo.jp")
                                .addHeader("Referer", "https://www.nicovideo.jp/")
                                .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 nico-proxy/1.0 Safari/537.3")
                                .build();
                        Response response_hls4 = client.newCall(request_hls4).execute();

                        byte[] bytes2 = response_hls4.body().bytes();

                        FileOutputStream stream2 = new FileOutputStream("./" + tempFolder + "/" + str.split("\\?")[0]);
                        stream2.write(cipher.doFinal(bytes2));
                        stream2.close();
                        response_hls4.close();
                        if (i % 5 == 0){
                            Thread.sleep(12000L);
                        }
                        i++;
                    }
                    timer.cancel();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}