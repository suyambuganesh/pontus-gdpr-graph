package uk.gov.cdp.shadow.user.auth;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.inject.Inject;
import javax.naming.NamingException;
import org.hamcrest.core.IsInstanceOf;
import org.jukito.JukitoRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.cdp.ldap.LdapService;
import uk.gov.cdp.shadow.user.auth.exception.AuthenticationFailureException;
import uk.gov.cdp.shadow.user.auth.exception.LdapServiceException;
import uk.gov.cdp.shadow.user.auth.exception.PasswordGenerationFailedException;

@RunWith(JukitoRunner.class)
public class AuthenticationServiceImplTest {

  @Inject private AuthenticationServiceImpl authenticationService;

  @Mock private Key key;

  @Rule public ExpectedException expectedException = ExpectedException.none();

  private String subject = "UID-1234";
  private String userName = "Deepesh";
  private String bizContext = "/org/bu/role";

  @Test
  public void userAuthenticatedWithGeneratedPassword_WhenUserExists(
      LdapService ldapService, CDPShadowUserPasswordGenerator cdpShadowUserPasswordGenerator) {

    String password = "password";
    when(cdpShadowUserPasswordGenerator.generate(any(Key.class), eq("HmacSHA512"), eq(subject)))
        .thenReturn(password);

    when(ldapService.userExist(userName)).thenReturn(true);

    authenticationService.authenticate(userName, subject, bizContext);

    verify(ldapService).login(userName, password);
    verify(ldapService, never()).createUserAccount(userName, password);
  }

  @Test
  public void userCreatedWithGeneratedPassword_WhenUserDoesntExist(
      LdapService ldapService, CDPShadowUserPasswordGenerator cdpShadowUserPasswordGenerator) {

    String password = "password";
    when(cdpShadowUserPasswordGenerator.generate(any(Key.class), eq("HmacSHA512"), eq(subject)))
        .thenReturn(password);

    when(ldapService.userExist(userName)).thenReturn(false);

    authenticationService.authenticate(userName, subject, bizContext);

    verify(ldapService).login(userName, password);
    verify(ldapService).createUserAccount(userName, password);
  }

  @Test
  public void throwAuthenticationFailureException_WhenPasswordGenerationFailed(
      CDPShadowUserPasswordGenerator cdpShadowUserPasswordGenerator) {
    doThrow(new PasswordGenerationFailedException(new NoSuchAlgorithmException()))
        .when(cdpShadowUserPasswordGenerator)
        .generate(any(), any(), any());
    expectedException.expect(AuthenticationFailureException.class);
    expectedException.expectCause(IsInstanceOf.instanceOf(PasswordGenerationFailedException.class));

    authenticationService.authenticate(userName, subject, bizContext);
  }

  @Test
  public void throwAuthenticationFailureException_WhenLdapExceptionOccur_WhileCreatingUser(
      LdapService ldapService, CDPShadowUserPasswordGenerator cdpShadowUserPasswordGenerator) {

    String password = "password";
    when(cdpShadowUserPasswordGenerator.generate(any(Key.class), eq("HmacSHA512"), eq(subject)))
        .thenReturn(password);

    doThrow(new LdapServiceException(new NamingException()))
        .when(ldapService)
        .createUserAccount(userName, password);

    when(ldapService.userExist(userName)).thenReturn(false);

    expectedException.expect(AuthenticationFailureException.class);
    expectedException.expectCause(IsInstanceOf.instanceOf(LdapServiceException.class));

    authenticationService.authenticate(userName, subject, bizContext);
  }

  @Test
  public void throwAuthenticationFailureException_WhenLdapExceptionOccurWhileLoggingIn(
      LdapService ldapService, CDPShadowUserPasswordGenerator cdpShadowUserPasswordGenerator) {
    String password = "password";
    when(cdpShadowUserPasswordGenerator.generate(any(Key.class), eq("HmacSHA512"), eq(subject)))
        .thenReturn(password);

    when(ldapService.userExist(userName)).thenReturn(true);
    doThrow(new LdapServiceException(new NamingException()))
        .when(ldapService)
        .login(userName, password);
    expectedException.expect(AuthenticationFailureException.class);
    expectedException.expectCause(IsInstanceOf.instanceOf(LdapServiceException.class));

    authenticationService.authenticate(userName, subject, bizContext);
  }
}