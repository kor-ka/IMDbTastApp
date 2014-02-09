package ru.example.imdbtestapp.utils;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.internal.base.BaseCard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import ru.example.imdbtestapp.R;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class CardInit {
	Context ctx;
	SharedPreferences starred;
	ArrayList<AsyncTask> asyncTasks;
	BmpSaver bs = new BmpSaver();
	
	public Card cardInit(Card card, String headerTitle, final String idmdbid, boolean initOld, final Context ctx, final SharedPreferences starred, ArrayList<AsyncTask> asyncTasks) {
		this.starred = starred;
		this.ctx=ctx;
		this.asyncTasks = asyncTasks;
		
		CardHeader header = new CardHeader(ctx);
		
		CardThumbnail thumb = new CardThumbnail(ctx);
		
		if(initOld){
			//if card is saved - load from offline
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			Bitmap bitmap = BitmapFactory.decodeFile(ctx.getFilesDir()+ "/IMDbTestApp/old/"+idmdbid+".jpg", options);
			if(bitmap==null){
				bitmap = BitmapFactory.decodeFile(ctx.getFilesDir()+ "/IMDbTestApp/"+idmdbid+".jpg", options);
			}
			thumb = new MyThumbnail(ctx,bitmap);
			thumb.setExternalUsage(true);
		} 
		
		
		//sei info
		card.setId(idmdbid);
		header.setId(idmdbid);
		header.setButtonOverflowVisible(true);
		header.setTitle(headerTitle);
		
		//Set up popup menu
		header.setPopupMenu(R.menu.cardmenu,
				new CardHeader.OnClickCardHeaderPopupMenuListener() {

					@Override
					public void onMenuItemClick(BaseCard cardm, MenuItem item) {
						switch (item.getItemId()) {
						case R.id.cardmenubookmark:
							//set starred/unstarred depends on current state
							cardm.getParentCard()
									.getCardView()
									.setStarred(
											!cardm.getParentCard().isStarred());
							cardm.getParentCard().setStarred(
									!cardm.getParentCard().isStarred());
							if (cardm.getParentCard().isStarred()) {
								//if starred after - save
								Set<String> ss = starred.getStringSet(
										"starred", null);
								Set<String> newList = new HashSet<String>();
								if (cardm.getParentCard().getId() != "") {
									if (ss != null) {
										for (String each : ss) {
											newList.add(each);
										}
									}
									newList.add(cardm.getParentCard().getId());
									Editor ed = starred.edit();
									ed.putStringSet("starred", newList);
									ed.apply();
									if (isOnline()) {
										getFullFilmInfoJsonString(cardm
												.getParentCard().getId());
									} else {
									
									//Save JSON string
										ed.putString(cardm.getParentCard().getId(), cardm.getParentCard().getCardExpand().getTitle());
										ed.apply();
									// Copy poster		
										
										String passto = ctx.getFilesDir().getAbsolutePath()+ "/IMDbTestApp";
										String passfrom = ctx.getFilesDir().getAbsolutePath()+ "/IMDbTestApp/old";
										File from = new File(passfrom, cardm.getParentCard().getId() + ".jpg");
										File to = new File(passto, cardm.getParentCard().getId() + ".jpg");
																
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
									}

								}

							} else {
								//if unsarred after- del from bookmarks
								Set<String> ss = starred.getStringSet(
										"starred", new HashSet<String>());
								Set<String> newList = new HashSet<String>();
								if (cardm.getParentCard().getId() != "") {
									if (ss != null) {
										for (String each : ss) {
											if (each != cardm.getParentCard()
													.getId()) {
												newList.add(each);
											}

										}
									}
									newList.remove(cardm.getParentCard()
											.getId());
									Editor ed = starred.edit();
									ed.putStringSet("starred", newList);
									ed.putString(cardm.getParentCard().getId(),
											null);
									ed.apply();
								}
							}

							break;
						case R.id.cardmenushare:
							Intent sendIntent = new Intent();
							sendIntent.setAction(Intent.ACTION_SEND);
							sendIntent.putExtra(Intent.EXTRA_TEXT,
									"http://www.imdb.com/title/" + idmdbid
											+ "/");
							sendIntent.setType("text/plain");
							ctx.startActivity(sendIntent);
						break;

						case R.id.cardmenuopeninbrowser:
							String url = "http://www.imdb.com/title/" + idmdbid
									+ "/";
							Intent oib = new Intent(Intent.ACTION_VIEW);
							oib.setData(Uri.parse(url));
							ctx.startActivity(oib);

							break;
						}

					}
				});
			
		card.addCardHeader(header);
		card.addCardThumbnail(thumb);
		card.setClickable(true);
		// Set onClick listener
		card.setOnClickListener(new Card.OnCardClickListener() {
			@Override
			public void onClick(Card card, View v) {
			}
		});
		return card;

	}
	
	public class MyThumbnail extends CardThumbnail {
		ImageView image;
		Bitmap bitmap2;
		public  MyThumbnail(Context context,Bitmap bitmap) {
	        super(context);
	        bitmap2 = bitmap;
	    }
	    @Override
	    public void setupInnerViewElements(ViewGroup parent, View viewImage) {
	    	image= (ImageView)viewImage ;
	    	if(image!=null&&bitmap2!=null){
	    		image.setImageBitmap(bitmap2);
	    	}
	        

	    
	    }
	    
	    public void setImageBMP(Bitmap bitmap){
	    	if(image!=null){
	    		image.setImageBitmap(bitmap);
	    	}
	    	
	    }
	}
	
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}
	
	void getFullFilmInfoJsonString(final String imdbid) {
		
		class Act extends AsyncTask<String, Void, String> {
			String result2 = null;

			@Override
			protected String doInBackground(String... params) {
		
				HttpClient httpclient2 = new DefaultHttpClient();
				HttpPost httppost2 = new HttpPost("http://www.omdbapi.com/?i="
						+ imdbid);
			
			
				InputStream inputStream2 = null;

				try {
					HttpResponse response2 = httpclient2.execute(httppost2);
					HttpEntity entity2 = response2.getEntity();

					inputStream2 = entity2.getContent();
				
				
					BufferedReader reader2 = new BufferedReader(
							new InputStreamReader(inputStream2, "UTF-8"), 8);
					StringBuilder sb2 = new StringBuilder();

					String line2 = null;
					while ((line2 = reader2.readLine()) != null) {
						sb2.append(line2 + "\n");
					}
					result2 = sb2.toString();

				} catch (Exception e) {
					// Oops
					e.printStackTrace();
					
				} finally {
					try {
						if (inputStream2 != null)
							inputStream2.close();
					} catch (Exception squish) {
						squish.printStackTrace();
					}
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
					bs.SaveBmp(url, imdbid, false, ctx, asyncTasks);
					asyncTasks = bs.getAsynkTasks();
				} catch (JSONException e) {
					
					
					e.printStackTrace();
				}

			};
		}
		;
		if(asyncTasks!=null){
			asyncTasks.add(new Act().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR));	
		} else{
			new Act().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		}
		

	}
	
	public ArrayList<AsyncTask> getAsynkTasks(){
		return asyncTasks;
		
	}
	
}
