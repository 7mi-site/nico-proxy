package xyz.n7mn.nico_proxy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.n7mn.nico_proxy.data.RequestVideoData;
import xyz.n7mn.nico_proxy.data.ResultVideoData;
import xyz.n7mn.nico_proxy.twitter.variants;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.regex.Pattern;


public class Twitter implements ShareService{
    @Override
    public ResultVideoData getVideo(RequestVideoData data) throws Exception {

        String url = data.getURL();

        String[] split = url.split("/");
        int i = 0;
        for (String str : split) {
            if (str.startsWith("status")) {
                break;
            }
            i++;
        }

        final String id = split[i + 1].split("\\?")[0];

        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final OkHttpClient client = data.getProxy() != null ? builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getProxy().getProxyIP(), data.getProxy().getPort()))).build() : new OkHttpClient();

        if (id.length() == 0){
            throw new Exception("Tweet NotFound");
        }

        String HtmlText = "";
        try {

            Request build = new Request.Builder()
                    .url("https://twitter.com/i/api/graphql/zZXycP0V6H7m-2r0mOnFcA/TweetDetail?variables=%7B%22focalTweetId%22%3A%22"+id+"%22%2C%22includePromotedContent%22%3Atrue%2C%22with_rux_injections%22%3Afalse%2C%22withBirdwatchNotes%22%3Atrue%2C%22withCommunity%22%3Atrue%2C%22withDownvotePerspective%22%3Afalse%2C%22withQuickPromoteEligibilityTweetFields%22%3Atrue%2C%22withReactionsMetadata%22%3Afalse%2C%22withReactionsPerspective%22%3Afalse%2C%22withSuperFollowsTweetFields%22%3Atrue%2C%22withSuperFollowsUserFields%22%3Atrue%2C%22withV2Timeline%22%3Atrue%2C%22withVoice%22%3Atrue%7D&features=%7B%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Afalse%2C%22interactive_text_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22responsive_web_enhance_cards_enabled%22%3Atrue%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Afalse%2C%22responsive_web_text_conversations_enabled%22%3Afalse%2C%22responsive_web_uc_gql_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Afalse%2C%22tweetypie_unmention_optimization_enabled%22%3Atrue%2C%22unified_cards_ad_metadata_container_dynamic_card_content_query_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22vibe_api_enabled%22%3Atrue%7D")
                    .header("Authorization", "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA")
                    .header("x-csrf-token", "174097c3ada3e824616d5338c210941c2bed55939b4d212fb02f3020971a5317c67b03f0e87dbe91c0dea06c7e53d87e3fa4ccc86dace2c3d4aa84076326737ff8750c007cae988006707d844e200e45")
                    .header("X-Twitter-Auth-Type", "OAuth2Session")
                    .header("x-twitter-active-user", "true")
                    .header("x-twitter-client-language","ja")
                    .header("cookie","ads_prefs=\"HBESAAA=\"; auth_multi=\"1678721370526724096:a08de7549dff7ee5ab5978918dcc649ce0c6aa49|870902081623805953:36e990f954d561ab7e21fe765e69d68bd0e0fda2\"; auth_token=06b47828860aeede3637b571ee19a7822df6d5b2; ct0=174097c3ada3e824616d5338c210941c2bed55939b4d212fb02f3020971a5317c67b03f0e87dbe91c0dea06c7e53d87e3fa4ccc86dace2c3d4aa84076326737ff8750c007cae988006707d844e200e45; des_opt_in=Y; dnt=1; guest_id=v1%3A169253002497683811; guest_id_ads=v1%3A169253002497683811; guest_id_marketing=v1%3A169253002497683811; kdt=0tP2Nv808a1bb48csUnHNdNag4bfWSRyYFT1nikG; lv-uid=AAAAEIAhSF8rpp4eBW5we0k_eL6F2dH5jdAWisXImElAWJ-gnIKQrULhZXr5Jds1; mbox=PC#ae60c067e1624b62b343b53d292776e2.32_0#1755751186|session#dea5174e5a7f4e068f13ce12264841f1#1692508246; personalization_id=\"v1_LCrfJSzv7EPvAb2doPhaDg==\"; twid=u%3D1548673979992670209\\r\\n")
                    .build();

            Response response = client.newCall(build).execute();
            //System.out.println(response.code());
            if (response.body() != null){
                HtmlText = response.body().string();
            }
            response.close();
        } catch (Exception e){
            throw new Exception("APIError : " + e.getMessage());
        }

        boolean isTweet2 = false;
        if (Pattern.compile("Could not authenticate you").matcher(HtmlText).find()){
            isTweet2 = true;
            try {

                Request build = new Request.Builder()
                        .url("https://twitter.com/i/api/graphql/mbnjGF4gOwo5gyp9pe5s4A/TweetResultByRestId?variables=%7B%22tweetId%22%3A%22"+id+"%22%2C%22withCommunity%22%3Afalse%2C%22includePromotedContent%22%3Afalse%2C%22withVoice%22%3Afalse%7D&features=%7B%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22tweetypie_unmention_optimization_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Atrue%2C%22view_counts_everywhere_api_enabled%22%3Atrue%2C%22longform_notetweets_consumption_enabled%22%3Atrue%2C%22responsive_web_twitter_article_tweet_consumption_enabled%22%3Afalse%2C%22tweet_awards_web_tipping_enabled%22%3Afalse%2C%22responsive_web_home_pinned_timelines_enabled%22%3Afalse%2C%22freedom_of_speech_not_reach_fetch_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Atrue%2C%22longform_notetweets_rich_text_read_enabled%22%3Atrue%2C%22longform_notetweets_inline_media_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22responsive_web_media_download_video_enabled%22%3Afalse%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D")
                        .header("Authorization", "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA")
                        .header("X-Client-Transaction-Id", "ozoi3NmSR6Q+mm10a6SD6Ip273gbYKGGhsUVW72QUk6s1chfi1qeS14PIS0fkt/XDlZCcaNY2e8U09ILFtFf++WNxmR3og")
                        .header("x-guest-token", "1710897623740322249")
                        .header("x-twitter-active-user", "yes")
                        .header("x-twitter-client-language","ja")
                        .header("cookie", "guest_id_marketing=v1%3A169674475483006773; guest_id_ads=v1%3A169674475483006773; personalization_id=\"v1_IN9WfC2t02KPvj8nTdejeQ==\"; guest_id=v1%3A169674475483006773; gt=1710897623740322249")
                        .build();

                Response response = client.newCall(build).execute();
                //System.out.println(response.code());
                if (response.body() != null){
                    HtmlText = response.body().string();
                }
                response.close();
            } catch (Exception e){
                throw new Exception("APIError : " + e.getMessage());
            }
        }
        //System.out.println(HtmlText);
        //return null;
///*
        JsonElement json = new Gson().fromJson(HtmlText, JsonElement.class);
        try {
            JsonElement element;
            if (!isTweet2){
                element = json.getAsJsonObject().getAsJsonObject().get("data").getAsJsonObject().getAsJsonObject("threaded_conversation_with_injections_v2").getAsJsonArray("instructions").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("content")
                        .getAsJsonObject("itemContent").getAsJsonObject("tweet_results").getAsJsonObject("result").getAsJsonObject("legacy")
                        .getAsJsonObject("extended_entities").getAsJsonArray("media").get(0).getAsJsonObject().getAsJsonObject("video_info").getAsJsonArray("variants");//.get(0).getAsJsonObject().getAsJsonObject();
            } else {
                element = json.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("tweetResult").getAsJsonObject("result").getAsJsonObject("legacy").getAsJsonObject("entities").getAsJsonArray("media").get(0).getAsJsonObject().getAsJsonObject("video_info").getAsJsonArray("variants");
            }

            String tempJson = element.toString();

            variants[] variants = new Gson().fromJson(tempJson, variants[].class);

            int maxCount = -1;
            int maxBitrate = -1;

            int count = 0;
            for (variants var : variants){
                if (var.getBitrate() >= maxBitrate && var.getBitrate() >= 0){
                    maxCount = count;
                    maxBitrate = var.getBitrate();
                }
                //if (var.getBitrate() == 0 && var.getContent_type().equals("application/x-mpegURL")){
                //    maxCount = count;
                //    break;
                //}
                count++;
            }

            return new ResultVideoData(variants[maxCount].getUrl(),"", false, false, false, "");
        } catch (Exception e){
            throw new Exception("Tweet is Not VideoTweet");
        }
 //*/
    }

    @Override
    public ResultVideoData getLive(RequestVideoData data) throws Exception {
        // 実装予定はなし
        return null;
    }

    @Override
    public String getTitle(RequestVideoData data) throws Exception {
        return "";
    }

    @Override
    public String getServiceName() {
        return "Twitter";
    }

    @Override
    public String getVersion() {
        return "20231008";
    }
}
