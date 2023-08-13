package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class Twitter implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        // https://twitter.com/miigo_Nazuti_MH/status/1689659633642909696?s=20
        String url = data.getURL();

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String[] split = url.split("/");
        int i = 0;
        for (String str : split){
            if (str.startsWith("status")){
                break;
            }
            i++;
        }

        final String id = split[i + 1].split("\\?")[0];

        if (id.length() == 0){
            throw new Exception("Video NotFound");
        }

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url("https://cdn.syndication.twimg.com/tweet-result?id="+id)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception("APIError : " + e.getMessage());
        }

        //System.out.println(HtmlText);
        //return null;

        JsonElement json = new Gson().fromJson(HtmlText, JsonElement.class);
        try {
            JsonElement element = json.getAsJsonObject().getAsJsonArray("mediaDetails").get(0).getAsJsonObject().get("video_info").getAsJsonObject().getAsJsonArray("variants").get(0).getAsJsonObject().get("url");
            return new ResultVideoData(element.getAsString(),"", false, false, false, "");
        } catch (Exception e){
            throw new Exception("Tweet is Not VideoTweet");
        }
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // 実装予定はなし
        return null;
    }

    @Override
    public String getServiceName() {
        return "TikTok";
    }

    @Override
    public String getVersion() {
        return "20230729";
    }
}
