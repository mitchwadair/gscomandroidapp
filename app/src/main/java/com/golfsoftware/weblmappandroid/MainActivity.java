package com.golfsoftware.weblmappandroid;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    private WebView myWebView;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("leagueManager", MODE_PRIVATE);

        setContentView(R.layout.activity_main);
        myWebView = (WebView)findViewById(R.id.webView);
        WebSettings settings = myWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        checkFirstRun();

        String leagueURL = prefs.getString("url", null);

        //redundant check on stored URL, as it should be set in checkFirstRun()
        if (leagueURL != null) {
            if (leagueURL.contains("admin/app/settings")) {
                MainActivity.this.setTitle("Settings");
                myWebView.loadUrl(leagueURL);
            } else {
                MainActivity.this.setTitle(leagueURL.replace("http://", ""));
                myWebView.loadUrl(leagueURL);
            }
        } else {
            MainActivity.this.setTitle("Settings");
            myWebView.loadUrl("https://golfleague.net/admin/app/settings.html");
        }
        myWebView.setWebViewClient(new WebViewClient() {
            //Each time a new webpage is loaded, check to see if it is intended to
            //update the device's stored URLs.  If so, update and remind user of
            //how to access org websites using the options menu.
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.contains("app/settings.html?data")) {
                    String jsonString = null;
                    try {
                        jsonString = java.net.URLDecoder.decode(url, "UTF-8").substring(url.indexOf("["));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    prefs.edit().putString("urls", jsonString).apply();
                    JSONArray urlJSONArray;
                    try {
                        urlJSONArray = new JSONArray(jsonString);
                        JSONObject obj = urlJSONArray.getJSONObject(0);
                        String toLoad = "http://" + obj.getString("website");
                        prefs.edit().putString("url", toLoad).apply();
                        MainActivity.this.setTitle(obj.getString("website"));
                        myWebView.loadUrl(toLoad);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                super.onPageFinished(view, url);
            }

            /**
             * Pre-nougat method to handle mailto and tel requests to device
             * @param view the webview
             * @param url the url
             * @return true if should override, false if not
             */
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("tel:")) {
                    Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.startsWith("mailto:")) {
                    Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.endsWith(".pdf")) {
                    //myWebView.loadUrl("http://docs.google.com/viewer?url=" + url); //This is an option, but want to stick with native functionality
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                }
                return false;
            }

            /**
             * Nougat+ method to handle mailto and tel requests to device
             * @param view the webview
             * @param request the web request containing url
             * @return true if should override, false if not
             */
            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("tel:")) {
                    Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.startsWith("mailto:")) {
                    Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.endsWith(".pdf")) {
                    //myWebView.loadUrl("http://docs.google.com/viewer?url=" + url); //This is an option, but want to stick with native functionality
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
          super.onBackPressed();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*
        Called each time the options menu is displayed
        Gets Arrays from json string stored in device memory,
        makes entries in the options menu based on website and
        description.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        //Check for stored URLs.  If there, create options in menu based on them.
        String urls = prefs.getString("urls", null);
        if (urls != null) {
            JSONArray urlJSONArray = null;
            try {
                urlJSONArray = new JSONArray(urls);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < urlJSONArray.length(); i++) {
                try {
                    final JSONObject obj = urlJSONArray.getJSONObject(i);
                    MenuItem item = menu.add(R.id.group0, Menu.NONE, Menu.NONE, obj.getString("description"));
                    item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            try {
                                //use http as sites using https will redirect from http regardless
                                String toLoad = "http://" + obj.getString("website");
                                prefs.edit().putString("url", toLoad).apply();
                                MainActivity.this.setTitle(obj.getString("website"));
                                myWebView.loadUrl(toLoad);
                                return true;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            return false;
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        MenuItem mainsite = menu.add(R.id.group0, Menu.NONE, Menu.NONE, "GolfSoftware.com");
        mainsite.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                MainActivity.this.setTitle("GolfSoftware.com");
                myWebView.loadUrl("http://golfsoftware.com");
                return true;
            }
        });

        //Hacky divider to avoid a complete rewrite of a custom menu
        MenuItem divider = menu.add("____________________");
        divider.setEnabled(false);

        //Create Update URLs button every instance of the menu.
        //Generates url with desired param string containing stored URLs
        MenuItem settings = menu.add(R.id.group1, Menu.NONE, Menu.NONE, "Settings");
        settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String url = "https://golfleague.net/admin/app/settings.html";
                String urls = prefs.getString("urls", null);
                if (urls != null) {
                    JSONArray urlJSONArray = null;
                    url = url + "?websites=[\"";
                    try {
                        urlJSONArray = new JSONArray(urls);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < urlJSONArray.length() - 1; i++) {
                        try {
                            url = url + urlJSONArray.getJSONObject(i).getString("website") + "\",\"";
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        url = url + urlJSONArray.getJSONObject(urlJSONArray.length()-1).getString("website") + "\"]";
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                prefs.edit().putString("url", url).apply();
                MainActivity.this.setTitle("Settings");
                myWebView.loadUrl(url);
                return true;
            }
        });
        MenuItem about = menu.add(R.id.group1, Menu.NONE, Menu.NONE, "About");
        about.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                MainActivity.this.setTitle("About");
                myWebView.loadUrl("https://golfleague.net/admin/app/about.html");
                return true;
            }
        });
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Checks if it is the first run of the app on the device.
     * If it is the first run, it will welcome the user and set
     * default url to the update page.
     */
    private void checkFirstRun() {
        boolean isFirstRun = prefs.getBoolean("isFirstRun", true);

        if (isFirstRun) {
            AlertDialog welcome = createDialog("Welcome!",
                    "Welcome to the GolfSoftware.com mobile app! Please enter the " +
                            "address of your organization. To access more, use the menu" +
                            " in the top right corner.");
            welcome.show();
            prefs.edit().putString("url", "https://golfleague.net/admin/app/settings.html").apply();
            prefs.edit().putBoolean("isFirstRun", false).apply();
        }
    }

    /**
     * Creates a generic alert dialog with title and message
     * @param title The title to use in the alert
     * @param message The message content of the dialog
     * @return the alert object to show
     */
    private AlertDialog createDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        return builder.create();
    }
}
