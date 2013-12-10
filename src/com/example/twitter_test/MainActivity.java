package com.example.twitter_test;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView.Tokenizer;

public class MainActivity extends Activity {
	private Button updateButton;
	private Button reloadButton;
	private EditText updateEdit;
	private WebView webView;

	private DefaultHttpClient httpClient;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		updateButton = (Button) findViewById(R.id.UpdateButton);

		// ボタン押下時の処理
		updateButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// TODO 自動生成されたメソッド・スタブ

				// 通信を伴うので別スレッドで
				new Thread(){
					@Override
					public void run() {
						// TODO 自動生成されたメソッド・スタブ
						updateButton.setClickable(false);
						reloadButton.setClickable(false);
						String str = updateEdit.getEditableText().toString();
						UpdateTwitter(str);
						reloadButton.setClickable(true);
						updateButton.setClickable(true);
					}
				}.start();
			}
		});

		reloadButton = (Button) findViewById(R.id.ReloadButton);

		reloadButton.setOnClickListener(new OnClickListener() {

			public void onClick(View view) {
				// TODO 自動生成されたメソッド・スタブ

				// 通信を伴うので別スレッドで
				new Thread(){
					@Override
					public void run() {
						updateButton.setClickable(false);
						reloadButton.setClickable(false);
						GetAndWriteHomeTimeLine();
						reloadButton.setClickable(true);
						updateButton.setClickable(true);
					}
				}.start();
			}
		});

		updateEdit = (EditText) findViewById(R.id.UpdateEdit);
		webView = (WebView) findViewById(R.id.twitter_web);

		httpClient = new DefaultHttpClient();
		Credentials cred = new UsernamePasswordCredentials( "Twitter User ID", "Password");//userとpassを
		httpClient.getCredentialsProvider().setCredentials( new AuthScope("twitter.com", 80), cred);

		GetAndWriteHomeTimeLine();
    }

	/**
	 * Twitterへつぶやく
	 * @param str
	 * 投稿内容
	 */
	private synchronized void UpdateTwitter( String str){
		try {
			// パラメータstatus=hoge でhogeを発言
			// status は必須パラメータ
			HttpPost post = new HttpPost( "http://twitter.com/statuses/update.json?status=" + str);
			// 一応これで発言するはず
			HttpResponse response = httpClient.execute(post);

			// 失敗時の処理
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
				return;
			}

			// ここからの描画はなぜか利かない・・・
			GetAndWriteHomeTimeLine();

		} catch (ClientProtocolException e) {
			// TODO 自動生成された catch ブロック
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
		}

	}

	/**
	 * friend_timelineは将来廃止予定らしいので home_timeline を取得し表示。<br />
	 * http://d.hatena.ne.jp/nowokay/20091030 を参考にしました。
	 */
	private synchronized void GetAndWriteHomeTimeLine(){
		try {
//			webView.clearCache(false);

			HttpGet get = new HttpGet( "http://twitter.com/statuses/home_timeline.json");
			// これで取得するはず
			HttpResponse response = httpClient.execute(get);

			// 失敗時の処理
			if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
				return;
			}

			//解析と出力
    		//サーバーからのデータを取得
			InputStream is = response.getEntity().getContent();
			InputStreamReader isr = new InputStreamReader(is);
			StringWriter strin = new StringWriter();
			BufferedReader buf = new BufferedReader(isr);
			for(String line; (line = buf.readLine()) != null;){
				strin.write(line);
			}
			buf.close();
			isr.close();
			is.close();


            //出力準備
			StringWriter strout = new StringWriter();
			PrintWriter out = new PrintWriter(strout);
			out.println("<html><head><title>Twitter testapites</title></head>");
			out.println("<body>");
			//JSONデータからタイムラインを取得してHTMLを生成
			JSONTokener token = new JSONTokener(strin.toString());
			JSONArray arr = new JSONArray(token);
			for(int i = 0; i < arr.length(); i++){
				JSONObject obj = arr.getJSONObject(i);
				JSONObject user = obj.getJSONObject("user");
				out.println("<div style='border-bottom: 1px solid #888888'>");
				out.println("<img style='width:48px; height:48px;' src='" + user.get("profile_image_url") + "' align='left'>");
				out.println("<bold><font color='#6666ff'>" + user.getString("screen_name") + "</font></bold><br />" );
				out.println(obj.get("text") + "<br clear='all' />");
				// ここで出力を確認しているが、発言内容が含まれているにもかかわらず、
				// loadData一回ではなぜか更新されない。
				Log.i("t_test", "" + obj.get("text"));
				out.println("</div>");
			}
			out.println("</body></html>");

			strin.close();
			out.close();

			// 二回呼び出すと反映される・・・
			// invalidate() の意味は？？
			webView.loadData(strout.toString(), "text/html", "utf-8");
			webView.loadData(strout.toString(), "text/html", "utf-8");


			strout.close();

			// 描画更新
			// 本体スレッドからの呼び出しではない場合に備えpostInvalidateを使用。
//			webView.postInvalidate();
//			Log.i("t_test", "Invalidate");


		} catch (ClientProtocolException e) {
			// TODO 自動生成された catch ブロック
		} catch (JSONException e) {
			// TODO 自動生成された catch ブロック
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
		}

	}
}