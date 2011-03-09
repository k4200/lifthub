package net.lifthub {
package lib {

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.http.S

trait MegaCRUDify[KeyType, CrudType <: KeyedMapper[KeyType, CrudType]]
extends CRUDify[KeyType, CrudType] {
  self: CrudType with KeyedMetaMapper[KeyType, CrudType] =>

  //TODO ugly
  def enableCreate: Boolean = createMenuLoc match { case Empty =>  false ; case _ => true }
  def enableEdit: Boolean = editMenuLoc match { case Empty =>  false ; case _ => true }
  def enableDelete: Boolean = deleteMenuLoc match { case Empty =>  false ; case _ => true }

  override def _showAllTemplate =
  <lift:crud.all>
    <table id={showAllId} class={showAllClass}>
      <thead>
        <tr>
          <crud:header_item><th><crud:name/></th></crud:header_item>
          {if(enableCreate){<th>&nbsp;</th>}}
          {if(enableEdit){<th>&nbsp;</th>}}
          {if(enableDelete){<th>&nbsp;</th>}}
        </tr>
      </thead>
      <tbody>
        <crud:row>
          <tr>
            <crud:row_item><td><crud:value/></td></crud:row_item>
            {if(enableCreate){<td><a crud:view_href="">{S.??("View")}</a></td>}}
            {if(enableEdit){<td><a crud:edit_href="">{S.??("Edit")}</a></td>}}
            {if(enableDelete){<td><a crud:delete_href="">{S.??("Delete")}</a></td>}}
          </tr>
        </crud:row>
      </tbody>
      <tfoot>
        <tr>
          <td colspan="3"><crud:prev>{previousWord}</crud:prev></td>
          <td colspan="3"><crud:next>{nextWord}</crud:next></td>
        </tr>
      </tfoot>
    </table>
  </lift:crud.all>




}


}
}
