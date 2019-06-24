package com.madsync.time

import java.time.{ Duration => JDuration }
import java.util.concurrent.TimeUnit

import play.api.libs.json.{ Format, JsError, JsNumber, JsSuccess, Reads, Writes }

import scala.concurrent.duration.FiniteDuration

final case class Duration(start: DateTime, end: DateTime) {

  def getMillis: Long = end.getMillis - start.getMillis

  override def toString: String = JDuration.between(start.date, end.date).toString
}

trait DurationParsing {
  implicit val _DurationReads: Reads[Duration] = {
    case json @ JsNumber(s) => Option(s) match {
      case Some(millis) => JsSuccess(FiniteDuration.apply(millis.toLong, TimeUnit.MILLISECONDS).asInstanceOf[Duration])
      case None         => JsError(s"Invalid value given for duration : '${json.toString()}'")
    }
    case _ => JsError("Long value expected")
  }
  implicit val _DurationsWrites: Writes[Duration] = (dur: Duration) => JsNumber(dur.getMillis)
  implicit val _durationFormat: Format[Duration] = Format(_DurationReads, _DurationsWrites)
}
