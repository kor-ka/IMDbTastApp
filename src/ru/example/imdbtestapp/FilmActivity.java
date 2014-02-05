package ru.example.imdbtestapp;

import it.gmariotti.cardslib.library.internal.Card;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class FilmActivity extends Activity implements OnClickListener {

	TextView title;
	TextView plot;
	TextView stuff;
	TextView stuff2;
	ImageView poster;
	ImageView bigPoster;
	ImageButton starr;
	ShareActionProvider mShareActionProvider;
	Intent sendIntent;
	boolean istarred;
	SharedPreferences starred;
	String imdbid;
	Intent result;
	Context ctx;
	Intent i;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_film);
		// Show the Up button in the action bar.
		setupActionBar();
		ctx = this;
		starred = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		stuff = (TextView) findViewById(R.id.afStuff);
		stuff2 = (TextView) findViewById(R.id.afStuff2);
		title = (TextView) findViewById(R.id.afTitele);
		plot = (TextView) findViewById(R.id.afPlot);
		poster = (ImageView) findViewById(R.id.imageViewposter);
		bigPoster = (ImageView) findViewById(R.id.bigPoster);
		starr = (ImageButton) findViewById(R.id.afstarr);
		starr.setOnClickListener(this);
		i = getIntent();
		result = new Intent();
		imdbid = i.getStringExtra("imdbid");
		title.setText(i.getStringExtra("Title"));
		getActionBar().setTitle(i.getStringExtra("Title"));
		istarred = starred.getString(imdbid, null)!=null;
		setResult(RESULT_CANCELED, result);
		
		if(istarred){
			try {
				if(starred.getString(imdbid, null)!=null){
					
				
				JSONObject jObject2 = new JSONObject(starred.getString(imdbid, null));
				stuff.setText(jObject2.getString("Country")+" | "+jObject2.getString("Released")+									
						"\n"+jObject2.getString("Genre").replace(",", " |")+
						"\n"+jObject2.getString("Runtime"));
				stuff2.setText("Rating: "+jObject2.getString("imdbRating")+"/10"+"\n"+
								"\n"+"Director: "+jObject2.getString("Director")+
								"\n"+"Writers: "+jObject2.getString("Writer")+
								"\n"+"Type: "+jObject2.getString("Type"));
				plot.setText(jObject2.getString("Plot"));
				
			
			
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inPreferredConfig = Bitmap.Config.ARGB_8888;
					Bitmap bitmap = BitmapFactory.decodeFile(
							Environment.getExternalStorageDirectory()
									+ "/IMDbTestApp/" + imdbid + ".jpg",
							options);
				if (bitmap!=null) {
					poster.setImageBitmap(bitmap);
					DisplayMetrics displaymetrics = new DisplayMetrics();
					getWindowManager().getDefaultDisplay().getMetrics(
							displaymetrics);
					int height = displaymetrics.heightPixels;
					int width = displaymetrics.widthPixels;
					Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, width,
							width * bitmap.getHeight() / bitmap.getWidth(),
							false);
					bigPoster.setImageBitmap(newBitmap);
				
				}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			if(isOnline()){
				setFullFilmInfo(i.getStringExtra("imdbid"));
			}else{
				
				String jsonString = i.getStringExtra("JSONString");
				if(jsonString!=null){
					 try {
							JSONObject jObject2 = new JSONObject(jsonString);
							stuff.setText(jObject2.getString("Country")+" | "+jObject2.getString("Released")+									
									"\n"+jObject2.getString("Genre").replace(",", " |")+
									"\n"+jObject2.getString("Runtime"));
							stuff2.setText("Rating: "+jObject2.getString("imdbRating")+"/10"+
											"\n"+"Director: "+jObject2.getString("Director")+
											"\n"+"Writers: "+jObject2.getString("Writer")+
											"\n"+"Type: "+jObject2.getString("Type"));
							plot.setText(jObject2.getString("Plot"));
							
							
							
						
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					 BitmapFactory.Options options = new BitmapFactory.Options();
						options.inPreferredConfig = Bitmap.Config.ARGB_8888;
						Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+ "/IMDbTestApp/old/"+imdbid+".jpg", options);
						if(bitmap!=null){
							poster.setImageBitmap(bitmap);
							
							DisplayMetrics displaymetrics = new DisplayMetrics();
							getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
							int height = displaymetrics.heightPixels;
							int width = displaymetrics.widthPixels;
							
					        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, width, width*bitmap.getHeight()/bitmap.getWidth(), false);
					        
							
					      bigPoster.setImageBitmap(newBitmap);
						}
					 
				}else{
					Toast.makeText(ctx, "Cant't load data. Offline.", Toast.LENGTH_SHORT).show();
					stuff.setVisibility(View.INVISIBLE);
					stuff2.setVisibility(View.INVISIBLE);
					plot.setVisibility(View.INVISIBLE);
					starr.setVisibility(View.INVISIBLE);
				}
			}
			
			
		}
		
		
		
		if(istarred){
			starr.setBackgroundResource(R.drawable.star_on_blue);
			
		}
		
		sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, "http://www.imdb.com/title/"+i.getStringExtra("imdbid")+"/");
		sendIntent.setType("text/plain");
		
		
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.film, menu);
		
		// Locate MenuItem with ShareActionProvider
	    MenuItem item = menu.findItem(R.id.menu_item_share);

	    // Fetch and store ShareActionProvider
	    mShareActionProvider = (ShareActionProvider) item.getActionProvider();
	    setShareIntent(sendIntent);
		return true;
	}
	
	private void setShareIntent(Intent shareIntent) {
	    if (mShareActionProvider != null) {
	        mShareActionProvider.setShareIntent(shareIntent);
	    }
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.bookmarks:
			Intent i = new Intent(getBaseContext(), BookmarkActivity.class);
			startActivity(i);
			break;
			
			
			
		case R.id.openibBr:
			
			String url = "http://www.imdb.com/title/"+imdbid+"/";
			Intent sendIntent = new Intent(Intent.ACTION_VIEW);
			sendIntent.setData(Uri.parse(url));
			startActivity(sendIntent);
			
			break;
			

		}
		return super.onOptionsItemSelected(item);
	}
	
	void setFullFilmInfo(final String imdbid){
	
		class Act extends AsyncTask<String, Void, String> {
			String result2 = null;
           @Override
           protected String doInBackground(String... params) {

           	HttpClient   httpclient2 = new DefaultHttpClient();
       		HttpPost httppost2 = new HttpPost("http://www.omdbapi.com/?i="+imdbid);
       		
       		InputStream inputStream2 = null;
       		
       		try {
       		    HttpResponse response2 = httpclient2.execute(httppost2);           
       		    HttpEntity entity2 = response2.getEntity();

       		    inputStream2 = entity2.getContent();
       		  
       		    BufferedReader reader2 = new BufferedReader(new InputStreamReader(inputStream2, "UTF-8"), 8);
       		    StringBuilder sb2 = new StringBuilder();

       		    String line2 = null;
       		    while ((line2 = reader2.readLine()) != null)
       		    {
       		        sb2.append(line2 + "\n");
       		    }
       		    result2 = sb2.toString();
       		    
       		} catch (Exception e) { 
       		    // Oops
       			Toast.makeText(getBaseContext(), "Ooops"+e.getMessage(), Toast.LENGTH_LONG).show();
       		}
       		finally {
       		    try{if(inputStream2 != null)inputStream2.close();}catch(Exception squish){}
       		}


               return null;
           }

           
           protected void onPostExecute(String result) {
           
                      
                       
                       try {
							JSONObject jObject2 = new JSONObject(result2);
							stuff.setText(jObject2.getString("Country")+" | "+jObject2.getString("Released")+									
									"\n"+jObject2.getString("Genre").replace(",", " |")+
									"\n"+jObject2.getString("Runtime"));
							stuff2.setText("Rating: "+jObject2.getString("imdbRating")+"/10"+
											"\n"+"Director: "+jObject2.getString("Director")+
											"\n"+"Writers: "+jObject2.getString("Writer")+
											"\n"+"Type: "+jObject2.getString("Type"));
							plot.setText(jObject2.getString("Plot"));
							
							new DownloadImageTask(poster).execute(jObject2.getString("Poster"));
							
							
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                       
                
                      };
       };
       
       new Act().execute();
		
		
		
	}
	
	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		  ImageView bmImage;

		  public DownloadImageTask(ImageView bmImage) {
		      this.bmImage = bmImage;
		  }

		  protected Bitmap doInBackground(String... urls) {
		      String urldisplay = urls[0];
		      Bitmap mIcon11 = null;
		      try {
		        InputStream in = new java.net.URL(urldisplay).openStream();
		        mIcon11 = BitmapFactory.decodeStream(in);
		      } catch (Exception e) {
		          Log.e("Error", e.getMessage());
		          e.printStackTrace();
		      }
		      return mIcon11;
		  }

		  protected void onPostExecute(Bitmap result) {
			  if(result!=null){
				  bmImage.setImageBitmap(result);
			      DisplayMetrics displaymetrics = new DisplayMetrics();
					getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
					int height = displaymetrics.heightPixels;
					int width = displaymetrics.widthPixels;
					
			        Bitmap newBitmap = Bitmap.createScaledBitmap(result, width, width*result.getHeight()/result.getWidth(), false);
			        
					
			      bigPoster.setImageBitmap(newBitmap);
			  }
		      
		  }
		}

	@Override
	public void onClick(View v) {
		istarred = starred.getString(imdbid, null)!=null;
		if(istarred){
			Set<String> ss = starred.getStringSet("starred", new HashSet<String>());
			Set<String> newList = new HashSet<String>();
			if (imdbid != ""){ 
			    if (ss != null){
			        for(String each: ss){
			        	
			        		newList.add(each);
			        	
			            
			        }
			    }
			    newList.remove(imdbid);
			    Editor ed = starred.edit();
			    ed.putStringSet("starred", newList);  
			    ed.putString(imdbid, null);
			    ed.apply();      
			}
			starr.setBackgroundResource(R.drawable.star_off);
			istarred = false;
			Editor ed = starred.edit();
			ed.putString(imdbid, null);
			ed.apply();
			result.putExtra("isstarred", false);
			setResult(RESULT_OK, result);
		}else{
			Set<String> ss = starred.getStringSet("starred", null);
			Set<String> newList = new HashSet<String>();
			if (imdbid != ""){ 
			    if (ss != null){
			        for(String each: ss){
			            newList.add(each);
			        }
			    }
			    newList.add(imdbid);
			    Editor ed = starred.edit();
			    ed.putStringSet("starred", newList);     
			    ed.apply();
			    
			}
			
			
			starr.setBackgroundResource(R.drawable.star_on_blue);
			istarred = true;
			if(isOnline()){
				getFullFilmInfoJsonString(imdbid);
			}else{ //Toast.makeText(ctx, "Cant't load data. Offline.", Toast.LENGTH_SHORT).show(); 
			Editor ed = starred.edit();
            ed.putString(imdbid, i.getStringExtra("JSONString"));
            ed.apply();
            JSONObject json;
				try {
					json = new JSONObject(i.getStringExtra("JSONString"));
				

					// Copy poster		
					
					String passto = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/IMDbTestApp";
					String passfrom = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/IMDbTestApp/old";
					File from = new File(passfrom, imdbid + ".jpg");
					File to = new File(passto, imdbid + ".jpg");
											
					try {
						to.createNewFile();
						InputStream in = new FileInputStream(from);
					    OutputStream out = new FileOutputStream(to);

					    // Transfer bytes from in to out
					    byte[] buf = new byte[1024];
					    int len;
					    while ((len = in.read(buf)) > 0) {
					        out.write(buf, 0, len);
					    }
					    in.close();
					    out.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	   
			}
            
			result.putExtra("isstarred", true);
			setResult(RESULT_OK, result);
		}
		
	}
	
	void getFullFilmInfoJsonString( final String imdbid){
		
		class Act extends AsyncTask<String, Void, String> {
			String result2 = null;
           @Override
           protected String doInBackground(String... params) {
       
           	HttpClient   httpclient2 = new DefaultHttpClient();
       		HttpPost httppost2 = new HttpPost("http://www.omdbapi.com/?i="+imdbid);
       	
       		InputStream inputStream2 = null;
       		
       		try {
       		    HttpResponse response2 = httpclient2.execute(httppost2);           
       		    HttpEntity entity2 = response2.getEntity();

       		    inputStream2 = entity2.getContent();
       		    
       		    BufferedReader reader2 = new BufferedReader(new InputStreamReader(inputStream2, "UTF-8"), 8);
       		    StringBuilder sb2 = new StringBuilder();

       		    String line2 = null;
       		    while ((line2 = reader2.readLine()) != null)
       		    {
       		        sb2.append(line2 + "\n");
       		    }
       		    result2 = sb2.toString();
       		    
       		} catch (Exception e) { 
       		    // Oops
       			Toast.makeText(getBaseContext(), "Ooops"+e.getMessage(), Toast.LENGTH_LONG).show();
       		}
       		finally {
       		    try{if(inputStream2 != null)inputStream2.close();}catch(Exception squish){}
       		}


               return null;
           }

           
           protected void onPostExecute(String result) {
           
                      Editor ed = starred.edit();
                      ed.putString(imdbid, result2);
                      ed.apply();
                      JSONObject json;
						try {
							json = new JSONObject(result2);
							String url = json.getString("Poster");
							SaveBmp(url,imdbid);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                  	   
         				
                      };
       };
       
       new Act().execute();
		
		
		
	}
	
	public void SaveBmp(final String url, final String id) {

		class SaveBmp extends AsyncTask<String, Void, String> {
			Bitmap bitmap;

			@Override
			protected String doInBackground(String... params) {
				URL imageurl;
				try {
					imageurl = new URL(url);

					try {
						bitmap = BitmapFactory.decodeStream(imageurl
								.openConnection().getInputStream());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
				

			
				File exportDir = new File(Environment.getExternalStorageDirectory(), "IMDbTestApp");

        		if (!exportDir.exists()) { exportDir.mkdirs(); }
				File f = new File(exportDir,id +".jpg");
				try {
					f.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				FileOutputStream fo;
				try {
					fo = new FileOutputStream(f);
				
				fo.write(bytes.toByteArray());

			
				fo.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		new SaveBmp().execute();

	}
	
	public boolean isOnline() {
		ConnectivityManager cm =(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
    		return true;
		}
		return false;
}
	

}
