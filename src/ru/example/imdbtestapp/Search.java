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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		handleIntent(getIntent());
		starred = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		tv = (TextView) findViewById(R.id.tv);
		tv.setText("nothig found.");
		tv.setVisibility(View.GONE);
		ids = new ArrayList<String>();
		ctx = this;
		oldcards = new ArrayList<Card>();
		cards = new ArrayList<Card>();

		mCardArrayAdapter = new CardArrayAdapter(this, cards);
		oldCardArrayAdapter = new CardArrayAdapter(this, oldcards);

		listView = (CardListView) findViewById(R.id.FilmList);
		listViewOld = (CardListView) findViewById(R.id.FilmListOld);
		// add the footer before adding the adapter, else the footer will not
		// load!
		footerView = ((LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.listviewfooter, null, false);
		footerView.setOnClickListener(new OnClickListener() {

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

		listView.setOnScrollListener(new OnScrollListener() {
			// useless here, skip!
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			// dumdumdum
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// what is the bottom iten that is visible
				int lastInScreen = firstVisibleItem + visibleItemCount;
				// is the bottom item visible & not loading more already ? Load
				// more !
				if ((lastInScreen == totalItemCount && !nowLoading)) {
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
						cardInit(newCard, title, imdbid, true);
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
							// Toast.makeText(ctx, "" , Toast.LENGTH_SHORT).show();
							// newCard.getCardView().setStarred(true);
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
								// ctx.startActivity (intent);
								startActivityForResult(onldintent, 1);
								// Toast.makeText(ctx,
								// onldintent.getStringExtra("imdbid"),
								// Toast.LENGTH_SHORT).show();
							}
						});

						oldcards.add(newCard);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					oldCardArrayAdapter.notifyDataSetChanged();
					// listViewOld.setVisibility(View.VISIBLE);
				}
			}
		}

	}

	private void handleIntent(Intent intent) {

		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {

			// Clear old

			if (isOnline()) {
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
				tk = new TestConnection().execute();
				nowLoading = true;
				Set<String> newList = new HashSet<String>();
				newList.clear();
				Editor ed = starred.edit();
				ed.putStringSet("old", newList);
				ed.apply();
				
				File dir = new File(Environment.getExternalStorageDirectory()+"/IMDbTestApp/old");
				if (dir.isDirectory()) {
			        String[] children = dir.list();
			        for (int i = 0; i < children.length; i++) {
			            new File(dir, children[i]).delete();
			        }
			    }

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search, menu);
		MenuItem searchViewItem = menu.findItem(R.id.search);
		SearchView searchView2 = (SearchView) searchViewItem.getActionView();

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchView = (SearchView) menu.findItem(R.id.search).getActionView();
		searchView2.setIconifiedByDefault(false);
		searchView2.setMaxWidth(500);
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
					// finish();
				}
			});

			break;

		}

		return progress;

	}

	String serverSay;
	final String TAG = "tag";
	String Response;
	private static final int PROGRESS_DLG_ID = 1;

	class TestConnection extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			Log.d(TAG,
					"������� ������� �������");
			publishProgress(new Void[] {});

			DefaultHttpClient httpclient = new DefaultHttpClient(
					new BasicHttpParams());
			HttpPost httppost = new HttpPost("http://www.omdbapi.com/?s="
					+ query + "*");
			// Depends on your web service
			httppost.setHeader("Content-type", "application/json");

			InputStream inputStream = null;

			try {
				HttpResponse response = httpclient.execute(httppost);
				HttpEntity entity = response.getEntity();

				inputStream = entity.getContent();
				// json is UTF-8 by default
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(inputStream, "UTF-8"), 8);
				StringBuilder sb = new StringBuilder();

				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				resultJSON = sb.toString();

			} catch (Exception e) {
				// Oops
			} finally {
				try {
					if (inputStream != null)
						inputStream.close();
				} catch (Exception squish) {
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

			lastAdded = -1;
			if (isOnline()) {
				loadSet();
			} else {
				Toast.makeText(ctx, "Cant't load data. Offline.",
						Toast.LENGTH_SHORT).show();
			}

			searchView.clearFocus();

		}

	}

	static String convertStreamToString(java.io.InputStream is) {
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	public Card cardInit(Card card, String headerTitle, final String idmdbid, boolean initOld) {

		CardHeader header = new CardHeader(getBaseContext());
		
		CardThumbnail thumb = new CardThumbnail(getBaseContext());
		
		if(initOld){
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+ "/IMDbTestApp/old/"+idmdbid+".jpg", options);
			
			thumb = new MyThumbnail(getBaseContext(),bitmap);
			thumb.setExternalUsage(true);
		} 
		
		
		
		card.setId(idmdbid);
		header.setId(idmdbid);
		header.setButtonOverflowVisible(true);
		header.setTitle(headerTitle);
		// thumb.setDrawableResource(R.drawable.ic_launcher);

		header.setPopupMenu(R.menu.cardmenu,
				new CardHeader.OnClickCardHeaderPopupMenuListener() {

					@Override
					public void onMenuItemClick(BaseCard cardm, MenuItem item) {
						switch (item.getItemId()) {
						case R.id.cardmenubookmark:

							cardm.getParentCard()
									.getCardView()
									.setStarred(
											!cardm.getParentCard().isStarred());
							cardm.getParentCard().setStarred(
									!cardm.getParentCard().isStarred());
							if (cardm.getParentCard().isStarred()) {
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
									/*	Toast.makeText(ctx,
												"Cant't load data. Offline.",
												Toast.LENGTH_SHORT).show();
										*/
									//Save JSON string
										ed.putString(cardm.getParentCard().getId(), cardm.getParentCard().getCardExpand().getTitle());
										ed.apply();
									// Copy poster		
										
										String passto = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/IMDbTestApp";
										String passfrom = Environment.getExternalStorageDirectory().getAbsolutePath()+ "/IMDbTestApp/old";
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
							startActivity(sendIntent);

						case R.id.cardmenuopeninbrowser:
							String url = "http://www.imdb.com/title/" + idmdbid
									+ "/";
							Intent oib = new Intent(Intent.ACTION_VIEW);
							oib.setData(Uri.parse(url));
							startActivity(oib);

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

	void setFullFilmInfo(final Card card, final String imdbid) {
		nowLoading = true;
		class Act extends AsyncTask<String, Void, String> {
			String result2 = null;

			@Override
			protected String doInBackground(String... params) {
				nowLoading = true;
				HttpClient httpclient2 = new DefaultHttpClient();
				HttpPost httppost2 = new HttpPost("http://www.omdbapi.com/?i="
						+ imdbid);
				// Depends on your web service
				// httppost2.setHeader("Content-type", "application/json");

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
					// Oops
					
				} finally {
					try {
						if (inputStream2 != null)
							inputStream2.close();
					} catch (Exception squish) {
					}
				}

				return null;
			}

			protected void onPostExecute(String result) {

				// card.getCardHeader().setTitle(result2);

				if (result2!=null) {
					try {
						final JSONObject jObject2 = new JSONObject(result2);
						card.setTitle(jObject2.getString("Country") + " | "
								+ jObject2.getString("Year") + "\n"
								+ jObject2.getString("Plot"));
						// card.getCardThumbnail().setExternalUsage(true);
						card.getCardThumbnail().setUrlResource(
								jObject2.getString("Poster"));
						card.getCardExpand().setTitle(result2);
						// Add to old
						Set<String> ss = starred.getStringSet("old", null);
						Set<String> newList = new HashSet<String>();
						if (result2 != "") {
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

						SaveBmp(jObject2.getString("Poster"), imdbid, true);

						// setPoster(card, jObject2.getString("Poster"));
						// card.getCardThumbnail().setDrawableResource(R.drawable.ic_launcher)
						mCardArrayAdapter.notifyDataSetChanged();
						// card.getCardHeader().setTitle(jObject2.getString("Plot"));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				nowLoading = false;
			};
		}
		;

		new Act().execute();

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:

			cardToChange.setStarred(data.getBooleanExtra("isstarred", false));
			mCardArrayAdapter.notifyDataSetChanged();
			oldCardArrayAdapter.notifyDataSetChanged();
			break;

		case RESULT_CANCELED:
			// do nothing
			break;
		}
	}

	public void loadSet() {
		try {

			JSONObject jObject;

			jObject = new JSONObject(resultJSON);

			JSONArray jArray = jObject.getJSONArray("Search");
			int lastMore = lastAdded + 1;
			for (int i = lastMore; i < jArray.length() & (i - lastMore) < 3; i++) {
				try {
					JSONObject oneObject = jArray.getJSONObject(i);
					// Pulling items from the array
					final String title = oneObject.getString("Title");
					final String imdbid = oneObject.getString("imdbID");

					Card newCard = new Card(getBaseContext());
					newCard = cardInit(newCard, title, imdbid, false);
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
							// ctx.startActivity (intent);
							startActivityForResult(intent, 1);
						}
					});
					// jObjectfilinfo= new JSONObject(getFilmInfo(imdbid));
					// newCard.getParentCard().setTitle(jObjectfilinfo.getString("Year"));

					// sdsssssssssssssssssssssssssssssssssssssssss
					// sdsssssssssssssssssssssssssssssssssssssssss
					cards.add(newCard);

					mCardArrayAdapter.notifyDataSetChanged();
					Set<String> ss = starred.getStringSet("starred",
							new HashSet<String>());
					if (ss.contains(imdbid)) {
						// Toast.makeText(ctx, "" , Toast.LENGTH_SHORT).show();
						// newCard.getCardView().setStarred(true);
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
						listView.removeFooterView(footerView);

						Card newCard2 = new Card(getBaseContext());
						newCard2 = cardInit(newCard2, title, imdbid, false);
						CardExpand card2ex = new CardExpand(ctx);
						newCard2.addCardExpand(card2ex);
						if (isOnline()) {
							setFullFilmInfo(newCard2, imdbid);
						} else {
							Toast.makeText(ctx, "Cant't load data. Offline.",
									Toast.LENGTH_SHORT).show();
						}

						// mCardArrayAdapter.notifyDataSetChanged();
						// Toast.makeText(getBaseContext(), "Stop!",
						// Toast.LENGTH_LONG).show();
					}
				} catch (JSONException e) {
					// Oops
				}
			}
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
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

	void getFullFilmInfoJsonString(final String imdbid) {
		nowLoading = true;
		class Act extends AsyncTask<String, Void, String> {
			String result2 = null;

			@Override
			protected String doInBackground(String... params) {
				nowLoading = true;
				HttpClient httpclient2 = new DefaultHttpClient();
				HttpPost httppost2 = new HttpPost("http://www.omdbapi.com/?i="
						+ imdbid);
				// Depends on your web service
				// httppost2.setHeader("Content-type", "application/json");

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
					// Oops
					Toast.makeText(getBaseContext(), "Ooops" + e.getMessage(),
							Toast.LENGTH_LONG).show();
				} finally {
					try {
						if (inputStream2 != null)
							inputStream2.close();
					} catch (Exception squish) {
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
					SaveBmp(url, imdbid, false);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			};
		}
		;

		new Act().execute();

	}

	public void SaveBmp(final String url, final String id, final boolean isOld) {

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
				if (bitmap!=null) {
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
					// you can create a new file name "test.jpg" in sdcard folder.
					String pass;
					String name;
					if (isOld) {
						pass = Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/IMDbTestApp/";
						name = "old";

					} else {
						pass = Environment.getExternalStorageDirectory()
								.getAbsolutePath();
						name = "IMDbTestApp";

					}
					File exportDir = new File(pass, name);
					if (!exportDir.exists()) {
						exportDir.mkdirs();
					}
					File f = new File(exportDir, id + ".jpg");
					try {
						f.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// write the bytes in file
					FileOutputStream fo;
					try {
						fo = new FileOutputStream(f);

						fo.write(bytes.toByteArray());

						// remember close de FileOutput
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
		}

		new SaveBmp().execute();

	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
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

}
