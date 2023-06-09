package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.EncryptedTokenJSON;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TestMain {
    public static void main(String[] args)  {
        /*
        ニコ生
         */
        ShareService service = new NicoNicoVideo();
        ResultVideoData video;
        String URL;
        try {
            video = service.getLive(new RequestVideoData("https://live.nicovideo.jp/watch/lv341857869", null));
            URL = video.getVideoURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /*

        暗号化されたhlsを落としてくるテスト実装

         */
/*
        ShareService service = new NicoNicoVideo();
        ResultVideoData video;
        String URL;
        try {
            video = service.getVideo(new RequestVideoData("https://www.nicovideo.jp/watch/so42348316", null));
            URL = video.getVideoURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(URL);

        EncryptedTokenJSON json = new Gson().fromJson(video.getTokenJson(), EncryptedTokenJSON.class);
        String encryptedURL = json.getEncryptedURL();
        //System.out.println(encryptedURL);

        final OkHttpClient client = new OkHttpClient();

        try {
            // master.m3u8の中身を見る
            Request request1 = new Request.Builder()
                    .url(URL)
                    .build();
            Response response1 = client.newCall(request1).execute();

            // 予めURL分解しておく
            String[] split1 = URL.split("/");
            StringBuilder s1 = new StringBuilder();
            for (int i = 0; i < split1.length - 1; i++){
                s1.append(split1[i]);
                s1.append("/");
            }
            //System.out.println(URL);
            //System.out.println(s1);

            if (response1.body() != null){
                String[] split2 = response1.body().string().split("\n");
                // s1+split2[2] -> m3u8
                // s1+"1/ts/" -> ts

                // playlist.m3u8の中身を覗く
                Request request2 = new Request.Builder()
                        .url(s1+split2[2])
                        .build();

                Response response2 = client.newCall(request2).execute();
                String m3u8_text = "";
                if (response2.body() != null){
                    m3u8_text = response2.body().string();
                    System.out.println(m3u8_text);
                }
                response2.close();

                String[] split3 = m3u8_text.split("\n");
                StringBuilder builder = new StringBuilder();
                for (String str : split3){
                    if (str.startsWith("#")){
                        continue;
                    }

                    if (str.length() == 0){
                        continue;
                    }
                    builder.append(s1);
                    builder.append("1/ts/");
                    builder.append(str);
                    builder.append("\n");
                }

                byte[] key_file = new byte[0];
                Request request3 = new Request.Builder()
                        .url(encryptedURL)
                        .addHeader("X-Frontend-Id", "6")
                        .addHeader("X-Frontend-Version", "0")
                        .addHeader("Access-Control-Request-Headers", "x-frontend-id,x-frontend-version")
                        .addHeader("Access-Control-Request-Method", "GET")
                        .build();

                Response response3 = client.newCall(request3).execute();
                if (response3.body() != null){
                    key_file = response3.body().bytes();
                }
                response3.close();

                //System.out.println(m3u8_text);
                String iv_text = "";
                Matcher matcher_iv = Pattern.compile("IV=([a-z0-9A-Z]+)").matcher(m3u8_text);
                if (matcher_iv.find()){
                    iv_text = matcher_iv.group(1);
                    //System.out.println(matcher_iv.group(1));
                }

                // 復号化処理するための前処理
                String s = iv_text.startsWith("0x") ? iv_text.substring(2) : iv_text;
                byte[] iv = new BigInteger(s, 16).toByteArray();
                byte[] ivDataEncoded = new byte[16];
                int offset = iv.length > 16 ? iv.length - 16 : 0;
                System.arraycopy(
                        iv,
                        offset,
                        ivDataEncoded,
                        ivDataEncoded.length - iv.length + offset,
                        iv.length - offset);

                Cipher decrypter = Cipher.getInstance("AES/CBC/NoPadding");

                // 保存
                String[] split4 = builder.toString().split("\n");
                int i = 1;
                String tempFolder = "./temp_"+new Date().getTime();
                for (String url : split4){
                    //System.out.println(url);
                    Request request4 = new Request.Builder()
                            .url(url)
                            .build();
                    Response response4 = client.newCall(request4).execute();
                    if (response4.body() != null){
                        if (!new File(tempFolder).exists()){
                            new File(tempFolder).mkdir();
                        }

                        if (!new File("./" + tempFolder + "/" + i + ".ts").exists()){
                            new File("./" + tempFolder + "/" + i + ".ts").createNewFile();
                        }

                        decrypter.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key_file, "AES"), new IvParameterSpec(ivDataEncoded));

                        byte[] bytes = response4.body().bytes();
                        FileOutputStream stream = new FileOutputStream("./" + tempFolder + "/" + i + ".ts");
                        stream.write(decrypter.doFinal(bytes));
                        stream.close();
                    }
                    response4.close();
                    i++;
                }

            }
            response1.close();
        } catch (Exception e){
            e.printStackTrace();
        }
*/
    }
}