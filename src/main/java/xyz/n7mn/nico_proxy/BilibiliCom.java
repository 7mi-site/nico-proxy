package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BilibiliCom implements ShareService{

    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {
        String s = data.getURL().split("\\?")[0];
        String[] strings = s.split("/");
        String id = strings[strings.length - 1];
        if (id.length() == 0 || id.startsWith("?")){
            id = strings[strings.length - 2];
        }

        //System.out.println("debug id : "+id);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();
        final String HtmlText;
        Request request_html = new Request.Builder()
                .url("https://api.bilibili.com/x/web-interface/view?bvid="+id)
                .build();

        try {
            Response response = client.newCall(request_html).execute();
            if (response.body() != null){
                HtmlText = response.body().string();
            } else {
                HtmlText = "";
            }
            response.close();
        } catch (IOException e) {
            throw new Exception("api.bilibili.com/x/web-interface " + e.getMessage() + (data.getProxy() == null ? "" : "(Use Proxy : "+data.getProxy().getProxyIP()+")"));
        }

        //
        Matcher matcher = Pattern.compile("\"cid\":(\\d+),").matcher(HtmlText);
        if (!matcher.find()){
            throw new Exception("api.bilibili.com (Not cid Found)");
        }

        String cid = matcher.group(1);

        //System.out.println(cid);

        final String ResultText;
        Request request_api = new Request.Builder()
                .url("https://api.bilibili.com/x/player/playurl?bvid="+id+"&cid="+cid)
                .build();

        try {
            Response response2 = client.newCall(request_api).execute();
            if (response2.body() != null){
                ResultText = response2.body().string();
            } else {
                ResultText = "";
            }
            response2.close();
        } catch (IOException e) {
            throw new Exception("api.bilibili.com/x/player " + e.getMessage() + (data.getProxy() == null ? "" : "(Use Proxy : "+data.getProxy().getProxyIP()+")"));
        }

        Matcher matcher2 = Pattern.compile("\"url\":\"(.*)\",\"backup_url\"").matcher(ResultText);
        Matcher matcher3 = Pattern.compile(",\"backup_url\":(.*)\\}\\],\"support_formats\":").matcher(ResultText);
        final String temp_url;
        if (matcher2.find()){
            temp_url = matcher2.group(1).replaceAll("\\\\u0026","&");
        } else {
            temp_url = null;
        }

        String temp = null;
        if (matcher3.find()){
            temp = matcher3.group(1).replaceAll("\\\\u0026", "&");
        }

        String temp_backupUrl = "";
        //System.out.println(temp);
        if (temp != null && temp.startsWith("[")){

            JsonArray json = new Gson().fromJson(temp, JsonArray.class);
            for (int i = 0; i < json.size(); i++){
                String str = json.get(i).getAsString();
                if (str.startsWith("https://upos-hz-mirrorakam.akamaized.net/")){
                    temp_backupUrl = str.replaceAll("\\\\u003d", "=");
                }
            }
        }


        //System.out.println("temp : "+temp_url);
        if (temp_url != null && !temp_url.startsWith("https://upos-hz-mirrorakam.akamaized.net/")){
            return new ResultVideoData(temp_backupUrl, null, false, false, false, null);
        }

        return new ResultVideoData(temp_url, null, false, false, false, null);
    }


    @Override
    public ResultVideoData getLive(RequestVideoData data) {
        return null;
    }

    @Override
    public String getServiceName() {
        return "bilibili";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
