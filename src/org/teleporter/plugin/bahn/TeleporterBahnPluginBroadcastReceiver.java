package org.teleporter.plugin.bahn;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.regex.MatchResult;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.teleporter.adt.IntentType;
import org.teleporter.adt.RideType;
import org.teleporter.plugin.TeleporterBasePluginBroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;


/**
 * @author Nicolas Gramlich
 * @since 10:03:16 - 28.05.2010
 */
public class TeleporterBahnPluginBroadcastReceiver extends TeleporterBasePluginBroadcastReceiver {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final SimpleDateFormat DATEFORMAT_ddMMyy = new SimpleDateFormat("ddMMyy");
	private static final SimpleDateFormat DATEFORMAT_HHmm = new SimpleDateFormat("HHmm");
	
	private static final String DEBUGTAG = TeleporterBahnPluginBroadcastReceiver.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onHandleTeleporterRequest(final Context pContext, final Intent pIntent) {
		final long searchTime = this.getSearchTime();
		final String startAddress = this.getStartAddress();
		final String destinationAddress = this.getDestinationAddress();
		
		final Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.scheme("http");
		uriBuilder.authority("mobile.bahn.de");
		uriBuilder.path("/bin/mobil/query.exe/dox");

		uriBuilder.appendQueryParameter("n", "1"); // ???
		
		uriBuilder.appendQueryParameter("f", "2").appendQueryParameter("s", startAddress);
//		case STATION:
//			uriBuilder.appendQueryParameter("f", "1").appendQueryParameter("s", pStart.getName() + ", " + pStart.getAddress());
		
		uriBuilder.appendQueryParameter("o", "2").appendQueryParameter("z", destinationAddress);
//		case STATION:
//			uriBuilder.appendQueryParameter("o", "1").appendQueryParameter("z", pDestination.getName() + ", " + pDestination.getAddress());
		
		uriBuilder.appendQueryParameter("d", DATEFORMAT_ddMMyy.format(searchTime));
		uriBuilder.appendQueryParameter("t", DATEFORMAT_HHmm.format(searchTime));
		uriBuilder.appendQueryParameter("start", "Suchen");
		final String uri = uriBuilder.build().toString();
		Log.d(DEBUGTAG, "url: " + uri);

		try {
			final AbstractHttpClient httpClient = new DefaultHttpClient();
			final InputStream content = httpClient.execute(new HttpGet(uri)).getEntity().getContent();

			final Scanner scanner = new Scanner(content, "iso-8859-1");

			while(scanner.findWithinHorizon("<a href=\"([^\"]*)\">(\\d\\d):(\\d\\d)<br />(\\d\\d):(\\d\\d)", 10000) != null) {
				final MatchResult m = scanner.match();
				Log.d(DEBUGTAG, "Found match: " + m);
				final GregorianCalendar departure = parseDate(m.group(2), m.group(3));
				final GregorianCalendar arrival = parseDate(m.group(4), m.group(5));
				final int price = 240; // TODO Actual price

				if(departure.getTimeInMillis() - searchTime > 100000) { // TODO Why 100000 ? What does it represent?
					String uriString = m.group(1).replace("&amp;", "&");
					if(!uriString.startsWith("http:")){
						uriString = "http:" + uriString;
					}
					final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
					
					sendTimedRideTeleporterResponse(RideType.PUBLICTRANSIT, price, intent, IntentType.STARTACTIVITY, departure.getTimeInMillis(), arrival.getTimeInMillis());
				}
			}
		} catch (final Exception e) {
			Log.e(DEBUGTAG, "Mist!", e);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private static GregorianCalendar parseDate(final String pHours, final String pMinutes) {
		final GregorianCalendar out = new GregorianCalendar();
		out.set(Calendar.HOUR_OF_DAY, Integer.parseInt(pHours));
		out.set(Calendar.MINUTE, Integer.parseInt(pMinutes));
		out.set(Calendar.SECOND, 0);
		out.set(Calendar.MILLISECOND, (out.get(Calendar.MILLISECOND) / MILLISECONDSPERSECOND) * MILLISECONDSPERSECOND);

		// TODO Write tests
		// TODO Why 100000 ? What does it represent? --> Use TimeConstants
		if(System.currentTimeMillis() - out.getTimeInMillis() > 36000 * MILLISECONDSPERSECOND) { // Midnight ???
			out.add(Calendar.DAY_OF_MONTH, 1);
		}

		return out;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
