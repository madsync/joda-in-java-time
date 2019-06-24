package com.madsync.time

import com.madsync.time.extras.ScalaDateRange
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.exceptions.TestFailedException

class ScalaDateRangeTest extends FunSuite {

  test("is in range") {
    val range = extras.ScalaDateRange(DateTime(2014, 1, 2, 0, 0, 0), DateTime(2014, 1, 3, 0, 0, 0))
    assertFalse("In range should not allow before", range.isInRange(DateTime(2014, 1, 1, 12, 0, 0)))
    assertFalse("In range should not allow after", range.isInRange(DateTime(2014, 1, 3, 12, 0, 0)))
    assertFalse("In range should not allow left boundary", range.isInRange(DateTime(2014, 1, 2, 0, 0, 0)))
    assertFalse("In range should not allow right boundary", range.isInRange(DateTime(2014, 1, 3, 0, 0, 0)))
    assertTrue("In range should allow between", range.isInRange(DateTime(2014, 1, 2, 12, 0, 0)))
    assertEquals("Include Start is true", expected = true, result = range.includeStart)
    assertEquals("Include End is false", expected = false, result = range.includeEnd)
  }

  test("is in range inclusive") {
    val range = extras.ScalaDateRange(DateTime(2014, 1, 2, 0, 0, 0), DateTime(2014, 1, 3, 0, 0, 0))
    assertFalse("In range inclusive should not allow before", range.isInRangeInclusive(DateTime(2014, 1, 1, 12, 0, 0)))
    assertFalse("In range inclusive should not allow after", range.isInRangeInclusive(DateTime(2014, 1, 3, 12, 0, 0)))
    assertTrue("In range inclusive should allow left boundary", range.isInRangeInclusive(DateTime(2014, 1, 2, 0, 0, 0)))
    assertTrue("In range inclusive should allow right boundary", range.isInRangeInclusive(DateTime(2014, 1, 3, 0, 0, 0)))
    assertTrue("In range inclusive should allow between", range.isInRangeInclusive(DateTime(2014, 1, 2, 12, 0, 0)))
    assertEquals("Include Start is true", expected = true, result = range.includeStart)
    assertEquals("Include End is false", expected = false, result = range.includeEnd)
  }

  test("test left and right inclusive") {
    val range = extras.ScalaDateRange(DateTime(2014, 1, 2, 0, 0, 0), DateTime(2014, 1, 3, 0, 0, 0), true, false)
    range.isInRangeLeftInclusive(DateTime(2014, 1, 2, 0, 0, 0)) should be(true)
    range.isInRangeLeftInclusive(DateTime(2014, 1, 3, 0, 0, 0)) should be(false)

    range.isInRangeRightInclusive(DateTime(2014, 1, 2, 0, 0, 0)) should be(false)
    range.isInRangeRightInclusive(DateTime(2014, 1, 3, 0, 0, 0)) should be(true)
  }

  test("test intersect logic") {
    //TODO: intersect needs to listen to the includes (commented out tests)
    val range1 = extras.ScalaDateRange(DateTime(2014, 1, 2, 0, 0, 0), DateTime(2014, 1, 3, 0, 0, 0), true, false)
    val range2 = extras.ScalaDateRange(DateTime(2014, 1, 2, 0, 0, 0), DateTime(2014, 1, 2, 12, 0, 0), true, false)
    val range3 = extras.ScalaDateRange(DateTime(2014, 1, 1, 0, 0, 0), DateTime(2014, 1, 2, 0, 0, 0), true, false)
    val range4 = extras.ScalaDateRange(DateTime(2014, 1, 4, 0, 0, 0), DateTime(2014, 1, 5, 0, 0, 0), true, false)
    val range5 = extras.ScalaDateRange(DateTime(2014, 1, 2, 0, 0, 0), DateTime(2014, 1, 2, 12, 0, 0), true, true)

    range1.intersect(range2) should be(Some(range2))
    //range1.intersect(range3) should be(None)
    range1.intersect(range4) should be(None)
    range1.intersect(range5) should be(Some(range2))

    range2.intersect(range1) should be(Some(range2))
    //    range2.intersect(range3) should be(None)
    range2.intersect(range4) should be(None)

    //    range3.intersect(range1) should be(None)
    //    range3.intersect(range2) should be(None)
    range3.intersect(range4) should be(None)

    range4.intersect(range1) should be(None)
    range4.intersect(range2) should be(None)
    range4.intersect(range3) should be(None)
  }

  test("include Start End test") {
    var range: ScalaDateRange = ScalaDateRange.empty
    assertEquals("Include Start is true", expected = true, result = range.includeStart)
    assertEquals("Include End is false", expected = false, result = range.includeEnd)

    range = range.withIncludeEnd(true)
    assertEquals("Include Start is true", expected = true, result = range.includeStart)
    assertEquals("Include End is false", expected = true, result = range.includeEnd)

    range = range.withIncludeStart(false)
    assertEquals("Include Start is true", expected = false, result = range.includeStart)
    assertEquals("Include End is false", expected = true, result = range.includeEnd)

    range = range.withIncludeEnd(false)
    assertEquals("Include Start is true", expected = false, result = range.includeStart)
    assertEquals("Include End is false", expected = false, result = range.includeEnd)

    range = new ScalaDateRange(null, null, true, true)
    assertEquals("Include Start is true", expected = true, result = range.includeStart)
    assertEquals("Include End is false", expected = true, result = range.includeEnd)

    range = new ScalaDateRange(null, null, false, true)
    assertEquals("Include Start is true", expected = false, result = range.includeStart)
    assertEquals("Include End is false", expected = true, result = range.includeEnd)
  }

  test("test union") {
    val s1 = DateTime(2014, 1, 2, 0, 0, 0)
    val e1 = DateTime(2014, 1, 3, 0, 0, 0)
    val dr1 = extras.ScalaDateRange(s1, e1)

    ScalaDateRange.union(Seq()) should be(Seq())
    ScalaDateRange.union(Seq(dr1)) should be(Seq(dr1))

    ScalaDateRange.union(Seq(dr1, dr1)) should be(Seq(dr1))

    ScalaDateRange.union(Seq(dr1, dr1.withEndDate(e1.plus(100)))) should be(Seq(dr1.withEndDate(e1.plus(100)))) //left contained in right
    ScalaDateRange.union(Seq(dr1, dr1.withEndDate(e1.minus(100)))) should be(Seq(dr1)) //left contained in right (reversed)
    ScalaDateRange.union(Seq(dr1, dr1.withStartDate(s1.plus(100)))) should be(Seq(dr1)) //right contained in left
    ScalaDateRange.union(Seq(dr1, dr1.withStartDate(s1.minus(100)))) should be(Seq(dr1.withStartDate(s1.minus(100)))) //right contained in left (reversed)

    ScalaDateRange.union(Seq(dr1.withEndDate(e1.plus(100)), dr1)) should be(Seq(dr1.withEndDate(e1.plus(100)))) //left contained in right (reversed)
    ScalaDateRange.union(Seq(dr1.withEndDate(e1.minus(100)), dr1)) should be(Seq(dr1)) //left contained in right
    ScalaDateRange.union(Seq(dr1.withStartDate(s1.plus(100)), dr1)) should be(Seq(dr1)) //right contained in left (reversed)
    ScalaDateRange.union(Seq(dr1.withStartDate(s1.minus(100)), dr1)) should be(Seq(dr1.withStartDate(s1.minus(100)))) //right contained in left

    ScalaDateRange.union(Seq(dr1, dr1.withEndDate(e1.plus(100)).withStartDate(e1.minus(100)))) should be(Seq(dr1.withEndDate(e1.plus(100)))) //right.start <= left.end
    ScalaDateRange.union(Seq(dr1.withEndDate(e1.plus(100)).withStartDate(e1.minus(100)), dr1)) should be(Seq(dr1.withEndDate(e1.plus(100)))) //right.start <= left.end (reversed)

    ScalaDateRange.union(Seq(dr1, dr1.withEndDate(e1.plus(200)).withStartDate(e1.plus(100)))) should be(Seq(dr1, dr1.withEndDate(e1.plus(200)).withStartDate(e1.plus(100)))) //keep both
    ScalaDateRange.union(Seq(dr1.withEndDate(e1.plus(200)).withStartDate(e1.plus(100)), dr1)) should be(Seq(dr1, dr1.withEndDate(e1.plus(200)).withStartDate(e1.plus(100)))) //keep both (reversed)

    val dr2 = extras.ScalaDateRange(s1.plusHours(12), e1.plusHours(12))
    val dr3 = extras.ScalaDateRange(s1.plusHours(24), e1.plusHours(24))

    ScalaDateRange.union(Seq(dr3, dr1, dr2)) should be(Seq(ScalaDateRange(dr1.start, dr3.end)))

    val dr4 = extras.ScalaDateRange(e1.plusHours(25), e1.plusHours(48))

    ScalaDateRange.union(Seq(dr3, dr4, dr1, dr2)) should be(Seq(ScalaDateRange(dr1.start, dr3.end), dr4))

  }

  test("test date range uses leftmost time zone") {

    ScalaDateRange(null, DateTime.now().withZone(DateTimeZone.forID("-3"))).timeZone() should be(DateTimeZone.forID("-3"))
    ScalaDateRange(DateTime.now().withZone(DateTimeZone.forID("-3")), null).timeZone() should be(DateTimeZone.forID("-3"))

    ScalaDateRange(DateTime.now().withZone(DateTimeZone.forID("-3")), DateTime.now().withZone(DateTimeZone.forID("-5"))).timeZone() should be(DateTimeZone.forID("-3"))
    ScalaDateRange(DateTime.now().withZone(DateTimeZone.forID("-5")), DateTime.now().withZone(DateTimeZone.forID("-3"))).timeZone() should be(DateTimeZone.forID("-5"))

    //secondary constructor behaves the same way
    ScalaDateRange(ScalaDateRange(null, DateTime.now().withZone(DateTimeZone.forID("-3")))).timeZone() should be(DateTimeZone.forID("-3"))
    ScalaDateRange(ScalaDateRange(DateTime.now().withZone(DateTimeZone.forID("-3")), null)).timeZone() should be(DateTimeZone.forID("-3"))

    ScalaDateRange(ScalaDateRange(DateTime.now().withZone(DateTimeZone.forID("-3")), DateTime.now().withZone(DateTimeZone.forID("-5")))).timeZone() should be(DateTimeZone.forID("-3"))
    ScalaDateRange(ScalaDateRange(DateTime.now().withZone(DateTimeZone.forID("-5")), DateTime.now().withZone(DateTimeZone.forID("-3")))).timeZone() should be(DateTimeZone.forID("-5"))

    //ScalaDateRange(null, null).timeZone() should be(DateTimeZone.UTC)
  }

  test("test compareTo") {
    val dt1 = DateTime.now()
    val dt2 = DateTime.now().plus(1)

    val dt3 = DateTime.now().plus(2)
    val dt4 = DateTime.now().plus(3)

    //different start dates less than
    ScalaDateRange(dt1, dt2).compareTo(ScalaDateRange(dt2, dt3)) should be(-1)
    ScalaDateRange(dt2, dt3).compareTo(ScalaDateRange(dt1, dt2)) should be(-1)

    //same start but different end, larger than
    ScalaDateRange(dt1, dt3).compareTo(ScalaDateRange(dt1, dt2)) should be(1)

    //same both equal
    ScalaDateRange(dt2, dt3).compareTo(ScalaDateRange(dt2, dt3)) should be(0)
    //    ScalaDateRange(dt1, dt2).compareTo(ScalaDateRange(dt1, dt2)) should be(0)
  }

  test("test date range picks early and late times by default") {
    ScalaDateRange().start should be(ScalaDateRange.dawnOfTime)
    ScalaDateRange().end should be(ScalaDateRange.endOfTime)

    ScalaDateRange(None, None).start should be(ScalaDateRange.dawnOfTime)
    ScalaDateRange(None, None).end should be(ScalaDateRange.endOfTime)

    ScalaDateRange(None, None, true, true).start should be(ScalaDateRange.dawnOfTime)
    ScalaDateRange(None, None, true, true).end should be(ScalaDateRange.endOfTime)
  }

  test("test is bounded understands default dates") {
    val nowInMinus3 = DateTime.now().withZone(DateTimeZone.forID("-3"))
    ScalaDateRange(null, nowInMinus3).isStartBounded should be(false)
    ScalaDateRange(null, nowInMinus3).isEndBounded should be(true)
    ScalaDateRange(null, nowInMinus3).isSingleTime should be(false)

    ScalaDateRange().isStartBounded should be(false)
    ScalaDateRange().isEndBounded should be(false)
    ScalaDateRange().isSingleTime should be(false)

    ScalaDateRange(nowInMinus3, null).isStartBounded should be(true)
    ScalaDateRange(nowInMinus3, null).isEndBounded should be(false)
    ScalaDateRange(nowInMinus3, null).isSingleTime should be(false)

    ScalaDateRange(nowInMinus3, nowInMinus3).isStartBounded should be(true)
    ScalaDateRange(nowInMinus3, nowInMinus3).isEndBounded should be(true)
    ScalaDateRange(nowInMinus3, nowInMinus3).isSingleTime should be(true)
  }

  test("test duration in millis returns correct values") {
    val nowInMinus3 = DateTime.now().withZone(DateTimeZone.forID("-3"))
    ScalaDateRange().getDurationInMillis should be(ScalaDateRange.endOfTime.getMillis - ScalaDateRange.dawnOfTime.getMillis)
    ScalaDateRange(nowInMinus3, nowInMinus3).getDurationInMillis should be(0)
    ScalaDateRange(nowInMinus3.plus(1), nowInMinus3).getDurationInMillis should be(-1)
    ScalaDateRange(nowInMinus3, nowInMinus3.plus(1)).getDurationInMillis should be(1)
  }

  test("test with zone applies to both dates") {
    ScalaDateRange().timeZone() should be(DateTimeZone.UTC)
    ScalaDateRange().withZone(DateTimeZone.forID("-3")).timeZone() should be(DateTimeZone.forID("-3"))

    ScalaDateRange().withZone(DateTimeZone.forID("-3")).start.getZone should be(DateTimeZone.forID("-3"))
    ScalaDateRange().withZone(DateTimeZone.forID("-3")).end.getZone should be(DateTimeZone.forID("-3"))
  }

  def assertFalse(msg: String, result: Boolean): Unit = {
    if (result) { throw new TestFailedException(msg, 0) }
  }

  def assertTrue(msg: String, result: Boolean): Unit = {
    if (!result) { throw new TestFailedException(msg, 0) }
  }

  def assertEquals(msg: String, expected: Boolean, result: Boolean): Unit = (expected, result) match {
    case (true, true)   =>
    case (false, false) =>
    case (e, r)         => throw new TestFailedException(msg, 0)
  }

  def assertEquals(msg: String, expected: Object, result: Object): Unit = {
    if (!expected.equals(result)) {
      val sb = new StringBuilder()
      sb.append(s"Diff -> $msg\n")
      sb.append(s"     -> expected = $expected\n")
      sb.append(s"     ->   result = $result\n")
      throw new TestFailedException(sb.toString, 0)
    }
  }

}