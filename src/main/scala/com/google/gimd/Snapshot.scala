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

package com.google.gimd

import file.{File, FileType}
import query.{Query, Predicate, Handle}

trait Snapshot {

  /**
   * Query database for all user objects of type U stored in files of
   * type FileType[W] satisfying predicate p.
   *
   * @deprecated use Query parameter instead
   * @throws GimdException 
   */
  @throws(classOf[GimdException])
  def query[U,W](ft: FileType[W], p: Predicate[U]): Iterator[(Handle[U],U)] =
    queryFiles(all(ft), p)

  def query[U,W](ft: FileType[W], q: Query[U,_]): Iterator[(Handle[U],U)] =
    queryFiles(all(ft), q.predicate)

  protected def queryFiles[U,W](files: Iterator[File[W]], p: Predicate[U]):
    Iterator[(Handle[U],U)] = {
    for {
      f <- files
      r <- f.query(p)
    } yield r
  }

  /**
   * @return iterator over collection of all Files that conform to passed FileType[T].
   */
  protected def all[T](fileType: FileType[T]): Iterator[File[T]]

}
