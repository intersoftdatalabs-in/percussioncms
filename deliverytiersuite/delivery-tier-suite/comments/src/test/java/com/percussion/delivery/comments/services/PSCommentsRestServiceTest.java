package com.percussion.delivery.comments.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSCommentIds;
import com.percussion.delivery.comments.data.PSComments;
import com.percussion.delivery.comments.data.PSRestComment;
import com.percussion.delivery.comments.service.rdbms.PSComment;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.InternalServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// REFACTORED: CP-JAVA11
@ExtendWith(MockitoExtension.class)
class PSCommentsRestServiceTest {

  @Mock private IPSCommentsService commentService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private ContainerRequest containerRequest;

  @Mock private HttpHeaders headers;

  @InjectMocks private PSCommentsRestService restService;

  private PSCommentCriteria testCriteria;
  private PSComments testComments;

  @BeforeEach
  void setUp() {
    testCriteria = new PSCommentCriteria();
    testCriteria.setSite("test-site");
    testCriteria.setPagepath("/test-page");

    testComments = new PSComments();
    var comment = new PSRestComment();
    comment.setId("1");
    comment.setUsername("Test User");
    comment.setText("Test comment");
    testComments.getComments().add(comment);
  }

  @Test
  void testCsrf_WithXsrfToken_SetsHeaders() {
    // Given
    var cookies = new Cookie[] {new Cookie("XSRF-TOKEN", "test-token")};
    when(request.getCookies()).thenReturn(cookies);

    // When
    restService.csrf(request, response);

    // Then
    verify(response).setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
    verify(response).setHeader("X-CSRF-TOKEN", "test-token");
  }

  @Test
  void testCsrf_WithoutXsrfToken_DoesNotSetHeaders() {
    // Given
    var cookies = new Cookie[] {new Cookie("OTHER-COOKIE", "value")};
    when(request.getCookies()).thenReturn(cookies);

    // When
    restService.csrf(request, response);

    // Then
    verify(response, never()).setHeader(eq("X-CSRF-HEADER"), anyString());
    verify(response, never()).setHeader(eq("X-CSRF-TOKEN"), anyString());
  }

  @Test
  void testGetComments_WithValidCriteria_ReturnsComments() throws Exception {
    // Given
    when(commentService.getComments(testCriteria, false)).thenReturn(testComments);

    // When
    var result = restService.getComments(testCriteria);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getComments().size());
    verify(commentService).getComments(testCriteria, false);
  }

  @Test
  void testGetComments_WithNullCriteria_ThrowsException() {
    // When & Then
    var exception =
        assertThrows(IllegalArgumentException.class, () -> restService.getComments(null));

    assertEquals("criteria cannot be null.", exception.getMessage());
  }

  @Test
  void testGetCommentsAsModerator_WithValidCriteria_ReturnsComments() throws Exception {
    // Given
    when(commentService.getComments(testCriteria, true)).thenReturn(testComments);

    // When
    var result = restService.getCommentsAsModerator(testCriteria);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getComments().size());
    verify(commentService).getComments(testCriteria, true);
  }

  @Test
  void testAddComment_WithValidData_ReturnsRedirect() throws Exception {
    // Given
    var params = createValidFormParams();
    var form = mock(Form.class);
    when(form.asMap()).thenReturn(params);
    when(containerRequest.getProperty(InternalServerProperties.FORM_DECODED_PROPERTY))
        .thenReturn(form);

    var headerParams = new MultivaluedHashMap<String, String>();
    headerParams.add("Referer", "http://localhost/test-page");
    when(headers.getRequestHeaders()).thenReturn(headerParams);

    var newComment = mock(PSComment.class);
    when(newComment.getId()).thenReturn("123");
    when(commentService.addComment(any(PSRestComment.class))).thenReturn(newComment);

    // When
    var response = restService.addComment(containerRequest, "add", headers);

    // Then
    assertNotNull(response);
    assertEquals(303, response.getStatus()); // See Other redirect
    verify(commentService).addComment(any(PSRestComment.class));
  }

  @Test
  void testAddComment_WithHoneypotFilled_ReturnsRedirectWithBotDetected() throws Exception {
    // Given
    var params = createValidFormParams();
    params.add("url", "http://spam.com"); // Honeypot field filled
    var form = mock(Form.class);
    when(form.asMap()).thenReturn(params);
    when(containerRequest.getProperty(InternalServerProperties.FORM_DECODED_PROPERTY))
        .thenReturn(form);

    var headerParams = new MultivaluedHashMap<String, String>();
    headerParams.add("Referer", "http://localhost/test-page");
    when(headers.getRequestHeaders()).thenReturn(headerParams);

    // When
    var response = restService.addComment(containerRequest, "add", headers);

    // Then
    assertNotNull(response);
    assertEquals(303, response.getStatus());
    verify(commentService, never()).addComment(any(PSRestComment.class));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "<script>alert('xss')</script>",
        "javascript:alert('xss')",
        "onclick=alert('xss')"
      })
  void testAddComment_WithSuspiciousContent_ThrowsException(String suspiciousText) {
    // Given
    var params = createValidFormParams();
    params.putSingle("text", suspiciousText);
    var form = mock(Form.class);
    when(form.asMap()).thenReturn(params);
    when(containerRequest.getProperty(InternalServerProperties.FORM_DECODED_PROPERTY))
        .thenReturn(form);

    var headerParams = new MultivaluedHashMap<String, String>();
    when(headers.getRequestHeaders()).thenReturn(headerParams);

    // When & Then
    assertThrows(
        WebApplicationException.class,
        () -> restService.addComment(containerRequest, "add", headers));
  }

  @Test
  void testAddComment_WithMissingSiteAndPagePath_ThrowsException() {
    // Given
    var params = new MultivaluedHashMap<String, String>();
    params.add("username", "Test User");
    params.add("text", "Test comment");
    // Missing site and pagePath

    var form = mock(Form.class);
    when(form.asMap()).thenReturn(params);
    when(containerRequest.getProperty(InternalServerProperties.FORM_DECODED_PROPERTY))
        .thenReturn(form);

    var headerParams = new MultivaluedHashMap<String, String>();
    when(headers.getRequestHeaders()).thenReturn(headerParams);

    // When & Then
    assertThrows(
        WebApplicationException.class,
        () -> restService.addComment(containerRequest, "add", headers));
  }

  @Test
  void testApprove_WithValidCommentIds_CallsService() {
    // Given
    var commentIds = new PSCommentIds();
    commentIds.setComments(List.of("1", "2", "3"));

    // When
    restService.approve(commentIds);

    // Then
    verify(commentService).approveComments(commentIds.getComments());
  }

  @Test
  void testReject_WithValidCommentIds_CallsService() {
    // Given
    var commentIds = new PSCommentIds();
    commentIds.setComments(List.of("1", "2", "3"));

    // When
    restService.reject(commentIds);

    // Then
    verify(commentService).rejectComments(commentIds.getComments());
  }

  @Test
  void testDelete_WithValidCommentIds_CallsService() {
    // Given
    var commentIds = new PSCommentIds();
    commentIds.setComments(List.of("1", "2", "3"));

    // When
    restService.delete(commentIds);

    // Then
    verify(commentService).deleteComments(commentIds.getComments());
  }

  @Test
  void testGetCommentsP_WithInvalidCallback_ThrowsException() {
    // Given
    testCriteria.setCallback("invalid_callback");

    // When & Then
    assertThrows(WebApplicationException.class, () -> restService.getCommentsP(testCriteria));
  }

  @Test
  void testGetCommentsP_WithValidCallback_ReturnsComments() throws Exception {
    // Given
    testCriteria.setCallback("_jqjsp");
    when(commentService.getComments(testCriteria, false)).thenReturn(testComments);

    // When
    var result = restService.getCommentsP(testCriteria);

    // Then
    assertNotNull(result);
    assertNotNull(result.getEntity());
  }

  private MultivaluedMap<String, String> createValidFormParams() {
    var params = new MultivaluedHashMap<String, String>();
    params.add("username", "Test User");
    params.add("text", "This is a valid test comment");
    params.add("site", "test-site");
    params.add("pagePath", "/test-page");
    params.add("email", "test@example.com");
    return params;
  }
}
