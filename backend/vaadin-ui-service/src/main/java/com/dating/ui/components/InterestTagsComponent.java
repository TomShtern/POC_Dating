package com.dating.ui.components;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;

import java.util.*;
import java.util.function.Consumer;

/**
 * InterestTagsComponent - Component for selecting/displaying user interests
 * Supports both view-only and editable modes
 */
public class InterestTagsComponent extends Composite<VerticalLayout> {

    private static final List<String> SUGGESTED_INTERESTS = Arrays.asList(
        "Travel", "Music", "Movies", "Fitness", "Reading", "Cooking",
        "Photography", "Gaming", "Art", "Sports", "Hiking", "Dancing",
        "Yoga", "Coffee", "Wine", "Dogs", "Cats", "Beach", "Mountains",
        "Technology", "Fashion", "Food", "Nature", "Concerts", "Museums"
    );

    private final FlexLayout tagsContainer;
    private final FlexLayout suggestionsContainer;
    private final TextField customTagField;

    @Getter
    private final Set<String> selectedInterests = new LinkedHashSet<>();

    private boolean editable = true;
    private int maxTags = 10;
    private Consumer<Set<String>> onChangeCallback;

    public InterestTagsComponent() {
        VerticalLayout layout = getContent();
        layout.setSpacing(true);
        layout.setPadding(false);

        // Selected tags container
        tagsContainer = new FlexLayout();
        tagsContainer.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        tagsContainer.getStyle()
            .set("gap", "8px")
            .set("min-height", "40px");

        // Custom tag input
        customTagField = new TextField();
        customTagField.setPlaceholder("Add custom interest...");
        customTagField.setWidth("200px");
        customTagField.addKeyPressListener(e -> {
            if (e.getKey().toString().equals("Enter")) {
                addTag(customTagField.getValue().trim());
                customTagField.clear();
            }
        });

        Button addButton = new Button(new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addButton.addClickListener(e -> {
            addTag(customTagField.getValue().trim());
            customTagField.clear();
        });

        FlexLayout inputRow = new FlexLayout(customTagField, addButton);
        inputRow.setAlignItems(FlexLayout.Alignment.CENTER);
        inputRow.getStyle().set("gap", "8px");

        // Suggestions
        Span suggestLabel = new Span("Suggestions:");
        suggestLabel.getStyle()
            .set("font-size", "0.85rem")
            .set("color", "#666");

        suggestionsContainer = new FlexLayout();
        suggestionsContainer.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        suggestionsContainer.getStyle().set("gap", "6px");

        // Add suggestion chips
        for (String interest : SUGGESTED_INTERESTS) {
            Button chip = createSuggestionChip(interest);
            suggestionsContainer.add(chip);
        }

        layout.add(tagsContainer, inputRow, suggestLabel, suggestionsContainer);
    }

    private Button createSuggestionChip(String text) {
        Button chip = new Button(text);
        chip.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        chip.getStyle()
            .set("border-radius", "16px")
            .set("font-size", "0.8rem");

        chip.addClickListener(e -> {
            if (editable && !selectedInterests.contains(text)) {
                addTag(text);
            }
        });

        return chip;
    }

    private Div createSelectedTag(String text) {
        Div tag = new Div();
        tag.getStyle()
            .set("display", "inline-flex")
            .set("align-items", "center")
            .set("gap", "4px")
            .set("padding", "4px 12px")
            .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
            .set("color", "white")
            .set("border-radius", "16px")
            .set("font-size", "0.85rem");

        Span textSpan = new Span(text);
        tag.add(textSpan);

        if (editable) {
            Button removeButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
            removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
            removeButton.getStyle()
                .set("color", "white")
                .set("padding", "0")
                .set("min-width", "20px");
            removeButton.addClickListener(e -> removeTag(text));
            tag.add(removeButton);
        }

        return tag;
    }

    public void addTag(String tag) {
        if (tag == null || tag.isEmpty()) return;
        if (selectedInterests.size() >= maxTags) return;
        if (selectedInterests.contains(tag)) return;

        selectedInterests.add(tag);
        refreshTags();

        if (onChangeCallback != null) {
            onChangeCallback.accept(selectedInterests);
        }
    }

    public void removeTag(String tag) {
        selectedInterests.remove(tag);
        refreshTags();

        if (onChangeCallback != null) {
            onChangeCallback.accept(selectedInterests);
        }
    }

    private void refreshTags() {
        tagsContainer.removeAll();
        for (String interest : selectedInterests) {
            tagsContainer.add(createSelectedTag(interest));
        }

        if (selectedInterests.isEmpty()) {
            Span placeholder = new Span("No interests selected");
            placeholder.getStyle()
                .set("color", "#999")
                .set("font-style", "italic");
            tagsContainer.add(placeholder);
        }
    }

    /**
     * Set the selected interests
     */
    public void setInterests(Collection<String> interests) {
        selectedInterests.clear();
        if (interests != null) {
            selectedInterests.addAll(interests);
        }
        refreshTags();
    }

    /**
     * Get selected interests as list
     */
    public List<String> getInterestsAsList() {
        return new ArrayList<>(selectedInterests);
    }

    /**
     * Set whether component is editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        customTagField.setVisible(editable);
        suggestionsContainer.setVisible(editable);
        refreshTags();
    }

    /**
     * Set maximum number of tags
     */
    public void setMaxTags(int max) {
        this.maxTags = max;
    }

    /**
     * Set callback for when interests change
     */
    public void setOnChange(Consumer<Set<String>> callback) {
        this.onChangeCallback = callback;
    }

    /**
     * Clear all selections
     */
    public void clear() {
        selectedInterests.clear();
        refreshTags();
    }
}
