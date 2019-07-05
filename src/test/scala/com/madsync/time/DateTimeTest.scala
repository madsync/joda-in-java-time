package com.madsync.time

import java.time.format.DateTimeFormatter
import java.time.{ LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime }

import com.madsync.time.format.{ CombinedDateTimeFormatterHelper, JodaDateTimeFormatterHelper, LocalDateTimeFormatterHelper, ZonedDateTimeFormatterHelper }
import org.scalatest.{ FunSuite, Matchers }
import play.api.libs.json.{ JsError, JsNumber, JsObject, JsPath, JsString }

class DateTimeTest extends FunSuite with Matchers {

  //wipe out defaults because group runs set it to UTC
  DateTimeZone.default = None

  test("test clearing methods remove the right fields") {

    val dt = DateTime(2019, 1, 2, 3, 4, 5, 123, DateTimeZone.forOffsetMillis(-6000 * 3600))
    dt.noMillis should be(DateTime(2019, 1, 2, 9, 4, 5, 0, DateTimeZone.UTC))
    dt.noSeconds should be(DateTime(2019, 1, 2, 9, 4, 0, 0, DateTimeZone.UTC))
    dt.noMinutes should be(DateTime(2019, 1, 2, 9, 0, 0, 0, DateTimeZone.UTC))
    dt.noHours should be(DateTime(2019, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC))
    dt.firstDayOfMonth should be(DateTime(2019, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC))
    dt.firstDayInWeek should be(DateTime(2018, 12, 31, 0, 0, 0, 0, DateTimeZone.UTC))
    dt.noMonths should be(DateTime(2019, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC))

    val dtNanos = DateTime(2019, 1, 2, 3, 4, 5, 123, 456789, DateTimeZone.forOffsetMillis(-6000 * 3600))
    dtNanos.trimMicros should be(dt)
  }

  test("test adding and subtracting methods") {
    val dt = DateTime(2019, 1, 2, 3, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600))

    dt.plusYears(2) should be(DateTime(2021, 1, 2, 3, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))
    dt.minusYears(4) should be(DateTime(2015, 1, 2, 3, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))

    dt.plusMonths(2) should be(DateTime(2019, 3, 2, 3, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))
    dt.minusMonths(4) should be(DateTime(2018, 9, 2, 3, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))

    dt.plusDays(2) should be(DateTime(2019, 1, 4, 3, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))
    dt.minusDays(4) should be(DateTime(2018, 12, 29, 3, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))

    dt.plusHours(27) should be(DateTime(2019, 1, 3, 6, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))
    dt.minusHours(27) should be(DateTime(2019, 1, 1, 0, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))

    dt.plusMinutes(27) should be(DateTime(2019, 1, 2, 3, 31, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))
    dt.minusMinutes(27) should be(DateTime(2019, 1, 2, 2, 37, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))

    dt.plusSeconds(27) should be(DateTime(2019, 1, 2, 3, 4, 32, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))
    dt.minusSeconds(27) should be(DateTime(2019, 1, 2, 3, 3, 38, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))

    dt.plusDays(80000) should be(DateTime(2238, 1, 14, 3, 4, 5, 123, DateTimeZone.forOffsetMillis(-4000 * 3600)))
  }

  test("test consistency of apply with joda API constructors") {
    import org.joda.time.{ DateTime => JDateTime, DateTimeZone => JDateTimeZone }

    DateTimeZone.getDefault.getId should be(JDateTimeZone.getDefault.getID)
    //default time zone no millis
    var sd = DateTime(2012, 12, 8, 0, 0, 0)
    var sdj = new JDateTime(2012, 12, 8, 0, 0, 0)

    println(sd)
    println(sdj)
    sd.getMillis should be(sdj.getMillis)
    println("Madsync zone was " + sd.getZone)
    println("Joda zone was " + sdj.getZone)
    sd.getZone.getOffset(DateTime.now()) should be(sdj.getZone.getOffset(JDateTime.now()))

    //just millis
    sd = DateTime(2012)
    sdj = new JDateTime(2012)
    println("Madsync DT was " + sd)
    println("Joda DT was " + sdj)
    sd.getMillis should be(sdj.getMillis)
    println("Madsync zone was " + sd.getZone)
    println("Joda zone was " + sdj.getZone)
    sd.getZone.getOffset(DateTime.now()) should be(sdj.getZone.getOffset(JDateTime.now()))

    //with timezone as ID no millis
    sd = DateTime(2012, 12, 8, 0, 0, 0, DateTimeZone.forID("America/Chicago"))
    sdj = new JDateTime(2012, 12, 8, 0, 0, 0, JDateTimeZone.forID("America/Chicago"))
    println("Madsync DT was " + sd)
    println("Joda DT was " + sdj)
    sd.getMillis should be(sdj.getMillis)
    println("Madsync zone was " + sd.getZone)
    println("Joda zone was " + sdj.getZone)
    sd.getZone.getOffset(DateTime.now()) should be(sdj.getZone.getOffset(JDateTime.now()))

    //with timezone as offset no millis
    sd = DateTime(2012, 12, 8, 0, 0, 0, DateTimeZone.forOffsetHours(1))
    sdj = new JDateTime(2012, 12, 8, 0, 0, 0, JDateTimeZone.forOffsetHours(1))
    println("Madsync DT was " + sd)
    println("Joda DT was " + sdj)
    sd.getMillis should be(sdj.getMillis)
    println("Madsync zone was " + sd.getZone)
    println("Joda zone was " + sdj.getZone)
    sd.getZone.getOffset(DateTime.now()) should be(sdj.getZone.getOffset(JDateTime.now()))

    //with timezone as ID with millis
    sd = DateTime(2012, 12, 8, 0, 0, 0, 123, DateTimeZone.forID("America/Chicago"))
    sdj = new JDateTime(2012, 12, 8, 0, 0, 0, 123, JDateTimeZone.forID("America/Chicago"))
    println("Madsync DT was " + sd)
    println("Joda DT was " + sdj)
    sd.getMillis should be(sdj.getMillis)
    println("Madsync zone was " + sd.getZone)
    println("Joda zone was " + sdj.getZone)
    sd.getZone.getOffset(DateTime.now()) should be(sdj.getZone.getOffset(JDateTime.now()))

    //with timezone as offset with millis
    sd = DateTime(2012, 12, 8, 0, 0, 0, 123, DateTimeZone.forOffsetHours(1))
    sdj = new JDateTime(2012, 12, 8, 0, 0, 0, 123, JDateTimeZone.forOffsetHours(1))
    println("Madsync DT was " + sd)
    println("Joda DT was " + sdj)
    sd.getMillis should be(sdj.getMillis)
    println("Madsync zone was " + sd.getZone)
    println("Joda zone was " + sdj.getZone)
    sd.getZone.getOffset(DateTime.now()) should be(sdj.getZone.getOffset(JDateTime.now()))

    //with default timezone with millis
    sd = DateTime(2012, 12, 8, 0, 0, 0, 123)
    sdj = new JDateTime(2012, 12, 8, 0, 0, 0, 123)
    println("Madsync DT was " + sd)
    println("Joda DT was " + sdj)
    sd.getMillis should be(sdj.getMillis)
    println("Madsync zone was " + sd.getZone)
    println("Joda zone was " + sdj.getZone)
    sd.getZone.getOffset(DateTime.now()) should be(sdj.getZone.getOffset(JDateTime.now()))

    //now
    sd = DateTime.now
    sdj = JDateTime.now
    println("Madsync DT was " + sd)
    println("Joda DT was " + sdj)
    sd.getMillis should be(sdj.getMillis)
    println("Madsync zone was " + sd.getZone)
    println("Joda zone was " + sdj.getZone)
    sd.getZone.getOffset(DateTime.now()) should be(sdj.getZone.getOffset(JDateTime.now()))

    //from util date
    sd = DateTime(new java.util.Date(2012))
    sdj = new JDateTime(new java.util.Date(2012))
    println("Madsync DT was " + sd)
    println("Joda DT was " + sdj)
    sd.getMillis should be(sdj.getMillis)
    println("Madsync zone was " + sd.getZone)
    println("Joda zone was " + sdj.getZone)
    sd.getZone.getOffset(DateTime.now()) should be(sdj.getZone.getOffset(JDateTime.now()))

  }

  test("testDtFormat") {

    val dtReads = DateTime.dtFormat
    //"yyyy-MM-dd'T'HH:mm:ssXXX"
    dtReads.reads(JsString("2018-05-15T19:00:00-03:00")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 19, 0, 0, DateTimeZone.forOffsetHours(-3)))
    }.isSuccess should be(true)

    dtReads.reads(JsString("2018-05-15T19:00:00Z")).map { dt =>
      val expected = DateTime(2018, 5, 15, 19, 0, 0, DateTimeZone.UTC)
      dt.date.toLocalDate should be(expected.date.toLocalDate)
      dt.date.getOffset should be(expected.date.getOffset)
    }.isSuccess should be(true)

    //yyyy-MM-dd'T'HH:mm:ss.SSSXXX
    dtReads.reads(JsString("2018-05-15T19:00:00.000-03:00")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 19, 0, 0, DateTimeZone.forOffsetHours(-3)))
    }.isSuccess should be(true)

    dtReads.reads(JsString("2018-05-15T19:00:00.000Z")).map { dt =>
      val expected = DateTime(2018, 5, 15, 19, 0, 0, DateTimeZone.UTC)
      dt.date.toLocalDate should be(expected.date.toLocalDate)
      dt.date.getOffset should be(expected.date.getOffset)
    }.isSuccess should be(true)

    //yyyy-MM-ddZ
    dtReads.reads(JsString("2018-05-15-03:00")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 0, 0, 0, DateTimeZone.forOffsetHours(-3)))
    }.isSuccess should be(true)

    dtReads.reads(JsString("2018-05-15Z")).map { dt: DateTime =>
      val expected = DateTime(2018, 5, 15, 0, 0, 0, DateTimeZone.UTC)
      dt.date.toLocalDate should be(expected.date.toLocalDate)
      dt.date.getOffset should be(expected.date.getOffset)
    }.isSuccess should be(true)

    //yyyy-MM-dd
    dtReads.reads(JsString("2018-05-15")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 0, 0, 0))
    }.isSuccess should be(true)

    dtReads.reads(JsString("2018-05-15")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 0, 0, 0))
    }.isSuccess should be(true)

    //MM-dd-yyyy HH:mm:ss.SSSX
    dtReads.reads(JsString("05-15-2018 19:00:00.000-0300")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 19, 0, 0, DateTimeZone.forOffsetHours(-3)))
    }.isSuccess should be(true)

    dtReads.reads(JsString("05-15-2018 19:00:00.000-03:00")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 19, 0, 0, DateTimeZone.forOffsetHours(-3)))
    }.isSuccess should be(true)
    dtReads.reads(JsString("05-15-2018 19:00:00.000Z")).map { dt =>
      val expected = DateTime(2018, 5, 15, 19, 0, 0, DateTimeZone.UTC)
      dt.date.toLocalDate should be(expected.date.toLocalDate)
      dt.date.getOffset should be(expected.date.getOffset)
    }.isSuccess should be(true)

    //MM-dd-yyyy HH:mm:ssZ
    dtReads.reads(JsString("05-15-2018 19:00:00-0300")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 19, 0, 0, DateTimeZone.forOffsetHours(-3)))
    }.isSuccess should be(true)

    dtReads.reads(JsString("05-15-2018 19:00:00Z")).map { dt =>
      val expected = DateTime(2018, 5, 15, 19, 0, 0, DateTimeZone.UTC)
      dt.date.toLocalDate should be(expected.date.toLocalDate)
      dt.date.getOffset should be(expected.date.getOffset)
    }.isSuccess should be(true)

    //yyyy-MM-dd HH:mm:ss.SSSSSSS
    dtReads.reads(JsString("2018-05-15 19:00:00.123")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 19, 0, 0, 123))
    }.isSuccess should be(true)

    dtReads.reads(JsString("2018-05-15 19:00:00.123")).map { dt =>
      dt should be(DateTime(2018, 5, 15, 19, 0, 0, 123))
    }.isSuccess should be(true)

    //yyyy-MM-dd HH:mm:ss.nnnZ
    dtReads.reads(JsString("2018-05-15 19:00:00.123-0300")).map { dt =>
      val expected = DateTime(2018, 5, 15, 19, 0, 0, 123, DateTimeZone.forOffsetHours(-3))
      dt.date.toLocalDate should be(expected.date.toLocalDate)
      dt.date.getOffset should be(expected.date.getOffset)
    }.isSuccess should be(true)

    dtReads.reads(JsString("2018-05-15 19:00:00.123Z")).map { dt =>
      val expected = DateTime(2018, 5, 15, 19, 0, 0, 123, DateTimeZone.UTC)
      dt.date.toLocalDate should be(expected.date.toLocalDate)
      dt.date.getOffset should be(expected.date.getOffset)
    }.isSuccess should be(true)

    dtReads.reads(JsNumber(1234567890000L)).map { dt =>
      val expected = DateTime(2009, 2, 13, 0, 0, 0, DateTimeZone.getDefault)
      dt.date.toLocalDate should be(expected.date.toLocalDate)
      dt.date.getOffset should be(expected.date.getOffset)
    }.isSuccess should be(true)

    dtReads.reads(JsObject(Seq(("a" -> JsString("b"))))) should be(JsError("formatted DateTime value expected"))
    dtReads.reads(JsString(null)) should be(JsError("Invalid value given for dateTime : 'null'"))
    dtReads.reads(JsString("bad date")).isSuccess should be(false)
  }

  test("test some withs") {
    val date = DateTime(2018, 5, 15, 0, 0, 0, DateTimeZone.UTC)

    date.withYear(2016) should be(DateTime(2016, 5, 15, 0, 0, 0, DateTimeZone.UTC))
    date.withHourOfDay(13) should be(DateTime(2018, 5, 15, 13, 0, 0, DateTimeZone.UTC))
    date.withMonthOfYear(11) should be(DateTime(2018, 11, 15, 0, 0, 0, DateTimeZone.UTC))
    date.withDayOfMonth(23) should be(DateTime(2018, 5, 23, 0, 0, 0, DateTimeZone.UTC))
  }

  test("test reads with nanos") {
    val dtReads = DateTime.dtFormat
    //nanoseconds now
    dtReads.reads(JsString("2018-05-15 19:00:00.123456789Z")).map { dt =>
      val expected = DateTime(2018, 5, 15, 19, 0, 0, 123, 456789, DateTimeZone.UTC)
      dt.date.toLocalDate should be(expected.date.toLocalDate)
      dt.date.getOffset should be(expected.date.getOffset)
    }.isSuccess should be(true)
  }

  test("test comparisons match") {
    val sd1 = DateTime(2012, 12, 8, 1, 2, 3, 125)
    val sd2 = DateTime(2012, 12, 8, 1, 2, 3, 203)
    val sd3 = DateTime(2012, 12, 8, 1, 2, 5, 125)

    sd1.isBefore(sd1) should be(false)
    sd2.isBefore(sd2) should be(false)
    sd3.isBefore(sd3) should be(false)

    sd1.isBefore(sd2) should be(true)
    sd2.isBefore(sd3) should be(true)
    sd1.isBefore(sd3) should be(true)
    sd3.isBefore(sd1) should be(false)

    sd1.isAfter(sd1) should be(false)
    sd2.isAfter(sd2) should be(false)
    sd3.isAfter(sd3) should be(false)

    sd2.isAfter(sd1) should be(true)
    sd3.isAfter(sd2) should be(true)
    sd3.isAfter(sd1) should be(true)
    sd1.isAfter(sd3) should be(false)

    sd1 >= sd1 should be(true)
    sd2 >= sd2 should be(true)
    sd2 >= sd2 should be(true)

    sd2 >= sd1 should be(true)
    sd3 >= sd1 should be(true)
    sd3 >= sd2 should be(true)
    sd1 >= sd2 should be(false)
    sd1 >= sd3 should be(false)
    sd2 >= sd3 should be(false)

    sd1 <= sd1 should be(true)
    sd2 <= sd2 should be(true)
    sd2 <= sd2 should be(true)

    sd1 <= sd2 should be(true)
    sd1 <= sd3 should be(true)
    sd2 <= sd3 should be(true)
    sd2 <= sd1 should be(false)
    sd3 <= sd1 should be(false)
    sd3 <= sd2 should be(false)

    sd1 > sd1 should be(false)
    sd2 > sd2 should be(false)
    sd2 > sd2 should be(false)

    sd2 > sd1 should be(true)
    sd3 > sd1 should be(true)
    sd3 > sd2 should be(true)
    sd1 > sd2 should be(false)
    sd1 > sd3 should be(false)
    sd2 > sd3 should be(false)

    sd1 < sd1 should be(false)
    sd2 < sd2 should be(false)
    sd2 < sd2 should be(false)

    sd1 < sd2 should be(true)
    sd1 < sd3 should be(true)
    sd2 < sd3 should be(true)
    sd2 < sd1 should be(false)
    sd3 < sd1 should be(false)
    sd3 < sd2 should be(false)

    sd1.isBeforeNow should be(true)
    DateTime.now().plus(1000).isBeforeNow should be(false)

    sd1.min(sd2) should be(sd1)
    sd2.min(sd1) should be(sd1)
    sd3.min(sd2) should be(sd2)
    sd2.min(sd3) should be(sd2)
    sd1.min(sd3) should be(sd1)
    sd3.min(sd1) should be(sd1)

    sd1.max(sd2) should be(sd2)
    sd2.max(sd1) should be(sd2)
    sd3.max(sd2) should be(sd3)
    sd2.max(sd3) should be(sd3)
    sd1.max(sd3) should be(sd3)
    sd3.max(sd1) should be(sd3)
  }

  test("test transformers are consistent with joda API") {
    import org.joda.time.{ DateTime => JDateTime, DateTimeZone => JDateTimeZone }
    val sd = DateTime(2012, 12, 8, 1, 2, 3, 10)
    val sdj = new JDateTime(2012, 12, 8, 1, 2, 3, 10)

    sd.getDayOfWeek should be(sdj.getDayOfWeek)
    sd.getDayOfYear should be(sdj.getDayOfYear)
    sd.getSecondOfMinute should be(sdj.getSecondOfMinute)
    sd.getMillisOfSecond should be(sdj.getMillisOfSecond)
    sd.getMinuteOfHour should be(sdj.getMinuteOfHour)
    sd.getHourOfDay should be(sdj.getHourOfDay)
    sd.getDayOfMonth should be(sdj.getDayOfMonth)
    sd.getMonthOfYear should be(sdj.getMonthOfYear)
    sd.getYear should be(sdj.getYear)

    //additional API
    sd.toDate should be(ZonedDateTime.of(2012, 12, 8, 1, 2, 3, 10000000, ZoneId.of(sd.getZone.getId)))
    sd.toJodaDateTime should be(sdj)
    sd.toUtilDate should be(sdj.toDate)
    sd.expressUTC().getMillis should be(sdj.withZone(JDateTimeZone.UTC).getMillis)
  }

  test("test DateTimeZone methods are consistent with joda API") {
    import org.joda.time.{ DateTime => JDateTime, DateTimeZone => JDateTimeZone }

    var z = DateTimeZone.forOffsetHours(3)
    var jz = JDateTimeZone.forOffsetHours(3)
    jz.getID should be(z.getId)

    z = DateTimeZone.forOffsetMillis(3 * 3600000)
    jz = JDateTimeZone.forOffsetMillis(3 * 3600000)
    jz.getID should be(z.getId)
    z should be(DateTimeZone.forOffsetHours(3))

    z = DateTimeZone.forID("Indian/Mahe")
    jz = JDateTimeZone.forID("Indian/Mahe")
    z.getOffset(DateTime.now()) should be(jz.getOffset(JDateTime.now()))
  }

  test("test equals is slightly more general than the java time passthrough") {
    val d0 = ZonedDateTime.now()
    val d1 = d0.withZoneSameInstant(ZoneOffset.UTC)
    val d2 = d0.withZoneSameInstant(ZoneId.of("UTC"))
    println(d1)
    println(d2)
    d1 should not be (d2)
    DateTime(d1) should be(DateTime(d2))

    val x = DateTime(CombinedDateTimeFormatterHelper(JodaDateTimeFormatterHelper.apply, ZonedDateTimeFormatterHelper.apply, LocalDateTimeFormatterHelper.apply)("MM/dd/YYYY").parse("3/5/2019"))
    val y = DateTime(2019, 3, 5, 0, 0, 0)
    x should be(y)

    //different minutes
    var t1 = DateTime.parse("2019-05-28T20:46:04.059Z")
    var t2 = DateTime.parse("2019-05-28T20:47:04.059Z")
    t1 should not be t2

    //different nanoseconds
    t1 = DateTime.parse("2019-05-28T20:46:04.059000001Z")
    t2 = DateTime.parse("2019-05-28T20:46:04.059000002Z")
    t1 should not be t2

    //different timezones
    t1 = DateTime.parse("2019-05-28T20:46:04.059Z")
    t2 = DateTime.parse("2019-05-28T20:46:04.059-07:00")
    t1 should not be t2
  }

  test("test date parse can do local date time and local date") {
    var sampleDateString = "11-24-2015 14:47:41"
    DateTime.parse(sampleDateString, DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")) should be(DateTime(2015, 11, 24, 14, 47, 41))

    sampleDateString = "11-24-2015"
    DateTime.parse(sampleDateString, DateTimeFormatter.ofPattern("MM-dd-yyyy")) should be(DateTime(2015, 11, 24, 0, 0, 0))

    sampleDateString = "2019-05-29T13:34:11.870+0000"
    DateTime.parse(sampleDateString) should be(DateTime(2019, 5, 29, 13, 34, 11, 870, DateTimeZone.UTC))

    DateTime(2014, 4, 10, 0, 0, 0, 0, DateTimeZone.UTC).toString should be("2014-04-10T00:00:00.000000000Z")
    DateTimeParsing.dtWrites.writes(DateTime(2014, 4, 10, 0, 0, 0, 0, DateTimeZone.UTC)) should be(JsString("2014-04-10T00:00:00.000000000Z"))

    DateTime(2014, 4, 10, 1, 2, 3, 4, 5, DateTimeZone.UTC).getNanos - DateTime(2014, 4, 10, 1, 2, 3, 0, DateTimeZone.UTC).getNanos should be(4000005)
    DateTime(2014, 4, 10, 1, 2, 3, 4, 5, DateTimeZone.UTC).toString should be("2014-04-10T01:02:03.004000005Z")
    DateTimeParsing.dtWrites.writes(DateTime(2014, 4, 10, 1, 2, 3, 4, 5, DateTimeZone.UTC)) should be(JsString("2014-04-10T01:02:03.004000005Z"))

    //    sampleDateString = "2014-04-10T00:00Z[UTC]"
    //    DateTime.parse(sampleDateString, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmOOOO")) should be(DateTime(2014, 4, 10, 0, 0, 0, 0, DateTimeZone.UTC))
  }

  test("test compareTo orders things ascending") {
    DateTime(2019, 5, 29, 13, 34, 11, 870, 123456, DateTimeZone.UTC).compareTo(DateTime(2019, 5, 29, 13, 34, 11, 870, 123456, DateTimeZone.UTC)) should be(0)
    DateTime(2019, 5, 29, 13, 34, 11, 870, 123456, DateTimeZone.UTC).compareTo(DateTime(2019, 5, 29, 13, 34, 11, 870, 123457, DateTimeZone.UTC)) should be(-1)

    Seq(
      DateTime(2019, 5, 29, 13, 34, 11, 870, 123458, DateTimeZone.UTC),
      DateTime(2019, 5, 29, 13, 34, 11, 870, 123455, DateTimeZone.UTC),
      DateTime(2019, 5, 29, 13, 34, 11, 870, 123457, DateTimeZone.UTC),
      DateTime(2019, 5, 29, 13, 34, 11, 870, 123456, DateTimeZone.UTC)).sorted should be(Seq(
        DateTime(2019, 5, 29, 13, 34, 11, 870, 123455, DateTimeZone.UTC),
        DateTime(2019, 5, 29, 13, 34, 11, 870, 123456, DateTimeZone.UTC),
        DateTime(2019, 5, 29, 13, 34, 11, 870, 123457, DateTimeZone.UTC),
        DateTime(2019, 5, 29, 13, 34, 11, 870, 123458, DateTimeZone.UTC)))
  }

  test("test plus periods works as expected") {
    val d1 = DateTime(2019, 3, 6, 1, 2, 3, DateTimeZone.UTC)
    d1.plus(Period.months(1)) should be(DateTime(2019, 4, 6, 1, 2, 3, DateTimeZone.UTC))
  }

  test("test scala distinct will separate out dates") {
    val jDate1 = ZonedDateTime.ofLocal(LocalDateTime.of(2014, 2, 18, 0, 0, 0), ZoneOffset.ofHours(0), ZoneOffset.ofHours(0))
    val jDate2 = ZonedDateTime.ofLocal(LocalDateTime.of(2014, 2, 18, 2, 0, 0), ZoneOffset.ofHours(0), ZoneOffset.ofHours(0))

    val jDate3 = ZonedDateTime.ofLocal(LocalDateTime.of(2014, 2, 18, 2, 0), ZoneId.of("UTC"), ZoneOffset.ofHours(0))

    val dates = Seq(jDate1, jDate2)

    val datesWithAdded = dates :+ jDate3

    datesWithAdded.distinct.size should be(3)

    jDate3 should not be jDate2

    DateTime(jDate3).getNanos should be(DateTime(jDate2).getNanos)
    jDate3.getOffset should be(jDate2.getOffset)
    jDate3.getZone should not be jDate2.getZone

    DateTime(datesWithAdded.last) should be(DateTime(jDate2))
    datesWithAdded.map(DateTime.apply).toSet.size should be(2)
    datesWithAdded.map(DateTime.apply).distinct.size should be(2)
  }

  test("test equality understands timezone equivalence") {
    println("Grouped time zones are")
    DateTimeZone.groupedTimeZones.map(println)

    DateTimeZone.forID("America/Chicago") should be(DateTimeZone.forID("US/Central"))
    DateTimeZone("America/Chicago") should be(DateTimeZone.forID("US/Central"))
    DateTimeZone.forID("-07:00") should be(DateTimeZone.forID("America/Phoenix"))

    DateTimeZone.forID("-3") should be(DateTimeZone.forOffsetHours(-3))
    DateTimeZone("-3") should be(DateTimeZone.forOffsetHours(-3))
    DateTimeZone(-3 * 3600000) should be(DateTimeZone.forOffsetHours(-3))

    DateTimeZone.forID("America/Chicago") should not be DateTimeZone.forID("America/Mexico_City")

    DateTime(2016, 3, 10, 1, 2, 3, DateTimeZone.forID("-06:00")) should be(DateTime(2016, 3, 10, 1, 2, 3, DateTimeZone.forID("America/Costa_Rica")))

    DateTime(2016, 3, 10, 1, 2, 3, DateTimeZone.forID("-06:00")) should not be DateTime(2016, 3, 10, 1, 2, 3, DateTimeZone.forID("America/Denver"))
    DateTime(2016, 3, 10, 1, 2, 3, DateTimeZone.forID("-07:00")) should not be DateTime(2016, 3, 10, 1, 2, 3, DateTimeZone.forID("America/Denver"))
  }

}
