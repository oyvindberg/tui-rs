package tui
package buffer

import tui.internal.breakableForeach._
import tui.internal._
import tui.layout.Rect
import tui.text.{Span, Spans}

import scala.collection.mutable

/// A buffer cell
//#[derive(Debug, Clone, PartialEq, Eq)]
case class Cell(
    var symbol: tui.Grapheme,
    var fg: Color,
    var bg: Color,
    var modifier: Modifier
) {
  override def clone(): Cell =
    new Cell(symbol, fg, bg, modifier)

  def set_symbol(symbol: String): this.type =
    set_symbol(Grapheme(symbol))

  def set_symbol(symbol: tui.Grapheme): this.type = {
    this.symbol = symbol
    this
  }

  def set_char(ch: Char): this.type = {
    this.symbol = Grapheme(ch.toString)
    this
  }

  def set_fg(color: Color): this.type = {
    this.fg = color
    this
  }

  def set_bg(color: Color): this.type = {
    this.bg = color
    this
  }

  def set_style(style: Style): this.type = {
    style.fg.foreach(fg = _)
    style.bg.foreach(bg = _)
    modifier = modifier.insert(style.add_modifier).remove(style.sub_modifier)
    this
  }

  def style: Style =
    Style(fg = Some(fg), bg = Some(bg), add_modifier = modifier)

  def reset() = {
    this.symbol = Grapheme.Empty
    this.fg = Color.Reset
    this.bg = Color.Reset
    this.modifier = Modifier.EMPTY
  }
}

object Cell {
  def default: Cell = Cell(Grapheme.Empty, fg = Color.Reset, bg = Color.Reset, modifier = Modifier.EMPTY)
}

/// A buffer that maps to the desired content of the terminal after the draw call
///
/// No widget in the library interacts directly with the terminal. Instead each of them is required
/// to draw their state to an intermediate buffer. It is basically a grid where each cell contains
/// a grapheme, a foreground color and a background color. This grid will then be used to output
/// the appropriate escape sequences and characters to draw the UI as the user has defined it.
///
/// # Examples:
///
/// ```
/// use tui::buffer::{Buffer, Cell};
/// use tui::layout::Rect;
/// use tui::style::{Color, Style, Modifier};
///
/// let mut buf = Buffer::empty(Rect{x: 0, y: 0, width: 10, height: 5});
/// buf.get_mut(0, 2).set_symbol("x");
/// assert_eq!(buf.get(0, 2).symbol, "x");
/// buf.set_string(3, 0, "string", Style::DEFAULT.fg(Color::Red).bg(Color::White));
/// assert_eq!(buf.get(5, 0), &Cell{
///     symbol: String::from("r"),
///     fg: Color::Red,
///     bg: Color::White,
///     modifier: Modifier::EMPTY
/// });
/// buf.get_mut(5, 0).set_char('x');
/// assert_eq!(buf.get(5, 0).symbol, "x");
/// ```
//#[derive(Debug, Clone, PartialEq, Eq, Default)]
case class Buffer(
    /// The area represented by this buffer
    var area: Rect,
    /// The content of the buffer. The length of this Vec should always be equal to area.width * / area.height
    var content: mutable.ArraySeq[Cell]
) {
  /// Returns a reference to Cell at the given coordinates
  def get(x: Int, y: Int): Cell = {
    val i = this.index_of(x, y)
    content(i)
  }
  /// Returns a reference to Cell at the given coordinates
  def set(x: Int, y: Int, cell: Cell): Unit = {
    val i = this.index_of(x, y)
    content(i) = cell
  }

  /// Returns the index in the Vec<Cell> for the given global (x, y) coordinates.
  ///
  /// Global coordinates are offset by the Buffer's area offset (`x`/`y`).
  ///
  /// # Examples
  ///
  /// ```
  /// # use tui::buffer::Buffer;
  /// # use tui::layout::Rect;
  /// let rect = Rect::new(200, 100, 10, 10);
  /// let buffer = Buffer::empty(rect);
  /// // Global coordinates to the top corner of this buffer's area
  /// assert_eq!(buffer.index_of(200, 100), 0);
  /// ```
  ///
  /// # Panics
  ///
  /// Panics when given an coordinate that is outside of this Buffer's area.
  ///
  /// ```should_panic
  /// # use tui::buffer::Buffer;
  /// # use tui::layout::Rect;
  /// let rect = Rect::new(200, 100, 10, 10);
  /// let buffer = Buffer::empty(rect);
  /// // Top coordinate is outside of the buffer in global coordinate space, as the Buffer's area
  /// // starts at (200, 100).
  /// buffer.index_of(0, 0); // Panics
  /// ```

  def index_of(x: Int, y: Int): Int = {
    debug_assert(
      x >= this.area.left
        && x < this.area.right
        && y >= this.area.top
        && y < this.area.bottom,
      "Trying to access position outside the buffer: x={}, y={}, area={:?}",
      x,
      y,
      this.area
    )
    (y - this.area.y) * this.area.width + (x - this.area.x)
  }

  /// Returns the (global) coordinates of a cell given its index
  ///
  /// Global coordinates are offset by the Buffer's area offset (`x`/`y`).
  ///
  /// # Examples
  ///
  /// ```
  /// # use tui::buffer::Buffer;
  /// # use tui::layout::Rect;
  /// let rect = Rect::new(200, 100, 10, 10);
  /// let buffer = Buffer::empty(rect);
  /// assert_eq!(buffer.pos_of(0), (200, 100));
  /// assert_eq!(buffer.pos_of(14), (204, 101));
  /// ```
  ///
  /// # Panics
  ///
  /// Panics when given an index that is outside the Buffer's content.
  ///
  /// ```should_panic
  /// # use tui::buffer::Buffer;
  /// # use tui::layout::Rect;
  /// let rect = Rect::new(0, 0, 10, 10); // 100 cells in total
  /// let buffer = Buffer::empty(rect);
  /// // Index 100 is the 101th cell, which lies outside of the area of this Buffer.
  /// buffer.pos_of(100); // Panics
  /// ```
  def pos_of(i: Int): (Int, Int) = {
    debug_assert(
      i < this.content.length,
      "Trying to get the coords of a cell outside the buffer: i={} len={}",
      i,
      this.content.length
    )
    (
      this.area.x + i % this.area.width,
      this.area.y + i / this.area.width
    )
  }

  /// Print a string, starting at the position (x, y)
  def set_string(x: Int, y: Int, string: String, style: Style): (Int, Int) =
    set_stringn(x, y, string, Int.MaxValue, style)

  /// Print at most the first n characters of a string if enough space is available
  /// until the end of the line
  def set_stringn(x: Int, y: Int, string: String, width: Int, style: Style): (Int, Int) = {
    var index = this.index_of(x, y)
    var x_offset = x
    val graphemes = UnicodeSegmentation.graphemes(string, true)
    val max_offset = math.min(this.area.right, width + x)

    graphemes.breakableForeach { case (s, _) =>
      if (s.width == 0) breakableForeach.Continue
      // `x_offset + width > max_offset` could be integer overflow on 32-bit machines if we
      // change dimenstions to usize or u32 and someone resizes the terminal to 1x2^32.
      else if (s.width > max_offset - x_offset) {
        breakableForeach.Break
      } else {
        content(index).set_symbol(s).set_style(style)

        // Reset following cells if multi-width (they would be hidden by the grapheme),
        ranges.range(index + 1, index + s.width)(i => content(i).reset())
        index += s.width
        x_offset += s.width
        breakableForeach.Continue
      }
    }

    (x_offset, y)
  }

  def set_spans(_x: Int, y: Int, spans: Spans, width: Int): (Int, Int) = {
    var remaining_width = width
    var x = _x

    spans.spans.foreach {
      case _ if remaining_width == 0 =>
        () // break
      case span =>
        val (newX, _) = set_stringn(x, y, span.content, remaining_width, span.style)
        val w = newX - x
        x = newX
        remaining_width = remaining_width - w;
    }
    (x, y)
  }

  def set_span(x: Int, y: Int, span: Span, width: Int): (Int, Int) =
    set_stringn(x, y, span.content, width, span.style)

  def set_style(area: Rect, style: Style): Unit = {
    var y = area.top
    while (y < area.bottom) {
      var x = area.left
      while (x < area.right) {
        this.get(x, y).set_style(style)
        x += 1
      }
      y += 1
    }
  }

  /// Resize the buffer so that the mapped area matches the given area and that the buffer
  /// length is equal to area.width * area.height
  def resize(area: Rect): Unit = {
    val length = area.area
    val newContent = content.take(length) ++ Array.fill(length - content.length)(Cell.default)
    content = newContent
    this.area = area
  }

  /// Reset all cells in the buffer
  def reset(): Unit =
    content.foreach(_.reset())

  /// Merge an other buffer into this one
  def merge(other: Buffer): Unit = {
    val newArea = area.union(other.area)
    val newContent = Array.fill(newArea.area)(Cell.default)

    val thisSize = area.area
    var thisI = thisSize - 1
    while (thisI >= 0) {
      val (x, y) = pos_of(thisI)
      // index in new content
      val k = (y - newArea.y) * newArea.width + x - newArea.x
      newContent(k) = content(thisI).clone()
      thisI -= 1
    }

    // Push content of the other buffer into this one (may erase previous data)
    val otherSize = other.area.area
    var otherI = 0
    while (otherI < otherSize) {
      val (x, y) = other.pos_of(otherI)
      // index in new content
      val k = (y - newArea.y) * newArea.width + x - newArea.x
      newContent(k) = other.content(otherI).clone()

      otherI += 1
    }
    this.area = newArea
    this.content = newContent
  }

  /// Builds a minimal sequence of coordinates and Cells necessary to update the UI from
  /// self to other.
  ///
  /// We're assuming that buffers are well-formed, that is no double-width cell is followed by
  /// a non-blank cell.
  ///
  /// # Multi-width characters handling:
  ///
  /// ```text
  /// (Index:) `01`
  /// Prev:    `コ`
  /// Next:    `aa`
  /// Updates: `0: a, 1: a'
  /// ```
  ///
  /// ```text
  /// (Index:) `01`
  /// Prev:    `a `
  /// Next:    `コ`
  /// Updates: `0: コ` (double width symbol at index 0 - skip index 1)
  /// ```
  ///
  /// ```text
  /// (Index:) `012`
  /// Prev:    `aaa`
  /// Next:    `aコ`
  /// Updates: `0: a, 1: コ` (double width symbol at index 1 - skip index 2)
  /// ```
  def diff(other: Buffer): Array[(Int, Int, Cell)] = {
    val previous_buffer = content
    val next_buffer = other.content
    val width = area.width

    val updates = Array.newBuilder[(Int, Int, Cell)]
    // Cells invalidated by drawing/replacing preceeding multi-width characters:
    var invalidated = 0
    // Cells from the current buffer to skip due to preceeding multi-width characters taking their
    // place (the skipped cells should be blank anyway):
    var to_skip = 0
    var i = 0
    val max = math.min(area.area, other.area.area)
    while (i < max) {
      val current = next_buffer(i)
      val previous = previous_buffer(i)
      if ((current != previous || invalidated > 0) && to_skip == 0) {
        val x = i % width
        val y = i / width
        updates += ((x, y, next_buffer(i)))
      }

      to_skip = current.symbol.width - 1
      val affected_width = math.max(current.symbol.width, previous.symbol.width)
      invalidated = math.max(affected_width, invalidated) - 1
      i += 1
    }
    updates.result()
  }
}

object Buffer {
  /// Returns a Buffer with all cells set to the default one
  def empty(area: Rect): Buffer = {
    val cell: Cell = Cell.default
    Buffer.filled(area, cell)
  }

  /// Returns a Buffer with all cells initialized with the attributes of the given Cell
  def filled(area: Rect, cell: Cell): Buffer = {
    val size = area.area
    val content = Array.fill(size)(cell.clone())
    Buffer(area, content)
  }

  /// Returns a Buffer containing the given lines
  def with_lines(lines: Array[String]): Buffer = {
    val width = lines
      .map(i => Grapheme(i).width)
      .maxOption
      .getOrElse(0)

    val buffer = Buffer.empty(Rect(x = 0, y = 0, width = width, height = lines.length))
    lines.zipWithIndex.foreach { case (line, y) =>
      buffer.set_string(0, y, line, Style());
    }
    buffer
  }
}
