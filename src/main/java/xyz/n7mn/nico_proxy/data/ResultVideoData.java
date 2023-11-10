package xyz.n7mn.nico_proxy.data;

public class ResultVideoData {

    private final String VideoURL;
    private final String AudioURL;
    private final boolean isHLS;
    private final boolean isEncrypted;
    private final boolean isStream;
    private final String TokenJson;
    private final String CaptionData;

    public ResultVideoData(String videoURL, String audioURL, boolean isHLS, boolean isEncrypted, boolean isStream, String tokenJson){
        this.VideoURL = videoURL;
        this.AudioURL = audioURL;
        this.isHLS = isHLS;
        this.isEncrypted = isEncrypted;
        this.isStream = isStream;
        this.TokenJson = tokenJson;
        this.CaptionData = null;
    }
    public ResultVideoData(String videoURL, String audioURL, boolean isHLS, boolean isEncrypted, boolean isStream, String tokenJson, String captionData){
        this.VideoURL = videoURL;
        this.AudioURL = audioURL;
        this.isHLS = isHLS;
        this.isEncrypted = isEncrypted;
        this.isStream = isStream;
        this.TokenJson = tokenJson;
        this.CaptionData = captionData;
    }

    public String getVideoURL() {
        return VideoURL;
    }

    public String getAudioURL() {
        return AudioURL;
    }

    public boolean isHLS() {
        return isHLS;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public boolean isStream() {
        return isStream;
    }

    public String getTokenJson() {
        return TokenJson;
    }

    public String getCaptionData() {
        return CaptionData;
    }
}
