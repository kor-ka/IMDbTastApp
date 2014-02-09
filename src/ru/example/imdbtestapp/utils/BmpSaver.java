package ru.example.imdbtestapp.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class BmpSaver {
	ArrayList<AsyncTask> asyncTasks;
	public void SaveBmp(final String url, final String id, final boolean isOld, final Context ctx, ArrayList<AsyncTask> asyncTasks) {
		this.asyncTasks = asyncTasks;
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
					
					
					String pass;
					String name;
					if (isOld) {
						pass = ctx.getFilesDir()
								.getAbsolutePath() + "/IMDbTestApp/";
						name = "old";

					} else {
						pass = ctx.getFilesDir()
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
		}
		if(asyncTasks!=null){
			asyncTasks.add(new SaveBmp().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR));	
		}else{
			new SaveBmp().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
		}
		
		
		
	}
	
	public ArrayList<AsyncTask> getAsynkTasks(){
		return asyncTasks;
		
	}
}
