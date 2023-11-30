package xyz.n7mn.nico_proxy;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.*;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TestMain {

    public static void main(String[] args) throws Exception {
        ResultVideoData video = new NicoNicoVideo().getVideo(new RequestVideoData("https://www.nicovideo.jp/watch/sm43052342", null));

        final OkHttpClient client = new OkHttpClient();
        if (video.getAudioURL() != null){

            JsonElement json = new Gson().fromJson(video.getTokenJson(), JsonElement.class);

            String nicosid = json.getAsJsonObject().get("nicosid").getAsString();
            String domand_bid = json.getAsJsonObject().get("domand_bid").getAsString();
            String watchTrackId = json.getAsJsonObject().get("watchTrackId").getAsString();
            String contentId = json.getAsJsonObject().get("contentId").getAsString();

            Request request_video_m3u8 = new Request.Builder()
                    .url(video.getVideoURL())
                    .addHeader("Cookie", "nicosid="+nicosid+"; domand_bid=" + domand_bid)
                    .build();
            Response response1 = client.newCall(request_video_m3u8).execute();

            String video_m3u8 = "";
            if (response1.body() != null){
                video_m3u8 = response1.body().string();
            }
            //System.out.println(video_m3u8);
            response1.close();

            Request request_audio_m3u8 = new Request.Builder()
                    .url(video.getAudioURL())
                    .addHeader("Cookie", "nicosid="+nicosid+"; domand_bid=" + domand_bid)
                    .build();
            Response response2 = client.newCall(request_audio_m3u8).execute();
            String audio_m3u8 = "";
            if (response2.body() != null){
                audio_m3u8 = response2.body().string();
            }
            response2.close();

            // 前準備
            String[] split = UUID.randomUUID().toString().split("-");
            final String basePass = "./" + new Date().getTime() + "_" + split[0] + "/";
            final String videoPass = basePass + "video/";
            final String audioPass = basePass + "audio/";
            File file1 = new File(basePass);
            File file2 = new File(videoPass);
            File file3 = new File(audioPass);

            if (!file1.exists()){
                file1.mkdir();
            }
            if (!file2.exists()){
                file2.mkdir();
            }
            if (!file3.exists()){
                file3.mkdir();
            }

            // 動画
            Matcher matcher1 = Pattern.compile("#EXT-X-KEY:METHOD=(.+),URI=\"(.+)\",IV=([a-z0-9A-Z]+)").matcher(video_m3u8);
            Matcher matcher1_1 = Pattern.compile("#EXT-X-MAP:URI=\"(.+)\"").matcher(video_m3u8);
            Matcher matcher2 = Pattern.compile("#EXT-X-KEY:METHOD=(.+),URI=\"(.+)\",IV=([a-z0-9A-Z]+)").matcher(audio_m3u8);
            StringBuilder sb = new StringBuilder();
            List<String> videoUrl = new ArrayList<>();
            int i = 1;
            for (String str : video_m3u8.split("\n")){
                if (str.startsWith("#")){
                    if (str.startsWith("#EXT-X-MAP") || str.startsWith("#EXT-X-KEY")){
                        if (str.startsWith("#EXT-X-KEY")){
                            sb.append("#EXT-X-MAP:URI=\"init01.cmfv\"\n");
                        }
                        continue;
                    }
                    sb.append(str).append("\n");
                    continue;
                }

                videoUrl.add(str);
                sb.append(i).append(".cmfv\n");
                i++;
            }

            FileOutputStream m3u8_stream = new FileOutputStream(videoPass + "video.m3u8");
            m3u8_stream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            m3u8_stream.flush();
            m3u8_stream.close();

            final String VideoKeyURL;
            final String VideoInitURL;
            final String VideoKeyIV;
            final String AudioKeyURL;
            final String AudioKeyIV;


            if (matcher1.find()){
                VideoKeyURL = matcher1.group(2);
                VideoKeyIV = matcher1.group(3);
            } else {
                VideoKeyURL = "";
                VideoKeyIV = "";
            }
            if (matcher1_1.find()){
                VideoInitURL = matcher1_1.group(1);
            } else {
                VideoInitURL = "";
            }
            if (matcher2.find()){
                AudioKeyURL = matcher2.group(2);
                AudioKeyIV = matcher2.group(3);
            } else {
                AudioKeyURL = "";
                AudioKeyIV = "";
            }

            new Thread(()->{

                // 復号化処理するための前処理
                String s = VideoKeyIV.startsWith("0x") ? VideoKeyIV.substring(2) : VideoKeyIV;
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

                Cipher decrypter = null;
                try {
                    byte[] key_file = new byte[0];
                    Request request_key = new Request.Builder()
                            .url(VideoKeyURL)
                            .addHeader("Cookie", "nicosid="+nicosid+"; domand_bid=" + domand_bid)
                            .build();

                    Response response_key = client.newCall(request_key).execute();
                    if (response_key.body() != null){
                        //System.out.println(response_key.code());
                        key_file = response_key.body().bytes();
                    }
                    response_key.close();

                    decrypter = Cipher.getInstance("AES/CBC/NoPadding");
                    decrypter.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key_file, "AES"), new IvParameterSpec(ivDataEncoded));
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | IOException | InvalidKeyException e) {
                    //e.printStackTrace();
                }

                try {
                    Request request_init = new Request.Builder()
                            .url(VideoInitURL)
                            .addHeader("Cookie", "nicosid="+nicosid+"; domand_bid=" + domand_bid)
                            .build();
                    Response response_init = client.newCall(request_init).execute();
                    if (response_init.body() != null){
                        byte[] bytes = response_init.body().bytes();
                        FileOutputStream stream = new FileOutputStream(videoPass + "init01.cmfv");
                        stream.write(bytes);
                        stream.close();
                    }
                    response_init.close();
                } catch (Exception e){
                    //e.printStackTrace();
                }

                int x = 1;
                for (String url : videoUrl){
                    try {
                        //System.out.println("- "+url+" --");
                        // https://asset.domand.nicovideo.jp/655c8569f16a0601757053f4/video/12/video-h264-720p/01.cmfv
                        Request request = new Request.Builder()
                                .url(url)
                                .addHeader("Cookie", "nicosid="+nicosid+"; domand_bid=" + domand_bid)
                                .build();
                        Response response = client.newCall(request).execute();
                        if (response.body() != null){
                            byte[] bytes = response.body().bytes();
                            if (bytes.length % 16 == 0){
                                FileOutputStream stream = new FileOutputStream(videoPass + x + ".cmfv");
                                if (decrypter != null){
                                    stream.write(decrypter.doFinal(bytes));
                                }
                                stream.close();
                            } else {
                                System.out.println(new String(bytes, StandardCharsets.UTF_8));
                            }
                        }
                        response.close();
                        x++;
                    } catch (Exception e){
                        //e.printStackTrace();
                    }
                }

            }).start();

            // 音声
            //System.out.println(audio_m3u8);


        }

    }
}