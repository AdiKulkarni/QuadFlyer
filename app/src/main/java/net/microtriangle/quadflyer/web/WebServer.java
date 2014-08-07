package net.microtriangle.quadflyer.web;

import android.content.Context;

import java.io.IOException;

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

}
