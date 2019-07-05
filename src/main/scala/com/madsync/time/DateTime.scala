package com.madsync.time

import java.time.format.{ DateTimeFormatter, DateTimeParseException }
import java.time.temporal.ChronoField
import java.time.temporal.ChronoField.{ INSTANT_SECONDS, NANO_OF_SECOND }
import java.time.{ LocalDate, LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime, Duration => JDuration, Period => JPeriod }
import java.util.Date

import com.madsync.time.format._

import scala.util.Success
import play.api.libs.json._

import scala.util.{ Failure, Try }

final case class DateTime(date: ZonedDateTime) {

  def compareTo(x: DateTime): Int = (getNanos - x.getNanos) match {
    case 0          => 0
    case x if x > 0 => 1
    case _          => -1
  }
  def withZoneRetainFields(zone: DateTimeZone): DateTime = copy(date = date.withZoneSameLocal(zone.jId(this)))
  def withZone(zone: DateTimeZone): DateTime = copy(date = date.withZoneSameInstant(zone.jId(this)))

  def withZoneDSTAware(dateTimeZone: DateTimeZone): DateTime = {
    dateTimeZone.getOffsetTime(this)
  }

  def withYear(value: Int): DateTime = copy(date = date.withYear(value))
  def withHourOfDay(value: Int): DateTime = copy(date = date.withHour(value))
  def withMonthOfYear(value: Int): DateTime = copy(date = date.withMonth(value))
  def withDayOfMonth(value: Int): DateTime = copy(date = date.withDayOfMonth(value))

  def getMillis: Long = {
    import java.time.temporal.ChronoField._
    1000 * date.getLong(INSTANT_SECONDS) + date.getLong(MILLI_OF_SECOND)
  }
  def getNanos: Long = 1000000000 * date.getLong(INSTANT_SECONDS) + date.getLong(NANO_OF_SECOND)
  def getZone: DateTimeZone = DateTimeZone(date.getZone)
  def minus(millis: Long): DateTime = copy(date = date.minus(JDuration.ofMillis(millis)))
  def minus(period: Period): DateTime = minus(period.toDurationTo(this).getMillis)
  def plus(millis: Long): DateTime = copy(date = date.plus(JDuration.ofMillis(millis)))
  def plus(period: Period): DateTime = plus(period.toDurationFrom(this).getMillis)

  def minusSeconds(c: Int): DateTime = minus(c * 1000L)
  def plusSeconds(c: Int): DateTime = plus(c * 1000L)

  def minusMinutes(c: Int) = minus(c * 60000L)
  def plusMinutes(c: Int): DateTime = plus(c * 60000L)

  def minusHours(c: Int): DateTime = minus(c * 3600000L)
  def plusHours(c: Int): DateTime = plus(c * 3600000L)

  def minusDays(c: Int): DateTime = minus(c * 86400000L)
  def plusDays(c: Int): DateTime = plus(c * 86400000L)

  def minusMonths(c: Int): DateTime = copy(date = date.minus(JPeriod.ofMonths(c)))
  def plusMonths(c: Int): DateTime = copy(date = date.plus(JPeriod.ofMonths(c)))

  def minusYears(c: Int): DateTime = copy(date = date.minus(JPeriod.ofYears(c)))
  def plusYears(c: Int): DateTime = copy(date = date.plus(JPeriod.ofYears(c)))

  def plusMillis(c: Long): DateTime = plus(c)
  def minusMillis(c: Long): DateTime = minus(c)

  def plusNanos(c: Long): DateTime = copy(date = date.plusNanos(c))
  def minusNanos(c: Long): DateTime = copy(date = date.minusNanos(c))

  def isBefore(other: DateTime): Boolean = getMillis < other.getMillis
  def isAfter(other: DateTime): Boolean = getMillis > other.getMillis

  def >(other: DateTime): Boolean = isAfter(other)
  def <(other: DateTime): Boolean = isBefore(other)
  def >=(other: DateTime): Boolean = !isBefore(other)
  def <=(other: DateTime): Boolean = !isAfter(other)

  def isBeforeNow: Boolean = isBefore(DateTime.now())

  def min(other: DateTime): DateTime = if (other.isAfter(this)) this else other
  def max(other: DateTime): DateTime = if (other.isBefore(this)) this else other

  def getDayOfWeek: Int = date.get(ChronoField.DAY_OF_WEEK)
  def getDayOfYear: Int = date.get(ChronoField.DAY_OF_YEAR)
  def getSecondOfMinute: Int = date.get(ChronoField.SECOND_OF_MINUTE)
  def getMillisOfSecond: Int = date.get(ChronoField.MILLI_OF_SECOND)
  def getMinuteOfHour: Int = date.get(ChronoField.MINUTE_OF_HOUR)
  def getHourOfDay: Int = date.get(ChronoField.HOUR_OF_DAY)
  def getDayOfMonth: Int = date.get(ChronoField.DAY_OF_MONTH)
  def getMonthOfYear: Int = date.get(ChronoField.MONTH_OF_YEAR)
  def getYear: Int = date.get(ChronoField.YEAR)

  def toDate: ZonedDateTime = date
  def toUtilDate: Date = {
    val r = new Date(getMillis)
    r
  }
  def toJodaDateTime: org.joda.time.DateTime = new org.joda.time.DateTime(getMillis, org.joda.time.DateTimeZone.forID(date.getZone.getId))

  def expressUTC(): DateTime = withZone(DateTimeZone.UTC)

  def trimMicros: DateTime = copy(date = {
    val millis = math.floor(date.getNano / 1000000D).toInt
    date.withNano(1000000 * millis)
  })

  def noMillis: DateTime = copy(date = date.withZoneSameInstant(ZoneOffset.UTC).withNano(0))
  def noSeconds: DateTime = copy(date = date.withZoneSameInstant(ZoneOffset.UTC).withSecond(0)).noMillis
  def noMinutes: DateTime = copy(date = date.withZoneSameInstant(ZoneOffset.UTC).withMinute(0)).noSeconds
  def noHours: DateTime = copy(date = date.withZoneSameInstant(ZoneOffset.UTC).withHour(0)).noMinutes
  def firstDayOfMonth: DateTime = copy(date = date.withZoneSameInstant(ZoneOffset.UTC).withDayOfMonth(1)).noHours
  def firstDayInWeek: DateTime = {
    //monday is 1, sunday is 7. should only subtract day of week if day is not 1
    val inModified = if (getDayOfWeek == 1) this else minusDays(getDayOfWeek - 1)
    inModified.noHours.withZone(DateTimeZone.UTC)
  }
  def noMonths: DateTime = copy(date = date.withZoneSameLocal(ZoneOffset.UTC).withMonth(1)).firstDayOfMonth

  override def toString: String = DateTimeParsing.dateToString(this)()

  override def equals(obj: Any): Boolean = super.equals(obj) || {
    obj match {
      case d: DateTime => d.getNanos == getNanos && DateTimeZone.standardizeId(d.date.getZone.getId) == DateTimeZone.standardizeId(date.getZone.getId)
      case _           => false
    }
  }

  override def hashCode(): Int = (getNanos ^ (getNanos >>> 32)).toInt + DateTimeZone(date.getZone).hashCode()
}
object DateTime extends DateTimeParsing {

  implicit def ordering: Ordering[DateTime] = (x: DateTime, y: DateTime) => x.compareTo(y)

  def apply(millis: Long, zone: DateTimeZone = DateTimeZone.getDefault): DateTime = {
    DateTime(ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneOffset.UTC)).withZone(zone)
  }
  def apply(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int): DateTime = apply(year, month, dayOfMonth, hour, minute, second, DateTimeZone.getDefault)
  def apply(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, zone: DateTimeZone): DateTime = apply(year, month, dayOfMonth, hour, minute, second, 0, zone)
  def apply(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, millis: Int): DateTime = apply(year, month, dayOfMonth, hour, minute, second, millis, DateTimeZone.getDefault)
  def apply(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, millis: Int, zone: DateTimeZone): DateTime = {
    apply(year, month, dayOfMonth, hour, minute, second, millis, 0, zone)
  }

  def apply(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, millis: Int, nanos: Int, zone: DateTimeZone): DateTime = {
    val local = LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, millis)
    DateTime(ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, millis * 1000000 + nanos, zone.jId(local)))
  }
  def apply(s: String): DateTime = parse(s)
  def apply(date: java.util.Date): DateTime = DateTime(ZonedDateTime.ofInstant(date.toInstant, ZoneId.systemDefault()))

  def now(): DateTime = nowWithNanos.trimMicros
  def nowWithNanos(): DateTime = DateTime(ZonedDateTime.now()).withZone(DateTimeZone.getDefault)
  def parse(s: String): DateTime = DateTimeParsing.stringValueToDateTime(s).get
  def parse(s: String, f: DateTimeFormatter) = {
    DateTime(Try(ZonedDateTime.parse(s, f)).recoverWith {
      case ex: DateTimeParseException if ex.getMessage.contains("Unable to obtain ZonedDateTime from TemporalAccessor") =>
        Try(LocalDateTime.parse(s, f).atZone(ZoneId.of(DateTimeZone.getDefault.getId))).recoverWith {
          case ex: DateTimeParseException =>
            Try(LocalDate.parse(s, f).atTime(0, 0).atZone(ZoneId.of(DateTimeZone.getDefault.getId))).recoverWith {
              case ex =>
                Failure(ex)
            }
          case ex: Throwable =>
            Failure(ex)
        }
      case ex: Throwable =>
        Failure(ex)
    }.get)
  }

}

object DateTimeParsing extends DateTimeParsing
trait DateTimeParsing {

  lazy val outputFormatter = ZonedDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnXXX")
  lazy val parsers: Array[DateTimeFormatterConverter] = Array[DateTimeFormatterConverter](
    JodaDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"),
    ZonedDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ss.SSSX"),
    ZonedDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ssZ"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ssZZ"),
    ZonedDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ssX"),
    ZonedDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ssXXX"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mmZ"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mmZZ"),
    ZonedDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mmX"),
    ZonedDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mmXXX"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd"),
    JodaDateTimeFormatterHelper("yyyy-MM-ddZ"),
    JodaDateTimeFormatterHelper("yyyy-MM-ddZZ"),
    LocalDateFormatterHelper("MM/dd/yyyy"),
    JodaDateTimeFormatterHelper("MM-dd-yyyy HH:mm:ss.SSSZ"),
    JodaDateTimeFormatterHelper("MM-dd-yyyy HH:mm:ss.SSSZZ"),
    JodaDateTimeFormatterHelper("MM-dd-yyyy HH:mm:ssZ"),
    JodaDateTimeFormatterHelper("MM-dd-yyyy HH:mm:ssZZ"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd HH:mm:ss.SSSZ"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd HH:mm:ss.SSSZZ"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd HH:mm:ssZ"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd HH:mm:ssZZ"),
    JodaDateTimeFormatterHelper("yyyy-MM-dd HH:mm:ss.SSS"),
    ZonedDateTimeFormatterHelper("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX"),
    outputFormatter,
    LocalDateTimeFormatterHelper("yyyy-MM-dd HH:mm:ss.nnnnnnnnn"),
    ZonedDateTimeFormatterHelper("yyyy-MM-dd HH:mm:ss.nnnnnnnnnX"),
    ZonedDateTimeFormatterHelper("yyyy-MM-dd HH:mm:ss.nnnnnnnnnXXX"))

  def stringValueToDateTime(s: String): Try[DateTime] = {
    val formatter = new MultiFormatFormatter(parsers)
    s forall Character.isDigit match {
      case true  => Try(DateTime(s.toLong))
      case false => formatter.parseDateTime(s)
    }
  }

  def dateToString(d: DateTime)(df: DateTimeFormatterConverter = outputFormatter): String = df.format(d.date)

  //
  // DateTime format
  //////////////////////////////
  implicit val dtReads: Reads[DateTime] = {
    case JsNumber(num) if num.isValidLong => JsSuccess(DateTime(num.toLong))
    case json @ JsString(str) => Option(str) match {
      case Some(s) => stringValueToDateTime(s) match {
        case Success(dt)  => JsSuccess(dt)
        case Failure(msg) => JsError(JsonValidationError(Seq(msg.toString)))
      }
      case None => JsError(s"Invalid value given for dateTime : '${json.toString()}'")
    }
    case _ => JsError("formatted DateTime value expected")
  }

  implicit val dtWrites: Writes[DateTime] = (d: com.madsync.time.DateTime) => JsString(dateToString(d)())

  implicit val dtFormat: Format[DateTime] = Format(dtReads, dtWrites)
}