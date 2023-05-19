package models

import org.scalacheck.Gen

case class GeoPosition(latitude: Double, longitude: Double)

case class Position(_type: String, _id: Long, key: String, name: String, fullName: String, iata_airport_code: String,
                    country: String, geo_position: GeoPosition, location_id: Long, inEurope: Boolean, countryCode: String,
                    coreCountry: Boolean, distance: Double)


object GeoPosition {
  val geoPositionGen: Gen[GeoPosition] = for {
    latitude <- Gen.choose(-90.0, 90.0)
    longitude <- Gen.choose(-180.0, 180.0)
  } yield GeoPosition (latitude, longitude)
}


object Position {
  val stringShorterThan: Int => Gen[String] = (n: Int) => Gen.alphaStr.map(str => if (str.length <= n) str else str.substring(0,n))

  val positionGen: Gen[Position] = for {
    _id <- Gen.long
    key <- Gen.uuid
    name <- stringShorterThan(20)
    iata_airport_code <- stringShorterThan(3)
    country  <- stringShorterThan(15)
    position <- GeoPosition.geoPositionGen
    location_id <- Gen.long
    inEurope <- Gen.oneOf(true, false)
    countryCode <- stringShorterThan(2)
    coreCountry <- Gen.oneOf(true, false)
    distance <- Gen.choose(0.0, 20000.0)
  } yield Position ("Position", math.abs(_id), key.toString, name, s"${name},${country}", iata_airport_code.toUpperCase,
    country, position, math.abs(location_id), inEurope, countryCode.toUpperCase, coreCountry, distance)

  val positionListGen: Int => Gen[List[Position]] = (n: Int) => Gen.listOfN(n, positionGen)
}