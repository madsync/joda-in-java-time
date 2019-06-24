package com.madsync.time.format

import java.time.{ Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, ZoneId, ZoneOffset, ZonedDateTime }
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal

import com.typesafe.scalalogging.LazyLogging

import com.madsync.time.{ DateTime, DateTimeZone }
import scala.util.{ Failure, Success, Try }

class MultiFormatFormatter(formatters: Seq[DateTimeFormatterConverter]) extends LazyLogging {
  def parseDateTime(s: String): Try[DateTime] = {
    val successes: Seq[(DateTimeFormatterConverter, ZonedDateTime)] = formatters.flatMap { f =>
      val o = Try(f.parse(s))
      o.recover {
        case ex =>
          logger.trace(s"   Failed with ${f.pattern} on $s with message ${ex.getMessage}")
      }
      o.toOption.map { oo => f -> oo }
    }
    successes.headOption.map {
      case (f, dt) =>
        Success(DateTime(dt))
    }.getOrElse(Failure(new IllegalArgumentException(s"Failed to parse input $s as a date time")))
  }
}

trait DateTimeFormatterConverter {
  val zone: DateTimeZone
  val pattern: String
  lazy val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern) //.withZone(ZoneId.of(zone.getId))
  def parse(s: String): ZonedDateTime
  def format(in: ZonedDateTime): String = formatter.format(in)
}

case class InstantFormatterHelper(pattern: String, zone: DateTimeZone = DateTimeZone.UTC) extends DateTimeFormatterConverter {
  def parse(s: String): ZonedDateTime = Instant.parse(s) atZone ZoneId.of(zone.getId)
}
object InstantFormatterHelper {
  def apply(s: String) = new InstantFormatterHelper(s)
}

case class LocalDateFormatterHelper(pattern: String) extends DateTimeFormatterConverter {
  lazy val zone: DateTimeZone = DateTimeZone.UTC
  def parse(s: String): ZonedDateTime = LocalDate.parse(s, formatter) atStartOfDay ZoneOffset.UTC
}

case class ZonedDateTimeFormatterHelper(pattern: String, zone: DateTimeZone = DateTimeZone.getDefault) extends DateTimeFormatterConverter {
  def parse(s: String): ZonedDateTime = {
    val r = ZonedDateTime.parse(s, formatter)
    r
  }
}
object ZonedDateTimeFormatterHelper {
  def apply(s: String) = new ZonedDateTimeFormatterHelper(s)
}

case class OffsetDateTimeFormatterHelper(pattern: String, zone: DateTimeZone = DateTimeZone.getDefault) extends DateTimeFormatterConverter {
  def parse(s: String): ZonedDateTime = OffsetDateTime.parse(s, formatter).toZonedDateTime
}
object OffsetDateTimeFormatterHelper {
  def apply(s: String) = new OffsetDateTimeFormatterHelper(s)
}

case class JodaDateTimeFormatterHelper(pattern: String, zone: DateTimeZone = DateTimeZone.getDefault) extends DateTimeFormatterConverter {
  val jFormatter = org.joda.time.format.DateTimeFormat.forPattern(pattern)
  def parse(s: String): ZonedDateTime = {
    val jDt = jFormatter.withOffsetParsed().parseDateTime(s)
    ZonedDateTime.ofInstant(Instant.ofEpochMilli(jDt.getMillis), ZoneId.of(jDt.getZone.getID)).withZoneSameInstant(ZoneId.of(jDt.getZone.getID))
  }
}
object JodaDateTimeFormatterHelper {
  def apply(s: String) = new JodaDateTimeFormatterHelper(s)
}

case class LocalDateTimeFormatterHelper(pattern: String) extends DateTimeFormatterConverter {
  lazy val zone: DateTimeZone = DateTimeZone.UTC
  def parse(s: String): ZonedDateTime = LocalDateTime.parse(s, formatter) atZone ZoneOffset.UTC
}

case class CombinedDateTimeFormatterHelper(candidates: (String => DateTimeFormatterConverter)*)(val pattern: String, val zone: DateTimeZone = DateTimeZone.getDefault) extends DateTimeFormatterConverter {

  val builtCandidates = candidates.flatMap { c => Try(c(pattern)).toOption }
  override def parse(s: String): ZonedDateTime = {
    val outcomes: Seq[Try[ZonedDateTime]] = builtCandidates.map { c => Try(c.parse(s)) }
    outcomes.dropWhile(_.isFailure).headOption.map { o: Try[ZonedDateTime] =>
      o.get
    }.getOrElse(throw outcomes.last.failed.get)
  }
}
