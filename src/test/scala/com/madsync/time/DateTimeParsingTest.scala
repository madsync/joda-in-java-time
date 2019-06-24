package com.madsync.time

import java.time.format.DateTimeFormatter
import java.time.{ ZoneOffset, ZonedDateTime }

import org.scalatest.{ FunSuite, Matchers }

import scala.util.{ Success, Try }

class DateTimeParsingTest extends FunSuite with DateTimeParsing with Matchers {

  test("test various date formats we expect to support") {
    //milliseconds
    var s = "2019-05-17T15:47:01.980-0500"
    Try(ZonedDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"))).toOption should be(
      Some(ZonedDateTime.of(2019, 5, 17, 15, 47, 1, 980000000, ZoneOffset.ofHours(-5))))

    s = "2019-05-17T15:47:01.980-05"
    Try(ZonedDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"))).toOption should be(
      Some(ZonedDateTime.of(2019, 5, 17, 15, 47, 1, 980000000, ZoneOffset.ofHours(-5))))

    s = "2019-05-17T15:47:01.980-05:00"
    Try(ZonedDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))).toOption should be(
      Some(ZonedDateTime.of(2019, 5, 17, 15, 47, 1, 980000000, ZoneOffset.ofHours(-5))))

    //nanoseconds
    s = "2019-05-17T15:47:01.000000980-05"
    Try(ZonedDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX"))) should be(
      Success(ZonedDateTime.of(2019, 5, 17, 15, 47, 1, 980, ZoneOffset.ofHours(-5))))

    s = "2019-05-17T15:47:01.000000980-05:00"
    Try(ZonedDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnXXX"))) should be(
      Success(ZonedDateTime.of(2019, 5, 17, 15, 47, 1, 980, ZoneOffset.ofHours(-5))))

    s = "2019-05-19T11:42:40.875-0600"
    ZonedDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")) should be(
      ZonedDateTime.of(2019, 5, 19, 11, 42, 40, 875000000, ZoneOffset.ofHours(-6)))
  }
}
