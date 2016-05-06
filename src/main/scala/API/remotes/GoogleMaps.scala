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
      req.await().headOption.map(_.geometry.location)
    }
    catch {
      case e: Throwable => None
    }
  }
}
