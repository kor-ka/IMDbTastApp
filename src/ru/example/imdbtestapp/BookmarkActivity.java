package ru.example.imdbtestapp;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.internal.base.BaseCard;
import it.gmariotti.cardslib.library.view.CardListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
		// Show the Up button in the action bar.
		setupActionBar();
		ctx = this;
		starred = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		cards = new ArrayList<Card>();
		mCardArrayAdapter = new CardArrayAdapter(this, cards);
		listView = (CardListView) findViewById(R.id.FilmList);
		listView.setAdapter(mCardArrayAdapter);
		listView.setPadding(0, 10, 0, 10);
		listView.setClipToPadding(false);
		Set<String> bookmarks = starred.getStringSet("starred", null);
		if (bookmarks!=null) {
			for (String s : starred.getStringSet("starred", null)) {
				String JsonString = starred.getString(s, null);
				if (JsonString != null) {
					try {
						JSONObject jObject;
						jObject = new JSONObject(JsonString);
						Card newCard = new Card(getBaseContext());
						final String imdbid = jObject.getString("imdbID");
						final String title = jObject.getString("Title");
						cardInit(newCard, title, imdbid);
						//newCard.getCardThumbnail().setDrawableResource(R.drawable.ic_launcher);
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

	
public Card cardInit (Card card, String headerTitle, final String idmdbid){
		
	BitmapFactory.Options options = new BitmapFactory.Options();
	options.inPreferredConfig = Bitmap.Config.ARGB_8888;
	Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+ "/IMDbTestApp/"+idmdbid+".jpg", options);
	
	
	
	
		
		CardHeader header = new CardHeader(getBaseContext());
		MyThumbnail thumb = new MyThumbnail(getBaseContext(),bitmap);
		thumb.setExternalUsage(true);
		
		
		
		
		card.setId(idmdbid);
		header.setId(idmdbid);
		header.setButtonOverflowVisible(true);
		header.setTitle(headerTitle);
		//thumb.setDrawableResource(R.drawable.ic_launcher);
		
		
		header.setPopupMenu(R.menu.cardmenu,
				new CardHeader.OnClickCardHeaderPopupMenuListener() {
			
					@Override
					public void onMenuItemClick(BaseCard cardm, MenuItem item) {
						switch(item.getItemId()){
						case R.id.cardmenubookmark:
							
							cardm.getParentCard().getCardView().setStarred(!cardm.getParentCard().isStarred());
							cardm.getParentCard().setStarred(!cardm.getParentCard().isStarred());
							if(cardm.getParentCard().isStarred()){
								Set<String> ss = starred.getStringSet("starred", null);
								Set<String> newList = new HashSet<String>();
								if (cardm.getParentCard().getId() != ""){ 
								    if (ss != null){
								        for(String each: ss){
								            newList.add(each);
								        }
								    }
								    newList.add(cardm.getParentCard().getId());
								    Editor ed = starred.edit();
								    ed.putStringSet("starred", newList);     
								    ed.apply();
								    
								}
								
							} else{
								Set<String> ss = starred.getStringSet("starred", new HashSet<String>());
								Set<String> newList = new HashSet<String>();
								if (cardm.getParentCard().getId() != ""){ 
								    if (ss != null){
								        for(String each: ss){
								        	
								        		newList.add(each);
								        	
								            
								        }
								    }
								    newList.remove(cardm.getParentCard().getId());
								    Editor ed = starred.edit();
								    ed.putStringSet("starred", newList);  
								    ed.putString(cardm.getParentCard().getId(), null);
								    ed.apply();   
								    Toast.makeText(ctx, "Bookmark deleted", Toast.LENGTH_LONG).show();
								}
							}
							
						break;
						case R.id.cardmenushare:
							Intent sendIntent = new Intent();
							sendIntent.setAction(Intent.ACTION_SEND);
							sendIntent.putExtra(Intent.EXTRA_TEXT, "http://www.imdb.com/title/"+cardm.getParentCard().getId()+"/");
							sendIntent.setType("text/plain");
							startActivity(sendIntent);
							break;
						case R.id.cardmenuopeninbrowser:
							String url = "http://www.imdb.com/title/"+idmdbid+"/";
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

}
