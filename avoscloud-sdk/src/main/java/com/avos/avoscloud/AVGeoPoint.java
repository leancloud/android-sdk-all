package com.avos.avoscloud;

import android.location.Location;

/**
 * <p>
 * AVGeoPoint represents a latitude / longitude point that may be associated with a key in a
 * AVObject or used as a reference point for geo queries. This allows proximity based queries on the
 * key.
 * </p>
 * <p>
 * Only one key in a class may contain a GeoPoint.
 * </p>
 * Example:
 * 
 * <pre>
 * AVGeoPoint point = new AVGeoPoint(30.0, -20.0);
 * AVObject object = new AVObject(&quot;PlaceObject&quot;);
 * object.put(&quot;location&quot;, point);
 * object.save();
 * </pre>
 */
public class AVGeoPoint {
  static double EARTH_MEAN_RADIUS_KM = 6378.140;
  static double ONE_KM_TO_MILES = 1.609344;
  private double latitude;
  private double longitude;

  /**
   * Creates a new default point with latitude and longitude set to 0.0.
   */
  public AVGeoPoint() {
    latitude = 0.0;
    longitude = 0.0;
  }

  /**
   * Get distance between this point and another geopoint in kilometers.
   * 
   * @param point GeoPoint describing the other point being measured against.
   */
  public double distanceInKilometersTo(AVGeoPoint point) {
    Location start = new Location("");
    start.setLatitude(latitude);
    start.setLongitude(longitude);
    Location end = new Location("");
    end.setLatitude(point.latitude);
    end.setLongitude(point.longitude);
    return start.distanceTo(end) / 1000;
  }

  /**
   * Get distance between this point and another geopoint in kilometers.
   * 
   * @param point GeoPoint describing the other point being measured against.
   */
  public double distanceInMilesTo(AVGeoPoint point) {
    return this.distanceInKilometersTo(point) / ONE_KM_TO_MILES;
  }

  /**
   * Get distance in radians between this point and another GeoPoint. This is the smallest angular
   * distance between the two points.
   * 
   * @param point GeoPoint describing the other point being measured against.
   */
  public double distanceInRadiansTo(AVGeoPoint point) {
    return this.distanceInKilometersTo(point) / EARTH_MEAN_RADIUS_KM;
  }

  /**
   * Creates a new point with the specified latitude and longitude.
   * 
   * @param latitude The point's latitude.
   * @param longitude The point's longitude.
   */
  public AVGeoPoint(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  /**
   * Get latitude.
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * Set latitude. Valid range is (-90.0, 90.0). Extremes should not be used.
   * 
   * @param l The point's latitude.
   */
  public void setLatitude(double l) {
    latitude = l;
  }

  /**
   * Get longitude.
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * Set longitude. Valid range is (-180.0, 180.0). Extremes should not be used.
   * 
   * @param l The point's longitude.
   */
  public void setLongitude(double l) {
    longitude = l;
  }

}
