package org.waveprotocol.box.server.robots.agent;
import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.appendLine;
import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.lastEnteredLineOf;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.wave.api.Blip;
import com.google.wave.api.event.DocumentChangedEvent;
import com.google.wave.api.event.WaveletSelfAddedEvent;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.robots.register.RobotRegistrar;
import org.waveprotocol.box.server.robots.register.RobotRegistrarImpl;
import org.waveprotocol.wave.model.id.TokenGenerator;

@SuppressWarnings("serial")
public class AbstractStkRobotAgent extends AbstractBaseRobotAgent {

  public static final String ROBOT_URI = AGENT_PREFIX_URI + "/maillist/user";

  public AbstractStkRobotAgent(Injector injector) {
    this(injector.getInstance(Key.get(String.class, Names.named(CoreSettings.WAVE_SERVER_DOMAIN))),
        injector.getInstance(TokenGenerator.class), injector
            .getInstance(ServerFrontendAddressHolder.class), injector
            .getInstance(AccountStore.class), injector.getInstance(RobotRegistrarImpl.class),
        injector.getInstance(Key.get(Boolean.class, Names.named(CoreSettings.ENABLE_SSL))));
  }

  AbstractStkRobotAgent(String waveDomain, TokenGenerator tokenGenerator,
      ServerFrontendAddressHolder frontendAddressHolder, AccountStore accountStore,
      RobotRegistrar robotRegistrar, Boolean sslEnabled) {
    super(waveDomain, tokenGenerator, frontendAddressHolder, accountStore, robotRegistrar, sslEnabled);
  }

  @Override
  public void onWaveletSelfAdded(WaveletSelfAddedEvent event) {
    Blip blip = event.getBlip();
    String robotAddress = event.getWavelet().getRobotAddress();
    appendLine(blip, "Added Maillist-bot to this wave: " + blip.getContent());
  }

  @Override
  public void onDocumentChanged(DocumentChangedEvent event) {
    Blip blip = event.getBlip();
    String modifiedBy = event.getModifiedBy();
    appendLine(blip, "Adding a new line to the blip after: " + blip.getContent());
  }

  @Override
  protected String getRobotProfilePageUrl() {
    return null;
  }
  @Override
  public String getRobotId() {
    return "maillist-bot";
  }
  @Override
  public String getRobotName() {
    return "Maillist-Bot";
  }

  @Override
  public String getRobotUri() {
    return ROBOT_URI;
  }
}
