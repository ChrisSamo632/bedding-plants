package uk.co.gmescouts.stmarys.beddingplants.geolocation.model;

import com.google.maps.StaticMapsRequest.ImageFormat;

public enum MapImageFormat {
	PNG(ImageFormat.png, "png"), PNG8(ImageFormat.png8, "png"), PNG32(ImageFormat.png32, "png"), GIF(ImageFormat.gif, "gif"), JPG(ImageFormat.jpg,
			"jpg"), JPG_BASELINE(ImageFormat.jpgBaseline, "jpg");

	private final ImageFormat googleStaticMapsImageFormat;

	private final String filenameExtension;

	MapImageFormat(final ImageFormat googleStaticMapsImageFormat, final String filenameExtension) {
		this.googleStaticMapsImageFormat = googleStaticMapsImageFormat;
		this.filenameExtension = filenameExtension;
	}

	public ImageFormat getGoogleStaticMapsImageFormat() {
		return googleStaticMapsImageFormat;
	}

	public String getFilenameExtension() {
		return filenameExtension;
	}
}
