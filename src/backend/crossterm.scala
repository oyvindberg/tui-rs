package tui
package backend

import tui.buffer.Cell
import tui.crossterm.{Attribute, CrosstermJni, Xy}
import tui.layout.Rect

class CrosstermBackend(buffer: CrosstermJni) extends Backend {
  override def flush(): Unit =
    buffer.flush()

  override def draw(content: Array[(Int, Int, Cell)]): Unit = {
    var fg: Color = Color.Reset;
    var bg: Color = Color.Reset;
    var modifier = Modifier.EMPTY;
    var last_pos: Option[(Int, Int)] = None;
    content.foreach { case (x, y, cell) =>
      // Move the cursor if the previous location was not (x - 1, y)
      def shouldMove = last_pos match {
        case Some((lastX, lastY)) if x == lastX + 1 && y == lastY => false
        case _                                                    => true
      }
      if (shouldMove) { buffer.enqueueCursorMoveTo(x, y) }
      last_pos = Some((x, y));

      if (cell.modifier != modifier) {
        val diff = ModifierDiff(from = modifier, to = cell.modifier)
        diff.queue(buffer)
        modifier = cell.modifier;
      }
      if (cell.fg != fg) {
        val color = CrosstermBackend.from(cell.fg);
        buffer.enqueueStyleSetForegroundColor(color)
        fg = cell.fg;
      }
      if (cell.bg != bg) {
        val color = CrosstermBackend.from(cell.bg);
        buffer.enqueueStyleSetBackgroundColor(color)
        bg = cell.bg;
      }
      buffer.enqueueStylePrint(cell.symbol.str)
    }
    buffer.enqueueStyleSetForegroundColor(new crossterm.Color.Reset())
    buffer.enqueueStyleSetBackgroundColor(new crossterm.Color.Reset())
    buffer.enqueueStyleSetAttribute(crossterm.Attribute.Reset)
  }

  override def hide_cursor(): Unit =
    buffer.enqueueCursorHide()

  override def show_cursor(): Unit =
    buffer.enqueueCursorShow()

  override def get_cursor(): (Int, Int) = {
    val xy = buffer.cursorPosition()
    (xy.x(), xy.y())
  }

  override def set_cursor(x: Int, y: Int): Unit =
    buffer.enqueueCursorMoveTo(x, y)

  override def clear(): Unit =
    buffer.enqueueTerminalClear(crossterm.ClearType.All)

  override def size(): Rect = {
    val xy = buffer.terminalSize()
    Rect(0, 0, xy.x(), xy.y())
  }
}

object CrosstermBackend {
  def apply(buffer: CrosstermJni) = new CrosstermBackend(buffer)

  def from(color: Color): crossterm.Color =
    color match {
      case Color.Reset        => new crossterm.Color.Reset()
      case Color.Black        => new crossterm.Color.Black()
      case Color.Red          => new crossterm.Color.DarkRed()
      case Color.Green        => new crossterm.Color.DarkGreen()
      case Color.Yellow       => new crossterm.Color.DarkYellow()
      case Color.Blue         => new crossterm.Color.DarkBlue()
      case Color.Magenta      => new crossterm.Color.DarkMagenta()
      case Color.Cyan         => new crossterm.Color.DarkCyan()
      case Color.Gray         => new crossterm.Color.Grey()
      case Color.DarkGray     => new crossterm.Color.DarkGrey()
      case Color.LightRed     => new crossterm.Color.Red()
      case Color.LightGreen   => new crossterm.Color.Green()
      case Color.LightBlue    => new crossterm.Color.Blue()
      case Color.LightYellow  => new crossterm.Color.Yellow()
      case Color.LightMagenta => new crossterm.Color.Magenta()
      case Color.LightCyan    => new crossterm.Color.Cyan()
      case Color.White        => new crossterm.Color.White()
      case Color.Indexed(i)   => new crossterm.Color.AnsiValue(i)
      case Color.Rgb(r, g, b) => new crossterm.Color.Rgb(r, g, b)
    }
}

//#[derive(Debug)]
case class ModifierDiff(from: Modifier, to: Modifier) {
  def queue(w: CrosstermJni): Unit = {
    val removed = from - to;
    if (removed.contains(Modifier.REVERSED)) {
      w.enqueueStyleSetAttribute(Attribute.NoReverse)
    }
    if (removed.contains(Modifier.BOLD)) {
      w.enqueueStyleSetAttribute(Attribute.NormalIntensity)
      if (to.contains(Modifier.DIM)) {
        w.enqueueStyleSetAttribute(Attribute.Dim)
      }
    }
    if (removed.contains(Modifier.ITALIC)) {
      w.enqueueStyleSetAttribute(Attribute.NoItalic)
    }
    if (removed.contains(Modifier.UNDERLINED)) {
      w.enqueueStyleSetAttribute(Attribute.NoUnderline)
    }
    if (removed.contains(Modifier.DIM)) {
      w.enqueueStyleSetAttribute(Attribute.NormalIntensity)
    }
    if (removed.contains(Modifier.CROSSED_OUT)) {
      w.enqueueStyleSetAttribute(Attribute.NotCrossedOut)
    }
    if (removed.contains(Modifier.SLOW_BLINK) || removed.contains(Modifier.RAPID_BLINK)) {
      w.enqueueStyleSetAttribute(Attribute.NoBlink)
    }

    val added = to - from;
    if (added.contains(Modifier.REVERSED)) {
      w.enqueueStyleSetAttribute(Attribute.Reverse)
    }
    if (added.contains(Modifier.BOLD)) {
      w.enqueueStyleSetAttribute(Attribute.Bold)
    }
    if (added.contains(Modifier.ITALIC)) {
      w.enqueueStyleSetAttribute(Attribute.Italic)
    }
    if (added.contains(Modifier.UNDERLINED)) {
      w.enqueueStyleSetAttribute(Attribute.Underlined)
    }
    if (added.contains(Modifier.DIM)) {
      w.enqueueStyleSetAttribute(Attribute.Dim)
    }
    if (added.contains(Modifier.CROSSED_OUT)) {
      w.enqueueStyleSetAttribute(Attribute.CrossedOut)
    }
    if (added.contains(Modifier.SLOW_BLINK)) {
      w.enqueueStyleSetAttribute(Attribute.SlowBlink)
    }
    if (added.contains(Modifier.RAPID_BLINK)) {
      w.enqueueStyleSetAttribute(Attribute.RapidBlink)
    }
  }
}
