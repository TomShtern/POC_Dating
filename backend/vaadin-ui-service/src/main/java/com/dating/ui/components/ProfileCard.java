package com.dating.ui.components;

import com.dating.ui.dto.User;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Reusable profile card component
 * Displays user profile information in a card format
 */
public class ProfileCard extends VerticalLayout {

    private Image profileImage;
    private H2 nameLabel;
    private Paragraph ageLocation;
    private Paragraph bio;
    private VerticalLayout contentLayout;

    public ProfileCard() {
        setSpacing(false);
        setPadding(false);
        addClassName("profile-card");

        getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 16px rgba(0,0,0,0.1)")
            .set("overflow", "hidden")
            .set("background", "white");

        createContent();
    }

    private void createContent() {
        // Profile image
        profileImage = new Image();
        profileImage.setWidth("100%");
        profileImage.setHeight("400px");
        profileImage.getStyle()
            .set("object-fit", "cover");

        // Content section
        contentLayout = new VerticalLayout();
        contentLayout.setPadding(true);
        contentLayout.setSpacing(true);

        nameLabel = new H2();
        nameLabel.getStyle()
            .set("margin", "0")
            .set("color", "var(--lumo-primary-text-color)");

        ageLocation = new Paragraph();
        ageLocation.getStyle()
            .set("margin", "0")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "14px");

        bio = new Paragraph();
        bio.getStyle()
            .set("margin-top", "10px")
            .set("font-size", "14px")
            .set("color", "var(--lumo-body-text-color)");

        contentLayout.add(nameLabel, ageLocation, bio);

        add(profileImage, contentLayout);
    }

    /**
     * Set user data to display
     */
    public void setUser(User user) {
        if (user == null) {
            showNoMoreProfiles();
            return;
        }

        // Set image
        String photoUrl = user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()
            ? user.getPhotoUrl()
            : "https://via.placeholder.com/400x400?text=" + user.getFirstName();
        profileImage.setSrc(photoUrl);
        profileImage.setAlt(user.getFirstName());

        // Set name and age
        nameLabel.setText(user.getFirstName() + ", " + (user.getAge() != null ? user.getAge() : "?"));

        // Set location
        String location = user.getCity() != null && !user.getCity().isEmpty()
            ? user.getCity()
            : "Location unknown";
        ageLocation.setText("📍 " + location);

        // Set bio
        String bioText = user.getBio() != null && !user.getBio().isEmpty()
            ? user.getBio()
            : "No bio available";
        bio.setText(bioText);

        profileImage.setVisible(true);
        contentLayout.setVisible(true);
    }

    /**
     * Show "no more profiles" state
     */
    public void showNoMoreProfiles() {
        profileImage.setVisible(false);
        contentLayout.removeAll();

        VerticalLayout emptyState = new VerticalLayout();
        emptyState.setSizeFull();
        emptyState.setAlignItems(Alignment.CENTER);
        emptyState.setJustifyContentMode(JustifyContentMode.CENTER);

        H2 title = new H2("🎉 You're all caught up!");
        title.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Paragraph text = new Paragraph("Check back later for more profiles");
        text.getStyle()
            .set("color", "var(--lumo-tertiary-text-color)")
            .set("text-align", "center");

        emptyState.add(title, text);
        contentLayout.add(emptyState);
        contentLayout.setVisible(true);
    }
}
