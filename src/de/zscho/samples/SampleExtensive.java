package de.zscho.samples;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import de.zscho.R;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.CloudmadeUtil;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.GeoPointTest;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MinimapOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.SimpleLocationOverlay;

import de.zscho.ResourceProxyImpl;
import de.zscho.constants.OpenStreetMapConstants;

import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

/**
 * 
 * @author Example Nicolas Gramlich
 * @author Patrick Kirsch
 * 
 */
public class SampleExtensive extends SampleMapActivity implements OpenStreetMapConstants {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int MENU_ZOOMIN_ID = Menu.FIRST;
	private static final int MENU_ZOOMOUT_ID = MENU_ZOOMIN_ID + 1;
	private static final int MENU_TILE_SOURCE_ID = MENU_ZOOMOUT_ID + 1;
	private static final int MENU_ANIMATION_ID = MENU_TILE_SOURCE_ID + 1;
	private static final int MENU_MINIMAP_ID = MENU_ANIMATION_ID + 1;
	private static final int MENU_HYDRANT_ID = MENU_MINIMAP_ID + 1;
	private static final int MENU_SUBMIT_ID = MENU_HYDRANT_ID + 1;

	// ===========================================================
	// Fields
	// ===========================================================

	private MapView mOsmv;
	private MapController mOsmvController;
	//private SimpleLocationOverlay mMyLocationOverlay;
	public ItemizedOverlayWithFocus<OverlayItem> mMyLocationOverlay;
	private MyLocationOverlay mLocationOverlay;
	private ResourceProxy mResourceProxy;
	private ScaleBarOverlay mScaleBarOverlay;
	private MinimapOverlay mMiniMapOverlay;
	public GeoPoint[] HydrantenGPs = null; 
	private RelativeLayout RelativeLayout_View = null; 
	// ===========================================================
	// Constructors
	// ===========================================================
	
	private GPSLocation gps = null; 
	
	/** Called when the activity is first created. */
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, false); // Pass true here to actually contribute to OSM!
		
		mResourceProxy = new ResourceProxyImpl(getApplicationContext());
		
		RelativeLayout rl = new RelativeLayout(this);

		CloudmadeUtil.retrieveCloudmadeKey(getApplicationContext());

		this.mOsmv = new MapView(this, 256);
		this.mOsmvController = this.mOsmv.getController();
		RelativeLayout_View = rl;
		rl.addView(this.mOsmv, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		
		/* Scale Bar Overlay */
		{
			this.mScaleBarOverlay = new ScaleBarOverlay(this, mResourceProxy);
			this.mOsmv.getOverlays().add(mScaleBarOverlay);
			// Scale bar tries to draw as 1-inch, so to put it in the top center, set x offset to
			// half screen width, minus half an inch.
			this.mScaleBarOverlay.setScaleBarOffset(getResources().getDisplayMetrics().widthPixels
					/ 2 - getResources().getDisplayMetrics().xdpi / 2, 10);
		}

		/* SingleLocation-Overlay 
		{
			this.mMyLocationOverlay = new SimpleLocationOverlay(this, mResourceProxy);
			this.mOsmv.getOverlays().add(mMyLocationOverlay);
		}*/

		/* ZoomControls */
		{
			/* Create a ImageView with a zoomIn-Icon. */
			final ImageView ivZoomIn = new ImageView(this);
			ivZoomIn.setImageResource(R.drawable.zoom_in);
			/* Create RelativeLayoutParams, that position it in the top right corner. */
			final RelativeLayout.LayoutParams zoominParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			zoominParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			zoominParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			rl.addView(ivZoomIn, zoominParams);

			ivZoomIn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					SampleExtensive.this.mOsmvController.zoomIn();
				}
			});

			/* Create a ImageView with a zoomOut-Icon. */
			final ImageView ivZoomOut = new ImageView(this);
			ivZoomOut.setImageResource(R.drawable.zoom_out);

			/* Create RelativeLayoutParams, that position it in the top left corner. */
			final RelativeLayout.LayoutParams zoomoutParams = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT);
			zoomoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			zoomoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			rl.addView(ivZoomOut, zoomoutParams);

			ivZoomOut.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					SampleExtensive.this.mOsmvController.zoomOut();
				}
			});
		}
		
		if (gps == null) {
        	gps = new GPSLocation();
        }
		
		/* Itemized Overlay */
		try {
			/* Create a static ItemizedOverlay showing a some Markers on some cities. */
			final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

			HTTP_Update update = new HTTP_Update();
			update.pos = "lat="+gps.lat+"&lon="+gps.lon;
			String tmp[] = update.overwriteFromBackend();
			if (tmp != null) {
				
			Log.d("Overlay","hydranten Anz:"+tmp.length);
			HydrantenGPs = new GeoPoint[Math.round((tmp.length/3))];
			gps.HydrantenGPs = HydrantenGPs;
			int HydrantenGPs_index = 0;
			/* Item 0 wird später für den zur Zeit (here) Standpunkt verwendet */
			
			for (int i=0;i<tmp.length;i+=3) {
				//if (tmp[i] == null || tmp[i+1] == null)
				//	break;
				//System.out.println("read at:"+i+","+tmp[i]+","+tmp[i+1]);
				HydrantenGPs[HydrantenGPs_index] = new GeoPoint(Integer.parseInt(tmp[i]),Integer.parseInt(tmp[i+1]));
				/* not needed, because of man overlay (mLocationOverlay)
				 * if (i==0)
					items.add(new OverlayItem("Sie sind hier.", "Position", HydrantenGPs[0]));
				*/
				//Log.d( "Overlay","overlay: lat:"+Integer.parseInt(tmp[i].replaceAll("\\.", ""))+" lon:"+Integer.parseInt(tmp[i+1].replaceAll("\\.", "")));
				//Log.d( "Overlay","overlay: lat:"+tmp[i]+" lon:"+tmp[i+1]);
				items.add(new OverlayItem(tmp[i+2].split("\t")[0], tmp[i+2].split("\t")[1], HydrantenGPs[HydrantenGPs_index++]));
			}
			} else {
				Toast.makeText(
						SampleExtensive.this,
						"Hydranten konnten nicht geladen werden, bitte wenden Sie sich an pkirsch@zscho.de ", Toast.LENGTH_LONG).show();
			}
						
			/* OnTapListener for the Markers, shows a simple Toast.*/ 
			this.mMyLocationOverlay = new ItemizedOverlayWithFocus<OverlayItem>(this, items,
					new ItemizedOverlay.OnItemGestureListener<OverlayItem>() {
						
						public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
							Toast.makeText(
									SampleExtensive.this,
									//"Item '" + item.mTitle + "' (index=" + index+ ") got single tapped up", Toast.LENGTH_LONG).show();
									item.mTitle+" "+item.mDescription, Toast.LENGTH_LONG).show();
							return true;
						}
						
						public boolean onItemLongPress(final int index, final OverlayItem item) {
							Toast.makeText(
									SampleExtensive.this, //"Item '" + item.mTitle + "' (index=" + index + ") got long pressed", Toast.LENGTH_LONG).show();
									item.mTitle+" "+item.mDescription, Toast.LENGTH_LONG).show();
							return false;
						}
					}, mResourceProxy);
			
			this.mMyLocationOverlay.setFocusItemsOnTap(true);
			this.mOsmv.getOverlays().add(this.mMyLocationOverlay);
		} catch (Exception e) {
			e.printStackTrace();
		}
		/* Person show */
		this.mLocationOverlay = new MyLocationOverlay(this.getBaseContext(), this.mOsmv,
				mResourceProxy);
		this.mLocationOverlay.enableMyLocation();
		this.mOsmv.getOverlays().add(this.mLocationOverlay);
		
		/* MiniMap */
		mMiniMapOverlay = new MinimapOverlay(this, mOsmv.getTileRequestCompleteHandler());
/*		{
			mMiniMapOverlay = new MinimapOverlay(this, mOsmv.getTileRequestCompleteHandler());
			this.mOsmv.getOverlays().add(mMiniMapOverlay);
		}*/
		
		this.setContentView(rl);
		
		/* center 
		 * 02-06 12:41:55.894: DEBUG/Overlay(1921): overlay: lat:49457943 lon:11068125
		 * 02-06 12:41:55.844: DEBUG/Overlay(1921): overlay: lat:49470621 lon:11064959
		 * Centering tut nicht, Mapping
		 * */
		this.mOsmv.getController().setZoom(18);
		
		/* no standard point: */
		//this.mOsmv.getController().setCenter(new GeoPoint(49456770,11082540));
		gps.mOsmv = this.mOsmv;
	}

	@Override
	public void onLocationLost() {
		// We'll do nothing here.
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu pMenu) {
		pMenu.add(0, MENU_ZOOMIN_ID, Menu.NONE, "ZoomIn");
		pMenu.add(0, MENU_ZOOMOUT_ID, Menu.NONE, "ZoomOut");

		/*final SubMenu subMenu = pMenu.addSubMenu(0, MENU_TILE_SOURCE_ID, Menu.NONE,
				"Choose Tile Source");
		{
			for (final ITileSource tileSource : TileSourceFactory.getTileSources()) {
				subMenu.add(0, 1000 + tileSource.ordinal(), Menu.NONE,
						tileSource.localizedName(mResourceProxy));
			}
		} */

		pMenu.add(0, MENU_MINIMAP_ID, Menu.NONE, "Minimap");
		pMenu.add(0, MENU_HYDRANT_ID, Menu.NONE, "Hydrant hinzu");
		pMenu.add(0, MENU_SUBMIT_ID, Menu.NONE, "Hochladen!");
		
		return true;
	}

	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ZOOMIN_ID:
			this.mOsmvController.zoomIn();
			return true;

		case MENU_ZOOMOUT_ID:
			this.mOsmvController.zoomOut();
			return true;

		case MENU_TILE_SOURCE_ID:
			this.mOsmv.invalidate();
			return true;

		case MENU_MINIMAP_ID:
			mMiniMapOverlay.setEnabled(!mMiniMapOverlay.isEnabled());
			this.mOsmv.invalidate();
			return true;

		case MENU_HYDRANT_ID:
			/* Textbox mit Abfrage:*/
			//mMiniMapOverlay.setEnabled(!mMiniMapOverlay.isEnabled());
			//this.mOsmv.invalidate();
			showHydrantMenu();
			return true;

		case MENU_SUBMIT_ID:
			hochladen();
			return true;

		case MENU_ANIMATION_ID:
			this.mOsmv.getController().animateTo(52370816, 9735936,
					MapController.AnimationType.MIDDLEPEAKSPEED,
					MapController.ANIMATION_SMOOTHNESS_HIGH,
					MapController.ANIMATION_DURATION_DEFAULT); // Hannover
			return true;

		default:
			ITileSource tileSource = TileSourceFactory.getTileSource(item.getItemId() - 1000);
			mOsmv.setTileSource(tileSource);
			mMiniMapOverlay.setTileSource(tileSource);
		}
		return false;
	}
	
	public void hochladen() {
		int i=0;
		Toast.makeText(
				SampleExtensive.this,
				"Hydranten werden hochgeladen. Bitte warten ...", Toast.LENGTH_LONG).show();
		try {
		    InputStream instream = openFileInput("hydranten.txt");
		    if (instream.available()>0) {
		      InputStreamReader inputreader = new InputStreamReader(instream);
		      BufferedReader buffreader = new BufferedReader(inputreader);
		      String line;
		      
		      while (( line = buffreader.readLine()) != null) {
		    	  HTTP_Update update = new HTTP_Update();
		    	  update.pushString(line);
		    	  i++;
		      }		 
		    }
		    instream.close();
		  } catch (java.io.IOException e) {
			  e.printStackTrace();
		  }
		Toast.makeText(
			SampleExtensive.this,
			""+i+" Hydranten hochgeladen", Toast.LENGTH_LONG).show();
	}
	
	public void showHydrantMenu() {
		
		setContentView(R.layout.main);
		String[] items = new String[] {"OberFlur", "UnterFlur"};
		Spinner spinner = (Spinner) findViewById(R.id.hydart);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
		            android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		items = new String[] {"80", "100", "200"};
		Spinner spinner2 = (Spinner) findViewById(R.id.hyddurch);
		ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,
		            android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner2.setAdapter(adapter2);
   
	}
	public void onCancelHydrant (final View SaveHydrantView) {
		 setContentView(RelativeLayout_View);
	}
	
	public void onSaveHydrant(final View SaveHydrantView) {
		 final Spinner spinner = (Spinner) findViewById(R.id.hydart);
		 final int pos = spinner.getSelectedItemPosition();
		 final Spinner spinner2 = (Spinner) findViewById(R.id.hyddurch);
		 final int pos2 = spinner2.getSelectedItemPosition();
		 final EditText strNr = (EditText) findViewById(R.id.edtStreet);
		 final EditText hyddurch = (EditText) findViewById(R.id.edthyddurch);
		 
		 final StringBuilder sb = new StringBuilder();
		    sb.append("when="+DateUtils.formatDateTime(this,System.currentTimeMillis(),
		        DateUtils.FORMAT_SHOW_DATE));
		    sb.append("&lat="+this.gps.lat);
		    sb.append("&lon="+this.gps.lon);
		    sb.append("&Art="+spinner.getItemAtPosition(pos));
		    sb.append("&Durch="+spinner2.getItemAtPosition(pos2));
		    sb.append("&StrNr="+strNr.getText().toString().replaceAll(" ","%20"));
		    sb.append("&ManDurch="+hyddurch.getText());
		    sb.append("\n");
		 try {
	        saveEntry(sb.toString());
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    
	    //RelativeLayout_View.removeView(SaveHydrantView);
	    //RelativeLayout_View.getChildAt(0).bringToFront();
	    //RelativeLayout_View.bringToFront();
	    setContentView(RelativeLayout_View);
	}

	private void saveEntry(final String entry) throws IOException {
	    OutputStreamWriter osw = null;
	    try {
	        final FileOutputStream fos = openFileOutput("hydranten.txt", MODE_PRIVATE );// | MODE_APPEND);
	        osw = new OutputStreamWriter(fos);
	        osw.write(entry);        
	    } catch (Exception e) { }        
	    finally {
	        osw.close();
	    } 
	}
	// ===========================================================
	// Methods
	// ===========================================================

	@Override
	public void onLocationChanged(Location pLoc) {
		// TODO: set marker on map
		// TODO: Calc distance
		//Log.v("GPS_Location2","Update Map, Location Changed: lat:"+pLoc.getLatitude()+", lon:"+pLoc.getLongitude());
		/* Problem: wird gps benötigt, oder reicht diese Callback aus; ja weil mapview fehlt*/
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

    private class GPSLocation implements LocationListener {

    	static final String tag = "GPS_Location"; // for Log
    	public String status = "";
    	private Context context;
    	LocationManager lm;
    	StringBuilder sb;
    	int noOfFixes = 0;
    	private HTTP_Update httpUpdate = null;
    	String nmea = "";
    	double lat=0.0;
    	double lon=0.0;
    	private MapView mOsmv;
    	private GeoPoint[] HydrantenGPs = null;
    	
    	GpsStatus.NmeaListener nl = new GpsStatus.NmeaListener() {
    			long prevTime = (long) (System.currentTimeMillis());
    			int updateIntervalSec = 4;
    		   public void onNmeaReceived(long timestamp, String _nmea) {
    			   if (!_nmea.startsWith("$GPRMC"))
    				   return;
    			   // last char is not printable 
    			   nmea = _nmea.substring(0, _nmea.length()-2);    			 
    	    		//httpUpdate.pushString("lat:"+location.getLatitude()+";long:"+location.getLongitude());
    			   if ((long)System.currentTimeMillis()-prevTime > updateIntervalSec * 1000) {
    				   //Log.v(tag, "Zeit: "+(long)System.currentTimeMillis()+" == "+prevTime);
    				   prevTime = (long)System.currentTimeMillis();
    				   //httpUpdate.pushString(nmea);
    				   // $GPRMC,HHMMSS,A,BBBB.BBBB,b,LLLLL.LLLL,l,GG.G,RR.R,DDMMYY,M.M,m,F*PP
    				   //  0       1    2  3        4    5
    				   //Log.v(tag, "lat:"+nmea.split(",")[3]+", lon:"+nmea.split(",")[5]);
    				   if (nmea.split(",").length <  5 || 
    						   (nmea.split(",")[3].length() < 2 || nmea.split(",")[5].length() < 2)
    					)
    					   return;

    				   lat = Double.parseDouble(nmea.split(",")[3]);
    				   lon = Double.parseDouble(nmea.split(",")[5]);
    				   lat /= 100;
    				   lon /= 100;
    				   Log.v(tag, "Location (update every: "+(updateIntervalSec * 1000)+")new nmea: "+_nmea+", lat:"+lat+", lon:"+lon);
    			   }
    		   }
    	};
	
    	// Called when the activity is first created. 
    	public GPSLocation() {
    		Log.v(tag, "Location new");
    		//this.context = context;
    		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
       		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5f, this);
//       		lm.addNmeaListener(nl);
       		Log.v(tag, "Location new: "+lm.toString());
       		if (LocationManager.GPS_PROVIDER != null && 
       				lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
       			this.lat = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude();
       			this.lon = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude();
       		}
    	}
    	
     	
    	public void onLocationChanged(Location location) {
    		Log.v(tag, "Location Changed lat:"+location.getLatitude()+", lon:"+ location.getLongitude());
    		lat = location.getLatitude();
    		lon = location.getLongitude();
    		Log.v(tag, "lat"+lat+", lon:"+lon);
    		GeoPoint here = new GeoPoint(lat,lon);
    		this.lat = lat;
    		this.lon = lon;
    		this.mOsmv.getController().setCenter(here);
    		/* TODO: */
    		//this._mOsmv ;
    		int minDistance = 99999;
    		int offset = 0;
    		for (int i=0;i<HydrantenGPs.length;i++) {
    			int m = here.distanceTo(HydrantenGPs[i]);
    			if (m < minDistance) { 
    				minDistance =  m ;
    				offset = i;
//    				Log.v(tag, "i"+i+", m:"+m);
    			}
    		}
    		
    		Log.v(tag, "nearest in "+minDistance+"m, at lat:"+HydrantenGPs[offset].getLatitudeE6()+", lon:"+ HydrantenGPs[offset].getLongitudeE6());
    		Toast.makeText(
					SampleExtensive.this,
					"Hydrant in "+minDistance+"m.", Toast.LENGTH_LONG).show();
    	}
    	
    	public void onProviderDisabled(String provider) {
    		Log.v(tag, "Disabled");
    		Intent intent = new Intent(
    				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    		startActivity(intent);
    	}

    	public void onProviderEnabled(String provider) {
    		Log.v(tag, "Enabled");
    		//Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();
    	}

    	
    	public void onStatusChanged(String provider, int status, Bundle extras) {
    		// This is called when the GPS status alters 
    		switch (status) {
    		case LocationProvider.OUT_OF_SERVICE:
    			Log.v(tag, "Status Changed: Out of Service");
    			this.status = "Kein GPS!";
    			break;
    		case LocationProvider.TEMPORARILY_UNAVAILABLE:
    			Log.v(tag, "Status Changed: Temporarily Unavailable");
    			this.status = "Z.Z. nicht verfügbar";
    			break;
    		case LocationProvider.AVAILABLE:
    			Log.v(tag, "Status Changed: Available");
    			this.status = "Verfügbar";
    			break;
    		}
    	}
    }
}
