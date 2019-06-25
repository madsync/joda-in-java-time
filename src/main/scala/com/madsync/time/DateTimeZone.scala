package com.madsync.time

import java.time.{ LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime }

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{ Format, JsNumber, JsResult, JsString, JsSuccess, JsValue }

import scala.collection.immutable
import scala.util.Try

final class DateTimeZone private (val zone: Either[ZoneOffset, ZoneId]) extends LazyLogging {

  def jId(time: LocalDateTime): ZoneId = zone match {
    case Left(offset) => offset
    case Right(id)    => id
  }
  def jId(time: DateTime = DateTime.now()): ZoneId = jId(time.date.toLocalDateTime)
  def jOffset(time: LocalDateTime): ZoneOffset = zone match {
    case Left(offset) => offset
    case Right(id)    => id.getRules.getOffset(time)
  }
  def jOffset(time: DateTime = DateTime.now()): ZoneOffset = jOffset(time.date.toLocalDateTime)

  def getOffset(time: DateTime): Int = jOffset(time).getTotalSeconds * 1000

  def getOffsetTime(time: DateTime): DateTime = {
    val offset: Int = getOffset(time)
    time.withZone(DateTimeZone.forOffsetMillis(offset))
  }

  def getId: String = zone match {
    case Left(offset) => offset.getId
    case Right(id)    => id.getId
  }

  override def equals(obj: Any): Boolean = {
    obj.isInstanceOf[DateTimeZone] && {
      DateTimeZone.standardizeId(obj.asInstanceOf[DateTimeZone].getId) == DateTimeZone.standardizeId(getId)
    }
  }

  override def toString: String = zone match {
    case Left(o)   => o.getId
    case Right(id) => id.getId
  }

  //joda's algorithm re-weighted with 1E6 to account for the difference between nanos and millis
  override def hashCode(): Int = 1000000 * ("ISO".hashCode * 11 + 57 + DateTimeZone.standardizeId(getId).hashCode)

}
object DateTimeZone extends LazyLogging {
  implicit val ziFormat: Format[ZoneId] = new Format[ZoneId] {
    override def reads(json: JsValue): JsResult[ZoneId] = json match {
      case JsString(value) => JsSuccess(ZoneId.of(value))
      case a               => throw new IllegalArgumentException(s"Invalid zoneId $a")
    }

    override def writes(o: ZoneId): JsValue = JsString(o.getId)
  }
  implicit val oFormat: Format[DateTimeZone] = new Format[DateTimeZone] {
    override def reads(json: JsValue): JsResult[DateTimeZone] = json match {
      case JsString(value) => JsSuccess(DateTimeZone(ZoneId.of(value)))
      case JsNumber(value) => JsSuccess(DateTimeZone(ZoneOffset.ofTotalSeconds(value.toInt)))
      case a               => throw new IllegalArgumentException(s"Invalid zoneId $a")
    }

    override def writes(o: DateTimeZone): JsValue = o.zone match {
      case Left(o)  => JsNumber(o.getTotalSeconds)
      case Right(i) => JsString(i.getId)
    }
  }

  def apply(id: String): DateTimeZone = forID(id)
  def apply(offset: Int): DateTimeZone = forOffsetMillis(offset)

  def apply(zone: ZoneId): DateTimeZone = new DateTimeZone(Right(zone))
  def apply(zone: ZoneOffset): DateTimeZone = new DateTimeZone(Left(zone))

  final val UTC = new DateTimeZone(Left(ZoneOffset.UTC))

  def forID(id: String): DateTimeZone = apply(ZoneId.of(id))
  def forOffsetMillis(millis: Long): DateTimeZone = apply(ZoneOffset.ofTotalSeconds((millis / 1000).toInt))
  def forOffsetHours(hours: Int): DateTimeZone = {
    val hrs = if (hours > 18) hours - 24 else if (hours < -18) hours else hours
    forOffsetMillis(hrs * 3600000)
  }

  var default: Option[DateTimeZone] = None
  def setDefault(z: DateTimeZone): Unit = {
    default = Some(z)
  } //TODO: how do we do this?

  def getDefault: DateTimeZone = default.getOrElse {
    val prop = Option(System.getProperty("user.timezone"))
    prop.flatMap { id =>
      Try(DateTimeZone.forID(id)).toOption
    }.getOrElse(DateTimeZone(ZoneId.systemDefault()))
  }

  def standardizeId(id: String): String = DateTimeZone.translator.get(id) match {
    case Some(z) => z
    case None =>
      logger.warn(s"Missing equivalence class for time zone $id")
      id
  }

  lazy val groupedTimeZones: Seq[Seq[String]] = {
    import scala.collection.JavaConverters._
    val offsets = (ZoneOffset.MIN.getTotalSeconds to ZoneOffset.MAX.getTotalSeconds by 15 * 60).map { s => ZoneOffset.ofTotalSeconds(s).getId }
    val groupedTimezonesMap: Map[immutable.IndexedSeq[Long], Seq[(String, immutable.IndexedSeq[Long])]] = idsToMap(ZoneId.getAvailableZoneIds.asScala.toVector)
    val groupedOffsetsMap: Map[immutable.IndexedSeq[Long], Seq[(String, immutable.IndexedSeq[Long])]] = idsToMap(offsets)

    val groupedMap = groupedTimezonesMap.map {
      case (k, ids) =>
        groupedOffsetsMap.get(k).map { offsets =>
          k -> (ids ++ offsets)
        }.getOrElse {
          logger.trace(s"No equivalent offset found for ${ids}")
          k -> ids
        }
    }
    val groupedTimezones: Seq[Seq[String]] = groupedMap.values.toVector.map { _.map { case (id, _) => id }.sorted }
    groupedTimezones
  }

  lazy val translator: Map[String, String] = groupedTimeZones.foldLeft(Map[String, String]()) { (acc, cur) =>
    acc ++ cur.map { v => v -> cur.head }
  }

  private def idsToMap(ids: Seq[String]): Map[immutable.IndexedSeq[Long], Seq[(String, immutable.IndexedSeq[Long])]] = {
    val startDate = LocalDateTime.of(2000, 1, 1, 0, 0, 0)
    ids.map { id =>
      id -> (0 until 1826).map { d =>
        ZonedDateTime.of(startDate.plusDays(d), ZoneId.of(id)).toEpochSecond
      }
    }.groupBy { case (_, timestamps) => timestamps }
  }

}
