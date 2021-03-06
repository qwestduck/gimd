// Copyright (C) 2010 The Android Open Source Project
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
package com.google.gimd.lucene

import com.google.gimd.query.{Handle, Query}
import org.eclipse.jgit.treewalk.TreeWalk
import com.google.gimd.jgit.{FileTypeTreeFilter, JGitSnapshot}
import com.google.gimd.file.{File, FileType}
import org.apache.lucene.search.{Query => LQuery}

/**
 * LuceneOptimizedSnapshot is a trait that implements query optimization by using Lucene's index.
 *
 * The strategy is to use Lucene (if possible) to narrow down the set of possible files where
 * user object we are looking for are stored.
 *
 * Once this set of files is obtained regular Gimd query is applied on them. 
 */
trait LuceneOptimizedSnapshot extends JGitSnapshot {

  val luceneDb: Database

  override def query[U,W](ft: FileType[W], q: Query[U,_]): Iterator[(Handle[U],U)] = {
    val luceneQuery = QueryBuilder(q)

    def searchLucene(luceneQuery: LQuery): Iterator[File[W]] = {
      val TIMEOUT = 10000
      import Database.Search
      val paths = (luceneDb.!?(TIMEOUT, Search(luceneQuery,commit))).
              asInstanceOf[Option[List[String]]] getOrElse Nil
      if (!paths.isEmpty)
        new TreeWalkIterator(ft, treeWalkWithPaths(ft, paths))
      else
        Iterator.empty
    }
    if (luceneQuery.isEmpty)
      println("Failed to obtain Lucene query for query: \n" + q)
    val files = luceneQuery.map(lq => searchLucene(lq)) getOrElse all(ft)

    queryFiles(files, q.predicate)
  }

  private def treeWalkWithPaths[W](ft: FileType[W], paths: List[String]): TreeWalk = {
    import org.eclipse.jgit.treewalk.filter.{PathFilterGroup, AndTreeFilter}
    val treeWalk = new TreeWalk(branch.repository)
    treeWalk.reset(commit.getTree)
    treeWalk.setRecursive(true)
    val ftFilter = FileTypeTreeFilter(ft)
    val pathsFilter = PathFilterGroup.createFromStrings(java.util.Arrays.asList(paths.toArray: _*))
    treeWalk.setFilter(AndTreeFilter.create(Array(ftFilter, pathsFilter)))
    treeWalk
  }

}
