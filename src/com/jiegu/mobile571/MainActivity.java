package com.jiegu.mobile571;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.DialogFragment;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.TextView;
import org.json.*;
import com.facebook.*;

import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;

public class MainActivity extends Activity {

	private TextView city;
	private TextView state;
	private TextView weather;
	private TextView temperature;
	private ImageView image;
	private String FBWF = "";
	private TextView FB1;
	private TextView FB2;
	private String FBname, FBcity, FBcaption, FBdes, FBCW, FBlink, FBimg,
			FBcond;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		city = (TextView) findViewById(R.id.city);
		state = (TextView) findViewById(R.id.state);
		weather = (TextView) findViewById(R.id.weather);
		temperature = (TextView) findViewById(R.id.temperature);
		FB1 = (TextView) findViewById(R.id.post1);
		FB2 = (TextView) findViewById(R.id.post2);
		image = (ImageView) findViewById(R.id.imageView1);

	}

	public boolean verify(String location) {
		String ERROR;
		if (location.matches("^[0-9]+$")) {
			if (!location.matches("^\\d{5}$")) {
				ERROR = "Invalid zip code: must be five digits\nExample: 90089";
				myalert(ERROR);
				return false;
			}
		} else {// not zip code
			// ^.+,\s*(\w{2})
			// "^[\\w\\s]+,\\s\\w{2}$"
			if (!location.matches("^.+,\\s*(\\w{2})(\\D+)?$")) {
				ERROR = "Invalid location: must include state or country separated by comma\nExample: Los Angeles, CA";
				myalert(ERROR);
				return false;
			}
		}

		return true;
	}

	public void postdialog(final int mode) {
		String msg = "";
		if (mode == 1) {
			msg = "Post Current Weather";
		}
		if (mode == 2) {
			msg = "Post Weather Forecast";
		}
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		// set title
		alertDialogBuilder.setTitle("POST TO FACEBOOK");
		// set dialog message
		alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton(msg, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						onClickLogin(mode);
					}
				})
				.setNegativeButton("CANCEL",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
		// show it
		alertDialog.show();

	}

	public void myalert(String ERROR) {

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		// set title
		alertDialogBuilder.setTitle("Input error");
		// set dialog message
		alertDialogBuilder.setMessage(ERROR).setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
		// show it
		alertDialog.show();
	}

	/** Called when the user clicks the Send button */
	public void sendMessage(View view) {

		EditText editText = (EditText) findViewById(R.id.edit_message);
		RadioGroup radioButtonGroup = (RadioGroup) findViewById(R.id.radioGroup1);
		int radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
		View radioButton = radioButtonGroup.findViewById(radioButtonID);
		int idx = radioButtonGroup.indexOfChild(radioButton);
		String location = editText.getText().toString();
		String type = "";
		String unit = "";

		if (verify(location)) {

			if (location.matches("^\\d{5}$")) {
				type = "zip";
			} else {
				type = "city";
				location = location.replace(',', ' ');
			}
			if (idx == 0) {
				unit = "F";
			}

			if (idx == 1) {
				unit = "C";
			}
			location = URLEncoder.encode(location);
			String urlString = "http://cs-server.usc.edu:25051/examples/servlet/MyServlet?";
			urlString = urlString + "type=" + type + "&tempUnit=" + unit
					+ "&location=" + location;
			new DownloadWebpageTask().execute(urlString);
		}
	}

	private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... urls) {

			// params comes from the execute() call: params[0] is the url.
			try {
				return downloadUrl(urls[0]);
			} catch (IOException e) {
				return "Unable to retrieve web page. URL may be invalid.";
			}
		}

		// onPostExecute displays the results of the AsyncTask.
		@Override
		protected void onPostExecute(String result) {
			String img = "";
			try {
				JSONObject myjson = new JSONObject(result);

				if (myjson.has("failed")) {
					noshow();
				} else {

					JSONObject temp = myjson.getJSONObject("units");
					String tempU = temp.getString("temperature");

					JSONObject loc = myjson.getJSONObject("location");
					String ct = loc.getString("city");
					city.setText(ct);

					// JSONObject loc = myjson.getJSONObject("location");
					String region = loc.getString("region") + ", "
							+ loc.getString("country");
					state.setText(region);
					FBcity = ct;
					FBname = ct +", " +region;

					FBlink = myjson.getString("link");

					JSONObject con = myjson.getJSONObject("condition");
					String wea = con.getString("text");
					FBcond = wea;
					weather.setText(wea);

					String tep = con.getString("temp") + "\u00B0" + tempU;
					;
					temperature.setText(tep);

					FBCW = "Temperature is " + tep + ".";

					img = myjson.getString("img");
					FBimg = img;
					new DownloadImage().execute(img);
					showtable(myjson);
					FBpost();
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	public void noshow(){
		//setContentView(R.layout.activity_main);
		city.setText("");
		weather.setText("");
		temperature.setText("");
		image.setImageBitmap(null);
		FB1.setText("");
		FB2.setText("");
		TableLayout table = (TableLayout) findViewById(R.id.table);
		table.removeAllViews();
		state.setText("Weather information can not be found!");
	}

	public void FBpost() {
		// Session session = Session.getActiveSession();

		FB1.setText("Share Current Weather");
		FB2.setText("Share Weather Forecast");

		FB1.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				postdialog(1);
			}
		});

		FB2.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				postdialog(2);
			}
		});

	}

	private void onClickLogin(final int mode) {
		Session.openActiveSession(this, true, new Session.StatusCallback() {

			// callback when session changes state
			@SuppressWarnings("deprecation")
			@Override
			public void call(Session session, SessionState state,
					Exception exception) {
				if (session.isOpened()) {

					// make request to the /me API
					Request.executeMeRequestAsync(session,
							new Request.GraphUserCallback() {

								// callback after Graph API response with user
								// object
								@Override
								public void onCompleted(GraphUser user,
										Response response) {

									try {
										publishFeedDialog(mode);
									} catch (JSONException e) {
										e.printStackTrace();
									}
								}
							});
				}
			}
		});

	}

	private void publishFeedDialog(int mode) throws JSONException {
		Bundle params = new Bundle();

		if (mode == 1) {
			FBcaption = "The Current Condition for " + FBcity + " is " + FBcond
					+ ".";
			FBdes = FBCW;
		}
		if (mode == 2) {
			FBcaption = "Weather Forecast for " + FBcity + ".";
			FBdes = FBWF;
			FBimg = "http://www-scf.usc.edu/~jiegu/images/weather.jpg";
		}

		JSONObject property = new JSONObject();
		property.put("text", "here\n");
		property.put("href", FBlink);
		JSONObject properties = new JSONObject();
		properties.put("Look at details", property);

		params.putString("name", FBname);
		params.putString("caption", FBcaption);
		params.putString("description", FBdes);
		params.putString("link", FBlink);
		params.putString("picture", FBimg);
		params.putString("properties", properties.toString());

		WebDialog feedDialog = (new WebDialog.FeedDialogBuilder(
				MainActivity.this, Session.getActiveSession(), params))
				.setOnCompleteListener(new OnCompleteListener() {

					@Override
					public void onComplete(Bundle values,
							FacebookException error) {
						if (error == null) {
							// When the story is posted, echo the success
							// and the post Id.
							final String postId = values.getString("post_id");
							if (postId != null) {
								Toast.makeText(MainActivity.this,
										"Post Success", Toast.LENGTH_SHORT)
										.show();
							} else {
								// User clicked the Cancel button
								Toast.makeText(
										MainActivity.this
												.getApplicationContext(),
										"Publish cancelled", Toast.LENGTH_SHORT)
										.show();
							}
						} else if (error instanceof FacebookOperationCanceledException) {
							// User clicked the "x" button
							Toast.makeText(
									MainActivity.this.getApplicationContext(),
									"Publish cancelled", Toast.LENGTH_SHORT)
									.show();
						} else {
							// Generic, ex: network error
							Toast.makeText(
									MainActivity.this.getApplicationContext(),
									"Error posting story", Toast.LENGTH_SHORT)
									.show();
						}
					}

				}).build();
		feedDialog.show();
	}

	public void showtable(JSONObject myjson) throws JSONException {
		JSONObject temp = myjson.getJSONObject("units");
		String tempU = temp.getString("temperature");
		JSONArray jArray = myjson.getJSONArray("forecast");
		TableLayout table = (TableLayout) findViewById(R.id.table);
		table.removeAllViews();
		TableRow rowhead = new TableRow(this);
		TextView h1 = new TextView(this);
		TextView h2 = new TextView(this);
		TextView h3 = new TextView(this);
		TextView h4 = new TextView(this);

		h1.setText("Day");
		h1.setMinWidth(150);
		h2.setText("Weather");
		h2.setMinWidth(300);
		h3.setText("High");
		h3.setMinWidth(200);
		h4.setText("Low");
		rowhead.addView(h1);
		rowhead.addView(h2);
		rowhead.addView(h3);
		rowhead.addView(h4);
		rowhead.setBackgroundColor(Color.LTGRAY);

		table.addView(rowhead, new TableLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		FBWF="";

		for (int i = 0; i < jArray.length(); i++) {
			try {
				JSONObject oneObject = jArray.getJSONObject(i);

				String text = oneObject.getString("text");
				String day = oneObject.getString("day");
				
				FBWF = FBWF + day + ": " + text + ",";

				String high = oneObject.getString("high");
				FBWF = FBWF + high + "/";
				high = high + "\u00B0" + tempU;
				String low = oneObject.getString("low");
				FBWF = FBWF + low + "\u00B0" + tempU + "; ";
				low = low + "\u00B0" + tempU;

				table = (TableLayout) findViewById(R.id.table);
				TableRow row = new TableRow(this);

				// create a new TextView
				TextView t1 = new TextView(this);
				TextView t2 = new TextView(this);
				TextView t3 = new TextView(this);
				TextView t4 = new TextView(this);

				t1.setText(day);
				t2.setText(text);
				t3.setText(high);
				t3.setTextColor(Color.RED);
				t4.setText(low);
				t4.setTextColor(Color.BLUE);

				// add the TextView and the CheckBox to the new TableRow
				row.addView(t1);
				row.addView(t2);
				row.addView(t3);
				row.addView(t4);

				if (i % 2 == 0) {
					row.setBackgroundColor(Color.WHITE);
				}
				// add the TableRow to the TableLayout
				table.addView(row, new TableLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

			} catch (JSONException e) {
				// Oops
			}
		}
	}

	private String downloadUrl(String myurl) throws IOException {
		InputStream is = null;
		int len = 2048;

		try {
			URL url = new URL(myurl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /* milliseconds */);
			conn.setConnectTimeout(15000 /* milliseconds */);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			// Starts the query
			conn.connect();
			is = conn.getInputStream();

			// Convert the InputStream into a string
			String contentAsString = readIt(is, len);
			String processed = contentAsString.replaceAll("@", "");
			return processed;

			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	// Reads an InputStream and converts it to a String.
	public String readIt(InputStream stream, int len) throws IOException,
			UnsupportedEncodingException {
		Reader reader = null;
		reader = new InputStreamReader(stream, "UTF-8");
		char[] buffer = new char[len];
		reader.read(buffer);
		return new String(buffer);
	}

	public class DownloadImage extends AsyncTask<String, Void, Bitmap> {

		@Override
		protected Bitmap doInBackground(String... urls) {
			Bitmap map = null;
			map = downloadImage(urls[0]);
			return map;
		}

		protected void onPostExecute(Bitmap result) {
			image.setImageBitmap(result);

		}

		private Bitmap downloadImage(String url) {
			Bitmap bitmap = null;
			InputStream stream = null;
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inSampleSize = 1;

			try {
				stream = getHttpConnection(url);
				bitmap = BitmapFactory.decodeStream(stream, null, bmOptions);
				stream.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return bitmap;
		}

		// Makes HttpURLConnection and returns InputStream
		private InputStream getHttpConnection(String urlString)
				throws IOException {
			InputStream stream = null;
			URL url = new URL(urlString);
			URLConnection connection = url.openConnection();

			try {
				HttpURLConnection httpConnection = (HttpURLConnection) connection;
				httpConnection.setRequestMethod("GET");
				httpConnection.connect();

				if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					stream = httpConnection.getInputStream();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return stream;
		}

	}

	@SuppressLint("ValidFragment")
	public class PostForecast extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the Builder class for convenient dialog construction
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage("Post to Facebook")
					.setPositiveButton("Post Current Weather",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// FIRE ZE MISSILES!
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// User cancelled the dialog
								}
							});
			// Create the AlertDialog object and return it
			return builder.create();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode,
				resultCode, data);
	}

}
