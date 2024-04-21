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

public class Iwara implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://www.iwara.tv/video/vwvOcGMRQyvlwD/56-iochi-mari

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        //System.out.println(data.getURL());
        String[] split = data.getURL().split("/");

        if (split.length < 5){
            throw new Exception("Not Support URL");
        }

        // https://api.iwara.tv/video/vwvOcGMRQyvlwD
        //System.out.println("https://api.iwara.tv/video/" + split[4]);
        Request request_api = new Request.Builder()
                .url("https://api.iwara.tv/video/" + split[4])
                .build();

        String api_result = "";

        try {
            Response response = client.newCall(request_api).execute();
            api_result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        //System.out.println(api_result);
        JsonElement json = new Gson().fromJson(api_result, JsonElement.class);
        if (json.getAsJsonObject().has("fileUrl")){

            Request request_api2 = new Request.Builder()
                    .url(json.getAsJsonObject().get("fileUrl").getAsString())
                    .build();

            try {
                Response response2 = client.newCall(request_api2).execute();
                api_result = response2.body().string();
                response2.close();
            } catch (Exception e){
                throw e;
            }

            System.out.println(api_result);

            json = new Gson().fromJson(api_result, JsonElement.class);

            return new ResultVideoData(json.getAsJsonArray().get(0).getAsJsonObject().get("src").getAsJsonObject().get("download").getAsString(), null, false, false, false, null);

        } else {
            throw new Exception("Not Found");
        }
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        //System.out.println(data.getURL());
        String[] split = data.getURL().split("/");

        if (split.length < 5){
            throw new Exception("Not Support URL");
        }

        // https://api.iwara.tv/video/vwvOcGMRQyvlwD
        //System.out.println("https://api.iwara.tv/video/" + split[4]);
        Request request_api = new Request.Builder()
                .url("https://api.iwara.tv/video/" + split[4])
                .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0")
                .build();

        String api_result = "";

        try {
            Response response = client.newCall(request_api).execute();
            api_result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        //System.out.println(api_result);

        JsonElement json = new Gson().fromJson(api_result, JsonElement.class);

        if (json.getAsJsonObject().has("title")){
            return json.getAsJsonObject().get("title").getAsString();
        }

        return "";
    }

    @Override
    public String getServiceName() {
        return "iwara";
    }

    @Override
    public String getVersion() {
        return "1.0-20240421";
    }
}
