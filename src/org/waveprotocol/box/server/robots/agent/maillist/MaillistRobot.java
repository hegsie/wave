package org.waveprotocol.box.server.robots.agent.maillist;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.waveprotocol.box.server.robots.agent.AbstractStkRobotAgent;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
@Singleton
public final class MaillistRobot extends AbstractStkRobotAgent {

  private static final Logger LOG = Logger.getLogger(MaillistRobot.class.getName());

  @Inject
  public MaillistRobot(Injector injector) {
    super(injector);
      LOG.log(Level.SEVERE, "Failed to register the agent:" + getRobotId());
  }

}
