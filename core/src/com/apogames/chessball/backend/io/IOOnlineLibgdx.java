package com.apogames.chessball.backend.io;

import com.apogames.chessball.Constants;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Online IO for ChessBall. All HTTP work is async via libGDX {@code Gdx.net}; results
 * are delivered via callback. JSON shape returned by the chessball PHP endpoints:
 * {@code {success: bool, data: {...}, error: string?}}.
 */
public class IOOnlineLibgdx {

    public interface DemoCallback {
        /** Called on the libGDX network thread. Marshal to render thread if needed. */
        void onDemo(int id, String solution);
        void onError(String message);
    }

    public interface DemosCallback {
        /** Called on the libGDX network thread with all demos returned by the
         *  server (id > since_id, capped at 1500). May be empty when nothing new
         *  exists. {@code maxId} mirrors the largest id in {@code demos}, or
         *  {@code since_id} when the list is empty. */
        void onDemos(List<DemoCache.Entry> demos, int maxId);
        void onError(String message);
    }

    public IOOnlineLibgdx() {
    }

    public String save(final String email, final String solution) {
        return "";
    }

    /**
     * Upload a complete-match demo string to {@link Constants#DEMO_SAVEPHP}. Fire-and-forget
     * — failures are logged but never block the game (called at game-over).
     */
    public void saveDemo(final String solution) {
        if (solution == null || solution.isEmpty()) return;
        try {
            HttpRequest req = new HttpRequestBuilder().newRequest()
                    .method(HttpMethods.POST)
                    .url(Constants.DEMO_SAVEPHP)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .content("solution=" + java.net.URLEncoder.encode(solution, "UTF-8"))
                    .build();

            Gdx.net.sendHttpRequest(req, new HttpResponseListener() {
                @Override public void handleHttpResponse(HttpResponse r) {
                    Gdx.app.log("DemoSave", "OK: " + r.getResultAsString());
                }
                @Override public void failed(Throwable t) {
                    Gdx.app.log("DemoSave", "fail: " + t.getMessage());
                }
                @Override public void cancelled() {
                    Gdx.app.log("DemoSave", "cancelled");
                }
            });
        } catch (Exception ex) {
            Gdx.app.log("DemoSave", "error: " + ex.getMessage());
        }
    }

    public boolean load() {
        try {
            HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
            HttpRequest httpRequest = requestBuilder.newRequest().method(HttpMethods.GET).url(Constants.USERLEVELS_GETPHP).build();

            Gdx.net.sendHttpRequest(httpRequest, new HttpResponseListener() {
                @Override
                public void handleHttpResponse(HttpResponse httpResponse) {
                    httpResponse.getResultAsString();
                }

                @Override
                public void failed(Throwable t) {
                    Gdx.app.log("Failed ", t.getMessage());
                }

                @Override
                public void cancelled() {
                    Gdx.app.log("Cancelled", "Load cancelled");
                }
            });

            return true;
        } catch (Exception me) {
            System.err.println("Exception: " + me);
        }
        return false;
    }

    /**
     * Fetch all demos with id > {@code sinceId} (capped at 1500 by the server).
     * On the very first launch {@code sinceId} is 0 and the response carries
     * the 1500 most recent demos; on subsequent launches only the new delta.
     * Result is delivered async on the libGDX network thread.
     */
    public void loadDemosSince(int sinceId, final DemosCallback callback) {
        try {
            HttpRequest req = new HttpRequestBuilder().newRequest()
                    .method(HttpMethods.GET)
                    .url(Constants.DEMOS_SINCE_GETPHP + "?since_id=" + sinceId)
                    .build();

            Gdx.net.sendHttpRequest(req, new HttpResponseListener() {
                @Override
                public void handleHttpResponse(HttpResponse httpResponse) {
                    String body = httpResponse.getResultAsString();
                    try {
                        JsonValue root = new JsonReader().parse(body);
                        if (root == null || !root.getBoolean("success", false)) {
                            String err = root != null ? root.getString("error", "unknown") : "empty response";
                            callback.onError(err);
                            return;
                        }
                        JsonValue data = root.get("data");
                        if (data == null) {
                            callback.onError("no data");
                            return;
                        }
                        int maxId = data.getInt("maxId", 0);
                        List<DemoCache.Entry> demos = new ArrayList<DemoCache.Entry>();
                        JsonValue demosJson = data.get("demos");
                        if (demosJson != null) {
                            for (JsonValue d = demosJson.child; d != null; d = d.next) {
                                int id = d.getInt("id", 0);
                                String solution = d.getString("solution", "");
                                if (id > 0 && !solution.isEmpty()) {
                                    demos.add(new DemoCache.Entry(id, solution));
                                }
                            }
                        }
                        callback.onDemos(demos, maxId);
                    } catch (Exception ex) {
                        callback.onError("parse error: " + ex.getMessage());
                    }
                }

                @Override
                public void failed(Throwable t) {
                    callback.onError("network: " + t.getMessage());
                }

                @Override
                public void cancelled() {
                    callback.onError("cancelled");
                }
            });
        } catch (Exception ex) {
            callback.onError("request error: " + ex.getMessage());
        }
    }

    /**
     * Fetch one random demo from the server. The callback is invoked once with either
     * the parsed solution string or an error message.
     */
    public void loadRandomDemo(final DemoCallback callback) {
        try {
            HttpRequest httpRequest = new HttpRequestBuilder().newRequest()
                    .method(HttpMethods.GET)
                    .url(Constants.DEMO_GETPHP)
                    .build();

            Gdx.net.sendHttpRequest(httpRequest, new HttpResponseListener() {
                @Override
                public void handleHttpResponse(HttpResponse httpResponse) {
                    String body = httpResponse.getResultAsString();
                    try {
                        JsonValue root = new JsonReader().parse(body);
                        if (root == null || !root.getBoolean("success", false)) {
                            String err = root != null ? root.getString("error", "unknown") : "empty response";
                            callback.onError(err);
                            return;
                        }
                        JsonValue data = root.get("data");
                        if (data == null) {
                            callback.onError("no data");
                            return;
                        }
                        int id = data.getInt("id", 0);
                        String solution = data.getString("solution", "");
                        if (solution.isEmpty()) {
                            callback.onError("empty solution");
                            return;
                        }
                        callback.onDemo(id, solution);
                    } catch (Exception ex) {
                        callback.onError("parse error: " + ex.getMessage());
                    }
                }

                @Override
                public void failed(Throwable t) {
                    callback.onError("network: " + t.getMessage());
                }

                @Override
                public void cancelled() {
                    callback.onError("cancelled");
                }
            });
        } catch (Exception ex) {
            callback.onError("request error: " + ex.getMessage());
        }
    }
}
