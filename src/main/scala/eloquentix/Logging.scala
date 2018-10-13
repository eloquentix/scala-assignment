package eloquentix

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory.getLogger

trait Logging {
  protected val log: Logger = Logger(getLogger(getClass.getName))
}
