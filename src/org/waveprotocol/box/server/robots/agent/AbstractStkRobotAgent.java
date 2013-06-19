package org.waveprotocol.box.server.robots.agent;
import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.appendLine;
import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.lastEnteredLineOf;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.wave.api.Blip;
import com.google.wave.api.BlipContentRefs;
import com.google.wave.api.event.AnnotatedTextChangedEvent;
import com.google.wave.api.event.BlipContributorsChangedEvent;
import com.google.wave.api.event.BlipSubmittedEvent;
import com.google.wave.api.event.DocumentChangedEvent;
import com.google.wave.api.event.Event;
import com.google.wave.api.event.EventHandler;
import com.google.wave.api.event.EventType;
import com.google.wave.api.event.FormButtonClickedEvent;
import com.google.wave.api.event.GadgetStateChangedEvent;
import com.google.wave.api.event.OperationErrorEvent;
import com.google.wave.api.event.WaveletBlipCreatedEvent;
import com.google.wave.api.event.WaveletBlipRemovedEvent;
import com.google.wave.api.event.WaveletCreatedEvent;
import com.google.wave.api.event.WaveletFetchedEvent;
import com.google.wave.api.event.WaveletParticipantsChangedEvent;
import com.google.wave.api.event.WaveletSelfAddedEvent;
import com.google.wave.api.event.WaveletSelfRemovedEvent;
import com.google.wave.api.event.WaveletTagsChangedEvent;
import com.google.wave.api.event.WaveletTitleChangedEvent;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.robots.register.RobotRegistrar;
import org.waveprotocol.box.server.robots.register.RobotRegistrarImpl;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.apache.commons.lang.StringUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.Thread;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Iterator;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

@SuppressWarnings("serial")
public class AbstractStkRobotAgent extends AbstractBaseRobotAgent {

  public static final String ROBOT_URI = AGENT_PREFIX_URI + "/maillist/user";
  private static final Logger LOG = Logger.getLogger(AbstractStkRobotAgent.class.getName());

  public AbstractStkRobotAgent(Injector injector) {
    this(injector.getInstance(Key.get(String.class, Names.named(CoreSettings.WAVE_SERVER_DOMAIN))),
        injector.getInstance(TokenGenerator.class), injector
            .getInstance(ServerFrontendAddressHolder.class), injector
            .getInstance(AccountStore.class), injector.getInstance(RobotRegistrarImpl.class),
        injector.getInstance(Key.get(Boolean.class, Names.named(CoreSettings.ENABLE_SSL))));
      LOG.log(Level.INFO, "Bot created: " + getRobotId());
  }

  AbstractStkRobotAgent(String waveDomain, TokenGenerator tokenGenerator,
      ServerFrontendAddressHolder frontendAddressHolder, AccountStore accountStore,
      RobotRegistrar robotRegistrar, Boolean sslEnabled) {
    super(waveDomain, tokenGenerator, frontendAddressHolder, accountStore, robotRegistrar, sslEnabled);
  }
  private String sendEmail(String subject, String body, String parentMsgId) throws Exception
  {
      try
      {
          Properties props = System.getProperties();
          props.put("mail.smtp.host", "smtp.example.com");
          props.put("mail.smtp.port", "587");
          props.put("mail.smtp.auth", "true");
          props.put("mail.smtp.starttls.enable", "true");

          Session session = Session.getDefaultInstance(props, null);
          MimeMessage msg = new MimeMessage(session);
          if (parentMsgId != null)
          {
              msg.addHeader("In-Reply-To", parentMsgId);
              msg.addHeader("References", parentMsgId);
          }
          msg.addRecipient(Message.RecipientType.TO, new InternetAddress("to_address@example.com"));
          msg.setSubject(subject);
          msg.setContent(body, "text/html");

          Transport transport = session.getTransport("smtp");
          transport.connect("smtp.example.com", "mailbot@example.com", "s3cr3t_p4ssw0rd");
          transport.sendMessage(msg, msg.getAllRecipients());
          transport.close();
          String msgid = msg.getMessageID();
          System.out.println(">>>>> Email title: " + subject + "\nid: " + msgid + "\nre: " + parentMsgId);
          return msgid;
      } catch( Exception e ) {
          System.out.println("Sending an email:" + e);
          throw new Exception("Could not send email.");
      }
  }

    public Map<String, String> query2map(String query)
    {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params)
        {
            if (param.split("=").length > 1)
            {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }
        }
        return map;
    }
    private String getQuery(String contents)
    {
        String result = "";
        Pattern pattern = Pattern.compile("\\[maillist-bot\\?([^]]*)\\]");
        Matcher m = pattern.matcher(contents);
        if (m.find()) {
            result = m.group(1);
        }
        return result;
    }
    private void setQuery(Blip blip, String query)
    {
        String contents = blip.getContent();
        String newQuery = "[maillist-bot?" + query              + "]";

        if (getQuery(contents).equals(""))
        {
            blip.append("\n" + newQuery);
        }
        else
        {
            String oldQuery = "[maillist-bot?" + getQuery(contents) + "]";
            replaceInBlip(blip, oldQuery, newQuery);
        }
    }
    private String map2query(Map<String, String> map)
    {
        String result = "";
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            String pair = pairs.getKey() + "=" + pairs.getValue();
            it.remove();
            if (result != "") result += "&";
            result += pair;
        }
        return result;
    }

  private String blip2title(Blip blip)
  {
      Blip rootBlip = getRootBlip(blip);
      String contents = rootBlip.getContent();
      String oldQuery = "[maillist-bot?" + getQuery(contents) + "]";
      contents = contents.replace(oldQuery, "");
      return contents.trim().split("\n")[0];
  }
  private String blip2body(Blip blip)
  {
      String contents = blip.getContent();
      String oldQuery = "[maillist-bot?" + getQuery(contents) + "]";
      contents = contents.replace(oldQuery, "");

      if (blip.isRoot()) // remote title from body of root blip
      {
          String[] lines = contents.trim().split("\n");
          String[] rootLines = Arrays.copyOfRange(lines, 1, lines.length);
          contents = StringUtils.join(rootLines, "\n");
      }

      return contents.replace("\n", "<br/>");
  }
  private Blip getRootBlip(Blip blip)
  {
      return blip.getWavelet().getRootBlip();
  }
  private String getMsgid(Blip blip)
  {
      if (blip.isRoot()) return null; // TODO bug with continue-thread blips?
      return query2map(getQuery(blip.getContent())).get("msgid");
  }
  private String getParentMsgid(Blip blip)
  {
      Blip parent = blip.getParentBlip();
      if (parent == null) return null;
      return query2map(getQuery(parent.getContent())).get("msgid");
  }
  private void removedBlip(Blip blip)
  {
    try
    {
        String subject = blip2title(blip);
        String body = null;

        String msgid = getMsgid(blip);
        if (msgid != null)
        {
            // cur already has msgid: blip was removed
            body = modifiedBody(blip);
        }

        Map<String, String> params = query2map(getQuery(blip.getContent()));
        String newMsgid = sendEmail(subject, body, msgid);
        params.put("msgid", newMsgid);
        setQuery(blip, map2query(params));
    }
    catch (Exception e)
    {
        LOG.log(Level.SEVERE, "Shit happens: " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
    }
  }
  private void modifiedBlip(Blip blip)
  {
    try
    {
        if (!replaceInBlip(blip, "bot:send\n", "")) return;

        String subject = blip2title(blip);
        String body = null;
        String prevMsgid = getMsgid(blip);
        if (prevMsgid == null)
        {
            body = newBody(blip);
        }
        else
        {
            prevMsgid = getParentMsgid(blip);
            if (prevMsgid == null)
            {
                body = newBody(blip);
            }
            else
            {
                body = modifiedBody(blip);
            }
        }
        Map<String, String> params = query2map(getQuery(blip.getContent()));
        String msgid = sendEmail(subject, body, prevMsgid);
        params.put("msgid", msgid);
        setQuery(blip, map2query(params));
    }
    catch (Exception e)
    {
        appendLine(blip, "[maillist-bot error " + e + "]");
        LOG.log(Level.SEVERE, "Shit happens: " + Thread.currentThread().getStackTrace()[1].getMethodName(), e);
    }
  }
  private boolean replaceInBlip(Blip blip, String from, String to)
  {
      // return false if from is not found
      // return true if from is found
      // replace only happens if from and to are different
      int pos = blip.getContent().indexOf(from);
      if (pos == -1) return false;
      if (from.equals(to)) return true;
      BlipContentRefs first = blip.range(pos, pos+from.length());
      first.replace(to);
      return true;
  }
  private String remvoedBody(Blip blip)
  {
      String waveUri = "wave://" + blip.getWaveId().serialise() + "~/conv+root/" + blip.getBlipId();
      String contributors = StringUtils.join(blip.getContributors(), ", ");
      String creator = blip.getCreator();

      return "<html>"
           + "<b><font color='#AA3F39'>(removed blip)</font></b>"
           + blip2body(blip)
           + "<font size='1' color='#808080'>"
               + "<hr/>Creator: " + creator
               + "<br/>Contributors: " + contributors
               + "<br/>Uri: <a href='" + waveUri + "'>" + waveUri + "</a>"
           + "</font>"
           + "</html>";
  }
  private String modifiedBody(Blip blip)
  {
      String waveUri = "wave://" + blip.getWaveId().serialise() + "~/conv+root/" + blip.getBlipId();
      String contributors = StringUtils.join(blip.getContributors(), ", ");
      String creator = blip.getCreator();

      return ""
           + "<b><font size='1' color='#AA8639'>(modified blip)</font></b>"
           + "<br/>"
           + blip2body(blip)
           + "<hr/>"
           + "<font size='1' color='#808080'>"
               + "<br/>Creator: " + creator
               + "<br/>Contributors: " + contributors
               + "<br/>Uri: <a href='" + waveUri + "'>" + waveUri + "</a>"
           + "</font>";
  }
  private String newBody(Blip blip)
  {
      String waveUri = "wave://" + blip.getWaveId().serialise() + "~/conv+root/" + blip.getBlipId();
      String contributors = StringUtils.join(blip.getContributors(), ", ");
      String creator = blip.getCreator();
      return ""
           + blip2body(blip)
           + "<hr/>"
           + "<font size='1' color='#808080'>"
               + "<b><font color='#789E35'>(new blip)</font></b>"
               + "<br/>Creator: " + creator
               + "<br/>Contributors: " + contributors
               + "<br/>Uri: <a href='" + waveUri + "'>" + waveUri + "</a>"
           + "</font>";
  }
  @Override
  public void onWaveletSelfAdded(WaveletSelfAddedEvent event) {
    LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName());
  }

  @Override
  public void onAnnotatedTextChanged(AnnotatedTextChangedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName());
      modifiedBlip(event.getBlip());
  }
  @Override
  public void onBlipContributorsChanged(BlipContributorsChangedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onBlipSubmitted(BlipSubmittedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onDocumentChanged(DocumentChangedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName());
      //modifiedBlip(event.getBlip());
  }
  @Override
  public void onFormButtonClicked(FormButtonClickedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onGadgetStateChanged(GadgetStateChangedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onWaveletBlipCreated(WaveletBlipCreatedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onWaveletBlipRemoved(WaveletBlipRemovedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName());
      removedBlip(event.getRemovedBlip());
  }
  @Override
  public void onWaveletCreated(WaveletCreatedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onWaveletFetched(WaveletFetchedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onWaveletParticipantsChanged(WaveletParticipantsChangedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onWaveletSelfRemoved(WaveletSelfRemovedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onWaveletTagsChanged(WaveletTagsChangedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onWaveletTitleChanged(WaveletTitleChangedEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }
  @Override
  public void onOperationError(OperationErrorEvent event) {
      LOG.log(Level.INFO, "-------- START " + Thread.currentThread().getStackTrace()[1].getMethodName()); }

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