package com.madsync.time.extras

import com.madsync.time.{ DateTime, DateTimeZone, Interval }
import com.sun.istack.internal.Nullable

/**
 * A class to handle nullable DateTimes in a range coming from a Java interface
 *
 * @param start  DateTime to begin the range with
 * @param end  DateTime to end the range with
 * @param includeStart   the range is closed on the left
 * @param includeEnd     the range is closed on the right
 */
final case class ScalaDateRange private[time] (
    start: DateTime,
    end: DateTime,
    includeStart: Boolean,
    includeEnd: Boolean //
) extends Comparable[ScalaDateRange] {
  def withZone(zone: DateTimeZone): ScalaDateRange = copy(start = start.withZoneDSTAware(zone), end = end.withZoneDSTAware(zone))

  // Members declared in java.lang.Comparable
  def compareTo(test: ScalaDateRange): Int = {
    if (start.getMillis - test.start.getMillis != 0) -1
    else if (end.getMillis - test.end.getMillis != 0) 1
    else 0
  }

  // Other members
  def getDurationInMillis: Long = end.getMillis - start.getMillis

  def timeZone(): DateTimeZone = { start.getZone }

  def isInRange(test: DateTime): Boolean = {
    (start.getMillis < test.getMillis) && (end.getMillis > test.getMillis)
  }

  def isInRangeInclusive(test: DateTime): Boolean = {
    (start.getMillis <= test.getMillis) && (end.getMillis >= test.getMillis)
  }

  def isInRangeLeftInclusive(test: DateTime): Boolean = {
    start.getMillis <= test.getMillis && (end.getMillis > test.getMillis)
  }

  def isInRangeRightInclusive(test: DateTime): Boolean = {
    start.getMillis < test.getMillis && (end.getMillis >= test.getMillis)
  }

  def withStartDate(start: DateTime): ScalaDateRange = copy(start = start)
  def withEndDate(end: DateTime): ScalaDateRange = copy(end = end)

  def withIncludeEnd(include: Boolean): ScalaDateRange = copy(includeEnd = include)
  def withIncludeStart(include: Boolean): ScalaDateRange = copy(includeStart = include)

  def isStartBounded: Boolean = start.getMillis != ScalaDateRange.dawnOfTime.getMillis
  def isEndBounded: Boolean = end.getMillis != ScalaDateRange.endOfTime.getMillis
  def isSingleTime: Boolean = start == end

  def intersect(dr: ScalaDateRange): Option[ScalaDateRange] = {
    val result = Interval(start, end).overlap(Interval(dr.start, dr.end)).map { i =>
      ScalaDateRange.apply(i.getStart, i.getEnd)
    }
    result.filter {
      case r if r.start == start && r.end == start => r.includeStart //single point overlap on this range's start
      case r if r.start == end && r.end == end     => r.includeEnd
      case _                                       => true
    }
  }

}

object ScalaDateRange {

  final val MAX_MILLIS = 253402300800000L
  DateTimeZone.setDefault(DateTimeZone.UTC)

  lazy val dawnOfTime: DateTime = DateTime(-62135596800000L) // --- 01/01/0001  00:00:00.000 Zulu
  lazy val endOfTime: DateTime = DateTime(MAX_MILLIS) // 01/01/10000 00:00:00.000 Zulu

  def apply(): ScalaDateRange = apply(dawnOfTime, endOfTime)

  def apply(in: ScalaDateRange): ScalaDateRange = {
    val start: DateTime = Option(in.start).getOrElse(dawnOfTime)
    val end: DateTime = Option(in.end).getOrElse(endOfTime)
    val z = getLeftmostTimeZone(in.start, in.end)
    ScalaDateRange(start.withZoneDSTAware(z), end.withZoneDSTAware(z), in.includeStart, in.includeEnd)
  }

  /**
   * Java interface
   *
   * @param start  (nullable) DateTime to begin the range with
   * @param end  (nullable) DateTime to end the range with
   * @return   ScalaDateRange(start, end)
   */
  def apply(@Nullable start: DateTime, @Nullable end: DateTime): ScalaDateRange = {
    val z = getLeftmostTimeZone(start, end)
    val s = Option(start).getOrElse(dawnOfTime)
    val e = Option(end).getOrElse(endOfTime)
    val incEnd = s.equals(e)
    ScalaDateRange(s.withZoneDSTAware(z), e.withZoneDSTAware(z), true, incEnd)
  }

  def apply(start: Option[DateTime], end: Option[DateTime]): ScalaDateRange = {
    apply(start.orNull, end.orNull)
  }

  def apply(start: Option[DateTime], end: Option[DateTime], includeStart: Boolean,
    includeEnd: Boolean): ScalaDateRange = {
    apply(start.orNull, end.orNull).copy(includeStart = includeStart, includeEnd = includeEnd)
  }

  def empty = apply(None, None)

  private def getLeftmostTimeZone(start: DateTime, end: DateTime): DateTimeZone = {
    if (start != null) {
      return start.getZone
    }
    if (end != null) {
      return end.getZone
    }
    DateTimeZone.UTC
  }

  def union(dateRanges: Seq[ScalaDateRange]): Seq[ScalaDateRange] = {
    dateRanges.size < 2 match {
      case true => dateRanges
      case false =>
        val odrs = dateRanges.toList.sortWith((a, b) => a.start.isBefore(b.start) || (a.start == b.start && !a.end.isAfter(b.end)))
        odrs.tail.foldLeft(Seq(odrs.head)) { (acc, right) =>
          val left = acc.last
          val vals = (right.start.getMillis - left.start.getMillis,
            right.end.getMillis - left.end.getMillis,
            right.start.getMillis - left.end.getMillis)

          vals match {
            case (0, 0, _)                     => acc //left and right the same the same => keep the left
            case (0, n, _) if n > 0            => acc.take(acc.size - 1) :+ right //left contained in right  => keepthe right
            case (n, m, _) if n >= 0 && m <= 0 => acc //right contained in left => keep the left
            case (_, _, n) if n <= 0           => acc.take(acc.size - 1) :+ ScalaDateRange.apply(acc.last.start, right.end) //right.start <= left.end => keep (left.start, right.end)
            case (_, _, n) if n > 0            => acc :+ right //right start > left.end    => keep both

          }

        }
    }

  }

}