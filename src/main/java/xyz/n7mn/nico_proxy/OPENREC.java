package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OPENREC implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        // https://www.openrec.tv/movie/p2zj7wj1nzw

        String hlsURL = getHlsURL(data.getURL(), data);
        if (hlsURL == null){
            throw new Exception("No Video");
        }

        return new ResultVideoData(hlsURL, "", true, false, false, "");
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // https://www.openrec.tv/live/em8xg1lwvr2

        String hlsURL = getHlsURL(data.getURL(), data);
        if (hlsURL == null){
            throw new Exception("No Video");
        }

        return new ResultVideoData(hlsURL, "", true, false, true, "");
    }

    private String getHlsURL(String url, RequestVideoData data){

        try {
            String[] split = url.split("/");
            String id = split[split.length - 1];

            final OkHttpClient.Builder builder = new OkHttpClient.Builder();
            final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

            Request build = new Request.Builder()
                    .url("https://public.openrec.tv/external/api/v5/movies/" + id)
                    .build();

            Response response = client.newCall(build).execute();

            String HtmlText = "";
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
            //System.out.println(HtmlText);

            JsonElement json = new Gson().fromJson(HtmlText, JsonElement.class);

            JsonElement element = json.getAsJsonObject().getAsJsonObject("media").getAsJsonObject();

            String tempJson = element.toString();

            Matcher matcher = Pattern.compile("\"url\":\"(.*)\",\"url_s").matcher(tempJson);
            if (matcher.find()) {
                return matcher.group(1);
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getServiceName() {
        return "Openrec";
    }

    @Override
    public String getVersion() {
        return "20230822";
    }
}
