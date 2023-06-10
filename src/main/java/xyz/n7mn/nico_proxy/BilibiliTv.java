package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.bilibili.BilibiliTvData;
import xyz.n7mn.nico_proxy.data.ProxyData;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class BilibiliTv implements ShareService{

    @Override
    @Deprecated
    public String getVideo(String url, ProxyData proxy) throws Exception {
        return getVideo(new RequestVideoData(url, proxy)).getVideoURL();
    }

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://www.bilibili.tv/en/video/4786094886751232
        String s = data.getURL().split("\\?")[0];
        String[] strings = s.split("/");
        String id = strings[strings.length - 1];
        if (id.length() == 0){
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
    public String getLive(String url, ProxyData proxy) {
        // 存在しないので実装しない
        return null;
    }

    @Override
    @Deprecated
    public ResultVideoData getLive(RequestVideoData data) {
        return null;
    }

    @Override
    public String getServiceName() {
        return "bilibili.tv";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
