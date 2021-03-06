package com.example.mikehhsu.personalnewsfeed.network;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.mikehhsu.personalnewsfeed.MyApplication;
import com.example.mikehhsu.personalnewsfeed.db.NewsFeedDBHelper;
import com.example.mikehhsu.personalnewsfeed.db.SQLiteDBObject;
import com.example.mikehhsu.personalnewsfeed.loeaders.ArticlesLoader;
import com.example.mikehhsu.personalnewsfeed.parser.NYTNewsListParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by mikehhsu on 10/10/16.
 */
// TODO: 10/10/16 The Result Generic Type should be modified based on the correct behavior
public class ArticlesFetchCommand extends HttpGetCommand {
    ArrayList<SQLiteDBObject> articles = null;

    @Override
    protected Void doInBackground(String... urls) {
        try {
            this.url = urls[0];
            URL url = new URL(this.url);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setReadTimeout(READ_TIMEOUT);
            httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT);//might throw SocketTimeoutException
            httpURLConnection.setRequestMethod(REQUEST_METHOD);//optional - GET is the default action
            httpURLConnection.setDoInput(true);
            httpURLConnection.connect();
            Log.d(ArticlesFetchCommand.class.toString(), "Response Code: " + httpURLConnection.getResponseCode() + httpURLConnection.getResponseMessage() + httpURLConnection.getHeaderField("Location"));
            //for the 303 See Other response
            if(httpURLConnection.getResponseCode() == 303 && httpURLConnection.getHeaderField("Location") != null){
                url = new URL(httpURLConnection.getHeaderField("Location"));
                httpURLConnection.disconnect();
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();
            }
            inputStream = httpURLConnection.getInputStream();
            if(inputStream != null){
                articles = new NYTNewsListParser().parse(inputStream);
                NewsFeedDBHelper.getInstance(MyApplication.getInstance().getApplicationContext())
                .insertOrUpdateAll(articles);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            if (inputStream != null){
                inputStream = null;
            }
            NewsFeedDBHelper.getInstance(MyApplication.getInstance().getApplicationContext()).close();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        //UI thread

        if(articles != null && articles.size() > 0) {
            LocalBroadcastManager.getInstance(MyApplication.getInstance().getApplicationContext()).sendBroadcast(new Intent(ArticlesLoader.getBroadcastString()));
        }
        super.onPostExecute(aVoid);
    }
}
