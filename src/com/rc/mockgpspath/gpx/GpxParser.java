package com.rc.mockgpspath.gpx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.text.format.Time;
import android.util.Log;
import android.util.Xml;

public class GpxParser {
	// We don't use namespaces
	private static final String ns = null;

	public List<ExtendedGeoPoint> parse(InputStream in)
			throws XmlPullParserException, IOException {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			return readFeed(parser);
		} finally {
			in.close();
		}
	}

	private List<ExtendedGeoPoint> readFeed(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		List<ExtendedGeoPoint> entries = new ArrayList<ExtendedGeoPoint>();

		parser.require(XmlPullParser.START_TAG, ns, "gpx");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			// Starts by looking for the entry tag
			if (name.equals("trk")) {
				entries = readTrack(parser);
			} else {
				skip(parser);
			}
		}
		return entries;
	}

	private List<ExtendedGeoPoint> readTrack(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		List<ExtendedGeoPoint> result = null;
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("trkseg")) {
				result = readTrackSegment(parser);
			} else {
				skip(parser);
			}
		}
		return result;
	}

	private List<ExtendedGeoPoint> readTrackSegment(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		List<ExtendedGeoPoint> entries = new ArrayList<ExtendedGeoPoint>();
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("trkpt")) {
				ExtendedGeoPoint trackPoint = readTrackPoint(parser);
				entries.add(trackPoint);
				Log.d(getClass().getCanonicalName(), trackPoint.toString());
			} else {
				skip(parser);
			}
		}
		return entries;
	}

	/**
	 * Parses the contents of a trkpt.
	 * 
	 * @param parser
	 * @return
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private ExtendedGeoPoint readTrackPoint(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		float latitude = Float.valueOf(parser.getAttributeValue(null, "lat"));
		float longitude = Float.valueOf(parser.getAttributeValue(null, "lon"));
		int hr = 0;
		float elevation = 0;
		long timestamp = 0;
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("ele")) {
				elevation = Float.valueOf(readText(parser));
			} else if (name.equals("time")) {
				Time time = new Time();
				time.parse3339(readText(parser));
				timestamp = time.toMillis(false);
			} else if (name.equals("extensions")) {
				hr = readExtensions(parser);
			} else {
				skip(parser);
			}
		}
		int latitudeE6 = (int) (latitude * 1E6);
		int longitudeE6 = (int) (longitude * 1E6);
		return new ExtendedGeoPoint(latitudeE6, longitudeE6, hr, elevation,
				timestamp);
	}

	private int readExtensions(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		int hr = 0;
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("gpxtpx:TrackPointExtension")) {
				hr = readExtension(parser);
			} else {
				skip(parser);
			}
		}
		return hr;
	}

	private int readExtension(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		int hr = 0;
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("gpxtpx:hr")) {
				hr = Integer.valueOf(readText(parser));
			} else {
				skip(parser);
			}
		}
		return hr;
	}

	// For the tags title and summary, extracts their text values.
	private String readText(XmlPullParser parser) throws IOException,
			XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	private void skip(XmlPullParser parser) throws XmlPullParserException,
			IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

}
