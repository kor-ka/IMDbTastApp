package ru.example.imdbtestapp;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardExpand;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.internal.base.BaseCard;
import it.gmariotti.cardslib.library.view.CardListView;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.example.imdbtestapp.utils.BmpSaver;
import ru.example.imdbtestapp.utils.CardInit;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

public class Search extends Activity {

	TextView tv;
	String query;
	List<String> ids;
	String print;
	Card card;
	ArrayList<Card> cards;
	ArrayList<Card> oldcards;
	CardArrayAdapter mCardArrayAdapter;
	CardArrayAdapter oldCardArrayAdapter;
	String resultJSON;
	String filmInfo;
	int lastAdded;
	CardListView listView;
	CardListView listViewOld;
	View footerView;
	boolean nowLoading;
	MenuItem lbc;
	SearchView searchView;
	AsyncTask tk;
	Context ctx;
	SharedPreferences starred;
	Card cardToChange;
	ArrayList<AsyncTask> asyncTasks;
	int lastMore;
	boolean taskCnceled;
	String serverSay;
	final String TAG = "tag";
	String Response;
	private static final int PROGRESS_DLG_ID = 1;
	CardInit ci;
	BmpSaver bs= new BmpSaver(); 

	//Init&setUp old search results
	//////////////////////////////
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		handleIntent(getIntent());
		taskCnceled = false;
		ci = new CardInit();
		starred = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		tv = (TextView) findViewById(R.id.tv);
		tv.setText("nothig found.");
		tv.setVisibility(View.GONE);
		ids = new ArrayList<String>();
		ctx = this;
		oldcards = new ArrayList<Card>();
		cards = new ArrayList<Card>();
		asyncTasks = new ArrayList<AsyncTask>();
		mCardArrayAdapter = new CardArrayAdapter(this, cards);
		oldCardArrayAdapter = new CardArrayAdapter(this, oldcards);

		listView = (CardListView) findViewById(R.id.FilmList);
		listViewOld = (CardListView) findViewById(R.id.FilmListOld);
		
		footerView = ((LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.listviewfooter, null, false);
		footerView.setOnClickListener(new OnClickListener() {
			//set up load on footer click
			@Override
			public void onClick(View v) {
				if (isOnline()) {
					loadSet();
					
				} else {
					Toast.makeText(ctx, "Cant't load data. Offline.",
							Toast.LENGTH_SHORT).show();
				}

			}
		});
		listView.addFooterView(footerView);
		if (listView != null) {
			listView.setAdapter(mCardArrayAdapter);
		}
		listView.setPadding(0, 10, 0, 10);
		listView.setClipToPadding(false);

		if (listViewOld != null) {
			listViewOld.setAdapter(oldCardArrayAdapter);
		}
		listViewOld.setPadding(0, 10, 0, 10);
		listViewOld.setClipToPadding(false);

		listView.setVisibility(View.GONE);
		//set up auto load on last card on screen
		listView.setOnScrollListener(new OnScrollListener() {
			// useless here, skip!
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			// dumdumdum
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// what is the bottom item that is visible
				int lastInScreen = firstVisibleItem + visibleItemCount;
				// is the bottom item visible & not loading more already ? Load more !
				
				if ((lastInScreen == /*E pur si muove!*/totalItemCount && !nowLoading)) {
					 
					if (!lbc.isChecked()) {

						if (isOnline()) {
							loadSet();
							
						} else {
							Toast.makeText(ctx, "Cant't load data. Offline.",
									Toast.LENGTH_SHORT).show();
						}

					}
				}
			}
		});
		
		//If we got old search result, add it!
		Set<String> ss = starred.getStringSet("old", null);
		if (ss != null) {

			for (String s : starred.getStringSet("old", null)) {
				// Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
				String JsonString = s;
				if (JsonString != null) {
					try {
						JSONObject jObject;
						jObject = new JSONObject(JsonString);
						Card newCard = new Card(getBaseContext());
						final String imdbid = jObject.getString("imdbID");
						final String title = jObject.getString("Title");
						
						ci.cardInit(newCard, title, imdbid, true, ctx, starred, asyncTasks);
						asyncTasks = ci.getAsynkTasks();
						CardExpand cardex = new CardExpand(ctx);
						cardex.setTitle(JsonString);
						newCard.addCardExpand(cardex);
						newCard.getCardThumbnail().setDrawableResource(
								R.drawable.ic_launcher);
						newCard.setTitle(jObject.getString("Country") + " | "
								+ jObject.getString("Year") + "\n"
								+ jObject.getString("Plot"));
						final String JsonToIntent = JsonString;
						Set<String> ss1 = starred.getStringSet("starred",
								new HashSet<String>());
						if (ss1.contains(imdbid)) {
							
							newCard.setStarred(true);
						}
						
						newCard.setOnClickListener(new Card.OnCardClickListener() {
							@Override
							public void onClick(Card card, View view) {

								Intent onldintent = new Intent(ctx,
										FilmActivity.class);
								onldintent.putExtra("imdbid", imdbid);
								onldintent.putExtra("Title", title);
								onldintent.putExtra("isstarred", false);								
								onldintent.putExtra("JSONString", JsonToIntent);
								cardToChange = card;
								
								startActivityForResult(onldintent, 1);
								
							}
						});

						oldcards.add(newCard);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					oldCardArrayAdapter.notifyDataSetChanged();
					
				}
			}
		}

	}

	//Here we peek up query & init search
	///////////////////////
	private void handleIntent(Intent intent) {
		
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

			if (isOnline()) {
				query = intent.getStringExtra(SearchManager.QUERY);
				if (!containsIllegals(query)) {
					//Fixing query
					query = intent.getStringExtra(SearchManager.QUERY);
					query = query.replaceAll(" ", "%20");
					query = query + "*";
					ids.clear();
					// use the query to search your data somehow
					cards.clear();
					listView.removeFooterView(footerView);
					listView.addFooterView(footerView);
					mCardArrayAdapter.notifyDataSetChanged();
					listViewOld.setVisibility(View.GONE);
					tv.setVisibility(View.INVISIBLE);
					listView.setVisibility(View.GONE);
					//Canceling old async tasks
					
					if (asyncTasks!=null) {
						for (AsyncTask atClose : asyncTasks) {
							if (atClose != null) {
								atClose.cancel(true);
								
							}
						}
						asyncTasks.clear();
					}
					
					nowLoading = true;
					
					//Clear list of last search
					Set<String> newList = new HashSet<String>();
					newList.clear();
					Editor ed = starred.edit();
					ed.putStringSet("old", newList);
					ed.apply();
					//Clear last search pictures
					File dir = new File(
							getFilesDir()
									+ "/IMDbTestApp/old");
					if (dir.isDirectory()) {
						String[] children = dir.list();
						for (int i = 0; i < children.length; i++) {
							new File(dir, children[i]).delete();
						}
					}
					//check if tk is not started at the moment (double start on Handle intent fix)
					if (tk==null) {
						tk = new LoadFilmIds()
								.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						
					}
					
				} else { Toast.makeText(ctx, "Please, don't use illegal characters", Toast.LENGTH_LONG).show(); }

			} else {
				Toast.makeText(ctx, "Cant't load data. Offline.",
						Toast.LENGTH_SHORT).show();
			}

		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		handleIntent(intent);
		
	}
	
	//Common stuff
	/////////////
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.search, menu);
		MenuItem searchViewItem = menu.findItem(R.id.search);
		SearchView searchView2 = (SearchView) searchViewItem.getActionView();

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView = (SearchView) menu.findItem(R.id.search).getActionView();
		searchView2.setIconifiedByDefault(false);
		//Set search widget size 
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int scrWidth = size.x;		
		searchView2.setMaxWidth(scrWidth/3*2);
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));

		lbc = menu.findItem(R.id.lbc);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		return super.onPrepareOptionsMenu(menu);

	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.lbc:
			if (item.isChecked()) {
				item.setChecked(false);
			} else {
				item.setChecked(true);
			}
			break;

		case R.id.bookmarks:

			Intent i = new Intent(ctx, BookmarkActivity.class);
			startActivity(i);
			break;
		}
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int dialogId) {

		ProgressDialog progress = null;

		
		switch (dialogId) {

		case PROGRESS_DLG_ID:

			progress = new ProgressDialog(this);

			progress.setMessage("Searching...");

			progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					tk.cancel(true);
					
				}
			});

			break;

		}

		return progress;

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			//Fix starred status
			cardToChange.setStarred(data.getBooleanExtra("isstarred", false));
			mCardArrayAdapter.notifyDataSetChanged();
			oldCardArrayAdapter.notifyDataSetChanged();
			break;

		case RESULT_CANCELED:
			// do nothing
			break;
		}
	}

	//Load&SetUp Card
	/////////////////
	class LoadFilmIds extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
		publishProgress(new Void[] {});
		
		//Set timeout for 7 sec.
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 7000);
		HttpConnectionParams.setSoTimeout(httpParams, 7000);
			DefaultHttpClient httpclient = new DefaultHttpClient(
					httpParams);
			
			HttpPost httppost = new HttpPost("http://www.omdbapi.com/?s="
					+ query + "*");
			
			httppost.setHeader("Content-type", "application/json");

			InputStream inputStream = null;

			try {
				
				
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();
				
				inputStream = entity.getContent();
				 
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inputStream, "UTF-8"), 8);
				StringBuilder sb = new StringBuilder();

				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				resultJSON = sb.toString();

			} catch (Exception e) {
				e.printStackTrace();
				//Timeout - cancel
				this.cancel(true);
				taskCnceled = true;
				
			} 
			finally {
				try {
					if (inputStream != null)
						inputStream.close();
				} catch (Exception squish) {
					squish.printStackTrace();
				}
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {

			super.onProgressUpdate(values);

			showDialog(PROGRESS_DLG_ID);

		}

		@Override
		protected void onPostExecute(String result) {

			super.onPostExecute(result);

			dismissDialog(PROGRESS_DLG_ID);
			//Added card count to -1
			lastAdded = -1;
			if (isOnline()) {
				
				loadSet();
				//Let know, that tk is not running any more
				tk=null;
			} else {
				Toast.makeText(ctx, "Cant't load data. Offline.",
						Toast.LENGTH_SHORT).show();
			}

			searchView.clearFocus();

		}
		private Boolean result;
		 @Override
		  protected void onCancelled() {
		    handleOnCancelled(this.result);
		 }
		
	
		  
		  private void handleOnCancelled(Boolean result) {
			  //hide keyboard
			  searchView.clearFocus();
			  if (taskCnceled) {
				Toast.makeText(ctx, "Cant't load data. Connection time out.",
						Toast.LENGTH_SHORT).show();
				taskCnceled=false;
			}
			dismissDialog(PROGRESS_DLG_ID);
			//Let know, that tk is not running any more
			  tk=null;
		  }

	}

	public void loadSet() {
		try {
			//load set of new cards from ids list
			JSONObject jObject;

			jObject = new JSONObject(resultJSON);

			JSONArray jArray = jObject.getJSONArray("Search");
			lastMore = lastAdded + 1;
			
			for (int i = lastMore; i < jArray.length() & (i - lastMore) < 3; i++) {
				try {
					
					JSONObject oneObject = jArray.getJSONObject(i);
					// Pulling items from the array
					final String title = oneObject.getString("Title");
					final String imdbid = oneObject.getString("imdbID");

					Card newCard = new Card(getBaseContext());
					newCard = ci.cardInit(newCard, title, imdbid, false, ctx, starred, asyncTasks);
					asyncTasks = ci.getAsynkTasks();
					CardExpand cardex = new CardExpand(ctx);
					cardex.setTitle("");
					newCard.addCardExpand(cardex);
					newCard.setOnClickListener(new Card.OnCardClickListener() {
						@Override
						public void onClick(Card card, View view) {

							Intent intent = new Intent(ctx, FilmActivity.class);
							intent.putExtra("imdbid", imdbid);
							intent.putExtra("Title", title);
							intent.putExtra("isstarred", card.isStarred());
							intent.putExtra("JSONString", card.getCardExpand().getTitle());
							cardToChange = card;
						
							startActivityForResult(intent, 1);
						}
					});
				
					cards.add(newCard);

					mCardArrayAdapter.notifyDataSetChanged();
					Set<String> ss = starred.getStringSet("starred",
							new HashSet<String>());
					if (ss.contains(imdbid)) {
					
					
						newCard.setStarred(true);
					}
					if (isOnline()) {
						setFullFilmInfo(newCard, imdbid);
					} else {
						Toast.makeText(ctx, "Cant't load data. Offline.",
								Toast.LENGTH_SHORT).show();
					}
					
					lastAdded = i;
					
					if (lastAdded + 1 >= jArray.length()) {
						//It was last card, remove footer
						listView.removeFooterView(footerView);

						Card newCard2 = new Card(getBaseContext());
						newCard2 = ci.cardInit(newCard2, title, imdbid, false, ctx, starred, asyncTasks);
						asyncTasks= ci.getAsynkTasks();
						CardExpand card2ex = new CardExpand(ctx);
						newCard2.addCardExpand(card2ex);
						if (isOnline()) {
							setFullFilmInfo(newCard2, imdbid);
						} else {
							Toast.makeText(ctx, "Cant't load data. Offline.",
									Toast.LENGTH_SHORT).show();
						}

					
					
					}
				} catch (JSONException e) {
					// Oops
					e.printStackTrace();
				}
			}
		} catch (JSONException e1) {
		
		
			e1.printStackTrace();
		}

		if (cards.isEmpty()) {
			tv.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		} else {
			tv.setVisibility(View.INVISIBLE);
			listView.setVisibility(View.VISIBLE);
		}

	}
	
	void setFullFilmInfo(final Card card, final String imdbid) {
		nowLoading = true;
		class Actsff extends AsyncTask<String, Void, String> {
			String result2 = null;

			@Override
			protected String doInBackground(String... params) {
				nowLoading = true;
				HttpClient httpclient2 = new DefaultHttpClient();
				HttpPost httppost2 = new HttpPost("http://www.omdbapi.com/?i="
						+ imdbid);
				
				InputStream inputStream2 = null;

				try {
					HttpResponse response2 = httpclient2.execute(httppost2);
					HttpEntity entity2 = response2.getEntity();

					inputStream2 = entity2.getContent();
					// json is UTF-8 by default
					BufferedReader reader2 = new BufferedReader(
							new InputStreamReader(inputStream2, "UTF-8"), 8);
					StringBuilder sb2 = new StringBuilder();

					String line2 = null;
					while ((line2 = reader2.readLine()) != null) {
						sb2.append(line2 + "\n");
					}
					result2 = sb2.toString();

				} catch (Exception e) {
					e.printStackTrace();
					
					// Oops
					
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

				// card.getCardHeader().setTitle(result2);

				if (result2!=null) {
					try {
						//Set info  from Json to card
						final JSONObject jObject2 = new JSONObject(result2);
						card.setTitle(jObject2.getString("Country") + " | "
								+ jObject2.getString("Year") + "\n"
								+ jObject2.getString("Plot"));
					
						card.getCardThumbnail().setUrlResource(
								jObject2.getString("Poster"));
						card.getCardExpand().setTitle(result2);
						// Add to old
						Set<String> ss = starred.getStringSet("old", null);
						Set<String> newList = new HashSet<String>();
						if (result2 != null) {
							if (ss != null) {
								for (String each : ss) {
									newList.add(each);
								}
							}
							newList.add(result2);
							Editor ed = starred.edit();
							ed.putStringSet("old", newList);
							ed.apply();

						}

						bs.SaveBmp(jObject2.getString("Poster"), imdbid, true, ctx, asyncTasks);
						asyncTasks = bs.getAsynkTasks();
						
						mCardArrayAdapter.notifyDataSetChanged();
						
					} catch (JSONException e) {
						
						e.printStackTrace();
					}
				}
				nowLoading = false;
			};
		}
		;
		
		//Add task to list for canceling
 		asyncTasks.add(new Actsff().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR));

	}

	//Usefull stuff
	///////////////
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}
	
	public boolean containsIllegals(String toExamine) {
	    Pattern pattern = Pattern.compile("[~#@*%+{}<>\\[\\]|\"\\_^\\\\]");
	    Matcher matcher = pattern.matcher(toExamine);
	    return matcher.find();
	}

	static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

}
