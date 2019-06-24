package com.madsync.time

import java.time.{ Duration => JDuration, Period => JPeriod }

import org.scalatest.{ FunSuite, Matchers }

class PeriodTest extends FunSuite with Matchers {

  test("test java period and duration for sanity checks") {
    var p = JPeriod.ofYears(1)
    p.getDays should be(0)
    p.getMonths should be(0)
    p.getYears should be(1)

    p.toTotalMonths should be(12)
    p = JPeriod.of(1, 2, 1)
    p.getDays should be(1)
    p.getMonths should be(2)
    p.getYears should be(1)
    p.toTotalMonths should be(14)

    val d = JDuration.parse("PT3H4M7S").withNanos(314567352)
    println(d)
    d.getSeconds should be(3 * 3600 + 4 * 60 + 7)
    d.getNano should be(314567352L)

    d.toNanos should be(1000000000L * (3 * 3600 + 4 * 60 + 7) + 314567352L)
  }

  test("test period to string") {
    Period(86400000).toString should be("P1D")
    Period(1000).toString should be("PT1S")

    val p = new Period(JPeriod.ofDays(2), JDuration.ofMinutes(1820))
    p.toString should be("P3DT6H20M")

    Period.parse(p.toString) should be(p.rebalanced)
  }

  test("test period parses things correctly") {
    Period.parse("P1D").periodIsEmpty should be(false)
    Period.parse("P1D").durationIsEmpty should be(true)

    Period.parse("PT1S").periodIsEmpty should be(true)
    Period.parse("PT1S").durationIsEmpty should be(false)

    Period.parse("P1D") should be(Period(86400000))
    Period.parse("PT1S") should be(Period(1000))

    Period("P1D") should be(Period(86400000))
    Period("PT1S") should be(Period(1000))
  }

  test("test period rebalancing works as expected") {
    var p = new Period(JPeriod.ofDays(2), JDuration.ofMinutes(1820))
    p.rebalanced should be(new Period(JPeriod.ofDays(3), JDuration.ofMinutes(380)))

    p.rebalanced.rebalanced should be(p.rebalanced)

    p = new Period(JPeriod.ofDays(0), JDuration.ofMinutes(1820))
    p.rebalanced should be(new Period(JPeriod.ofDays(1), JDuration.ofMinutes(380)))

    p = p * 3
    p should be(new Period(JPeriod.ofDays(3), JDuration.ofMinutes(1140)))

    p = p * 2
    p should be(new Period(JPeriod.ofDays(7), JDuration.ofMinutes(840)))

    p = p + 1841 * 60000 + 322
    p should be(new Period(JPeriod.ofDays(8), JDuration.ofMinutes(1241).plus(JDuration.ofMillis(322))))

    p = p - 162
    p should be(new Period(JPeriod.ofDays(8), JDuration.ofMinutes(1241).plus(JDuration.ofMillis(160))))

    p = p - 1440 * 60000
    p should be(new Period(JPeriod.ofDays(7), JDuration.ofMinutes(1241).plus(JDuration.ofMillis(160))))

    p = p - 86400000
    p should be(new Period(JPeriod.ofDays(6), JDuration.ofMinutes(1241).plus(JDuration.ofMillis(160))))

    p = p - 3 * 86400000
    p should be(new Period(JPeriod.ofDays(3), JDuration.ofMinutes(1241).plus(JDuration.ofMillis(160))))

    p = p - (2 * 86400000 + 400 * 60000 + 110)
    p should be(new Period(JPeriod.ofDays(1), JDuration.ofMinutes(841).plus(JDuration.ofMillis(50))))

    p = p - 2 * 86400000
    p should be(new Period(JPeriod.ofDays(-1), JDuration.ofMinutes(841).plus(JDuration.ofMillis(50))))
  }

  test("test toDurationFrom is sensible") {
    val now = DateTime.now()
    var p = Period(JPeriod.ofDays(1), JDuration.ofMinutes(380))
    p.toDurationFrom(now) should be(Duration(now, now.plus(86400000L + 380 * 60000)))

    val d1 = DateTime(2016, 2, 15, 3, 0, 0, DateTimeZone.UTC)
    p = Period.months(1)
    p.toDurationFrom(d1) should be(Duration(d1, DateTime(2016, 3, 15, 3, 0, 0, DateTimeZone.UTC)))

    p = Period.millis(42)
    p.toDurationFrom(d1) should be(Duration(d1, DateTime(2016, 2, 15, 3, 0, 0, 42, DateTimeZone.UTC)))

    p = Period.nanos(42)
    p.toDurationFrom(d1) should be(Duration(d1, DateTime(2016, 2, 15, 3, 0, 0, 0, 42, DateTimeZone.UTC)))
  }

  test("test floor works for various dates") {
    val d1 = DateTime(2016, 2, 15, 3, 0, 0, DateTimeZone.UTC)
    val p = Period.months(1)

    p.floor(d1) should be(DateTime(2016, 2, 1, 0, 0, 0, DateTimeZone.UTC))

  }

  test("test floor for dates after the epoch") {
    val current = DateTime(1999, 3, 6, 11, 23, 46, 123, DateTimeZone.forOffsetHours(-5))
    Period.seconds(1).floor(current) should be(DateTime(1999, 3, 6, 11, 23, 46, 0, DateTimeZone.forOffsetHours(-5)))
    Period.minutes(15).floor(current) should be(DateTime(1999, 3, 6, 11, 15, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.minutes(1).floor(current) should be(DateTime(1999, 3, 6, 11, 23, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.hours(1).floor(current) should be(DateTime(1999, 3, 6, 11, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.days(1).floor(current) should be(DateTime(1999, 3, 6, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.weeks(1).floor(current) should be(DateTime(1999, 3, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.days(30).floor(current) should be(DateTime(1999, 2, 28, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.days(360).floor(current) should be(DateTime(1998, 8, 2, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.months(1).floor(current) should be(DateTime(1999, 3, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.years(1).floor(current) should be(DateTime(1999, 1, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))

    Period.parse("P5Y5M").floor(current) should be(DateTime(1997, 2, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))

    val leapYear = DateTime(2000, 3, 6, 11, 23, 46, 123, DateTimeZone.forOffsetHours(-5))
    Period.weeks(1).floor(leapYear) should be(DateTime(2000, 3, 6, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.days(30).floor(leapYear) should be(DateTime(2000, 2, 23, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.days(360).floor(leapYear) should be(DateTime(1999, 7, 28, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.months(1).floor(leapYear) should be(DateTime(2000, 3, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.years(1).floor(leapYear) should be(DateTime(2000, 1, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))

    Period.parse("P5Y5M").floor(leapYear) should be(DateTime(1997, 2, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
  }

  test("test floor for dates before the epoch") {
    val beforeEpoch = DateTime(1969, 3, 6, 11, 23, 46, 123, DateTimeZone.forOffsetHours(-5))
    Period.seconds(1).floor(beforeEpoch) should be(DateTime(1969, 3, 6, 11, 23, 46, 0, DateTimeZone.forOffsetHours(-5)))
    Period.minutes(15).floor(beforeEpoch) should be(DateTime(1969, 3, 6, 11, 15, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.minutes(1).floor(beforeEpoch) should be(DateTime(1969, 3, 6, 11, 23, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.hours(1).floor(beforeEpoch) should be(DateTime(1969, 3, 6, 11, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.days(1).floor(beforeEpoch) should be(DateTime(1969, 3, 6, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.weeks(1).floor(beforeEpoch) should be(DateTime(1969, 3, 3, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.days(30).floor(beforeEpoch) should be(
      DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)).minusDays(330))
    Period.days(360).floor(beforeEpoch) should be(
      DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)).minusDays(360))
    Period.months(1).floor(beforeEpoch) should be(DateTime(1969, 3, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    Period.years(1).floor(beforeEpoch) should be(DateTime(1969, 1, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))

  }

  test("test floor for dates before the epoch with a complex period") {
    val beforeEpoch = DateTime(1969, 3, 6, 11, 23, 46, 123, DateTimeZone.forOffsetHours(-5))
    val period = Period.parse("P5Y5M")

    val floor = period.floor(beforeEpoch)

    //5 years and 5 months
    floor should be(DateTime(1964, 8, 1, 0, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
  }

  test("maxTimeInMillis") {
    Period.years(1).toMaxDuration.getMillis should be(31622400000L)
    Period.days(360).toMaxDuration.getMillis should be(31104000000L)
    Period.months(1).toMaxDuration.getMillis should be(2678400000L)
    Period.days(30).toMaxDuration.getMillis should be(2592000000L)
    Period.weeks(1).toMaxDuration.getMillis should be(604800000L)
    Period.days(1).toMaxDuration.getMillis should be(86400000L)
    Period.hours(1).toMaxDuration.getMillis should be(3600000L)
    Period.minutes(15).toMaxDuration.getMillis should be(900000L)
    Period.minutes(1).toMaxDuration.getMillis should be(60000L)
    Period.seconds(1).toMaxDuration.getMillis should be(1000L)
  }

  test("expectedCount") {
    Period.years(1).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(366 * 24 * 3600)
    Period.years(1).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(365 * 24 * 3600)
    Period.days(360).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(360 * 24 * 3600)
    Period.days(360).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(360 * 24 * 3600)
    Period.months(1).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(29 * 24 * 3600)
    Period.months(1).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(28 * 24 * 3600)
    Period.days(30).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(30 * 24 * 3600)
    Period.days(30).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(30 * 24 * 3600)
    Period.weeks(1).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(7 * 24 * 3600)
    Period.weeks(1).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(7 * 24 * 3600)
    Period.days(1).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(24 * 3600)
    Period.days(1).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(24 * 3600)
    Period.hours(1).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(3600)
    Period.hours(1).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(3600)
    Period.minutes(15).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(15 * 60)
    Period.minutes(15).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(15 * 60)
    Period.minutes(1).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(60)
    Period.minutes(1).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(60)
    Period.seconds(1).expectedCount(1000L, DateTime(2016, 2, 1, 0, 0, 0)) should be(1)
    Period.seconds(1).expectedCount(1000L, DateTime(2015, 2, 1, 0, 0, 0)) should be(1)
  }

  test("test plus and minus period match joda expectations") {
    val dt = DateTime("1964-08-03T00:00:00.000000000-05:00")
    dt should be(DateTime(1964, 8, 3, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    val jdt = new org.joda.time.DateTime(1964, 8, 3, 0, 0, 0, org.joda.time.DateTimeZone.forOffsetHours(-5))

    val period = Period.parse("P5Y5M")
    val jPeriod = org.joda.time.Period.parse("P5Y5M")

    //sanity check
    jPeriod.toDurationFrom(jdt).getMillis should be(period.toDurationFrom(dt).getMillis)
    jPeriod.toDurationTo(jdt).getMillis should be(period.toDurationTo(dt).getMillis)

    jdt.minus(jPeriod) should be(new org.joda.time.DateTime(1959, 3, 3, 0, 0, 0, org.joda.time.DateTimeZone.forOffsetHours(-5)))

    dt.minus(period) should be(DateTime(1959, 3, 3, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))
    dt.plus(period) should be(DateTime(1970, 1, 3, 0, 0, 0, DateTimeZone.forOffsetHours(-5)))

  }

}
