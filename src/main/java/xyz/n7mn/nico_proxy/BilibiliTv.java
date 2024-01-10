package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.bilibili.BilibiliTvData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BilibiliTv implements ShareService{


    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://www.bilibili.tv/en/play/2091717
        //
        String s = data.getURL().split("\\?")[0];
        String[] strings = s.split("/");
        String id = strings[strings.length - 1];
        if (id.isEmpty()){
            id = strings[strings.length - 2];
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        final String JsonText;
        String tempText = null;

        Request api = new Request.Builder()
                .url("https://api.bilibili.tv/intl/gateway/web/playurl?s_locale=en_US&platform=web&ep_id=13011309&qn=64&type=0&device=wap&tf=0&spm_id=bstar-web.pgc-video-detail.0.0&from_spm_id=")
                .addHeader("Referer", data.getURL())
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Origin", "https://www.bilibili.tv")
                //.addHeader("Accept-Encoding","gzip")
                .addHeader("Cookie", "buvid3=fcec0a20-eb85-45e2-859d-f3d559285d8c12752infoc; bstar-web-lang=en")
                .build();


        try {
            Response response = client.newCall(api).execute();
            if (response.body() != null){
                tempText = response.body().string();
                System.out.println(tempText);
            }
            response.close();
        } catch (Exception e){
            throw new Exception("api.bilibili.tv "+ e.getMessage() + (data.getProxy() != null ? "(Use Proxy : "+data.getProxy().getProxyIP()+")" : ""));
        }
        JsonText = tempText;

        BilibiliTvData fromJson = new Gson().fromJson(JsonText, BilibiliTvData.class);
        if (fromJson.getData() == null){
            //System.out.println(JsonText);
            throw new Exception("api.bilibili.tv Not APIData");
        }
        String videoURL = fromJson.getData().getPlayurl().getVideo()[0].getVideo_resource().getUrl();
        String audioURL = fromJson.getData().getPlayurl().getAudio_resource()[0].getUrl();

        return new ResultVideoData(videoURL, audioURL, false, false, false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // 存在しないので実装しない
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String title = "";

        Request html = new Request.Builder()
                .url(data.getURL())
                .build();

        String string = "";
        try {
            Response response = client.newCall(html).execute();
            if (response.body() != null){
                string = response.body().string();
            }
            response.close();
        } catch (Exception e){
            return "";
        }

        //System.out.println(string);

        Matcher matcher = Pattern.compile("<script type=\"application/ld\\+json\">\\[(.*)\\]").matcher(string);
        if (!matcher.find()){
            return "";
        }

        String jsonText = "["+matcher.group(1)+"]";
        //System.out.println(jsonText);
        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
        title = json.getAsJsonArray().get(2).getAsJsonObject().get("name").getAsString().split(" - ")[0];

        return title;
    }

    @Override
    public String getServiceName() {
        return "bilibili.tv";
    }

    @Override
    public String getVersion() {
        return "2.1";
    }
}
