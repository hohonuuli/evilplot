/*
 * Copyright 2017 CiBO Technologies
 */

package com.cibo.evilplot.numeric

class Histogram(data: Seq[Double], numBins: Int) {
  private val _bins: Array[Long] = Array.fill(numBins){0}

  /** Histogram bins: a sequence of counts */
  // Expose an immutable Seq, not the mutable Array. We don't want the caller to be able to change values.
  lazy val bins: Seq[Long] = _bins.toSeq

  require(data.length > numBins, s"Data size = ${data.length} must be bigger than the number of bins = $numBins")
  private val sorted = data.sorted

  /** smallest data value */
  val min: Double = sorted.head

  /** largest data value */
  val max: Double = sorted.last

  /** width of histogram bin */
  val binWidth: Double = (max - min) / numBins

  // Assign each datum to a bin. Make sure that points at the end don't go out of bounds due to numeric imprecision.
  for (value <- sorted) {
    val bin: Int = math.min(math.round(math.floor((value - min) / binWidth)).toInt, numBins - 1)
    _bins(bin) = _bins(bin) + 1
  }
}