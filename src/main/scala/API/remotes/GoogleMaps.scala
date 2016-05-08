package API.remotes

import com.google.maps.model.LatLng
import com.google.maps.{GeoApiContext, GeocodingApi}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object GoogleMaps {

  private val context = new GeoApiContext().setApiKey(sys.env("GOOGLE_API_KEY"))

  def geocode(address: String): Option[LatLng] = {
    try {
      val req = GeocodingApi.geocode(context, address)
      req.await() match {
        // Only considered valid if GM returns only a single location
          // Also ignore all the idiots who say they live at the poles
        case Array(result) if Math.abs(result.geometry.location.lat) < 70 => Some(result.geometry.location)
        case _ => None
      }
    }
    catch {
      case e: Throwable => None
    }
  }
}
