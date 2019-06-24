package com.madsync.time

import java.time.{ Duration => JDuration, Period => JPeriod }

import play.api.libs.json.{ Format, JsError, JsString, JsSuccess, Reads, Writes }

import scala.math.BigDecimal.RoundingMode
import scala.util.Try

final case class Period(period: JPeriod, duration: JDuration = JDuration.ofNanos(0)) {
  def getMonths: Int = period.getMonths
  def getYears: Int = period.getYears
  def toDurationFrom(date: DateTime): Duration = Duration(
    date,
    date.plusYears(period.getYears).plusMonths(period.getMonths).plusDays(period.getDays).plusMillis(duration.getSeconds * 1000).plusNanos(duration.getNano))

  def toDurationTo(date: DateTime): Duration = Duration(
    date.minusYears(period.getYears).minusMonths(period.getMonths).minusDays(period.getDays).minusMillis(duration.getSeconds * 1000).minusNanos(duration.getNano),
    date)

  def toStandardDuration: Duration = {
    val p = rebalanced
    if (p.getMonths > 0 || p.getYears > 0)
      throw new Exception(s"No standard duration exists for $p")
    else
      toDurationFrom(DateTime(0))
  }

  ////// Extensions
  def floor(current: DateTime): DateTime = {
    if (getMonths > 0 || getYears > 0) {
      //expressed in irregular intervals, need special handling
      val start = DateTime(1970, 1, 1, 0, 0, 0, current.getZone)
      current.isBefore(start) match {
        case false =>
          val s = Stream.iterate((start, start.plus(this))) { case (d1, d2) => (d2, d2.plus(this)) }
          s.takeWhile {
            case (_, d2) =>
              d2.plus(this).isBefore(current)
          }.lastOption.map { _._2.plus(this) }.getOrElse(start)
        case true =>
          //it's before so at least one iteration is assured
          val earlyStart = start.minus(this)
          val s = Stream.iterate((earlyStart, earlyStart.minus(this))) {
            case (_, d2) =>
              (d2, d2.minus(this))
          }
          s.takeWhile {
            case (_, d2) =>
              d2.minus(this).isAfter(current)
          }.lastOption.map { _._2.minus(this).minus(this) }.getOrElse(earlyStart)
      }
    }
    else {
      //weeks start on monday
      val startingAt = DateTime(1970, 1, if (this == Period.weeks(1)) 5 else 1, 0, 0, 0).withZoneRetainFields(current.getZone)
      val incMillis: BigDecimal = toDurationFrom(current).getMillis
      val numerand: BigDecimal = Duration(startingAt, current).getMillis
      val ratio: BigDecimal = numerand / incMillis
      val mils = ratio.setScale(0, RoundingMode.FLOOR) * incMillis
      if (incMillis == 0) current else startingAt.plus(mils.longValue())
    }
  }

  def rebalanced: Period = {
    import Period._
    val totalNanosInDuration = duration.toNanos
    if (totalNanosInDuration < 0) {
      //need to remove days and redistributes remainder into duration
      val subtractedDays = daysFromNanos(-totalNanosInDuration) + 1
      val remainder = totalNanosInDuration % NANO_DAY
      val nanosLeftInDuration = NANO_DAY + remainder
      copy(period = period.minusDays(subtractedDays), duration = JDuration.ofNanos(nanosLeftInDuration))
    }
    else {
      val (days, r) = daysAndRemainderFromNanos(totalNanosInDuration)
      copy(period = period.plus(JPeriod.ofDays(days)), duration = duration.minus(JDuration.ofDays(days)))
    }
  }

  def multipliedBy(factor: Int): Period = {
    copy(period = period.multipliedBy(factor), duration = duration.multipliedBy(factor)).rebalanced
  }
  def *(factor: Int): Period = multipliedBy(factor)

  def plus(millis: Long): Period = copy(duration = duration.plus(JDuration.ofMillis(millis))).rebalanced
  def +(millis: Long): Period = plus(millis)

  def minus(millis: Long): Period = copy(duration = duration.minus(JDuration.ofMillis(millis))).rebalanced
  def -(millis: Long): Period = minus(millis)

  lazy val toMinDuration: Duration = {
    Try(toStandardDuration).getOrElse {
      //the duration can't be standardized
      toDurationFrom(DateTime(2001, 2, 1, 0, 0, 0)) //minimums: 365 days in a year, 28 days in a month
    }
  }

  lazy val toMaxDuration: Duration = {
    Try(toStandardDuration).getOrElse {
      //the duration can't be standardized
      toDurationFrom(DateTime(2000, 1, 1, 0, 0, 0)) //maximums: 366 days in a year, 31 days in a month
    }
  }

  def expectedCount(measurementInterval: Long, timestamp: DateTime): Int = {
    val millisInBucket: Long = toDurationFrom(timestamp).getMillis
    (BigDecimal(millisInBucket) / BigDecimal(measurementInterval)).toInt
  }

  def periodIsEmpty: Boolean = period.getDays == 0 && period.getMonths == 0 && period.getYears == 0
  def durationIsEmpty: Boolean = duration.toNanos == 0

  override def toString: String = {
    rebalanced match {
      case per @ Period(_, d) if per.periodIsEmpty   => d.toString
      case per @ Period(p, _) if per.durationIsEmpty => p.toString
      case Period(p, d)                              => s"${p}${d.toString.replace("P", "")}"
    }
  }
}

object Period extends PeriodParsing {
  private final val NANO_DAY = 86400 * 1000000000L

  private def daysFromNanos(nanos: Long): Int = (nanos / NANO_DAY).toInt
  private def daysAndRemainderFromNanos(nanos: Long): (Int, Long) = {
    val absNanos = math.abs(nanos)
    val sign = if (nanos > 0) 1 else -1

    val out = if (absNanos >= NANO_DAY) {
      val days = daysFromNanos(absNanos)
      val remainder = absNanos % NANO_DAY
      days.toInt -> remainder
    }
    else 0 -> absNanos

    out._1 * sign -> out._2 * sign
  }

  def apply(millis: Long): Period = {
    val (days, remainder) = daysAndRemainderFromNanos(millis * 1000000)
    new Period(JPeriod.ofDays(days), JDuration.ofNanos(remainder))
  }
  def apply(duration: JDuration): Period = new Period(JPeriod.ofDays(0), duration).rebalanced
  def apply(s: String): Period = Period.parse(s)

  def nanos(n: Long): Period = Period(JDuration.ofNanos(n))
  def millis(n: Long): Period = Period(JDuration.ofMillis(n))
  def seconds(n: Long): Period = Period(JDuration.ofSeconds(n))
  def minutes(n: Long): Period = Period(JDuration.ofMinutes(n))
  def hours(n: Long): Period = Period(JDuration.ofHours(n))
  def days(n: Int): Period = Period(JPeriod.ofDays(n))
  def weeks(n: Int): Period = Period(JPeriod.ofWeeks(n))
  def months(n: Int): Period = Period(JPeriod.ofMonths(n))
  def years(n: Int): Period = Period(JPeriod.ofYears(n))

  def parse(s: String): Period = {
    s.split("T").toList match {
      case "P" :: _ :: Nil => //this is a duration only
        Period(JDuration.parse(s))
      case _ :: Nil => //this is a period only
        Period(JPeriod.parse(s))
      case s :: t :: Nil =>
        Period(JPeriod.parse(s), JDuration.parse(s"PT$t"))
      case x =>
        throw new IllegalArgumentException(s"Cannot parse $s as a Period (parse result $x)")
    }
    //    if (s.toUpperCase().startsWith("PT")) Period(JDuration.parse(s)) else Period(JPeriod.parse(s))
  }
}

trait PeriodParsing {
  implicit val periodReads: Reads[Period] = {
    case json @ JsString(s) => Option(s) match {
      case Some(s) => JsSuccess(Period.parse(s))
      case None    => JsError(s"Invalid value given for IOS8601 period : '${json.toString()}'")
    }
    case _ => JsError("String value expected for IOS8601 period")
  }

  implicit val periodWrites: Writes[Period] = (p: Period) => JsString(p.toString)

  implicit val periodFormat: Format[Period] = Format(periodReads, periodWrites)

}
