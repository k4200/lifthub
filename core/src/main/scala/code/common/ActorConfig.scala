package net.lifthub {
package common {

import net.liftweb.common._
import net.liftweb.util._
import Helpers._

case class ActorConfig(host: String, port: Int, name: String) {
}

object ActorConfig {
  def apply(confName: String): Box[ActorConfig] = {
    (for(host <- Props.get("server." + confName + ".host");
	portstr <- Props.get("server." + confName + ".port");
	port = Integer.parseInt(portstr);
	name <- Props.get("server." + confName + ".name"))
    yield {
      Full(ActorConfig(host, port, name))
    }) getOrElse {
      Failure("couldn't read actor config " + confName)
    }
  }
}


}
}
