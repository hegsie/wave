package org.waveprotocol.box.server.robots.agent.maillist;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.waveprotocol.box.server.robots.agent.AbstractStkRobotAgent;

@SuppressWarnings("serial")
@Singleton
public final class MaillistRobot extends AbstractStkRobotAgent {

  @Inject
  public MaillistRobot(Injector injector) {
    super(injector);
  }

}
