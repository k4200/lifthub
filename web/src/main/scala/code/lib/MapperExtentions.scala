// Some methods used here are private[mapper], so I need to put these traits
// under net.liftweb.mapper.
package net.liftweb {
package mapper {


trait AggregateFunctions[A<:Mapper[A]] {
  self: A with MetaMapper[A] =>
  def max(field: SelectableField): Long =
    aggregateDb(dbDefaultConnectionIdentifier, "max", field, Nil :_*)
  def max(field: SelectableField, by: QueryParam[A]*): Long =
    aggregateDb(dbDefaultConnectionIdentifier, "max", field, by :_*)
  //TODO implement average, sum and others if need be.
  protected def aggregateDb(dbId: ConnectionIdentifier, func: String,
                            field: SelectableField, by: QueryParam[A]*): Long = {
    DB.use(dbId) {
      conn =>
      val bl = by.toList ::: addlQueryParams.is
      val (query, start, max) =
	addEndStuffs(addFields("SELECT %s(%s) FROM %s   "
			       .format(func, field.dbSelectString, 
                                       MapperRules.quoteTableName.vend(
                                         _dbTableNameLC)),
                               false, bl, conn),
                    bl, conn)
      DB.prepareStatement(query, conn) {
        st =>
        setStatementFields(st, bl, 1, conn)
        DB.exec(st) {
          rs =>
          if (rs.next) rs.getLong(1)
          else 0
        }
      }
    }
  }
}

}
}
