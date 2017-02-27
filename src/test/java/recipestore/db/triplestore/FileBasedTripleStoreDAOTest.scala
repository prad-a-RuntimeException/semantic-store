package recipestore.db.triplestore

import java.io.IOException

import com.google.common.io.Resources.getResource

object FileBasedTripleStoreDAOTest {
  private val TEST_FILE: String = "recipe.nq"
  private var tripleStoreDAO: FileBasedTripleStoreDAO = null
  try {
    val recipeStream = getResource(TEST_FILE).openStream
    tripleStoreDAO = new FileBasedTripleStoreDAO("test")
    tripleStoreDAO.populate(recipeStream)
  }
  catch {
    case e: IOException => {
      throw new RuntimeException("Failed initializing query ", e)
    }
  }
}

class FileBasedTripleStoreDAOTest extends AbstractTripleStoreDAOTest {
  def getTripleStoreDAO: TripleStoreDAO = {
    return FileBasedTripleStoreDAOTest.tripleStoreDAO
  }
}