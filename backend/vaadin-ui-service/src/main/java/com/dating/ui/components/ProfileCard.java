package com.dating.ui.components;

import com.dating.ui.dto.User;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

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
    private Div loadingOverlay;
    private OnlineStatusBadge onlineStatus;
    private VerificationBadge verificationBadge;
    private DistanceBadge distanceBadge;
    private FlexLayout interestsLayout;

    public ProfileCard() {
        setSpacing(false);
        setPadding(false);
        addClassName("profile-card");

        getStyle()
            .set("border", "1px solid var(--lumo-contrast-10pct)")
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 16px rgba(0,0,0,0.1)")
            .set("overflow", "hidden")
            .set("background", "white")
            .set("position", "relative");

        createContent();
        createLoadingOverlay();
    }

    private void createLoadingOverlay() {
        loadingOverlay = new Div();
        loadingOverlay.getStyle()
            .set("position", "absolute")
            .set("top", "0")
            .set("left", "0")
            .set("right", "0")
            .set("bottom", "0")
            .set("background", "rgba(255, 255, 255, 0.9)")
            .set("display", "none")
            .set("justify-content", "center")
            .set("align-items", "center")
            .set("z-index", "10");

        Div spinner = new Div();
        spinner.getStyle()
            .set("width", "40px")
            .set("height", "40px")
            .set("border", "4px solid #f3f3f3")
            .set("border-top", "4px solid #667eea")
            .set("border-radius", "50%")
            .set("animation", "spin 1s linear infinite");

        loadingOverlay.add(spinner);
        add(loadingOverlay);
    }

    /**
     * Show loading state
     */
    public void setLoading(boolean loading) {
        loadingOverlay.getStyle().set("display", loading ? "flex" : "none");
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

        // Name with verification badge
        HorizontalLayout nameRow = new HorizontalLayout();
        nameRow.setAlignItems(Alignment.CENTER);
        nameRow.setSpacing(true);

        nameLabel = new H2();
        nameLabel.getStyle()
            .set("margin", "0")
            .set("color", "var(--lumo-primary-text-color)");

        verificationBadge = new VerificationBadge();
        verificationBadge.setCompact(true);

        nameRow.add(nameLabel, verificationBadge);

        // Online status
        onlineStatus = new OnlineStatusBadge();

        // Location with distance
        HorizontalLayout locationRow = new HorizontalLayout();
        locationRow.setAlignItems(Alignment.CENTER);
        locationRow.setSpacing(true);

        ageLocation = new Paragraph();
        ageLocation.getStyle()
            .set("margin", "0")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "14px");

        distanceBadge = new DistanceBadge();

        locationRow.add(ageLocation, distanceBadge);

        // Bio
        bio = new Paragraph();
        bio.getStyle()
            .set("margin-top", "10px")
            .set("font-size", "14px")
            .set("color", "var(--lumo-body-text-color)");

        // Interests
        interestsLayout = new FlexLayout();
        interestsLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        interestsLayout.getStyle()
            .set("gap", "4px")
            .set("margin-top", "8px");

        contentLayout.add(nameRow, onlineStatus, locationRow, bio, interestsLayout);

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

        // Set verification badge
        verificationBadge.setVerified(user.getIsVerified());

        // Set online status
        onlineStatus.setStatus(user.getIsOnline(), user.getLastActiveAt());

        // Set location
        String location = user.getCity() != null && !user.getCity().isEmpty()
            ? user.getCity()
            : "Location unknown";
        ageLocation.setText(location);

        // Set bio
        String bioText = user.getBio() != null && !user.getBio().isEmpty()
            ? user.getBio()
            : "No bio available";
        bio.setText(bioText);

        // Set interests
        interestsLayout.removeAll();
        List<String> interests = user.getInterests();
        if (interests != null && !interests.isEmpty()) {
            for (String interest : interests) {
                Span tag = new Span(interest);
                tag.addClassName("interest-tag");
                interestsLayout.add(tag);
            }
        }

        profileImage.setVisible(true);
        contentLayout.setVisible(true);
    }

    /**
     * Set distance to user
     */
    public void setDistance(Integer distanceKm) {
        distanceBadge.setDistanceKm(distanceKm);
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

        H2 title = new H2("You're all caught up!");
        title.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Paragraph text = new Paragraph("Check back later for more profiles");
        text.getStyle()
            .set("color", "var(--lumo-tertiary-text-color)")
            .set("text-align", "center");

        emptyState.add(title, text);
        contentLayout.add(emptyState);
        contentLayout.setVisible(true);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        // Clean up sub-component references to prevent memory leaks
        if (profileImage != null) {
            profileImage.setSrc("");
        }

        if (interestsLayout != null) {
            interestsLayout.removeAll();
        }

        if (contentLayout != null) {
            contentLayout.removeAll();
        }

        // Nullify component references
        profileImage = null;
        nameLabel = null;
        ageLocation = null;
        bio = null;
        contentLayout = null;
        loadingOverlay = null;
        onlineStatus = null;
        verificationBadge = null;
        distanceBadge = null;
        interestsLayout = null;
    }
}
