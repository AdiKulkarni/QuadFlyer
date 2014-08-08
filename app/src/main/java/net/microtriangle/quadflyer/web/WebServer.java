package net.microtriangle.quadflyer.web;

import android.content.Context;
import android.content.SharedPreferences;

import net.microtriangle.quadflyer.MjpegHelper;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by soedar on 4/8/14.
 */
public class WebServer extends NanoHTTPD {
    private Context context;

    public WebServer(int port, Context context) {
        super(port);
        this.context = context;
    }

    @Override
    public NanoHTTPD.Response serve(IHTTPSession session) {
        String uri = session.getUri();

        try {
            if ("/videostream".equals(uri)) {
                return new MjpegResponse();
            } else if ("/video".equals(uri)) {
                return new Response("<img src='/videostream'>");
            } else if ("/".equals(uri)) {
                return new Response(Response.Status.OK, MIME_HTML, context.getAssets().open("web/index.html"));
            } else if ("/api/videoData".equals(uri)) {
                Map<String, String> params = session.getParms();
                String key = params.get("key");
                String value = params.get("value");

                if (key != null && value != null) {
                    if (!editVideoSettings(key, value)) {
                        return new Response(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Bad Request");
                    }
                }
                return new Response(Response.Status.OK, "application/json", getVideoData());
            } else {
                String mimeType = MIME_HTML;
                if (uri.contains(".js")) {
                    mimeType = "text/javascript";
                } else if (uri.contains(".css")) {
                    mimeType = "text/css";
                }
                return new Response(Response.Status.OK, mimeType, context.getAssets().open("web/" + uri.substring(1)));
            }
        } catch (IOException e) {
            return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
        } catch (Exception e) {
            return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal Error");
        }
    }

    private String getVideoData() {
        SharedPreferences preference = context.getSharedPreferences("quadflyer.video", Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        Map<String, ?> videoData = preference.getAll();
        for (Map.Entry<String, ?>entry : videoData.entrySet()) {
            sb.append("\"");
            sb.append(entry.getKey());
            sb.append("\":");
            sb.append(entry.getValue());
            sb.append(",");
        }

        sb.append("\"version\": 0");
        sb.append("}");
        return sb.toString();
    }

    private boolean editVideoSettings(String key, String value) {
        if ("quality".equals(key)) {
            MjpegHelper.getInstance().setQuality(Integer.parseInt(value));
            return true;
        } else if ("zoom".equals(key)) {
            MjpegHelper.getInstance().setZoom(Integer.parseInt(value));
            return true;
        } else if ("flash".equals(key)) {
            MjpegHelper.getInstance().setFlash(Boolean.parseBoolean(value));
            return true;
        } else if ("delay".equals(key)) {
            MjpegHelper.getInstance().setDelay(Integer.parseInt(value));
            return true;
        } else if ("resolution".equals(key)) {
            MjpegHelper.getInstance().setResolution(Integer.parseInt(value));
            return true;
        }

        return false;
    }
}
