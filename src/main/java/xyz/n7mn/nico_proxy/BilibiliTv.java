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

    private final Pattern matcher_title = Pattern.compile("<script type=\"application/ld\\+json\">\\[(.*)\\]");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://www.bilibili.tv/en/video/4786094886751232
        String s = data.getURL().split("\\?")[0];
        String[] strings = s.split("/");
        String id = strings[strings.length - 1];
        if (id.isEmpty()){
            id = strings[strings.length - 2];
        }
        Request api = new Request.Builder()
                .url("https://api.bilibili.tv/intl/gateway/web/playurl?s_locale=en_US&platform=web&aid="+id)
                .build();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        final String JsonText;
        String tempText = null;

        try {
            Response response = client.newCall(api).execute();
            if (response.body() != null){
                tempText = response.body().string();
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

        Matcher matcher = matcher_title.matcher(string);
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
        return "2.0";
    }
}
