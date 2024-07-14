package xyz.n7mn.nico_proxy;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sonicbowl implements ShareService{

    private final Pattern matcher_URL = Pattern.compile("https://player\\.sonicbowl\\.cloud/episode/(.+)/");
    private final Pattern matcher_Audio = Pattern.compile("<meta property=\"og:audio\" content=\"(.+)\">");
    private final Pattern matcher_Title = Pattern.compile("<meta property=\"og:title\" content=\"(.+)\">");

    private final OkHttpClient.Builder builder = new OkHttpClient.Builder();

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        if (!matcher_URL.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        Request request = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        String result = "";
        try {
            Response response = client.newCall(request).execute();
            result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        Matcher matcher = matcher_Audio.matcher(result);
        if (matcher.find()){
            return new ResultVideoData(null, matcher.group(1), false, false, false, null);
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
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        if (!matcher_URL.matcher(data.getURL()).find()){
            throw new Exception("Not Support URL");
        }

        Request request = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();

        String result = "";
        try {
            Response response = client.newCall(request).execute();
            result = response.body().string();
            response.close();
        } catch (Exception e){
            throw e;
        }

        Matcher matcher = matcher_Title.matcher(result);
        if (matcher.find()){
            return matcher.group(1);
        } else {
            throw new Exception("Not Found");
        }


    }

    @Override
    public String getServiceName() {
        return "Sonicbowl";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
