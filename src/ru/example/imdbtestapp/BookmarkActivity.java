package ru.example.imdbtestapp;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardExpand;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.internal.base.BaseCard;
import it.gmariotti.cardslib.library.view.CardListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import ru.example.imdbtestapp.utils.CardInit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

public class BookmarkActivity extends Activity {

	ArrayList<Card> cards;
	CardArrayAdapter mCardArrayAdapter;
	CardListView listView;
	SharedPreferences starred;
	Context ctx;
	Card cardToChange;
	CardInit ci;
	
	@Override
    public void onActivityResult(int requestCode,int  resultCode, Intent data){
    	switch(resultCode){
    	case RESULT_OK:
    	
    		cardToChange.setStarred(data.getBooleanExtra("isstarred", false));
    		mCardArrayAdapter.notifyDataSetChanged();
    		
    		break;
    	
    	case RESULT_CANCELED:
    		//do nothing
    		break;
    	}
    } 
	 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bookmark);
		
		setupActionBar();
		ctx = this;
		ci = new CardInit();
		starred = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		cards = new ArrayList<Card>();
		mCardArrayAdapter = new CardArrayAdapter(this, cards);
		listView = (CardListView) findViewById(R.id.FilmList);
		listView.setAdapter(mCardArrayAdapter);
		listView.setPadding(0, 10, 0, 10);
		listView.setClipToPadding(false);
		
		//get list of bookmarked films
		Set<String> bookmarks = starred.getStringSet("starred", null);
		if (bookmarks!=null) {
			for (String s : starred.getStringSet("starred", null)) {
				String JsonString = starred.getString(s, null);
				if (JsonString != null) {
					try {
						//get info from JSON
						JSONObject jObject;
						jObject = new JSONObject(JsonString);
						Card newCard = new Card(getBaseContext());
						final String imdbid = jObject.getString("imdbID");
						final String title = jObject.getString("Title");
						ArrayList<AsyncTask> asyncTasks = new ArrayList<AsyncTask>();
						//init card
						ci.cardInit(newCard, title, imdbid, true, ctx, starred, asyncTasks);
						CardExpand cardex = new CardExpand(ctx);
						cardex.setTitle(JsonString);
						newCard.addCardExpand(cardex);
						final String JsonToIntent = JsonString;
						//setting up full info
						newCard.setTitle(jObject.getString("Country") + " | "
								+ jObject.getString("Year") + "\n"
								+ jObject.getString("Plot"));
						newCard.setOnClickListener(new Card.OnCardClickListener() {
							@Override
							public void onClick(Card card, View view) {

								Intent intent = new Intent(ctx,
										FilmActivity.class);
								intent.putExtra("imdbid", imdbid);
								intent.putExtra("Title", title);
								intent.putExtra("isstarred", card.isStarred());
								intent.putExtra("JSONString", JsonToIntent);
								cardToChange = card;
								//ctx.startActivity (intent);
								startActivityForResult(intent, 1);
							}
						});
						newCard.setStarred(true);
						cards.add(newCard);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					mCardArrayAdapter.notifyDataSetChanged();
				}

			}
		}else{
			Toast.makeText(ctx, "No bookmarks yet.", Toast.LENGTH_SHORT).show();
		}
		
		
	}


	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.bookmark, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
		
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
