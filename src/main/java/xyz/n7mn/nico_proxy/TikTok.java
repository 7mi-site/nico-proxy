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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TikTok implements ShareService{

    private final Pattern matcher_json = Pattern.compile("<script type=\"application/ld\\+json\" id=\"BreadcrumbList\">\\{(.*)\\}</script><style data-emotion=\"tiktok");
    private final Pattern matcher_DataJson = Pattern.compile("<script id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\" type=\"application/json\">(.+)</script><script data-chunk=\"webapp-desktop\" async src=\"https://sf16-website-login\\.neutral\\.ttwstatic\\.com/obj/tiktok_web_login_static/tiktok/webapp/main/webapp-desktop/runtime\\.ad47afed13cf237e8ae4\\.js\"></script>");

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        // https://www.tiktok.com/@komedascoffee/video/7258220227773746433
        String url = data.getURL();

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String[] split = url.split("/");
        int i = 0;
        for (String str : split){
            if (str.startsWith("video")){
                break;
            }
            i++;
        }

        final String id = split[i + 1];

        if (id.isEmpty()){
            throw new Exception("Video NotFound");
        }

        String HtmlText = "";
        String SetCookie = "";
        StringBuilder sb = new StringBuilder();
        try {
            Request request_html = new Request.Builder()
                    .url(data.getURL())
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            //System.out.println(response.header("Set-Cookie"));
            response.headers().forEach((value)->{
                //System.out.println(value.component1() + " : " + value.component2());
                if (value.component1().equalsIgnoreCase("set-cookie")){
                    sb.append(value.component2().split(";")[0]).append("; ");
                }
            });
            response.close();
        } catch (Exception e){
            throw new Exception("APIError : " + e.getMessage());
        }
        SetCookie = sb.substring(0, sb.length() - 2);
        //System.out.println(SetCookie);

        //System.out.println(HtmlText);

        Matcher matcher = matcher_DataJson.matcher(HtmlText);
        if (!matcher.find()){
            return null;
        }

        //System.out.println(matcher.group(1));

        JsonElement json = new Gson().fromJson(matcher.group(1), JsonElement.class);
        //JsonArray object = json.getAsJsonObject().getAsJsonArray("aweme_list").get(0).getAsJsonObject().get("video").getAsJsonObject().getAsJsonObject("play_addr").getAsJsonArray("url_list");
        //return new ResultVideoData(object.get(0).getAsString(), "", false, false, false, "");

        //throw new Exception("VideoURL Not Found");
        return new ResultVideoData(json.getAsJsonObject().get("__DEFAULT_SCOPE__").getAsJsonObject().get("webapp.video-detail").getAsJsonObject().get("itemInfo").getAsJsonObject().get("itemStruct").getAsJsonObject().get("video").getAsJsonObject().get("downloadAddr").getAsString(), "", false, false, false, SetCookie, null);
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // 実装予定はなし
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        String title = "";

        String HtmlText = "";
        Request request_html = new Request.Builder()
                .url(data.getURL())
                .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                .build();
        try {
            Response response = client.newCall(request_html).execute();
            if (response.body() != null) {
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            return "";
        }

        Matcher matcher = matcher_json.matcher(HtmlText);

        if (!matcher.find()){
            return "";
        }

        String jsonText = "{"+matcher.group(1)+"}";
        //System.out.println(jsonText);

        JsonElement json = new Gson().fromJson(jsonText, JsonElement.class);
        title = json.getAsJsonObject().getAsJsonArray("itemListElement").get(2).getAsJsonObject().get("item").getAsJsonObject().get("name").getAsString();
        title = title.split(" \\| TikTok")[0];

        return title;
    }

    @Override
    public String getServiceName() {
        return "TikTok";
    }

    @Override
    public String getVersion() {
        return "20230909";
    }
}
