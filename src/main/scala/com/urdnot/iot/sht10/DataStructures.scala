package com.urdnot.iot.sht10

trait DataStructures {
  final case class ShtReading(
                               timestamp: Long,
                               dew_point: Double,
                               humidity: Double,
                               temperature_f: Double,
                               temperature_c: Double
                             )
  // {"temperature_f": 70.44, "timestamp": 1599280760273, "humidity": 67.25, "dew_point": 13.15, "temperature_c": 21.38}
  private def round(d: Double) = BigDecimal(d).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
  def celsiusToFahrenheit(d: Double): Double = round(d * 9 / 5 + 32)
  def fahrenheitToCelsius(d: Double): Double = round((d - 32) * 5 / 9)
}
