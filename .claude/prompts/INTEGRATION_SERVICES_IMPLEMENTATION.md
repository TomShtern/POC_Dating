# Integration Services Templates Implementation Prompt

## Context
You are implementing **template-based integration services** for a POC Dating application. These are starter templates that can be easily swapped for production services later. You have **full internet access** to research email APIs, SMS providers, and cloud storage.

**Scale:** 100-10K users (small scale - use simple/free tiers initially)

**Philosophy:** Create interfaces with simple local implementations first. Production implementations (SendGrid, Twilio, S3) can be swapped in later without changing business logic.

## ⚠️ CRITICAL: Code Quality Requirements

**WRITE MODULAR, SWAPPABLE, WELL-DOCUMENTED TEMPLATES.**

This is non-negotiable. Every integration must be:
- **INTERFACE-FIRST** - Define contract, then implement
- **SWAPPABLE** - Switch providers with one config change
- **DOCUMENTED** - Clear instructions for implementing real providers
- **TESTABLE** - Mock implementations for testing
- **FAIL-SAFE** - Graceful degradation when service unavailable

**Template Structure:**
```java
// ✅ GOOD: Interface + multiple implementations
public interface EmailService {
    void sendEmail(EmailRequest request);
}

@Service
@Profile("dev")
@ConditionalOnProperty(name = "integration.email.provider", havingValue = "console")
public class ConsoleEmailService implements EmailService {
    // Logs to console - for development
}

@Service
@Profile("prod")
@ConditionalOnProperty(name = "integration.email.provider", havingValue = "sendgrid")
public class SendGridEmailService implements EmailService {
    // Real SendGrid implementation
}

// ❌ BAD: Hardcoded to one provider
@Service
public class EmailService {
    private final SendGrid sendGrid;  // Can't swap providers
}
```

**Why This Matters:** You'll switch from development (console/local) to production (SendGrid/Twilio/S3) without rewriting business logic.

## Scope
Create these integration services:

1. **Email Service** - Notifications, verification emails
2. **SMS Service** - Phone verification (optional)
3. **File Storage Service** - Profile photos
4. **Notification Service** - Orchestrates email/SMS/push

## Implementation Tasks

### Task 1: Email Service

**Interface:**
```java
/**
 * ============================================================================
 * EMAIL SERVICE INTERFACE
 * ============================================================================
 *
 * PURPOSE:
 * Send transactional emails (verification, notifications, etc.)
 *
 * IMPLEMENTATIONS:
 * - ConsoleEmailService: Logs to console (development)
 * - SmtpEmailService: Uses SMTP server (staging)
 * - SendGridEmailService: Uses SendGrid API (production)
 *
 * TO SWITCH PROVIDER:
 * Set 'integration.email.provider' in application.yml
 *
 * ============================================================================
 */
public interface EmailService {

    /**
     * Send a single email.
     *
     * @param request Email details (to, subject, body, template)
     * @throws EmailSendException if sending fails
     */
    void sendEmail(EmailRequest request);

    /**
     * Send a templated email.
     *
     * @param templateName Name of the template (e.g., "verification", "match-notification")
     * @param to Recipient email
     * @param variables Template variables
     */
    void sendTemplatedEmail(String templateName, String to, Map<String, Object> variables);
}

public record EmailRequest(
    String to,
    String subject,
    String body,
    String htmlBody,  // Optional HTML version
    Map<String, String> headers
) {
    public EmailRequest(String to, String subject, String body) {
        this(to, subject, body, null, Map.of());
    }
}
```

**Console Implementation (Development):**
```java
/**
 * Development email service - logs emails to console.
 * Use this for local development to see what emails would be sent.
 */
@Service
@ConditionalOnProperty(name = "integration.email.provider", havingValue = "console", matchIfMissing = true)
@Slf4j
public class ConsoleEmailService implements EmailService {

    @Override
    public void sendEmail(EmailRequest request) {
        log.info("========== EMAIL SENT ==========");
        log.info("To: {}", request.to());
        log.info("Subject: {}", request.subject());
        log.info("Body: {}", request.body());
        log.info("================================");
    }

    @Override
    public void sendTemplatedEmail(String templateName, String to, Map<String, Object> variables) {
        log.info("========== TEMPLATED EMAIL ==========");
        log.info("Template: {}", templateName);
        log.info("To: {}", to);
        log.info("Variables: {}", variables);
        log.info("=====================================");
    }
}
```

**SMTP Implementation (Staging):**
```java
/**
 * SMTP email service - uses Spring's JavaMailSender.
 * Configure SMTP server in application.yml.
 */
@Service
@ConditionalOnProperty(name = "integration.email.provider", havingValue = "smtp")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${integration.email.from}")
    private String fromAddress;

    @Override
    public void sendEmail(EmailRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(request.to());
            message.setSubject(request.subject());
            message.setText(request.body());

            mailSender.send(message);
            log.info("Email sent to: {}", request.to());
        } catch (Exception e) {
            log.error("Failed to send email to: {}", request.to(), e);
            throw new EmailSendException("Failed to send email", e);
        }
    }

    @Override
    public void sendTemplatedEmail(String templateName, String to, Map<String, Object> variables) {
        // Load template, replace variables, send
        String body = loadAndProcessTemplate(templateName, variables);
        sendEmail(new EmailRequest(to, getSubjectForTemplate(templateName), body));
    }
}
```

**SendGrid Implementation (Production):**
```java
/**
 * SendGrid email service for production.
 *
 * SETUP:
 * 1. Create SendGrid account
 * 2. Get API key
 * 3. Set SENDGRID_API_KEY environment variable
 * 4. Set integration.email.provider=sendgrid
 *
 * DOCUMENTATION: https://docs.sendgrid.com/for-developers/sending-email/quickstart-java
 */
@Service
@ConditionalOnProperty(name = "integration.email.provider", havingValue = "sendgrid")
@Slf4j
public class SendGridEmailService implements EmailService {

    private final SendGrid sendGrid;

    @Value("${integration.email.from}")
    private String fromAddress;

    public SendGridEmailService(@Value("${SENDGRID_API_KEY}") String apiKey) {
        this.sendGrid = new SendGrid(apiKey);
    }

    @Override
    public void sendEmail(EmailRequest request) {
        Email from = new Email(fromAddress);
        Email to = new Email(request.to());
        Content content = new Content("text/plain", request.body());
        Mail mail = new Mail(from, request.subject(), to, content);

        try {
            Request sendGridRequest = new Request();
            sendGridRequest.setMethod(Method.POST);
            sendGridRequest.setEndpoint("mail/send");
            sendGridRequest.setBody(mail.build());

            Response response = sendGrid.api(sendGridRequest);

            if (response.getStatusCode() >= 400) {
                throw new EmailSendException("SendGrid error: " + response.getBody());
            }

            log.info("Email sent via SendGrid to: {}", request.to());
        } catch (IOException e) {
            log.error("SendGrid error", e);
            throw new EmailSendException("Failed to send email", e);
        }
    }
}
```

### Task 2: SMS Service

**Interface:**
```java
/**
 * ============================================================================
 * SMS SERVICE INTERFACE
 * ============================================================================
 *
 * PURPOSE:
 * Send SMS messages (phone verification, alerts)
 *
 * IMPLEMENTATIONS:
 * - ConsoleSmsService: Logs to console (development)
 * - TwilioSmsService: Uses Twilio API (production)
 *
 * ============================================================================
 */
public interface SmsService {

    /**
     * Send an SMS message.
     *
     * @param phoneNumber Recipient phone (E.164 format: +1234567890)
     * @param message Message text (max 160 chars for single SMS)
     */
    void sendSms(String phoneNumber, String message);
}
```

**Console Implementation:**
```java
@Service
@ConditionalOnProperty(name = "integration.sms.provider", havingValue = "console", matchIfMissing = true)
@Slf4j
public class ConsoleSmsService implements SmsService {

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("========== SMS SENT ==========");
        log.info("To: {}", phoneNumber);
        log.info("Message: {}", message);
        log.info("==============================");
    }
}
```

**Twilio Implementation:**
```java
/**
 * Twilio SMS service for production.
 *
 * SETUP:
 * 1. Create Twilio account
 * 2. Get Account SID and Auth Token
 * 3. Get a phone number
 * 4. Set environment variables: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER
 * 5. Set integration.sms.provider=twilio
 *
 * DOCUMENTATION: https://www.twilio.com/docs/sms/quickstart/java
 */
@Service
@ConditionalOnProperty(name = "integration.sms.provider", havingValue = "twilio")
@Slf4j
public class TwilioSmsService implements SmsService {

    @Value("${TWILIO_PHONE_NUMBER}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        String authToken = System.getenv("TWILIO_AUTH_TOKEN");
        Twilio.init(accountSid, authToken);
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        try {
            Message twilioMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(fromNumber),
                message
            ).create();

            log.info("SMS sent via Twilio to: {}, SID: {}",
                phoneNumber, twilioMessage.getSid());
        } catch (Exception e) {
            log.error("Twilio error", e);
            throw new SmsSendException("Failed to send SMS", e);
        }
    }
}
```

### Task 3: File Storage Service

**Interface:**
```java
/**
 * ============================================================================
 * FILE STORAGE SERVICE INTERFACE
 * ============================================================================
 *
 * PURPOSE:
 * Store and retrieve files (profile photos, documents)
 *
 * IMPLEMENTATIONS:
 * - LocalFileStorageService: Stores on local disk (development)
 * - S3FileStorageService: Uses AWS S3 (production)
 *
 * ============================================================================
 */
public interface FileStorageService {

    /**
     * Store a file.
     *
     * @param fileName Original filename
     * @param content File content
     * @param contentType MIME type (e.g., "image/jpeg")
     * @return URL or path to access the file
     */
    String store(String fileName, byte[] content, String contentType);

    /**
     * Retrieve a file.
     *
     * @param fileId File identifier (returned from store)
     * @return File content
     */
    byte[] retrieve(String fileId);

    /**
     * Delete a file.
     *
     * @param fileId File identifier
     */
    void delete(String fileId);

    /**
     * Get a public URL for the file.
     *
     * @param fileId File identifier
     * @return Public URL (for local, returns path; for S3, returns signed URL)
     */
    String getUrl(String fileId);
}
```

**Local Implementation:**
```java
/**
 * Local file storage for development.
 * Stores files in a local directory.
 */
@Service
@ConditionalOnProperty(name = "integration.storage.provider", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${integration.storage.local.path:./uploads}")
    private String storagePath;

    @Value("${integration.storage.local.base-url:http://localhost:8081/files}")
    private String baseUrl;

    @PostConstruct
    public void init() throws IOException {
        Path path = Paths.get(storagePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Created storage directory: {}", storagePath);
        }
    }

    @Override
    public String store(String fileName, byte[] content, String contentType) {
        String fileId = UUID.randomUUID() + "_" + fileName;
        Path filePath = Paths.get(storagePath, fileId);

        try {
            Files.write(filePath, content);
            log.info("File stored locally: {}", fileId);
            return fileId;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file", e);
        }
    }

    @Override
    public byte[] retrieve(String fileId) {
        Path filePath = Paths.get(storagePath, fileId);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to retrieve file", e);
        }
    }

    @Override
    public void delete(String fileId) {
        Path filePath = Paths.get(storagePath, fileId);
        try {
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", fileId);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file", e);
        }
    }

    @Override
    public String getUrl(String fileId) {
        return baseUrl + "/" + fileId;
    }
}
```

**S3 Implementation:**
```java
/**
 * AWS S3 file storage for production.
 *
 * SETUP:
 * 1. Create AWS account and S3 bucket
 * 2. Create IAM user with S3 access
 * 3. Set environment variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
 * 4. Set integration.storage.provider=s3
 *
 * DOCUMENTATION: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_s3_code_examples.html
 */
@Service
@ConditionalOnProperty(name = "integration.storage.provider", havingValue = "s3")
@Slf4j
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;

    @Value("${integration.storage.s3.bucket}")
    private String bucketName;

    @Value("${integration.storage.s3.region}")
    private String region;

    public S3FileStorageService() {
        this.s3Client = S3Client.builder()
            .region(Region.of(region))
            .build();
    }

    @Override
    public String store(String fileName, byte[] content, String contentType) {
        String key = UUID.randomUUID() + "/" + fileName;

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        log.info("File stored in S3: {}", key);

        return key;
    }

    @Override
    public byte[] retrieve(String fileId) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(fileId)
            .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    @Override
    public void delete(String fileId) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(fileId)
            .build();

        s3Client.deleteObject(request);
        log.info("File deleted from S3: {}", fileId);
    }

    @Override
    public String getUrl(String fileId) {
        // Generate pre-signed URL for temporary access
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
            bucketName, region, fileId);
    }
}
```

### Task 4: Configuration

**application.yml:**
```yaml
# ==============================================================================
# INTEGRATION SERVICES CONFIGURATION
# ==============================================================================

integration:
  # Email configuration
  email:
    provider: console  # Options: console, smtp, sendgrid
    from: noreply@poc-dating.com

  # SMTP settings (when provider=smtp)
  # spring.mail.host: smtp.gmail.com
  # spring.mail.port: 587
  # spring.mail.username: ${SMTP_USERNAME}
  # spring.mail.password: ${SMTP_PASSWORD}

  # SMS configuration
  sms:
    provider: console  # Options: console, twilio

  # File storage configuration
  storage:
    provider: local  # Options: local, s3
    local:
      path: ./uploads
      base-url: http://localhost:8081/files
    s3:
      bucket: poc-dating-uploads
      region: us-east-1
```

### Task 5: Notification Service (Orchestrator)

```java
/**
 * High-level notification service that decides which channel to use.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailService emailService;
    private final SmsService smsService;

    /**
     * Send a notification through the appropriate channel.
     */
    public void sendNotification(User user, NotificationType type, Map<String, Object> data) {
        switch (type) {
            case NEW_MATCH -> {
                emailService.sendTemplatedEmail("new-match", user.getEmail(), data);
                if (user.isPhoneVerified() && user.isSmsNotificationsEnabled()) {
                    smsService.sendSms(user.getPhoneNumber(),
                        "You have a new match! Open the app to see who.");
                }
            }
            case NEW_MESSAGE -> {
                emailService.sendTemplatedEmail("new-message", user.getEmail(), data);
            }
            case VERIFICATION_CODE -> {
                String code = (String) data.get("code");
                if (data.containsKey("phone")) {
                    smsService.sendSms((String) data.get("phone"),
                        "Your verification code is: " + code);
                } else {
                    emailService.sendTemplatedEmail("verification", user.getEmail(), data);
                }
            }
        }
    }
}

public enum NotificationType {
    NEW_MATCH,
    NEW_MESSAGE,
    VERIFICATION_CODE,
    PROFILE_LIKED,
    SUBSCRIPTION_EXPIRING
}
```

## Iteration Loop (Repeat Until Complete)

### Phase 1: Create Interfaces
```bash
cd backend/common-lib  # Or user-service
mvn compile
```

### Phase 2: Implement Console Versions
```bash
# Test console implementations locally
mvn spring-boot:run
# Trigger emails/SMS and check console output
```

### Phase 3: Test Provider Switching
```yaml
# Change provider in application.yml
integration:
  email:
    provider: smtp  # Switch from console to smtp
```
- Verify the correct implementation is used

### Phase 4: Add Production Implementations
- Implement SendGrid, Twilio, S3
- Test with real credentials
- Verify graceful error handling

## Success Criteria
- [ ] All services have interfaces defined
- [ ] Console implementations work for development
- [ ] Provider switching works via configuration
- [ ] Production implementations are templated with setup instructions
- [ ] Error handling is graceful (no crashes)
- [ ] All implementations are documented
- [ ] Configuration is centralized in application.yml

## When Stuck
1. **Search internet** for SendGrid/Twilio/S3 Java SDK examples
2. **Check:** provider documentation for API changes
3. **Test:** with provider's test/sandbox mode first

## DO NOT
- Hardcode API keys (use environment variables)
- Skip error handling (services may be unavailable)
- Forget to log important events
- Mix business logic with integration code
- Use production providers in development

## Future Enhancements (Not Now)
- Push notifications (Firebase)
- Email templates with HTML
- File CDN integration
- Webhook handlers

---
**Iterate until all services can be switched via configuration. Use internet access freely to research provider APIs.**
