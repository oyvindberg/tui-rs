package tui
package widgets
package canvas

/// A shape to draw a group of points with the given color
case class Points(
    coords: Array[(Double, Double)] = Array.empty,
    color: Color = Color.Reset
) extends Shape {
  override def draw(painter: Painter): Unit =
    this.coords.foreach { case (x, y) =>
      painter.get_point(x, y) match {
        case None => ()
        case Some((x, y)) =>
          painter.paint(x, y, this.color)
      }
    }
}
