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
        try {
            Request request_html = new Request.Builder()
                    .url("https://api16-normal-c-useast1a.tiktokv.com/aweme/v1/feed/?aweme_id="+id+"&version_name=26.1.3&version_code=260103&build_number=26.1.3&manifest_version_code=260103&update_version_code=260103&openudid=7fa1e2eb684ec9c2&uuid=5697458837675649&_rticket=1690897755327&ts=1690897755&device_brand=Google&device_type=Pixel+4&device_platform=android&resolution=1080%2A1920&dpi=420&os_version=10&os_api=29&carrier_region=US&sys_region=US&region=US&app_name=trill&app_language=en&language=en&timezone_name=America%2FNew_York&timezone_offset=-14400&channel=googleplay&ac=wifi&mcc_mnc=310260&is_my_cn=0&aid=1180&ssmix=a&as=a1qwert123&cp=cbfhckdckkde1")
                    .addHeader("User-Agent", Constant.nico_proxy_UserAgent)
                    .build();
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception("APIError : " + e.getMessage());
        }

        JsonElement json = new Gson().fromJson(HtmlText, JsonElement.class);
        JsonArray object = json.getAsJsonObject().getAsJsonArray("aweme_list").get(0).getAsJsonObject().get("video").getAsJsonObject().getAsJsonObject("play_addr").getAsJsonArray("url_list");
        return new ResultVideoData(object.get(0).getAsString(), "", false, false, false, "");

        //throw new Exception("VideoURL Not Found");
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
