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

import com.google.gimd.file.{FileType, File}
import com.google.gimd.text.Parser
import org.eclipse.jgit.lib.{ObjectId, Repository}
import java.io.{InputStreamReader, ByteArrayInputStream}

final class JGitFile[T](val path: String, val blobId: ObjectId, val fileType: FileType[T],
                        val branch: JGitBranch) extends File[T] {

  lazy val message = try {
    Parser.parse(new InputStreamReader(blobInputStream, "UTF-8"))
  } catch {
    case e => throw new JGitDatabaseException(branch, "Failed to parse file with path " + path, e)
  }

  lazy val userObject = fileType.userType.toUserObject(message)

  private lazy val blobInputStream = {
    val objectLoader = branch.repository.openBlob(blobId)
    if (objectLoader == null)
      throw new JGitDatabaseException(branch, "Blob '" + blobId.name + "' does not exist.")

    new ByteArrayInputStream(objectLoader.getCachedBytes)
  }

}
