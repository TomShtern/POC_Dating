package com.dating.ui.views.admin;

import com.dating.ui.dto.admin.AppConfigDTO;
import com.dating.ui.service.admin.AdminConfigService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration management view
 */
@Route(value = "admin/config", layout = AdminLayout.class)
@PageTitle("Configuration | Admin")
@RolesAllowed({"ADMIN"})
public class AdminConfigurationView extends VerticalLayout {

    private final AdminConfigService configService;
    private Grid<AppConfigDTO> configGrid;
    private String selectedCategory = "";
    private final Map<String, Tab> categoryTabs = new HashMap<>();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public AdminConfigurationView(AdminConfigService configService) {
        this.configService = configService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(
                createHeader(),
                createCategoryTabs(),
                createConfigGrid()
        );

        loadConfigurations();
    }

    private H2 createHeader() {
        H2 header = new H2("Application Configuration");
        header.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        return header;
    }

    private Tabs createCategoryTabs() {
        Tabs tabs = new Tabs();

        // Add "All" tab
        Tab allTab = new Tab("All");
        tabs.add(allTab);
        categoryTabs.put("", allTab);

        // Add category tabs
        List<String> categories = configService.getAllCategories();
        for (String category : categories) {
            Tab tab = new Tab(capitalizeCategory(category));
            tabs.add(tab);
            categoryTabs.put(category, tab);
        }

        tabs.addSelectedChangeListener(event -> {
            Tab selected = event.getSelectedTab();
            for (Map.Entry<String, Tab> entry : categoryTabs.entrySet()) {
                if (entry.getValue().equals(selected)) {
                    selectedCategory = entry.getKey();
                    loadConfigurations();
                    break;
                }
            }
        });

        return tabs;
    }

    private VerticalLayout createConfigGrid() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSizeFull();

        configGrid = new Grid<>();
        configGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        configGrid.setSizeFull();

        configGrid.addColumn(AppConfigDTO::getKey)
                .setHeader("Key")
                .setSortable(true)
                .setAutoWidth(true);

        configGrid.addColumn(config -> {
            if (config.isSensitive()) {
                return "********";
            }
            return config.getValue();
        })
                .setHeader("Value")
                .setAutoWidth(true);

        configGrid.addColumn(AppConfigDTO::getValueType)
                .setHeader("Type")
                .setAutoWidth(true);

        configGrid.addColumn(AppConfigDTO::getCategory)
                .setHeader("Category")
                .setSortable(true)
                .setAutoWidth(true);

        configGrid.addColumn(AppConfigDTO::getDescription)
                .setHeader("Description")
                .setAutoWidth(true);

        configGrid.addColumn(config -> config.getUpdatedAt() != null ?
                        config.getUpdatedAt().format(DATE_FORMATTER) : "-")
                .setHeader("Last Updated")
                .setSortable(true)
                .setAutoWidth(true);

        // Actions column
        configGrid.addComponentColumn(this::createActionsColumn)
                .setHeader("Actions")
                .setAutoWidth(true)
                .setFlexGrow(0);

        section.add(configGrid);
        return section;
    }

    private HorizontalLayout createActionsColumn(AppConfigDTO config) {
        Button editButton = new Button(VaadinIcon.EDIT.create(), e -> showEditDialog(config));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        editButton.getElement().setAttribute("title", "Edit");

        return new HorizontalLayout(editButton);
    }

    private void loadConfigurations() {
        List<AppConfigDTO> configs;
        if (selectedCategory.isEmpty()) {
            configs = configService.getAllConfigurations();
        } else {
            configs = configService.getConfigurationsByCategory(selectedCategory);
        }
        configGrid.setItems(configs);
    }

    private void showEditDialog(AppConfigDTO config) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Configuration");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();

        // Key (read-only)
        TextField keyField = new TextField("Key");
        keyField.setValue(config.getKey());
        keyField.setReadOnly(true);
        keyField.setWidthFull();

        // Description
        Span description = new Span(config.getDescription());
        description.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        // Value input based on type
        VerticalLayout valueContainer = new VerticalLayout();
        valueContainer.setPadding(false);
        valueContainer.setSpacing(false);

        switch (config.getValueType()) {
            case "BOOLEAN" -> {
                Checkbox checkbox = new Checkbox("Enabled");
                checkbox.setValue(Boolean.parseBoolean(config.getValue()));
                valueContainer.add(checkbox);
                dialog.getFooter().add(
                        new Button("Cancel", e -> dialog.close()),
                        new Button("Save", e -> {
                            saveConfig(config.getKey(), String.valueOf(checkbox.getValue()));
                            dialog.close();
                        })
                );
            }
            case "INTEGER" -> {
                IntegerField intField = new IntegerField("Value");
                intField.setValue(Integer.parseInt(config.getValue()));
                intField.setWidthFull();
                valueContainer.add(intField);
                dialog.getFooter().add(
                        new Button("Cancel", e -> dialog.close()),
                        createSaveButton(dialog, config.getKey(), () -> String.valueOf(intField.getValue()))
                );
            }
            default -> {
                TextField textField = new TextField("Value");
                textField.setValue(config.getValue());
                textField.setWidthFull();
                if (config.isSensitive()) {
                    textField.setHelperText("This is a sensitive value");
                }
                valueContainer.add(textField);
                dialog.getFooter().add(
                        new Button("Cancel", e -> dialog.close()),
                        createSaveButton(dialog, config.getKey(), textField::getValue)
                );
            }
        }

        form.add(keyField, description, valueContainer);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        dialog.add(form);
        dialog.open();
    }

    private Button createSaveButton(Dialog dialog, String key, java.util.function.Supplier<String> valueSupplier) {
        Button saveButton = new Button("Save", e -> {
            saveConfig(key, valueSupplier.get());
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return saveButton;
    }

    private void saveConfig(String key, String value) {
        try {
            configService.updateConfiguration(key, value);
            showSuccess("Configuration updated successfully");
            loadConfigurations();
        } catch (Exception e) {
            showError("Failed to update configuration: " + e.getMessage());
        }
    }

    private String capitalizeCategory(String category) {
        if (category == null || category.isEmpty()) return category;
        return category.substring(0, 1).toUpperCase() +
                category.substring(1).replace("_", " ");
    }

    private void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
