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

package com.google.gimd.jgit


import java.io.{ByteArrayInputStream, IOException, File}
import org.junit.{After, Before}
import org.spearce.jgit.lib._
abstract class AbstractJGitTestCase {

  private val repositoryPath = "test-repository/.git"

  var repository: Repository = null

  @Before protected def createRepository {
    val file = new File(repositoryPath)
    repository = new Repository(file)
    if (file.exists)
      throw new IOException("Repository already exists: " + file)
    file.mkdirs()
    repository.create()
  }

  @After protected def removeRepository {
    org.apache.commons.io.FileUtils.deleteDirectory(new File(repositoryPath))
  }

  protected def writeTextContent(text: String): ObjectId = {
    val ow = new ObjectWriter(repository)
    ow.writeBlob(text.getBytes("UTF-8"))
  }

  protected def writeMessage[U](userType: UserType[U], userObject: U): ObjectId =
    writeTextContent(userType.toMessageBuffer(userObject).readOnly.toString)

  protected def addFiles(files: List[(String, ObjectId)]): ObjectId = {
    val dc = org.spearce.jgit.dircache.DirCache.newInCore
    val builder = dc.builder
    for ((path, blobId) <- files) {
      val entry = new org.spearce.jgit.dircache.DirCacheEntry(path)
      entry.setFileMode(org.spearce.jgit.lib.FileMode.REGULAR_FILE)
      entry.setObjectId(blobId)
      builder.add(entry)
    }
    builder.finish()
    dc.writeTree(new ObjectWriter(repository))
  }

  protected def createCommit(message: String, treeId: ObjectId): ObjectId = {
    val commit = new Commit(repository, Array())
    val person = new PersonIdent("A U Thor", "author@example.com")
    commit.setAuthor(person)
    commit.setCommitter(person)
    commit.setMessage(message)
    commit.setTreeId(treeId)
    commit.commit()
    commit.getCommitId
  }

  protected def moveHEAD(commitId: ObjectId) {
    val refUpdate = repository.updateRef(Constants.HEAD)
    refUpdate.setNewObjectId(commitId)
    refUpdate.forceUpdate()
  }

}