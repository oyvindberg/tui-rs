package tui
package examples
package table

import tui.crossterm.CrosstermJni
import tui.layout.{Constraint, Layout, Margin}
import tui.terminal.{Frame, Terminal}
import tui.text.{Spans, Text}
import tui.widgets._

object App {
  val items = Array(
    Array("Row11", "Row12", "Row13"),
    Array("Row21", "Row22", "Row23"),
    Array("Row31", "Row32", "Row33"),
    Array("Row41", "Row42", "Row43"),
    Array("Row51", "Row52", "Row53"),
    Array("Row61", "Row62\nTest", "Row63"),
    Array("Row71", "Row72", "Row73"),
    Array("Row81", "Row82", "Row83"),
    Array("Row91", "Row92", "Row93"),
    Array("Row101", "Row102", "Row103"),
    Array("Row111", "Row112", "Row113"),
    Array("Row121", "Row122", "Row123"),
    Array("Row131", "Row132", "Row133"),
    Array("Row141", "Row142", "Row143"),
    Array("Row151", "Row152", "Row153"),
    Array("Row161", "Row162", "Row163"),
    Array("Row171", "Row172", "Row173"),
    Array("Row181", "Row182", "Row183"),
    Array("Row191", "Row192", "Row193")
  )
}

case class App(
    state: TableState,
    items: Array[Array[String]]
) {
  def next(): Unit = {
    val i = state.selected match {
      case Some(i) =>
        if (i >= items.length - 1) {
          0
        } else {
          i + 1
        }

      case None => 0
    }
    state.select(Some(i))
  }

  def previous(): Unit = {
    val i = state.selected match {
      case Some(i) =>
        if (i == 0) {
          items.length - 1
        } else {
          i - 1
        }
      case None => 0
    }
    state.select(Some(i))
  }
}

object Main {
  def main(args: Array[String]): Unit = withTerminal { (jni, terminal) =>
    // create app and run it
    val app = App(state = TableState(), items = App.items)
    run_app(terminal, app, jni)
  }

  def run_app(terminal: Terminal, app: App, jni: CrosstermJni): Unit =
    while (true) {
      terminal.draw(f => ui(f, app))

      jni.read() match {
        case key: tui.crossterm.Event.Key =>
          key.keyEvent.code match {
            case char: tui.crossterm.KeyCode.Char if char.c() == 'q' => return
            case _: tui.crossterm.KeyCode.Down                       => app.next()
            case _: tui.crossterm.KeyCode.Up                         => app.previous()
            case _                                                   => ()
          }
        case _ => ()
      }
    }

  def ui(f: Frame, app: App): Unit = {
    val rects = Layout(constraints = Array(Constraint.Percentage(100)), margin = Margin(5)).split(f.size)

    val selected_style = Style(add_modifier = Modifier.REVERSED)
    val normal_style = Style(bg = Some(Color.Blue))
    val header_cells = Array("Header1", "Header2", "Header3").map(h => Cell(Text.raw(h), style = Style(fg = Some(Color.Red))))
    val header = Row(cells = header_cells, style = normal_style, bottom_margin = 1)

    val rows = app.items.map { item =>
      val height = item.map(_.count(_ == '\n')).maxOption.getOrElse(0) + 1
      val cells = item.map(c => Cell(Text.raw(c)))
      Row(cells, height = height, bottom_margin = 1)
    }

    val t = Table(
      rows = rows,
      header = Some(header),
      block = Some(Block(borders = Borders.ALL, title = Some(Spans.from("Table")))),
      highlight_style = selected_style,
      highlight_symbol = Some(">> "),
      widths = Array(Constraint.Percentage(50), Constraint.Length(30), Constraint.Min(10))
    )
    f.render_stateful_widget(t, rects(0))(app.state)
  }
}
