package com.madsync.time

import com.madsync.time.format.{ CombinedDateTimeFormatterHelper, JodaDateTimeFormatterHelper, LocalDateTimeFormatterHelper, ZonedDateTimeFormatterHelper }
import org.scalatest.{ FunSuite, Matchers }

class CombinedDateTimeFormatterHelperTest extends FunSuite with Matchers {

  test("test parse of some common date and time formats") {
    import org.joda.time.format.{ DateTimeFormat => JDateTimeFormat }
    import org.joda.time.{ DateTime => JDateTime }

    JDateTimeFormat.forPattern("MM/dd/YYYY").parseDateTime("03/05/2019") should be(new JDateTime(2019, 3, 5, 0, 0, 0)) //, JDateTimeZone.UTC
    JDateTimeFormat.forPattern("MM/dd/yyyy").parseDateTime("03/05/2019") should be(new JDateTime(2019, 3, 5, 0, 0, 0)) //, JDateTimeZone.UTC
    JDateTimeFormat.forPattern("MM/dd/YYYY").parseDateTime("3/5/2019") should be(new JDateTime(2019, 3, 5, 0, 0, 0)) //, JDateTimeZone.UTC

    var cb1 = CombinedDateTimeFormatterHelper(JodaDateTimeFormatterHelper.apply, ZonedDateTimeFormatterHelper.apply, LocalDateTimeFormatterHelper.apply)("MM/dd/YYYY")
    val dExpected = DateTime(2019, 3, 5, 0, 0, 0)
    cb1.parse("03/05/2019") should be(DateTime(2019, 3, 5, 0, 0, 0).date)
    cb1.parse("3/5/2019") should be(dExpected.date)

    cb1 = CombinedDateTimeFormatterHelper(JodaDateTimeFormatterHelper.apply, ZonedDateTimeFormatterHelper.apply, LocalDateTimeFormatterHelper.apply)("MM/dd/yyyy HH:mm:ssXXX")
    cb1.parse("03/05/2019 03:58:29-05:00") should be(DateTime(2019, 3, 5, 3, 58, 29).withZoneRetainFields(DateTimeZone.forOffsetHours(-5)).date)

    cb1 = CombinedDateTimeFormatterHelper(JodaDateTimeFormatterHelper.apply, ZonedDateTimeFormatterHelper.apply, LocalDateTimeFormatterHelper.apply)("MM/dd/YYYY HH:mm:ssZ")
    cb1.parse("03/05/2019 03:58:29-05:00") should be(DateTime(2019, 3, 5, 3, 58, 29).withZoneRetainFields(DateTimeZone.forOffsetHours(-5)).date)

  }

}
