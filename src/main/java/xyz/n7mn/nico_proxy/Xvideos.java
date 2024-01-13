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


public class Xvideos implements ShareService{

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        String url = data.getURL();

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0")
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        //System.out.println(HtmlText);

        Matcher matcher = Pattern.compile("html5player\\.setVideoUrlHigh\\('(.*)'\\)").matcher(HtmlText);
        if (matcher.find()){
            return new ResultVideoData(matcher.group(1), "", false, false, false, "");
        }

        throw new Exception("VideoURL Not Found");
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // なし
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        String title = "";
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url(data.getURL())
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        //System.out.println(HtmlText);

        Matcher matcher = Pattern.compile("html5player.setVideoTitle\\('(.*)'\\);").matcher(HtmlText);

        if (!matcher.find()){
            return "";
        }

        title = matcher.group(1);

        return title;
    }

    @Override
    public String getServiceName() {
        return "XVIDEOS.com";
    }

    @Override
    public String getVersion() {
        return "20230909";
    }
}
