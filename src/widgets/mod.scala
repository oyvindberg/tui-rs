package tui
package widgets

import tui.buffer.Buffer
import tui.layout.Rect

//! `widgets` is a collection of types that implement [`Widget`] or [`StatefulWidget`] or both.
//!
//! All widgets are implemented using the builder pattern and are consumable objects. They are not
//! meant to be stored but used as *commands* to draw common figures in the UI.
//!
//! The available widgets are:
//! - [`Block`]
//! - [`Tabs`]
//! - [`List`]
//! - [`Table`]
//! - [`Paragraph`]
//! - [`Chart`]
//! - [`BarChart`]
//! - [`Gauge`]
//! - [`Sparkline`]
//! - [`Clear`]

/// Bitflags that can be composed to set the visible borders essentially on the block widget.
case class Borders(bits: Int) {
  def fmt(sb: StringBuilder): Unit = {
    var first = true;
    if (Borders.NONE.contains(this)) {
      if (!first) {
        sb.append(" | ");
      }
      first = false;
      sb.append("NONE");
    }
    if (Borders.TOP.contains(this)) {
      if (!first) {
        sb.append(" | ")
      }
      first = false;
      sb.append("TOP")
    }
    if (Borders.RIGHT.contains(this)) {
      if (!first) {
        sb.append(" | ")
      }
      first = false;
      sb.append("RIGHT")
    }
    if (Borders.BOTTOM.contains(this)) {
      if (!first) {
        sb.append(" | ")
      }
      first = false;
      sb.append("BOTTOM")
    }
    if (Borders.LEFT.contains(this)) {
      if (!first) {
        sb.append(" | ")
      }
      first = false;
      sb.append("LEFT")
    }
    if (first) {
      sb.append("(empty)")
    }
    ()
  }

  override def toString: String = {
    val sb = new StringBuilder()
    fmt(sb)
    sb.toString()
  }

  /// Returns `true` if all of the flags in `other` are contained within `self`.
  def contains(other: Borders): Boolean =
    other != Borders.EMPTY && (bits & other.bits) == other.bits

  def intersects(other: Borders): Boolean =
    Borders.EMPTY.bits != (bits & other.bits)

  /// Inserts the specified flags in-place.
  def insert(other: Borders): Borders =
    copy(bits = bits | other.bits)

  /// Removes the specified flags in-place.
  def remove(other: Borders): Borders =
    copy(bits = bits & Integer.reverse(other.bits))

  def |(other: Borders): Borders =
    copy(bits = bits | other.bits)

  def -(other: Borders): Borders =
    remove(other)
}
object Borders {
  /// Show no border (default)
  val NONE = Borders(1 << 0)
  /// Show the top border
  val TOP = Borders(1 << 1)
  /// Show the right border
  val RIGHT = Borders(1 << 2)
  /// Show the bottom border
  val BOTTOM = Borders(1 << 3)
  /// Show the left border
  val LEFT = Borders(1 << 4)
  /// Returns an empty set of flags.
  val EMPTY = Borders(bits = 0)
  /// Show all borders
  val ALL = List(TOP, RIGHT, BOTTOM, LEFT).reduce(_ | _)
}

/// Base requirements for a Widget
trait Widget {
  /// Draws the current state of the widget in the given buffer. That is the only method required
  /// to implement a custom widget.
  def render(area: Rect, buf: Buffer): Unit
}

/// A `StatefulWidget` is a widget that can take advantage of some local state to remember things
/// between two draw calls.
///
/// Most widgets can be drawn directly based on the input parameters. However, some features may
/// require some kind of associated state to be implemented.
///
/// For example, the [`List`] widget can highlight the item currently selected. This can be
/// translated in an offset, which is the number of elements to skip in order to have the selected
/// item within the viewport currently allocated to this widget. The widget can therefore only
/// provide the following behavior: whenever the selected item is out of the viewport scroll to a
/// predefined position (making the selected item the last viewable item or the one in the middle
/// for example). Nonetheless, if the widget has access to the last computed offset then it can
/// implement a natural scrolling experience where the last offset is reused until the selected
/// item is out of the viewport.
///
/// ## Examples
///
/// ```rust,no_run
/// # use std::io;
/// # use tui::Terminal;
/// # use tui::backend::{Backend, TestBackend};
/// # use tui::widgets::{Widget, List, ListItem, ListState};
///
/// // Let's say we have some events to display.
/// struct Events {
///     // `items` is the state managed by your application.
///     items: Vec<String>,
///     // `state` is the state that can be modified by the UI. It stores the index of the selected
///     // item as well as the offset computed during the previous draw call (used to implement
///     // natural scrolling).
///     state: ListState
/// }
///
/// impl Events {
///     fn new(items: Vec<String>) -> Events {
///         Events {
///             items,
///             state: ListState::default(),
///         }
///     }
///
///     pub fn set_items(&mut self, items: Vec<String>) {
///         self.items = items;
///         // We reset the state as the associated items have changed. This effectively reset
///         // the selection as well as the stored offset.
///         self.state = ListState::default();
///     }
///
///     // Select the next item. This will not be reflected until the widget is drawn in the
///     // `Terminal::draw` callback using `Frame::render_stateful_widget`.
///     pub fn next(&mut self) {
///         let i = match self.state.selected() {
///             Some(i) => {
///                 if i >= self.items.len() - 1 {
///                     0
///                 } else {
///                     i + 1
///                 }
///             }
///             None => 0,
///         };
///         self.state.select(Some(i));
///     }
///
///     // Select the previous item. This will not be reflected until the widget is drawn in the
///     // `Terminal::draw` callback using `Frame::render_stateful_widget`.
///     pub fn previous(&mut self) {
///         let i = match self.state.selected() {
///             Some(i) => {
///                 if i == 0 {
///                     self.items.len() - 1
///                 } else {
///                     i - 1
///                 }
///             }
///             None => 0,
///         };
///         self.state.select(Some(i));
///     }
///
///     // Unselect the currently selected item if any. The implementation of `ListState` makes
///     // sure that the stored offset is also reset.
///     pub fn unselect(&mut self) {
///         self.state.select(None);
///     }
/// }
///
/// # let backend = TestBackend::new(5, 5);
/// # let mut terminal = Terminal::new(backend).unwrap();
///
/// let mut events = Events::new(vec![
///     String::from("Item 1"),
///     String::from("Item 2")
/// ]);
///
/// loop {
///     terminal.draw(|f| {
///         // The items managed by the application are transformed to something
///         // that is understood by tui.
///         let items: Vec<ListItem>= events.items.iter().map(|i| ListItem::new(i.as_ref())).collect();
///         // The `List` widget is then built with those items.
///         let list = List::new(items);
///         // Finally the widget is rendered using the associated state. `events.state` is
///         // effectively the only thing that we will "remember" from this draw call.
///         f.render_stateful_widget(list, f.size(), &mut events.state);
///     });
///
///     // In response to some input events or an external http request or whatever:
///     events.next();
/// }
/// ```
trait StatefulWidget {
  type State;
  def render(area: Rect, buf: Buffer, state: State): Unit
}
