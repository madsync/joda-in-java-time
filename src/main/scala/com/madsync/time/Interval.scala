package com.madsync.time

final case class Interval private (start: DateTime, end: DateTime) {
  def getStart: DateTime = start
  def getEnd: DateTime = end
  def overlap(i: Interval): Option[Interval] = if (end.isBefore(i.start) || start.isAfter(i.end)) None else {
    Some(
      Interval(start.max(i.start), end.min(i.end)))
  }
}
