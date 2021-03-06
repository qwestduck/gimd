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

package com.google.gimd.query

import com.google.gimd.{MessageField, UserType}
import com.google.gimd.file.File
import com.google.gimd.UserType
import com.google.gimd.MessageField

/** Any handle to a stored message which corresponds to user object of type T. */
abstract sealed class Handle[T]

/**
 * <p>Handle to mested message determined by path given by list of
 * MessageFields.</p>
 *
 * <p>In order to get message this handle is pointing at one should use
 * <code>path.last.value</code></p>
 *
 * <p>Path stores also all UserTypes corresponding each to Message that contains
 * given MessageField.</p>
 *
 * <p>For any non-empty path condition T == U should yield true for T,U defined by expression:
 * PathHandle[T].path.last.userType[U]</p>
 */
final case class PathHandle[+T](path: List[(UserType[_], MessageField)])

object PathHandle {
  val empty = PathHandle(Nil)
}

/**
 * <p>Complete handle consisting of file and pathHandle</p>
 *
 * <p>The top level message can be accessed using <code>file.message</code>.</p>
 */
final case class CompleteHandle[T](file: File[_],
                                   pathHandle: PathHandle[T]) extends Handle[T]
