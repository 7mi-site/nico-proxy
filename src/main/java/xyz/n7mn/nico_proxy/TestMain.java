package xyz.n7mn.nico_proxy;


import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TestMain {

    private static final String ffmpegPass = "ffmpeg";
    private static final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) throws Exception {

        try{
            if (new File("./temp").mkdir()){
                BilibiliCom bilibiliCom = new BilibiliCom();
                ResultVideoData video = bilibiliCom.getVideo(new RequestVideoData("https://www.bilibili.com/video/BV1y5411Y7A2/?spm_id_from=333.999.0.0", null));

                String[] split = video.getVideoURL().split("\\?")[0].split("/");
                String fileName = split[split.length - 1];
                System.out.println("video : "+ fileName);

                Request request = new Request.Builder()
                        .url(video.getVideoURL())
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    if (response.body() != null){
                        FileOutputStream stream = new FileOutputStream("./temp/" + fileName);
                        stream.write(response.body().bytes());
                        stream.close();
                    }
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String[] split2 = video.getAudioURL().split("\\?")[0].split("/");
                String fileName2 = split2[split2.length - 1];
                System.out.println("audio : "+ fileName2);
                Request request2 = new Request.Builder()
                        .url(video.getAudioURL())
                        .addHeader("Referer", "https://www.bilibili.com/")
                        .build();
                try {
                    Response response2 = client.newCall(request2).execute();
                    if (response2.body() != null){
                        FileOutputStream stream = new FileOutputStream("./temp/" + fileName2);
                        stream.write(response2.body().bytes());
                        stream.close();
                    }
                    response2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String str = ffmpegPass+" -i ./temp/"+fileName+" -i ./temp/"+fileName2+" -c:v copy -c:a copy ./video.mp4";
                System.out.println(str);

                try {
                    Runtime runtime = Runtime.getRuntime();
                    Process exec = runtime.exec(str);
                    exec.waitFor();
                } catch (IOException e) {
                    e.fillInStackTrace();
                }
            }

            File[] files = new File("./temp").listFiles();
            if (files != null){
                for (File file : files) {
                    file.delete();
                }
            }

            new File("./temp").delete();


        } catch (Exception e){
            e.printStackTrace();
        }

    }
}