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

import scala.util.parsing.input.Reader
import com.google.gimd.Message

/** Tiny helper to let pure Java use Parser to obtain a Message. */
class MessageParser {
  def parse(in: String)         = Parser.parse(in)
  def parse(in: java.io.Reader) = Parser.parse(in)
  def parse(in: Reader[Char])   = Parser.parse(in)
}
