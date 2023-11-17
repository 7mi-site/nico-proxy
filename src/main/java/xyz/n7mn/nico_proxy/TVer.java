package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVer implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://tver.jp/episodes/epq882oemn
        Matcher matcher = Pattern.compile("https://tver.jp/episodes/(.+)").matcher(data.getURL());

        if (!matcher.find()){
            throw new Exception("Not Support URL");
        }

        String id = matcher.group(1);

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        Request request1 = new Request.Builder()
                .url("https://platform-api.tver.jp/v2/api/public/current_time")
                .build();

        Response response1 = client.newCall(request1).execute();
        response1.close();

        String jsonText = "";

        Request request2 = new Request.Builder()
                .url("https://platform-api.tver.jp/v2/api/platform_users/info?platform_uid=b7cd9e1b8ae94ed3865233c74ccfcf5d47f5&platform_token=xxyjo2stlpqv5a93plut3u4bcr08fwcv395t454c")
                .build();

        Response response2 = client.newCall(request2).execute();
        response2.close();

        //System.out.println(jsonText);

        String videoRefID = "";
        String accountID = "";
        Request request3 = new Request.Builder()
                .url("https://statics.tver.jp/content/episode/"+id+".json?v=13")
                .build();
        Response response3 = client.newCall(request3).execute();
        if (response3.body() != null){
            jsonText = response3.body().string();
        }
        response3.close();
        //System.out.println(jsonText);
        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
        videoRefID = json.getAsJsonObject().getAsJsonObject("video").get("videoRefID").getAsString();
        accountID = json.getAsJsonObject().getAsJsonObject("video").get("accountID").getAsString();


        Request request4 = new Request.Builder()
                .url("https://edge.api.brightcove.com/playback/v1/accounts/"+accountID+"/videos/ref%3A"+videoRefID)
                .addHeader("Accept", "application/json;pk=BCpkADawqM2XqfdZX45o9xMUoyUbUrkEjt-dMFupSdYwCw6YH7Dgd_Aj4epNSPEGgyBOFGHmLa_IPqbf8qv8CWSZaI_8Cd8xkpoMSNkyZrzzX7_TGRmVjAmZ_q_KxemVvC2gsMyfCqCzRrRx")
                .build();
        Response response4 = client.newCall(request4).execute();
        if (response4.body() != null){
            jsonText = response4.body().string();
        }
        response4.close();
        //System.out.println(jsonText);
        json = new Gson().fromJson(jsonText, JsonElement.class);

        String url = json.getAsJsonObject().getAsJsonArray("sources").get(0).getAsJsonObject().get("src").getAsString();
        return new ResultVideoData(url, null, true, false, false, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        //
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        Matcher matcher_video = Pattern.compile("https://tver.jp/episodes/(.+)").matcher(data.getURL());

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        boolean isVideo = matcher_video.find();

        String id = "";
        if (isVideo){
            id = matcher_video.group(1);
        }


        if (isVideo){
            Request request1 = new Request.Builder()
                    .url("https://platform-api.tver.jp/v2/api/public/current_time")
                    .build();

            Response response1 = client.newCall(request1).execute();
            response1.close();

            Request request2 = new Request.Builder()
                    .url("https://platform-api.tver.jp/v2/api/platform_users/info?platform_uid=b7cd9e1b8ae94ed3865233c74ccfcf5d47f5&platform_token=xxyjo2stlpqv5a93plut3u4bcr08fwcv395t454c")
                    .build();

            String jsonText = "";
            Response response2 = client.newCall(request2).execute();
            response2.close();

            Request request3 = new Request.Builder()
                    .url("https://statics.tver.jp/content/episode/"+id+".json?v=13")
                    .build();
            Response response3 = client.newCall(request3).execute();
            if (response3.body() != null){
                jsonText = response3.body().string();
            }
            response3.close();

            JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
            return json.getAsJsonObject().get("title").getAsString();
        }

        return null;
    }

    @Override
    public String getServiceName() {
        return null;
    }

    @Override
    public String getVersion() {
        return "20231117";
    }
}
