import com.google.gimd._
import com.google.gimd.UserType._
import query.Query._

case class User(name: String, email: String, age: Int)
object MyUserType extends com.google.gimd.UserType[User] {
  //define binding between Messages that Gimd stores and some custom class
  val name  = FieldSpecOne("name", StringField, _.name)
  val email = FieldSpecOne("email", StringField, _.email)
  val age = FieldSpecOne("age", IntField, _.age)
  val fields = name :: email :: age :: Nil
  def toUserObject(m: Message) = User(name(m), email(m), age(m))
}

object MyUserFileType extends com.google.gimd.file.FileType[User] {
  //define file type which specifices how to store Messages corresponding to given UserType
  val pathPrefix = Some("user/")
  val pathSuffix = None
  val userType = MyUserType
  def name(m: Message) = userType.name(m)
}

val db: Database = GimdConsole.openDb("sample-repo", MyUserFileType :: Nil)

db.modifyAndReturn[String]( (s: Snapshot) => {
  val users = s.query(MyUserFileType, MyUserType.query)

  if (users.isEmpty) {
    val newUsers = for (i <- 1 to 1000) yield User("user"+i, "email"+i+"@google.com", i%91)
    val mod: modification.DatabaseModification = newUsers.foldLeft(modification.DatabaseModification.empty)( (m, user) => m.insertFile(MyUserFileType, user))
    val commitMessage: String = "Add new users"
    
    (mod, commitMessage)
  }
  else {
    val mod: modification.DatabaseModification = modification.DatabaseModification.empty
    val commitMessage: String = "No action performed"

    (mod, commitMessage)
  }
})

println("-- Querying for users with age less than 3")
db.query(MyUserFileType, MyUserType.query where { _.age < 3 }) foreach println
println("-- Querying for user with name 'user78'")
db.query(MyUserFileType, MyUserType.query where { _.name === "user78" }) foreach println

GimdConsole.closeDb()
