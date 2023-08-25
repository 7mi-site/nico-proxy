package xyz.n7mn.nico_proxy.twitter;

public class variants {

    /*
    [
    {
        "bitrate": 256000,
        "content_type": "video/mp4",
        "url": "https://video.twimg.com/ext_tw_video/1688175068118159360/pu/vid/336x270/44NmNQynMJFju8hf.mp4?tag=12"
    },
    {
        "bitrate": 832000,
        "content_type": "video/mp4",
        "url": "https://video.twimg.com/ext_tw_video/1688175068118159360/pu/vid/450x360/46rVozhXasnuQe9k.mp4?tag=12"
    },
    {
        "bitrate": 2176000,
        "content_type": "video/mp4",
        "url": "https://video.twimg.com/ext_tw_video/1688175068118159360/pu/vid/900x720/DC92lJTInXrE6B_k.mp4?tag=12"
    },
    {
        "content_type": "application/x-mpegURL",
        "url": "https://video.twimg.com/ext_tw_video/1688175068118159360/pu/pl/cjEtZasimmQDemiN.m3u8?tag=12&container=fmp4&v=1b5"
    }
]
     */

    private int bitrate;
    private String content_type;
    private String url;

    public variants(int bitrate, String content_type, String url){
        this.bitrate = bitrate;
        this.content_type = content_type;
        this.url = url;
    }

    public variants(String content_type, String url){
        this.bitrate = Integer.MAX_VALUE;
        this.content_type = content_type;
        this.url = url;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public String getContent_type() {
        return content_type;
    }

    public void setContent_type(String content_type) {
        this.content_type = content_type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
