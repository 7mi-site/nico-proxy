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
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestMain {

    public static void main(String[] args) throws Exception {

        try{

            ResultVideoData video = new NicoNicoVideo().getVideo(new RequestVideoData("https://www.nicovideo.jp/watch/so42042971", null));

            String hlsUrl = video.getVideoURL();

            OkHttpClient client = new OkHttpClient();

            // ハートビート信号送る

            TokenJSON json = new Gson().fromJson(video.getTokenJson(), TokenJSON.class);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    RequestBody body = RequestBody.create(json.getTokenValue(), MediaType.get("application/json; charset=utf-8"));
                    Request request0 = new Request.Builder()
                            .url(json.getTokenSendURL())
                            .post(body)
                            .build();
                    try {
                        Response response0 = client.newCall(request0).execute();
                        //System.out.println(response.body().string());
                        response0.close();
                    } catch (IOException e) {
                        // e.printStackTrace();
                        return;
                    }
                }
            }, 0L, 40000L);

            System.out.println(hlsUrl);

            Matcher matcher1 = Pattern.compile("https://(.+)/master.m3u8").matcher(hlsUrl);

            if (matcher1.find()){
                String hlsBaseUrl = "https://"+matcher1.group(1)+"/";

                String masterHls = "";
                Request request1 = new Request.Builder()
                        .url(hlsUrl)
                        .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0 nico-proxy/1.0")
                        .build();
                Response response1 = client.newCall(request1).execute();
                if (response1.body() != null){
                    masterHls = response1.body().string();
                }
                response1.close();

                // System.out.println(masterHls);
                String hlsPlaylistUrl = "";
                for (String str : masterHls.split("\n")){
                    if (str.startsWith("#")){
                        continue;
                    }
                    hlsPlaylistUrl = hlsBaseUrl+str;
                    break;
                }

                String playlistHls = "";
                Request request2 = new Request.Builder()
                        .url(hlsPlaylistUrl)
                        .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0 nico-proxy/1.0")
                        .build();
                Response response2 = client.newCall(request2).execute();
                if (response2.body() != null){
                    playlistHls = response2.body().string();
                }
                response2.close();
                //System.out.println(playlistHls);

                Matcher matcher = Pattern.compile("#EXT-X-KEY:METHOD=AES-128,URI=\"(.+)\",IV=").matcher(playlistHls);

                if (matcher.find()){
                    byte[] key_file = new byte[0];
                    Request request3 = new Request.Builder()
                            .url(matcher.group(1))
                            .addHeader("X-Frontend-Id", "6")
                            .addHeader("X-Frontend-Version", "0")
                            .addHeader("Access-Control-Request-Headers", "x-frontend-id,x-frontend-version")
                            .addHeader("Access-Control-Request-Method", "GET")
                            .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0 nico-proxy/1.0")
                            .build();

                    Response response3 = client.newCall(request3).execute();
                    if (response3.body() != null){
                        key_file = response3.body().bytes();
                    }
                    response3.close();

                    String iv_text = "";
                    Matcher matcher_iv = Pattern.compile("IV=([a-z0-9A-Z]+)").matcher(playlistHls);
                    if (matcher_iv.find()){
                        iv_text = matcher_iv.group(1);
                        //System.out.println(iv_text);
                    }

                    // 復号化処理するための前処理
                    String s = iv_text.startsWith("0x") ? iv_text.substring(2) : iv_text;
                    byte[] iv = new BigInteger(s, 16).toByteArray();
                    //System.out.println(iv.length);
                    byte[] ivDataEncoded = new byte[16];
                    int offset = iv.length > 16 ? iv.length - 16 : 0;
                    System.arraycopy(
                            iv,
                            offset,
                            ivDataEncoded,
                            ivDataEncoded.length - iv.length + offset,
                            iv.length - offset);

                    Cipher decrypter = Cipher.getInstance("AES/CBC/NoPadding");

                    String tempFileName = "./temp"+new Date().getTime();
                    if (!new File(tempFileName).exists()){
                        new File(tempFileName).mkdir();
                    }

                    // m3u8保存
                    final String playlistHlsFinal = playlistHls;
                    StringBuilder sb = new StringBuilder();

                    for (String str : playlistHlsFinal.split("\n")){
                        if (str.startsWith("#")){
                            if (str.startsWith("#EXT-X-KEY:METHOD=AES-128")){
                                continue;
                            }
                            sb.append(str);
                            sb.append("\n");
                            continue;
                        }

                        sb.append(str.split("\\?")[0]);
                        sb.append("\n");
                    }

                    FileOutputStream m3u8_stream = new FileOutputStream(tempFileName + "/main.m3u8");
                    m3u8_stream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                    m3u8_stream.flush();
                    m3u8_stream.close();

                    decrypter.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key_file, "AES"), new IvParameterSpec(ivDataEncoded));

                    Matcher matcher2 = Pattern.compile("https://(.+)/playlist.m3u8").matcher(hlsPlaylistUrl);
                    String tsBaseUrl = "";
                    if (matcher2.find()){
                        tsBaseUrl = "https://"+matcher2.group(1)+"/";
                    }

                    for (String str : playlistHlsFinal.split("\n")){
                        if (str.startsWith("#")){
                            continue;
                        }

                        if (str.isEmpty()){
                            continue;
                        }

                        Request request4 = new Request.Builder()
                                .url(tsBaseUrl+str)
                                .addHeader("X-Frontend-Id", "6")
                                .addHeader("X-Frontend-Version", "0")
                                .addHeader("Access-Control-Request-Headers", "x-frontend-id,x-frontend-version")
                                .addHeader("Access-Control-Request-Method", "GET")
                                .addHeader("User-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:115.0) Gecko/20100101 Firefox/115.0 nico-proxy/1.0")
                                .build();

                        Response response4 = client.newCall(request4).execute();
                        if (response4.body() != null && response4.code() == 200){
                            byte[] bytes = response4.body().bytes();
                            //System.out.println(bytes.length);
                            //System.out.println(new String(bytes));
                            //new File(tempFileName + "/" + str.split("\\?")[0]).createNewFile();
                            System.out.println(tempFileName + "/" + str.split("\\?")[0]);
                            if (bytes.length % 16 == 0){
                                FileOutputStream stream = new FileOutputStream(tempFileName + "/" + str.split("\\?")[0]);
                                stream.write(decrypter.doFinal(bytes));
                                stream.close();
                            }
                        }
                        response4.close();
                    }
                }

            }
            timer.cancel();

        } catch (Exception e){
            e.printStackTrace();
        }


    }
}