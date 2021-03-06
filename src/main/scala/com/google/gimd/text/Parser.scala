// Copyright (C) 2009 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gimd.text

import scala.util.parsing.combinator.RegexParsers

import scala.util.parsing.input.Reader
import scala.util.parsing.input.StreamReader
import scala.util.parsing.input.CharSequenceReader
import scala.util.parsing.input.CharSequenceReader.EofCh

import com.google.gimd._

final class ParserException(msg: String) extends RuntimeException(msg)

object Parser {
  def parse(in: String)        : Message = parse(new CharSequenceReader(in))
  def parse(in: java.io.Reader): Message = parse(StreamReader(in))
  def parse(in: Reader[Char])  : Message = {
    val parser = new Parser
    parser.phrase(parser.message)(in) match {
      case parser.Success(result, _) => result
      case err => throw new ParserException(err.toString)
    }
  }
}

class Parser extends RegexParsers {
  override def skipWhitespace = false

  def message: Parser[Message] = message(0)
  def field: Parser[Field] = field(0)

  private def checkSorting(fields: List[Field]): Option[String] = fields match {
    case x :: y :: tail => if (x < y)
                             checkSorting(y :: tail)
                           else
                             Some("""|Fields X, Y do not satisfy condition X < Y where
                                     |X:
                                     |%1s
                                     |Y:
                                     |%2s""".format(x, y))
    case x :: Nil => None
    case Nil => None
  }

  private def message(level: Int): Parser[Message] = (field(level) *) into {
    case fieldList => checkSorting(fieldList) match {
                        case None => success(Message(fieldList))
                        //TODO Right now message for failure can be very big depending on contents
                        //TODO of fields that are in wrong order.
                        //TODO It would be much better to rewrite Message parser from scratch and
                        //TODO fail as soon as field that is out of order is parsed. Then it would
                        //TODO be enough just to report the line number where parsing really failed.
                        case Some(errorMsg) => failure(errorMsg)
                      }
  }

  private def field(level: Int): Parser[Field] =
    (indent(level) ~> ident <~ ' ') ~ (value(level) <~ '\n') ^^ {
      case name ~ f => f(name)
    }

  private def value(level: Int): Parser[String => Field] =
    ( "<\n" ~> message(level + 1) <~ indent(level) <~ '>' ^^ {
        case msg => x: String => Field(x, msg)
      }
    | quotedString(level) ^^ { case str => x: String => Field(x, str) }
    | numeric
    | timestamp
    | bareString ^^ { case str => x: String => Field(x, str) }
    )

  private[text] val numeric: Parser[String => NumberField] =
    // This functions tries to parse numeric value until end of line, which
    // is the end of the field value. The permitted format consists of three
    // disjoint cases, permitting decimals but disallowing "-0" as input:
    //   1. x = 0
    //   2. x in [-1, 0) or (0, 1]
    //   3. x in (-Inf, 0) or (0, Inf)
    //
    """(?:0|-?0\.(?:0?[1-9])+|-?[1-9][0-9]*(?:\.(?:0?[1-9])+)?)(?=\n)""".r ^^ {
    case s => {
      if (s.contains('.'))
        Field(_, BigDecimal(s))
      else
        try {
          val l = s.toLong
          if (Integer.MIN_VALUE <= l && l <= Integer.MAX_VALUE)
            Field(_, l.toInt)
          else
            Field(_, l)
        } catch {
          case _: NumberFormatException =>
            Field(_, BigInt(s))
        }
    }
  }

  private[text] val timestamp: Parser[String => TimestampField] = (
    // Roughly ISO 8601, see http://www.w3.org/TR/NOTE-datetime
    // and also RFC 3339.
    //
    ( """[0-9]{4}-[0-9]{2}-[0-9]{2}"""
    + """T"""
    + """[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3}"""
    + """(?:Z|[+-][0-9]{4})"""
    + """(?=\n)""").r into {
      case s => {
        try {
          val inUTC = s endsWith "Z"
          val when = TextLanguageRules.timestampFormat(inUTC).parse(s)
          val offset =
            if (inUTC)
              0
            else
              parseOffset(s.substring(s.length() - 5))
          if (offset != 0 || inUTC)
            success(Field(_, Timestamp(when, offset)))
          else
            failure("Invalid timestamp, nonzero timezone: " + s)
        } catch {
          case e: java.text.ParseException =>
            failure("Invalid timestamp " + s + ": " + e.getMessage)
        }
      }
    }
  )

  /** Parse "-0712" to -432. */
  private def parseOffset(offset: String): Int = {
    val offsetHours = offset.substring(1,3).toInt
    val offsetMins = offset.substring(3).toInt
    val sign = if (offset startsWith "-") -1 else 1
    sign * offsetHours * 60 + offsetMins
  }

  private val bareString: Parser[String] =
    chrExcept('\"', '<', '\n') ~! (chrExcept('\n') *) into {
      case first ~ rest => {
        val s = (first :: rest) mkString ""
        if (TextLanguageRules.isQuotingRequired(s))
          failure("String requires quoting: " + s)
        else
          success(s)
      }
    }

  private def quotedString(level: Int): Parser[String] =
    (quotedSingleLineString | quotedMultiLineString(level)) into {
      case s => {
        if (TextLanguageRules.isQuotingRequired(s)) {
          success(s)
        }
        else
          failure("String does not require quoting: " + s)
      }
    }

  private val quotedSingleLineString: Parser[String] =
    '"' ~> ((esc | chrExcept('\n', '\\', '"')) *) <~ '"' into {
      case list => success(list mkString "")
    }

  private def indentedLine(level: Int): Parser[String] =
    indent(level) ~> ((esc | chrExcept('\n', '\\', '"')) *) into {
      case list => success(list mkString "")
    }
  private def quotedMultiLineString(level: Int): Parser[String] =
    "\"\n" ~> ((indentedLine(level+1) <~ "\n") *) <~ (indent(level) ~! "\"") into {
      case listOfLines => success(listOfLines.mkString("\n"))
    }

  private val esc =
    ( "\\\"" ^^^ '\"'
    | "\\\\" ^^^ '\\'
    | "\\x" ~> hexDigit ~! hexDigit ^^ {
        case u ~ l => {
          val ch = hexToChar(u, l)
          if (TextLanguageRules.isNeedHexEscape(ch))
            ch
          else
            failure("Invalid character escape \\x" + u + l)
        }
      }
    )

  private def isHexDigit(ch: Char) =
    ( ('0' <= ch && ch <= '9')
    | ('a' <= ch && ch <= 'f')
    )
  private val hexDigit = elem("hex digit", isHexDigit _)
  private def hexToInt(ch: Char): Int =
    if ('0' <= ch && ch <= '9')
      ch - '0'
    else
      10 + (ch - 'a')
  private def hexToChar(u: Char, l: Char) =
    ((hexToInt(u) << 4) | hexToInt(l)).toChar

  private val ident: Parser[String] = "[a-zA-Z][a-zA-Z0-9_]*".r
  private def chrExcept(except: Char*): Parser[Char] = elem("", ch =>
    (  ch != EofCh
    && !except.contains(ch)
    && !TextLanguageRules.isNeedHexEscape(ch))
    )

  private def indent(level: Int): Parser[List[Char]] = repN(level * 2, ' ')
}
