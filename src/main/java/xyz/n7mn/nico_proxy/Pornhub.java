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

public class Pornhub implements ShareService{

    private final Pattern Support_URL1 = Pattern.compile("https://(.*)\\.pornhub\\.com/view_video\\.php\\?viewkey=(.*)");
    private final Pattern matcher_json = Pattern.compile("var flashvars_(\\d+) = \\{(.*)\\}\\;");
    private final Pattern matcher_title = Pattern.compile("<meta name=\"twitter:title\" content=\"(.*)\">");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        Matcher matcher = Support_URL1.matcher(data.getURL());
        if (!matcher.find()){
            throw new Exception("Not Found");
        }
        String id = matcher.group(2);
        //System.out.println(id);


        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url("https://jp.pornhub.com/view_video.php?viewkey="+id)
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
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
        Matcher matcher1 = matcher_json.matcher(HtmlText);

        if (!matcher1.find()){
            //throw new Exception("Not Found");
            return null;
        }

        String mediaUrlTemp = "{"+matcher1.group(2)+"}";
        //System.out.println(mediaUrlTemp);

        JsonElement json = new Gson().fromJson(mediaUrlTemp, JsonElement.class);
        JsonElement url = json.getAsJsonObject().getAsJsonArray("mediaDefinitions").get(0).getAsJsonObject().get("videoUrl");


        return new ResultVideoData(url.getAsString(), "", true, false, false, "");
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        String title = "";

        Matcher matcher = Support_URL1.matcher(data.getURL());
        if (!matcher.find()){
            throw new Exception("Not Found");
        }
        String id = matcher.group(2);
        //System.out.println(id);


        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String HtmlText = "";
        try {
            Request request_html = new Request.Builder()
                    .url("https://jp.pornhub.com/view_video.php?viewkey="+id)
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            return "";
        }

        Matcher matcher1 = matcher_title.matcher(HtmlText);
        if (!matcher1.find()){
            //System.out.println("naiyo");
            return title;
        }

        title = matcher1.group(1);

        return title;
    }

    @Override
    public String getServiceName() {
        return "Pornhub";
    }

    @Override
    public String getVersion() {
        return "20240502";
    }
}
