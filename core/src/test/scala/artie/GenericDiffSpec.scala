package artie

import artie.implicits._

import org.specs2.mutable.Specification

final case class User(name: String, age: Int)
final case class Friends(base: User, friend: User)
final case class Group(id: Long, users: Seq[User])
final case class UserGroups(groups: Map[String, Group])
final case class UserGroupsId(groups: Map[Int, Group])
final case class ArrayOfUsers(id: Long, users: Array[User])
final case class SetOfUsers(id: Long, users: Set[User])

final class GenericDiffSpec extends Specification {

  val usr0 = User("foo", 1)
  val usr1 = User("bar", 2)
  val usr2 = User("bar", 3)

  def diff[A](l: A, r: A)(implicit d: GenericDiffRunner[A]): Seq[Diff] = d(l, r)

  "Generic difference calculation of case classes" >> {
    "simple case class" >> {
      diff(usr0, usr0) === Nil
      diff(usr0, usr1) === Seq(FieldDiff("name", "foo", "bar"), FieldDiff("age", 1, 2))
    }

    "nested case classes" >> {
      diff(Friends(usr0, usr1), Friends(usr0, usr1)) === Nil
      diff(Friends(usr0, usr1), Friends(usr0, usr2)) === Seq(
        ClassDiff("friend", Seq(FieldDiff("age", 2, 3)))
      )

      implicit val friendsIg = IgnoreFields[Friends].ignore('friend)
      diff(Friends(usr0, usr1), Friends(usr0, usr2)) === Nil
    }

    "nested case classes with sequences" >> {
      diff(Group(0L, Nil), Group(0L, Nil)) === Nil
      diff(Group(0L, Seq(usr0, usr1)), Group(0L, Seq(usr0, usr1))) === Nil
      diff(Group(0L, Seq(usr0, usr1)), Group(0L, Seq(usr0, usr2))) === Seq(
        CollectionElementsDiff(Some("users"), Seq(TotalDiff(Seq(FieldDiff("age", 2, 3)))))
      )
      diff(Group(0L, Seq(usr0, usr1)), Group(0L, Seq(usr1, usr0))) === Seq(
        CollectionElementsDiff(Some("users"), Seq(
          TotalDiff(Seq(
            FieldDiff("name", "foo", "bar"),
            FieldDiff("age", 1, 2)
          )),
          TotalDiff(Seq(
            FieldDiff("name", "bar", "foo"),
            FieldDiff("age", 2, 1)
          ))
        ))
      )
      diff(Group(0L, Seq(usr0)), Group(0L, Seq(usr0, usr1))) === Seq(
        CollectionSizeDiff(Some("users"), 1, 2),
        CollectionElementsDiff(Some("users"), Seq(
          MissingValue(usr1)
        ))
      )

      implicit val groupIg = IgnoreFields[Group].ignore('users)
      diff(Group(0L, Seq(usr0, usr1)), Group(0L, Seq(usr0, usr2))) === Nil
    }

    "nested case classes with arrays" >> {
      diff(ArrayOfUsers(0L, Array(usr0, usr1)), ArrayOfUsers(0L, Array(usr0, usr1))) === Nil
      diff(ArrayOfUsers(0L, Array(usr0, usr1)), ArrayOfUsers(0L, Array(usr1, usr0))) === Seq(
        CollectionElementsDiff(Some("users"), Seq(
          TotalDiff(Seq(
            FieldDiff("name", "foo", "bar"),
            FieldDiff("age", 1, 2)
          )),
          TotalDiff(Seq(
            FieldDiff("name", "bar", "foo"),
            FieldDiff("age", 2, 1)
          ))
        ))
      )

      implicit val arrayIg = IgnoreFields[ArrayOfUsers].ignore('users)
      diff(ArrayOfUsers(0L, Array(usr0, usr1)), ArrayOfUsers(0L, Array(usr1, usr0))) === Nil
    }

    "nested case classes with sets" >> {
      diff(SetOfUsers(0L, Set(usr0, usr1)), SetOfUsers(0L, Set(usr1, usr0))) === Nil
      diff(SetOfUsers(0L, Set(usr0, usr2)), SetOfUsers(0L, Set(usr1, usr0))) === Seq(
        CollectionElementsDiff(Some("users"), Seq(
          MissingValue(usr2),
          MissingValue(usr1)
        ))
      )

      implicit val setIg = IgnoreFields[SetOfUsers].ignore('users)
      diff(SetOfUsers(0L, Set(usr0, usr2)), SetOfUsers(0L, Set(usr1, usr0))) === Nil
    }

    "nested case classes with maps" >> {
      diff(UserGroups(Map.empty), UserGroups(Map.empty)) === Nil
      diff(UserGroups(Map("a" -> Group(0L, Seq(usr0, usr1)))), UserGroups(Map("a" -> Group(0L, Seq(usr0, usr1))))) === Nil
      diff(UserGroupsId(Map(1 -> Group(0L, Seq(usr0, usr1)))), UserGroupsId(Map(1 -> Group(0L, Seq(usr0, usr1))))) === Nil
      diff(UserGroups(Map("a" -> Group(0L, Seq(usr0, usr1)))), UserGroups(Map("a" -> Group(0L, Seq(usr0, usr2))))) === Seq(
        MapDiff(Some("groups"), Seq(
          "a" -> TotalDiff(Seq(CollectionElementsDiff(Some("users"), Seq(TotalDiff(Seq(FieldDiff("age", 2, 3)))))))
        ))
      )
      diff(UserGroups(Map("a" -> Group(0L, Seq(usr0)))), UserGroups(Map.empty)) === Seq(
        CollectionSizeDiff(Some("groups"), 1, 0),
        MapDiff(Some("groups"), Seq(
          "a" -> MissingValue(Group(0L, Seq(usr0)))
        ))
      )

      implicit val mapIg = IgnoreFields[UserGroups].ignore('groups)
      diff(UserGroups(Map("a" -> Group(0L, Seq(usr0, usr1)))), UserGroups(Map("a" -> Group(0L, Seq(usr0, usr2))))) === Nil
    }

    "sequence of case classes" >> {
      diff(Seq.empty[User], Seq.empty[User]) === Nil
      diff(Seq(usr0), Seq(usr0)) === Nil
      diff(Seq(usr0), Seq(usr1)) === Seq(CollectionElementsDiff(None, Seq(TotalDiff(Seq(FieldDiff("name", "foo", "bar"), FieldDiff("age", 1, 2))))))
    }

    "array of case classes" >> {
      // relies on seqGenDiff
      diff(Array.empty[User], Array.empty[User]) === Nil
      diff(Array(usr0), Array(usr0)) === Nil
      diff(Array(usr0), Array(usr1)) === Seq(CollectionElementsDiff(None, Seq(TotalDiff(Seq(FieldDiff("name", "foo", "bar"), FieldDiff("age", 1, 2))))))
    }

    "map of case classes" >> {
      diff(Map.empty[Long, User], Map.empty[Long, User]) === Nil
      diff(Map(0L -> usr0), Map(0L -> usr0)) === Nil
      diff(Map(0L -> usr0), Map(0L -> usr1)) === Seq(MapDiff(None, Seq(("0", TotalDiff(Seq(FieldDiff("name", "foo", "bar"), FieldDiff("age", 1, 2)))))))
    }
  }
}
