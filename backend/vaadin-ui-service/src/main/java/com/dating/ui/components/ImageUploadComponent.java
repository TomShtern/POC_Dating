package com.dating.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * ImageUploadComponent - Reusable component for uploading profile photos
 * Supports preview, drag-and-drop, and validation
 */
@Slf4j
public class ImageUploadComponent extends Composite<VerticalLayout> {

    private final MemoryBuffer buffer = new MemoryBuffer();
    private final Upload upload;
    private final Image preview;
    private final Div previewContainer;

    @Getter
    private String imageDataUrl;

    @Getter
    private String fileName;

    private Consumer<String> onUploadComplete;

    public ImageUploadComponent() {
        VerticalLayout layout = getContent();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        // Preview container
        previewContainer = new Div();
        previewContainer.setWidth("200px");
        previewContainer.setHeight("200px");
        previewContainer.getStyle()
            .set("border-radius", "50%")
            .set("overflow", "hidden")
            .set("background", "#f5f5f5")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("border", "3px dashed #ddd");

        // Default placeholder
        Icon placeholder = new Icon(VaadinIcon.USER);
        placeholder.setSize("80px");
        placeholder.getStyle().set("color", "#ccc");
        previewContainer.add(placeholder);

        // Preview image
        preview = new Image();
        preview.setWidth("100%");
        preview.setHeight("100%");
        preview.getStyle().set("object-fit", "cover");
        preview.setVisible(false);

        // Upload component
        upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif", "image/webp");
        upload.setMaxFileSize(5 * 1024 * 1024); // 5MB
        upload.setDropLabel(new Paragraph("Drop image here"));

        Button uploadButton = new Button("Upload Photo");
        uploadButton.setIcon(new Icon(VaadinIcon.UPLOAD));
        uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        upload.setUploadButton(uploadButton);

        // Success handler
        upload.addSucceededListener(event -> {
            try {
                fileName = event.getFileName();
                InputStream inputStream = buffer.getInputStream();
                byte[] bytes = inputStream.readAllBytes();

                // Convert to base64 data URL for preview
                String mimeType = event.getMIMEType();
                String base64 = Base64.getEncoder().encodeToString(bytes);
                imageDataUrl = "data:" + mimeType + ";base64," + base64;

                // Show preview
                showPreview(imageDataUrl);

                Notification.show("Photo uploaded successfully",
                    2000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Callback
                if (onUploadComplete != null) {
                    onUploadComplete.accept(imageDataUrl);
                }

            } catch (Exception ex) {
                log.error("Failed to process uploaded image", ex);
                Notification.show("Failed to process image",
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        // Error handlers
        upload.addFileRejectedListener(event -> {
            Notification.show(event.getErrorMessage(),
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        upload.addFailedListener(event -> {
            log.error("Upload failed", event.getReason());
            Notification.show("Upload failed: " + event.getReason().getMessage(),
                3000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        // Helper text
        Paragraph helperText = new Paragraph("JPEG, PNG, GIF or WebP. Max 5MB.");
        helperText.getStyle()
            .set("font-size", "0.85rem")
            .set("color", "#666")
            .set("margin-top", "0.5rem");

        layout.add(previewContainer, upload, helperText);
    }

    private void showPreview(String imageUrl) {
        previewContainer.removeAll();
        preview.setSrc(imageUrl);
        preview.setVisible(true);
        previewContainer.add(preview);
        previewContainer.getStyle()
            .set("border", "3px solid #667eea");
    }

    /**
     * Set existing image URL for preview
     */
    public void setImageUrl(String url) {
        if (url != null && !url.isEmpty()) {
            this.imageDataUrl = url;
            showPreview(url);
        }
    }

    /**
     * Set callback for when upload completes
     */
    public void setOnUploadComplete(Consumer<String> callback) {
        this.onUploadComplete = callback;
    }

    /**
     * Clear the current image
     */
    public void clear() {
        imageDataUrl = null;
        fileName = null;
        preview.setVisible(false);
        previewContainer.removeAll();

        Icon placeholder = new Icon(VaadinIcon.USER);
        placeholder.setSize("80px");
        placeholder.getStyle().set("color", "#ccc");
        previewContainer.add(placeholder);
        previewContainer.getStyle()
            .set("border", "3px dashed #ddd");
    }

    /**
     * Check if an image has been uploaded
     */
    public boolean hasImage() {
        return imageDataUrl != null && !imageDataUrl.isEmpty();
    }
}
