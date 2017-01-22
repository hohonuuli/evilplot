package example
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom
import org.scalajs.dom.{CanvasRenderingContext2D, html}
import DSL._

import scala.util.Random

case class Point(x: Int, y: Int){
  def +(p: Point) = Point(x + p.x, y + p.y)
  def /(d: Int) = Point(x / d, y / d)
}

@JSExport
object ScalaJSExample {

  private def fullscreenAndHiRes(ctx: CanvasRenderingContext2D): Unit = {

    val canvasResolutionScaleHack = 3
    val screenWidth = dom.window.innerWidth.toInt
    val screenHeight = dom.window.innerHeight.toInt

    ctx.canvas.style.width = screenWidth + "px"
    ctx.canvas.style.height = screenHeight + "px"
    ctx.canvas.width = screenWidth * canvasResolutionScaleHack
    ctx.canvas.height = screenHeight * canvasResolutionScaleHack

    ctx.scale(canvasResolutionScaleHack, canvasResolutionScaleHack)
  }

  private def createGridLines(heights: Seq[Double], bars: Renderable): Renderable =
    DistributeV {
      val lineEveryXUnits     = 40
      val lineThick           = 0.25
      val textHeight          = Text.defaultSize
      val labelFloatAboveLine = 2

      val countOfGridLines = (heights.max / lineEveryXUnits).toInt

      Seq.tabulate(countOfGridLines){ x =>
        val yHeightLabel = (heights.max / lineEveryXUnits - x) * lineEveryXUnits

        Pad(bottom = lineEveryXUnits - lineThick){

          val label = Translate(y = -textHeight - labelFloatAboveLine){
            Text(yHeightLabel) filled "grey"
          }

          Line(bars.extent.width, lineThick) behind label
        }
      }
    }

  private def createYAxis(maxValue: Double, tickThick: Double, textAndPadHeight: Double, interTickDist: Double, tickLength: Double = 5) = {

    val numTicks = (maxValue / interTickDist).floor.toInt
    val ticks = Seq.fill(numTicks + 1)(
      Line(tickLength, tickThick) rotated 90 padRight (interTickDist - tickThick)
    ).distributeH

    Line(maxValue, tickThick * 2) behind ticks /*titled "Awesomeness"*/ rotated -90 padTop (textAndPadHeight - interTickDist)
  }

  private def createBarChart(heights: Seq[Double], colors: Seq[String]) = {
    val barWidth = 50
    val barSpacing = 5

      Align.bottomSeq {
        val rects = heights.map { h => Rect(barWidth, h) titled h.toString}
        rects.zip(colors).map { case (rect, color) => rect filled color labeled color }
      }.distributeH(barSpacing)
  }

  def createBarGraph(size: Extent): Renderable = {
    val heights = Seq(10D, 100D, 200D)
    val colors = Seq("red", "green", "blue")

    val tickThick = 0.5
    val textAndPadHeight = Text.defaultSize + 5 + tickThick / 2D // text size, label pad, stroke width

    val yAxis     = createYAxis(    heights.max, tickThick, textAndPadHeight, 10)
    val bars      = createBarChart( heights, colors)
    val gridLines = createGridLines(heights, bars)

    val barChart =
      yAxis beside
        (gridLines padTop (textAndPadHeight - tickThick/2D) behind bars)

    barChart titled ("A Swanky BarChart", 20) padAll 10
  }

  @JSExport
  def main(canvasDiv: html.Canvas): Unit = {
    val ctx: CanvasRenderingContext2D = canvasDiv.getContext("2d")
                    .asInstanceOf[CanvasRenderingContext2D]

    fullscreenAndHiRes(ctx)

    val graphSize = Extent(200,200)
    val barGraph = createBarGraph(graphSize)

    val scatterPlotGraph = {

      case class Point(x: Double, y: Double)

      val data = Seq.tabulate(50){ i =>
        val x = Random.nextDouble()
        Point(x, x + Random.nextDouble()/2.0 - 0.5)
      }

      val minX = data.map(_.x).min
      val maxY = data.map(_.y).max
      val minY = data.map(_.y).min

      Fit(barGraph.extent){
        val scatter = data.map{ case Point(x, y) => Disc(x - math.min(0, minX), maxY - y - math.min(0, minY), 0.01) }.group
        createYAxis(scatter.extent.height, scatter.extent.width * 0.0025, 0.25, 0.25, scatter.extent.width * 0.05) beside scatter
      } titled "A Scatter Plot" titled "A"
    }

//    val letterbox = Rect(160, 90) filled "red" behind Fit(160, 90){
//      Rect(4, 3)
//    }

    (barGraph beside scatterPlotGraph).render(ctx)
  }
}


case class Extent(width: Double, height: Double)
trait Renderable {
  // bounding boxen must be of stable size
  val extent: Extent
  def render(canvas: CanvasRenderingContext2D): Unit
}

case class Style(fill: String)(r: Renderable) extends Renderable {
  val extent = r.extent
  def render(canvas: CanvasRenderingContext2D): Unit = {
    CanvasOp(canvas){ c =>
      c.fillStyle = fill
      r.render(c)
    }
  }
}

case class Line(length: Double, strokeWidth: Double) extends Renderable {

  val extent = Extent(length, strokeWidth)

  def render(canvas: CanvasRenderingContext2D): Unit =
    CanvasOp(canvas) { c =>
      canvas.beginPath()
      canvas.lineWidth = strokeWidth
      canvas.moveTo(0, strokeWidth / 2.0)
      canvas.lineTo(length, strokeWidth / 2.0)
      canvas.closePath()
      canvas.stroke()
    }
}

case class Rect(width: Double, height: Double) extends Renderable {
  def render(canvas: CanvasRenderingContext2D): Unit = canvas.fillRect(0, 0, width, height)
  val extent: Extent = Extent(width, height)
}
object Rect {
  def apply(side: Double): Rect = Rect(side, side)
}


//TODO: Dont allow negatives?
case class Disc(x: Double, y: Double, radius: Double) extends Renderable {
  require(x >= 0 && y >=0, s"x {$x} and y {$y} must both be positive")
  val extent = Extent(x + radius*2, y + radius*2)

  def render(canvas: CanvasRenderingContext2D): Unit =
    CanvasOp(canvas) { c =>
      c.beginPath()
      c.arc(x + radius, y + radius, radius, 0, 2 * Math.PI)
      c.closePath()
      c.fill()
    }
}

case class Text(msgAny: Any, size: Double = Text.defaultSize) extends Renderable {
  private val msg = msgAny.toString

  val extent: Extent = Text.measure(size)(msg)

  def render(canvas: CanvasRenderingContext2D): Unit = Text.withStyle(size){_.fillText(msg, 0, 0)}(canvas)
}
object Text {
  val defaultSize = 10

  // TODO: THIS IS A DIRTY HACK
  private val offscreenBuffer = dom.window.document.getElementById("measureBuffer").asInstanceOf[html.Canvas].getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

  private val replaceSize = """\d+px""".r
  private val fontSize = """[^\d]*(\d+)px.*""".r
  private def extractHeight = {
    val fontSize(size) = offscreenBuffer.font
    size.toDouble
  }

  private def swapFont(canvas: CanvasRenderingContext2D, size: Double) =
    Text.replaceSize.replaceFirstIn(canvas.font, size.toString + "px")

  private def withStyle[T](size: Double)(f: CanvasRenderingContext2D => T): CanvasRenderingContext2D => T = {
    c =>
      c.textBaseline = "top"
      c.font = swapFont(c, size)
      f(c)
  }

  private def measure(size: Double)(msg: String) = withStyle(size){ c =>
    Extent(c.measureText(msg).width, extractHeight)
  }(offscreenBuffer)
}

object CanvasOp {
  // loan it out
  def apply(canvas: CanvasRenderingContext2D)(f: CanvasRenderingContext2D => Unit) = {
    canvas.save()
    f(canvas)
    canvas.restore()
  }
}
case class Translate(x: Double = 0, y: Double = 0)(r: Renderable) extends Renderable {
  // TODO: is this correct with negative translations?
  val extent: Extent = Extent(
    r.extent.width + x,
    r.extent.height + y
  )

  def render(canvas: CanvasRenderingContext2D): Unit = CanvasOp(canvas){ c =>
    c.translate(x, y)
    r.render(c)
  }
}
object Translate {
  def apply(r: Renderable, bbox: Extent): Translate = Translate(bbox.width, bbox.height)(r)
}

// Our rotate semantics are, rotate about your centroid, and shift back to all positive coordinates
case class Rotate(degrees: Double)(r: Renderable) extends Renderable {

  // TODO: not bringing in a matrix library for just this one thing ... yet
  private case class Point(x: Double, y: Double) {
    def originRotate(thetaDegrees: Double): Point = {
      val thetaRad = math.toRadians(thetaDegrees)
      Point(
        x * math.cos(thetaRad) - y * math.sin(thetaRad),
        y * math.cos(thetaRad) + x * math.sin(thetaRad)
      )
    }
  }

  private val rotatedCorners = Seq(
    Point(-0.5 * r.extent.width, -0.5 * r.extent.height),
    Point( 0.5 * r.extent.width, -0.5 * r.extent.height),
    Point(-0.5 * r.extent.width,  0.5 * r.extent.height),
    Point( 0.5 * r.extent.width,  0.5 * r.extent.height)
  ).map(_.originRotate(degrees))

  private val minX = rotatedCorners.map(_.x).min
  private val maxX = rotatedCorners.map(_.x).max
  private val minY = rotatedCorners.map(_.y).min
  private val maxY = rotatedCorners.map(_.y).max

  val extent = Extent(maxX - minX, maxY - minY)

  def render(canvas: CanvasRenderingContext2D): Unit =
    CanvasOp(canvas) { c =>
      c.translate(-1 * minX , -1 * minY)
      c.rotate(math.toRadians(degrees))
      c.translate(r.extent.width / -2, r.extent.height / -2)

      r.render(c)
    }
}

case class Pad(left: Double = 0, right: Double = 0, top: Double = 0, bottom: Double = 0)(item: Renderable) extends Renderable {
  val extent = Extent(
    item.extent.width + left + right,
    item.extent.height + top + bottom
  )

  def render(canvas: CanvasRenderingContext2D): Unit = Translate(x = left, y = top)(item).render(canvas)
}
object Pad {
  def apply(surround: Double)(item: Renderable): Pad = Pad(surround, surround, surround, surround)(item)
  def apply(x: Double, y: Double)(item: Renderable): Pad = Pad(x, x, y, y)(item)
}

case class Group(items: Renderable*) extends Renderable {
  val extent: Extent = Extent(
    items.map(_.extent.width).max,
    items.map(_.extent.height).max
  )

  def render(canvas: CanvasRenderingContext2D): Unit = items.foreach(_.render(canvas))
}

case class Above(top: Renderable, bottom: Renderable) extends Renderable {
  val extent: Extent = Extent(
    math.max(top.extent.width, bottom.extent.width),
    top.extent.height + bottom.extent.height
  )

  def render(canvas: CanvasRenderingContext2D): Unit = (
      top behind Translate(y = top.extent.height)(bottom)
    ).render(canvas)
}

case class Beside(head: Renderable, tail: Renderable) extends Renderable {
  def render(canvas: CanvasRenderingContext2D): Unit =
    (
      head behind Translate(x = head.extent.width)(tail)
    ).render(canvas)

  val extent: Extent = Extent(
    head.extent.width + tail.extent.width,
    math.max(head.extent.height, tail.extent.height)
  )
}


object Align {
  def bottomSeq(items: Seq[Renderable]) = bottom(items :_*)

  def bottom(items: Renderable*): Seq[Renderable] = {
    val groupHeight = items.foldLeft(0D)((max, elem) => math.max(max, elem.extent.height))

    items.map(r => Translate(y = groupHeight - r.extent.height)(r) )
  }

  def centerSeq(items: Seq[Renderable]) = center(items :_*)

  def center(items: Renderable*): Seq[Renderable] = {
    val groupWidth = items.foldLeft(0D)((max, elem) => math.max(max, elem.extent.width))

    items.map( r => Translate(x = (groupWidth - r.extent.width) / 2.0)(r) )
  }
}

case class Labeled(msg: String, r: Renderable, textSize: Double = Text.defaultSize) extends Renderable {

  private val composite = Align.center(r, Text(msg, textSize) padTop 5 ).reduce(Above)

  val extent: Extent = composite.extent
  def render(canvas: CanvasRenderingContext2D): Unit = composite.render(canvas)
}

case class Titled(msg: String, r: Renderable, textSize: Double = Text.defaultSize) extends Renderable {

  private val paddedTitle = Pad(bottom = 5)(Text(msg, textSize))
  private val composite = Align.center(paddedTitle, r).reduce(Above)

  val extent = composite.extent
  def render(canvas: CanvasRenderingContext2D): Unit = composite.render(canvas)
}

case class Fit(width: Double, height: Double)(item: Renderable) extends Renderable {
  val extent = Extent(width, height)

  def render(canvas: CanvasRenderingContext2D): Unit = {
    val oldExtent = item.extent

    val newAspectRatio = width / height
    val oldAspectRatio = oldExtent.width / oldExtent.height

    val widthIsLimiting = newAspectRatio < oldAspectRatio

    val (scale, padFun) = if(widthIsLimiting) {
      val scale = width / oldExtent.width
      (
        scale,
        Pad(top = ((height - oldExtent.height * scale) / 2) / scale) _
      )
    } else { // height is limiting
      val scale = height / oldExtent.height
      (
        scale,
        Pad(left = ((width - oldExtent.width * scale) / 2) / scale) _
      )
    }

    CanvasOp(canvas){c =>
      c.scale(scale, scale)
      padFun(item).render(c)
    }
  }
}
object Fit {
  def apply(extent: Extent)(item: Renderable): Fit = Fit(extent.width, extent.height)(item)
}

object DSL {
  implicit class Placeable(r: Renderable){
    def above(other: Renderable) = Above(r, other)
    def beside(other: Renderable) = Beside(r, other)
    def behind(other: Renderable) = Group(r, other)

    def labeled(msg: String) = Labeled(msg, r)
    def labeled(msgSize: (String, Double)) = Labeled(msgSize._1, r, msgSize._2)
    def titled(msg: String) = Titled(msg, r)
    def titled(msgSize: (String, Double)) = Titled(msgSize._1, r, msgSize._2)

    def padRight(pad: Double) = Pad(right = pad)(r)
    def padLeft(pad: Double) = Pad(left  = pad)(r)
    def padBottom(pad: Double) = Pad(bottom = pad)(r)
    def padTop(pad: Double) = Pad(top = pad)(r)
    def padAll(pad: Double) = Pad(pad)(r)

    def rotated(degress: Double) = Rotate(degress)(r)

    def filled(color: String) = Style(fill = color)(r)

    // Experimental
    def -->(nudge: Double) = Translate(x = nudge)(r)
    def |^(nudge: Double) = Translate(y = nudge)(r)
    def -|(pad: Double) = Pad(right = pad)(r)
    def |-(pad: Double) = Pad(left  = pad)(r)
    def ⊥(pad: Double) = Pad(bottom = pad)(r)
    def ⊤(pad: Double) = Pad(top = pad)(r)

    // end Experimental
  }

  implicit class SeqPlaceable(sp: Seq[Renderable]){
    def distributeH: Renderable = DistributeH(sp)
    def distributeH(spacing: Double = 0): Renderable = DistributeH(sp, spacing)
    def distributeV: Renderable = DistributeV(sp)

    def group: Renderable = Group(sp :_*)
  }


  def DistributeH(rs: Seq[Renderable], spacing: Double = 0): Renderable =
    if(spacing == 0) rs.reduce(Beside)
    else {
      val padded = rs.init.map(_ padRight spacing) :+ rs.last
      padded.reduce(Beside)
    }

  def DistributeV(rs: Seq[Renderable]): Renderable = rs.reduce(Above)
}