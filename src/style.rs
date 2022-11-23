//! `style` contains the primitives used to control how your user interface will look.

use core::fmt;
use std::ops;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub enum Color {
    Reset,
    Black,
    Red,
    Green,
    Yellow,
    Blue,
    Magenta,
    Cyan,
    Gray,
    DarkGray,
    LightRed,
    LightGreen,
    LightYellow,
    LightBlue,
    LightMagenta,
    LightCyan,
    White,
    Rgb(u8, u8, u8),
    Indexed(u8),
}

/*  Modifier changes the way a piece of text is displayed.
 *
 *  They are bitflags so they can easily be composed.
 *
 *  ## Examples
 *
 *  ```rust
 *  # use tui::style::Modifier;
 *
 *  let m = Modifier::BOLD | Modifier::ITALIC;
 *  ```
 */
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
#[derive(Copy, PartialEq, Eq, Clone, PartialOrd, Ord, Hash)]
pub struct Modifier {
    bits: u16,
}

impl fmt::Debug for Modifier {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        let mut first = true;
        if Modifier::BOLD.contains(*self) {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("BOLD")?;
        }
        if Modifier::DIM.contains(*self) {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("DIM")?;
        }
        if Modifier::ITALIC.contains(*self) {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("ITALIC")?;
        }
        if Modifier::UNDERLINED.contains(*self) {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("UNDERLINED")?;
        }
        if Modifier::SLOW_BLINK.contains(*self) {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("SLOW_BLINK")?;
        }
        if Modifier::RAPID_BLINK.contains(*self) {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("RAPID_BLINK")?;
        }
        if Modifier::REVERSED.contains(*self) {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("REVERSED")?;
        }
        if Modifier::HIDDEN.contains(*self) {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("HIDDEN")?;
        }
        if Modifier::CROSSED_OUT.contains(*self) {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("CROSSED_OUT")?;
        }
        let extra_bits = self.bits & !Modifier::all().bits();
        if extra_bits != 0 {
            if !first {
                f.write_str(" | ")?;
            }
            first = false;
            f.write_str("0x")?;
            fmt::LowerHex::fmt(&extra_bits, f)?;
        }
        if first {
            f.write_str("(empty)")?;
        }
        Ok(())
    }
}

#[allow(dead_code)]
impl Modifier {
    pub const BOLD: Modifier = Modifier {
        bits: 0b0000_0000_0001,
    };
    pub const DIM: Modifier = Modifier {
        bits: 0b0000_0000_0010,
    };
    pub const ITALIC: Modifier = Modifier {
        bits: 0b0000_0000_0100,
    };
    pub const UNDERLINED: Modifier = Modifier {
        bits: 0b0000_0000_1000,
    };
    pub const SLOW_BLINK: Modifier = Modifier {
        bits: 0b0000_0001_0000,
    };
    pub const RAPID_BLINK: Modifier = Modifier {
        bits: 0b0000_0010_0000,
    };
    pub const REVERSED: Modifier = Modifier {
        bits: 0b0000_0100_0000,
    };
    pub const HIDDEN: Modifier = Modifier {
        bits: 0b0000_1000_0000,
    };
    pub const CROSSED_OUT: Modifier = Modifier {
        bits: 0b0001_0000_0000,
    };

    ///   Returns an empty set of flags.
    #[inline]
    pub const fn empty() -> Modifier {
        Modifier { bits: 0 }
    }

    ///   Returns the set containing all flags.
    #[inline]
    pub const fn all() -> Modifier {
        Modifier {
            bits: 0b0001_1111_1111,
        }
    }

    ///   Returns the raw value of the flags currently stored.
    #[inline]
    pub const fn bits(&self) -> u16 {
        self.bits
    }

    ///   Convert from underlying bit representation, unless that
    ///   representation contains bits that do not correspond to a flag.
    #[inline]
    pub const fn from_bits(bits: u16) -> Option<Modifier> {
        if (bits & !Modifier::all().bits()) == 0 {
            Some(Modifier { bits })
        } else {
            None
        }
    }

    ///   Convert from underlying bit representation, dropping any bits
    ///   that do not correspond to flags.
    #[inline]
    pub const fn from_bits_truncate(bits: u16) -> Modifier {
        Modifier {
            bits: bits & Modifier::all().bits,
        }
    }

    ///   Convert from underlying bit representation, preserving all
    ///   bits (even those not corresponding to a defined flag).
    ///
    ///   # Safety
    ///
    ///   The caller of the  `bitflags!`  macro can chose to allow or
    ///   disallow extra bits for their bitflags type.
    ///
    ///   The caller of  `from_bits_unchecked()`  has to ensure that
    ///   all bits correspond to a defined flag or that extra bits
    ///   are valid for this bitflags type.
    #[inline]
    pub const unsafe fn from_bits_unchecked(bits: u16) -> Modifier {
        Modifier { bits }
    }

    ///   Returns  `true`  if no flags are currently stored.
    #[inline]
    pub const fn is_empty(&self) -> bool {
        self.bits() == Modifier::empty().bits()
    }

    ///   Returns  `true`  if all flags are currently set.
    #[inline]
    pub const fn is_all(&self) -> bool {
        Modifier::all().bits | self.bits == self.bits
    }

    ///   Returns  `true`  if there are flags common to both  `self`  and  `other` .
    #[inline]
    pub const fn intersects(&self, other: Modifier) -> bool {
        !(Modifier {
            bits: self.bits & other.bits,
        })
        .is_empty()
    }

    ///   Returns  `true`  if all of the flags in  `other`  are contained within  `self` .
    #[inline]
    pub const fn contains(&self, other: Modifier) -> bool {
        other != Modifier::EMPTY && (self.bits & other.bits) == other.bits
    }

    ///   Inserts the specified flags in-place.
    #[inline]
    pub fn insert(&mut self, other: Modifier) {
        self.bits |= other.bits;
    }

    ///   Removes the specified flags in-place.
    #[inline]
    pub fn remove(&mut self, other: Modifier) {
        self.bits &= !other.bits;
    }

    ///   Returns the intersection between the flags in  `self`  and
    ///   `other` .
    ///
    ///   Specifically, the returned set contains only the flags which are
    ///   present in  *both*   `self`   *and*   `other` .
    ///
    ///   This is equivalent to using the  `&`  operator (e.g.
    ///   [ `ops::BitAnd` ] ), as in  `flags & other` .
    ///
    ///   [`ops::BitAnd`] :  https://doc.rust-lang.org/std/ops/trait.BitAnd.html
    #[inline]
    #[must_use]
    pub const fn intersection(self, other: Modifier) -> Modifier {
        Modifier {
            bits: self.bits & other.bits,
        }
    }

    ///   Returns the union of between the flags in  `self`  and  `other` .
    ///
    ///   Specifically, the returned set contains all flags which are
    ///   present in  *either*   `self`   *or*   `other` , including any which are
    ///   present in both (see  [ `Modifier::symmetric_difference` ]  if that
    ///   is undesirable).
    ///
    ///   This is equivalent to using the  `|`  operator (e.g.
    ///   [ `ops::BitOr` ] ), as in  `flags | other` .
    ///
    ///   [`ops::BitOr`] :  https://doc.rust-lang.org/std/ops/trait.BitOr.html
    #[inline]
    #[must_use]
    pub const fn union(self, other: Modifier) -> Modifier {
        Modifier {
            bits: self.bits | other.bits,
        }
    }

    ///   Returns the difference between the flags in  `self`  and  `other` .
    ///
    ///   Specifically, the returned set contains all flags present in
    ///   `self` , except for the ones present in  `other` .
    ///
    ///   It is also conceptually equivalent to the "bit-clear" operation:
    ///   `flags & !other`  (and this syntax is also supported).
    ///
    ///   This is equivalent to using the  `-`  operator (e.g.
    ///   [ `ops::Sub` ] ), as in  `flags - other` .
    ///
    ///   [`ops::Sub`] :  https://doc.rust-lang.org/std/ops/trait.Sub.html
    #[inline]
    #[must_use]
    pub const fn difference(self, other: Modifier) -> Modifier {
        Modifier {
            bits: self.bits & !other.bits,
        }
    }

    ///   Returns the  [symmetric difference] [sym-diff]  between the flags
    ///   in  `self`  and  `other` .
    ///
    ///   Specifically, the returned set contains the flags present which
    ///   are present in  `self`  or  `other` , but that are not present in
    ///   both. Equivalently, it contains the flags present in  *exactly
    ///   one*  of the sets  `self`  and  `other` .
    ///
    ///   This is equivalent to using the  `^`  operator (e.g.
    ///   [ `ops::BitXor` ] ), as in  `flags ^ other` .
    ///
    ///   [sym-diff] :  https://en.wikipedia.org/wiki/Symmetric_difference
    ///   [`ops::BitXor`] :  https://doc.rust-lang.org/std/ops/trait.BitXor.html
    #[inline]
    #[must_use]
    pub const fn symmetric_difference(self, other: Modifier) -> Modifier {
        Modifier {
            bits: self.bits ^ other.bits,
        }
    }

    ///   Returns the complement of this set of flags.
    ///
    ///   Specifically, the returned set contains all the flags which are
    ///   not set in  `self` , but which are allowed for this type.
    ///
    ///   Alternatively, it can be thought of as the set difference
    ///   between  [ `Modifier::all()` ]  and  `self`  (e.g.  `Modifier::all() - self` )
    ///
    ///   This is equivalent to using the  `!`  operator (e.g.
    ///   [ `ops::Not` ] ), as in  `!flags` .
    ///
    ///   [`Modifier::all()`] :  Modifier::all
    ///   [`ops::Not`] :  https://doc.rust-lang.org/std/ops/trait.Not.html
    #[inline]
    #[must_use]
    pub const fn complement(self) -> Modifier {
        Modifier::from_bits_truncate(!self.bits)
    }
}
impl ops::BitOr for Modifier {
    type Output = Modifier;

    ///   Returns the union of the two sets of flags.
    #[inline]
    fn bitor(self, other: Modifier) -> Modifier {
        Modifier {
            bits: self.bits | other.bits,
        }
    }
}

impl ops::BitXor for Modifier {
    type Output = Modifier;

    ///   Returns the left flags, but with all the right flags toggled.
    #[inline]
    fn bitxor(self, other: Modifier) -> Modifier {
        Modifier {
            bits: self.bits ^ other.bits,
        }
    }
}

impl ops::BitAnd for Modifier {
    type Output = Modifier;

    ///   Returns the intersection between the two sets of flags.
    #[inline]
    fn bitand(self, other: Modifier) -> Modifier {
        Modifier {
            bits: self.bits & other.bits,
        }
    }
}

impl ops::Sub for Modifier {
    type Output = Modifier;

    ///   Returns the set difference of the two sets of flags.
    #[inline]
    fn sub(self, other: Modifier) -> Modifier {
        Modifier {
            bits: self.bits & !other.bits,
        }
    }
}
impl ops::Not for Modifier {
    type Output = Modifier;

    ///   Returns the complement of this set of flags.
    #[inline]
    fn not(self) -> Modifier {
        Modifier { bits: !self.bits } & Modifier::all()
    }
}

/// Style let you control the main characteristics of the displayed elements.
///
/// ```rust
/// # use tui::style::{Color, Modifier, Style};
/// Style::default()
///     .fg(Color::Black)
///     .bg(Color::Green)
///     .add_modifier(Modifier::ITALIC | Modifier::BOLD);
/// ```
///
/// It represents an incremental change. If you apply the styles S1, S2, S3 to a cell of the
/// terminal buffer, the style of this cell will be the result of the merge of S1, S2 and S3, not
/// just S3.
///
/// ```rust
/// # use tui::style::{Color, Modifier, Style};
/// # use tui::buffer::Buffer;
/// # use tui::layout::Rect;
/// let styles = [
///     Style::default().fg(Color::Blue).add_modifier(Modifier::BOLD | Modifier::ITALIC),
///     Style::default().bg(Color::Red),
///     Style::default().fg(Color::Yellow).remove_modifier(Modifier::ITALIC),
/// ];
/// let mut buffer = Buffer::empty(Rect::new(0, 0, 1, 1));
/// for style in &styles {
///   buffer.get_mut(0, 0).set_style(*style);
/// }
/// assert_eq!(
///     Style {
///         fg: Some(Color::Yellow),
///         bg: Some(Color::Red),
///         add_modifier: Modifier::BOLD,
///         sub_modifier: Modifier::empty(),
///     },
///     buffer.get(0, 0).style(),
/// );
/// ```
///
/// The default implementation returns a `Style` that does not modify anything. If you wish to
/// reset all properties until that point use [`Style::reset`].
///
/// ```
/// # use tui::style::{Color, Modifier, Style};
/// # use tui::buffer::Buffer;
/// # use tui::layout::Rect;
/// let styles = [
///     Style::default().fg(Color::Blue).add_modifier(Modifier::BOLD | Modifier::ITALIC),
///     Style::reset().fg(Color::Yellow),
/// ];
/// let mut buffer = Buffer::empty(Rect::new(0, 0, 1, 1));
/// for style in &styles {
///   buffer.get_mut(0, 0).set_style(*style);
/// }
/// assert_eq!(
///     Style {
///         fg: Some(Color::Yellow),
///         bg: Some(Color::Reset),
///         add_modifier: Modifier::empty(),
///         sub_modifier: Modifier::empty(),
///     },
///     buffer.get(0, 0).style(),
/// );
/// ```
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[cfg_attr(feature = "serde", derive(serde::Serialize, serde::Deserialize))]
pub struct Style {
    pub fg: Option<Color>,
    pub bg: Option<Color>,
    pub add_modifier: Modifier,
    pub sub_modifier: Modifier,
}

impl Default for Style {
    fn default() -> Style {
        Style {
            fg: None,
            bg: None,
            add_modifier: Modifier::empty(),
            sub_modifier: Modifier::empty(),
        }
    }
}

impl Style {
    /// Returns a `Style` resetting all properties.
    pub fn reset() -> Style {
        Style {
            fg: Some(Color::Reset),
            bg: Some(Color::Reset),
            add_modifier: Modifier::empty(),
            sub_modifier: Modifier::all(),
        }
    }

    /// Changes the foreground color.
    ///
    /// ## Examples
    ///
    /// ```rust
    /// # use tui::style::{Color, Style};
    /// let style = Style::default().fg(Color::Blue);
    /// let diff = Style::default().fg(Color::Red);
    /// assert_eq!(style.patch(diff), Style::default().fg(Color::Red));
    /// ```
    pub fn fg(mut self, color: Color) -> Style {
        self.fg = Some(color);
        self
    }

    /// Changes the background color.
    ///
    /// ## Examples
    ///
    /// ```rust
    /// # use tui::style::{Color, Style};
    /// let style = Style::default().bg(Color::Blue);
    /// let diff = Style::default().bg(Color::Red);
    /// assert_eq!(style.patch(diff), Style::default().bg(Color::Red));
    /// ```
    pub fn bg(mut self, color: Color) -> Style {
        self.bg = Some(color);
        self
    }

    /// Changes the text emphasis.
    ///
    /// When applied, it adds the given modifier to the `Style` modifiers.
    ///
    /// ## Examples
    ///
    /// ```rust
    /// # use tui::style::{Color, Modifier, Style};
    /// let style = Style::default().add_modifier(Modifier::BOLD);
    /// let diff = Style::default().add_modifier(Modifier::ITALIC);
    /// let patched = style.patch(diff);
    /// assert_eq!(patched.add_modifier, Modifier::BOLD | Modifier::ITALIC);
    /// assert_eq!(patched.sub_modifier, Modifier::empty());
    /// ```
    pub fn add_modifier(mut self, modifier: Modifier) -> Style {
        self.sub_modifier.remove(modifier);
        self.add_modifier.insert(modifier);
        self
    }

    /// Changes the text emphasis.
    ///
    /// When applied, it removes the given modifier from the `Style` modifiers.
    ///
    /// ## Examples
    ///
    /// ```rust
    /// # use tui::style::{Color, Modifier, Style};
    /// let style = Style::default().add_modifier(Modifier::BOLD | Modifier::ITALIC);
    /// let diff = Style::default().remove_modifier(Modifier::ITALIC);
    /// let patched = style.patch(diff);
    /// assert_eq!(patched.add_modifier, Modifier::BOLD);
    /// assert_eq!(patched.sub_modifier, Modifier::ITALIC);
    /// ```
    pub fn remove_modifier(mut self, modifier: Modifier) -> Style {
        self.add_modifier.remove(modifier);
        self.sub_modifier.insert(modifier);
        self
    }

    /// Results in a combined style that is equivalent to applying the two individual styles to
    /// a style one after the other.
    ///
    /// ## Examples
    /// ```
    /// # use tui::style::{Color, Modifier, Style};
    /// let style_1 = Style::default().fg(Color::Yellow);
    /// let style_2 = Style::default().bg(Color::Red);
    /// let combined = style_1.patch(style_2);
    /// assert_eq!(
    ///     Style::default().patch(style_1).patch(style_2),
    ///     Style::default().patch(combined));
    /// ```
    pub fn patch(mut self, other: Style) -> Style {
        self.fg = other.fg.or(self.fg);
        self.bg = other.bg.or(self.bg);

        self.add_modifier.remove(other.sub_modifier);
        self.add_modifier.insert(other.add_modifier);
        self.sub_modifier.remove(other.add_modifier);
        self.sub_modifier.insert(other.sub_modifier);

        self
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn styles() -> Vec<Style> {
        vec![
            Style::default(),
            Style::default().fg(Color::Yellow),
            Style::default().bg(Color::Yellow),
            Style::default().add_modifier(Modifier::BOLD),
            Style::default().remove_modifier(Modifier::BOLD),
            Style::default().add_modifier(Modifier::ITALIC),
            Style::default().remove_modifier(Modifier::ITALIC),
            Style::default().add_modifier(Modifier::ITALIC | Modifier::BOLD),
            Style::default().remove_modifier(Modifier::ITALIC | Modifier::BOLD),
        ]
    }

    #[test]
    fn combined_patch_gives_same_result_as_individual_patch() {
        let styles = styles();
        for &a in &styles {
            for &b in &styles {
                for &c in &styles {
                    for &d in &styles {
                        let combined = a.patch(b.patch(c.patch(d)));

                        assert_eq!(
                            Style::default().patch(a).patch(b).patch(c).patch(d),
                            Style::default().patch(combined)
                        );
                    }
                }
            }
        }
    }
}
